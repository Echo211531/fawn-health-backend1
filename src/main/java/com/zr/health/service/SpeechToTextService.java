package com.zr.health.service;

import com.zr.health.model.vo.ai.SpeechTranscriptionVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 语音转文字服务接口
 * 基于讯飞实时语音转写大模型，通过WebSocket协议将音频流发送到服务端并获取转写结果
 */
public interface SpeechToTextService {

    /**
     * 将上传的PCM音频文件转写为文本
     * <p>
     * 注意：
     * 1. 当前接口假定前端上传的是16k采样率、16bit、单声道的PCM原始音频（.pcm），并且不做格式转换；
     * 2. 若后续需要支持wav/mp3等格式，应在调用本方法前先做格式转换为符合要求的PCM。
     *
     * @param audioFile 前端上传的PCM音频文件（采样率16k、16bit、单声道）
     * @param userUuid  业务侧用户标识，用于在讯飞侧标记不同用户，可为空
     * @return 语音转文字结果视图对象
     */
    SpeechTranscriptionVO transcribePcmAudio(MultipartFile audioFile, String userUuid);
}

