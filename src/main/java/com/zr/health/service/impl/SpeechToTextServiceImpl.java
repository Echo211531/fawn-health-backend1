package com.zr.health.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zr.health.exception.BusinessException;
import com.zr.health.exception.ErrorCode;
import com.zr.health.model.vo.ai.SpeechTranscriptionVO;
import com.zr.health.service.SpeechToTextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 语音转文字服务实现类
 * <p>
 * 基于讯飞实时语音转写大模型WebSocket接口实现：
 * 1. 按照文档规则构造鉴权参数和signature签名
 * 2. 通过JDK自带HttpClient WebSocket客户端建立长连接
 * 3. 将PCM音频按固定大小切片后以Binary消息形式推送
 * 4. 解析服务端返回的JSON结果，提取最终转写文本
 */
@Slf4j
@Service
public class SpeechToTextServiceImpl implements SpeechToTextService {

    /**
     * 讯飞应用ID（来自控制台）
     */
    @Value("${iflytek.asr.app-id}")
    private String appId;

    /**
     * 讯飞接入key（accessKeyId）
     */
    @Value("${iflytek.asr.access-key-id}")
    private String accessKeyId;

    /**
     * 讯飞接入密钥（accessKeySecret），用于生成HmacSHA1签名
     */
    @Value("${iflytek.asr.access-key-secret}")
    private String accessKeySecret;

    /**
     * WebSocket基础地址
     * 默认值为官方文档中的wss地址
     */
    @Value("${iflytek.asr.base-url:wss://office-api-ast-dx.iflyaisol.com/ast/communicate/v1}")
    private String baseUrl;

    /**
     * 语言类型，默认自动识别中英+方言
     */
    @Value("${iflytek.asr.lang:autodialect}")
    private String lang;

    /**
     * 音频编码格式，默认pcm_s16le（16k 16bit 单声道PCM）
     */
    @Value("${iflytek.asr.audio-encode:pcm_s16le}")
    private String audioEncode;

    /**
     * 采样率，默认16000
     */
    @Value("${iflytek.asr.samplerate:16000}")
    private long samplerate;

    /**
     * 语音数据切片大小（字节）
     * 文档建议每40ms发送1280字节
     */
    private static final int AUDIO_CHUNK_SIZE = 1280;

    /**
     * WebSocket最长等待时长（秒），用于控制整体转写超时
     */
    private static final long MAX_WAIT_SECONDS = 60;

    /**
     * Jackson对象映射器，用于解析返回的JSON结果
     */
    private final ObjectMapper objectMapper;

    public SpeechToTextServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将上传的PCM音频文件转写为文本
     *
     * @param audioFile 前端上传的PCM音频文件
     * @param userUuid  业务用户唯一标识，可为空
     * @return 语音转文字结果
     */
    @Override
    public SpeechTranscriptionVO transcribePcmAudio(MultipartFile audioFile, String userUuid) {
        // 1. 基础参数校验，避免NPE及无效调用
        if (audioFile == null || audioFile.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "音频文件不能为空");
        }
        if (audioFile.getSize() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "音频文件大小必须大于0");
        }

        // 2. 构造WebSocket URL（包含鉴权参数和signature）
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        String uuid = Optional.ofNullable(userUuid)
                .filter(s -> !s.isBlank())
                .orElse(sessionId);

        try {
            String wsUrl = buildSignedWebSocketUrl(uuid);
            log.info("准备连接讯飞实时转写服务, url={}", wsUrl);

            // 3. 构造HttpClient和WebSocket监听器
            HttpClient client = HttpClient.newHttpClient();
            StringBuilder textResultBuilder = new StringBuilder();
            StringBuilder rawResultBuilder = new StringBuilder();
            CountDownLatch finishLatch = new CountDownLatch(1);
            List<String> errorHolder = Collections.synchronizedList(new ArrayList<>());

            WebSocket.Listener listener = createWebSocketListener(textResultBuilder, rawResultBuilder, finishLatch, errorHolder);

            // 4. 建立WebSocket连接
            WebSocket webSocket = client.newWebSocketBuilder()
                    .buildAsync(URI.create(wsUrl), listener)
                    .join();

            // 5. 发送音频数据（二进制），严格按照文档建议分片发送
            sendAudioStream(webSocket, audioFile);

            // 6. 发送结束标识，通知服务端音频已经发送完成
            sendEndSignal(webSocket, sessionId);

            // 7. 等待转写完成或超时
            boolean completed = finishLatch.await(MAX_WAIT_SECONDS, TimeUnit.SECONDS);
            // 主动关闭WebSocket
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "finished").join();

            if (!errorHolder.isEmpty()) {
                log.error("调用讯飞实时转写服务发生错误: {}", errorHolder.get(0));
                throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE,
                        "调用语音转写服务失败：" + errorHolder.get(0));
            }
            if (!completed) {
                log.error("语音转写超时, sessionId={}", sessionId);
                throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "语音转写超时，请稍后重试");
            }

            String finalText = textResultBuilder.toString();
            if (finalText.isBlank()) {
                log.warn("语音转写结果为空, sessionId={}", sessionId);
            }

            // 8. 封装返回结果
            SpeechTranscriptionVO vo = new SpeechTranscriptionVO();
            vo.setSessionId(sessionId);
            vo.setText(finalText);
            vo.setRawResultJson(rawResultBuilder.toString());
            return vo;
        } catch (BusinessException e) {
            // 业务异常直接抛出，由全局异常处理器统一封装
            throw e;
        } catch (Exception e) {
            log.error("调用讯飞实时语音转写服务异常", e);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "语音转写服务异常，请稍后重试");
        }
    }

    /**
     * 构造带签名的WebSocket URL
     * <p>
     * 参考文档中的signature生成规则：
     * 1. 所有参数（不包含signature）按参数名升序排序
     * 2. 对key和value分别做URL编码后，以 key=value& 形式拼接
     * 3. 使用accessKeySecret + HmacSHA1计算签名，并进行Base64编码
     *
     * @param uuid 业务侧用户标识
     * @return 完整的WebSocket请求URL
     */
    private String buildSignedWebSocketUrl(String uuid) throws Exception {
        // 构造业务参数
        Map<String, String> params = new TreeMap<>();
        params.put("accessKeyId", accessKeyId);
        params.put("appId", appId);
        params.put("lang", lang);
        params.put("audio_encode", audioEncode);
        params.put("samplerate", String.valueOf(samplerate));
        params.put("utc", buildUtcTime());
        if (uuid != null && !uuid.isBlank()) {
            params.put("uuid", uuid);
        }

        // 按参数名升序排序后拼接成baseString
        StringBuilder baseStringBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String encodedKey = URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8);
            String encodedValue = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
            baseStringBuilder.append(encodedKey)
                    .append("=")
                    .append(encodedValue)
                    .append("&");
        }
        // 去掉最后一个多余的&
        if (baseStringBuilder.length() > 0) {
            baseStringBuilder.setLength(baseStringBuilder.length() - 1);
        }
        String baseString = baseStringBuilder.toString();
        log.debug("iflytek asr baseString={}", baseString);

        // 生成signature
        String signature = hmacSha1Base64(baseString, accessKeySecret);
        String encodedSignature = URLEncoder.encode(signature, StandardCharsets.UTF_8);

        // 构造最终queryString（在原参数基础上追加signature）
        StringBuilder queryBuilder = new StringBuilder(baseString);
        queryBuilder.append("&signature=").append(encodedSignature);

        return baseUrl + "?" + queryBuilder;
    }

    /**
     * 构造UTC时间字符串，格式如：2025-09-04T15%3A38%3A07%2B0800
     * <p>
     * 注意：文档中示例中的时间整体进行了URL编码，
     * 这里先拼出原始格式：yyyy-MM-dd'T'HH:mm:ssZ，再交由URLEncoder在上一步统一编码。
     *
     * @return UTC时间字符串
     */
    private String buildUtcTime() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.of("+8"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
        return now.format(formatter);
    }

    /**
     * 使用HmacSHA1算法计算签名并做Base64编码
     *
     * @param data 待签名字符串
     * @param key  密钥accessKeySecret
     * @return Base64编码后的签名字符串
     */
    private String hmacSha1Base64(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1");
        mac.init(secretKeySpec);
        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(rawHmac);
    }

    /**
     * 将上传的音频流按固定字节大小分片，通过WebSocket发送至讯飞服务端。
     * 自动检测WAV格式并跳过头部，仅发送纯PCM数据。
     *
     * @param webSocket 已建立的WebSocket连接
     * @param audioFile 前端上传的音频文件（支持WAV或原始PCM）
     */
    private void sendAudioStream(WebSocket webSocket, MultipartFile audioFile) throws IOException, InterruptedException {
        try (InputStream inputStream = audioFile.getInputStream()) {
            // 读取前12字节判断WAV格式（RIFF + WAVE标识）
            byte[] magic = new byte[12];
            int magicLen = readFully(inputStream, magic);
            boolean isWav = magicLen == 12
                    && magic[0] == 'R' && magic[1] == 'I' && magic[2] == 'F' && magic[3] == 'F'
                    && magic[8] == 'W' && magic[9] == 'A' && magic[10] == 'V' && magic[11] == 'E';

            InputStream pcmStream;
            if (isWav) {
                log.info("检测到WAV格式音频，将跳过头部提取PCM数据");
                pcmStream = skipWavHeader(inputStream);
            } else {
                // 非WAV格式，将已读取的magic字节作为数据头部重新拼接
                byte[] remaining = inputStream.readAllBytes();
                byte[] allData = new byte[magicLen + remaining.length];
                System.arraycopy(magic, 0, allData, 0, magicLen);
                System.arraycopy(remaining, 0, allData, magicLen, remaining.length);
                pcmStream = new java.io.ByteArrayInputStream(allData);
            }

            try (pcmStream) {
                byte[] buffer = new byte[AUDIO_CHUNK_SIZE];
                int len;
                while ((len = pcmStream.read(buffer)) != -1) {
                    ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, len);
                    CompletableFuture<WebSocket> future = webSocket.sendBinary(byteBuffer, true);
                    future.join();
                    Thread.sleep(40);
                }
            }
        }
    }

    /**
     * 跳过WAV文件头部，定位到PCM数据起始位置（"data" chunk）。
     */
    private InputStream skipWavHeader(InputStream inputStream) throws IOException {
        // 读取fmt chunk大小（字节16-19，小端序uint32）
        byte[] fmtChunkHeader = new byte[12];
        readFully(inputStream, fmtChunkHeader);
        int fmtChunkSize = ((fmtChunkHeader[4] & 0xFF)
                | ((fmtChunkHeader[5] & 0xFF) << 8)
                | ((fmtChunkHeader[6] & 0xFF) << 16)
                | ((fmtChunkHeader[7] & 0xFF) << 24));

        // 跳过fmt chunk剩余部分（fmtChunkSize - 已读的4字节 + 4字节fmt标识）
        int fmtDataRemaining = fmtChunkSize - (fmtChunkHeader.length - 8);
        if (fmtDataRemaining > 0) {
            inputStream.skipNBytes(fmtDataRemaining);
        }

        // 查找"data" chunk
        byte[] chunkHeader = new byte[8];
        while (readFully(inputStream, chunkHeader) == 8) {
            int chunkSize = ((chunkHeader[4] & 0xFF)
                    | ((chunkHeader[5] & 0xFF) << 8)
                    | ((chunkHeader[6] & 0xFF) << 16)
                    | ((chunkHeader[7] & 0xFF) << 24));
            if (chunkHeader[0] == 'd' && chunkHeader[1] == 'a' && chunkHeader[2] == 't' && chunkHeader[3] == 'a') {
                // 找到data chunk，返回从此处开始的流（仅包含PCM数据）
                return inputStream;
            }
            // 非data chunk，跳过该块
            inputStream.skipNBytes(chunkSize);
        }

        throw new IOException("WAV文件中未找到data chunk");
    }

    /**
     * 从输入流中读取数据填满缓冲区，返回实际读取的字节数。
     */
    private int readFully(InputStream inputStream, byte[] buffer) throws IOException {
        int offset = 0;
        int remaining = buffer.length;
        while (remaining > 0) {
            int read = inputStream.read(buffer, offset, remaining);
            if (read == -1) {
                break;
            }
            offset += read;
            remaining -= read;
        }
        return offset;
    }

    /**
     * 发送音频结束标识，通知讯飞服务端不再有新的音频数据
     *
     * @param webSocket WebSocket连接
     * @param sessionId 当前会话ID
     */
    private void sendEndSignal(WebSocket webSocket, String sessionId) {
        String endJson = "{\"end\": true, \"sessionId\": \"" + sessionId + "\"}";
        webSocket.sendText(endJson, true).join();
    }

    /**
     * 创建WebSocket监听器，用于处理服务端返回的转写结果
     * <p>
     * 解析逻辑：
     * 1. 仅处理msg_type = "result" 且 res_type = "asr" 的消息
     * 2. 从 data.cn.st.rt[].ws[].cw[].w 中提取词结果并按顺序拼接
     * 3. 如果data.ls为true，表示最后一帧，触发计数器结束等待
     */
    private WebSocket.Listener createWebSocketListener(StringBuilder textResultBuilder,
                                                       StringBuilder rawResultBuilder,
                                                       CountDownLatch finishLatch,
                                                       List<String> errorHolder) {
        return new WebSocket.Listener() {
            // 处理分片文本的临时缓冲区
            private final StringBuilder partialText = new StringBuilder();

            @Override
            public void onOpen(WebSocket webSocket) {
                log.info("已连接到讯飞实时语音转写服务");
                webSocket.request(1);
            }

            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                try {
                    partialText.append(data);
                    if (last) {
                        String jsonStr = partialText.toString();
                        partialText.setLength(0);

                        // 记录原始结果，方便排查（可选）
                        if (rawResultBuilder.length() > 0) {
                            rawResultBuilder.append("\n");
                        }
                        rawResultBuilder.append(jsonStr);

                        // 解析JSON并抽取文本
                        handleResultJson(jsonStr, textResultBuilder, finishLatch);
                    }
                } catch (Exception e) {
                    log.error("解析讯飞转写结果异常", e);
                    errorHolder.add(e.getMessage());
                    finishLatch.countDown();
                } finally {
                    webSocket.request(1);
                }
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
                // 本服务场景下，客户端只关心文本结果，不期望收到Binary消息
                webSocket.request(1);
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                log.info("讯飞转写WebSocket连接关闭, statusCode={}, reason={}", statusCode, reason);
                finishLatch.countDown();
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public void onError(WebSocket webSocket, Throwable error) {
                log.error("讯飞转写WebSocket发生错误", error);
                errorHolder.add(error.getMessage());
                finishLatch.countDown();
            }
        };
    }

    /**
     * 解析单条JSON结果并将识别文本追加到结果构建器中
     *
     * @param jsonStr           服务端返回的JSON字符串
     * @param textResultBuilder 文本结果累加器
     * @param finishLatch       结束信号计数器
     */
    private void handleResultJson(String jsonStr,
                                  StringBuilder textResultBuilder,
                                  CountDownLatch finishLatch) throws IOException {
        JsonNode root = objectMapper.readTree(jsonStr);
        String msgType = root.path("msg_type").asText("");
        String resType = root.path("res_type").asText("");

        // 只处理转写结果类型
        if (!"result".equalsIgnoreCase(msgType) || !"asr".equalsIgnoreCase(resType)) {
            // 如果是错误类型的返回（例如code非0），可以根据需要补充更详细的错误处理
            return;
        }

        JsonNode dataNode = root.path("data");
        boolean isLast = dataNode.path("ls").asBoolean(false);
        JsonNode cnNode = dataNode.path("cn");
        JsonNode stNode = cnNode.path("st");
        int resultType = stNode.path("type").asInt(0);

        // type=0 表示确定性结果，type=1 表示中间结果；这里两种都可以累加
        JsonNode rtArray = stNode.path("rt");
        if (rtArray.isArray()) {
            for (JsonNode rt : rtArray) {
                JsonNode wsArray = rt.path("ws");
                if (!wsArray.isArray()) {
                    continue;
                }
                for (JsonNode ws : wsArray) {
                    JsonNode cwArray = ws.path("cw");
                    if (!cwArray.isArray()) {
                        continue;
                    }
                    for (JsonNode cw : cwArray) {
                        String word = cw.path("w").asText("");
                        if (!word.isEmpty()) {
                            textResultBuilder.append(word);
                        }
                    }
                }
            }
        }

        // 如果是最终结果帧，通知主线程可以结束等待
        if (isLast && resultType == 0) {
            finishLatch.countDown();
        }
    }
}

