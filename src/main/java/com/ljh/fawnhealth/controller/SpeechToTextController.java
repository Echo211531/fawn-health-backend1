package com.ljh.fawnhealth.controller;

import com.ljh.fawnhealth.commen.BaseResponse;
import com.ljh.fawnhealth.config.ResultUtils;
import com.ljh.fawnhealth.model.vo.ai.SpeechTranscriptionVO;
import com.ljh.fawnhealth.service.SpeechToTextService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 语音转文字控制器
 * <p>
 * 小鹿健康 App 调用入口：
 * 1. 前端通过 multipart/form-data 上传PCM音频文件
 * 2. 后端转发到讯飞实时语音转写大模型，获取转写文本
 * 3. 统一使用 BaseResponse 包装返回结果
 */
@RestController
@RequestMapping("/speech")
@Slf4j
public class SpeechToTextController {

    @Resource
    private SpeechToTextService speechToTextService;

    /**
     * 语音转文字接口
     * <p>
     * 前端调用示例（HTTP）：
     * POST /api/speech/transcribe
     * Content-Type: multipart/form-data
     * form-data:
     *   - audioFile: 文件字段，PCM音频文件
     *   - userUuid:  可选，业务方自定义用户ID
     *
     * @param audioFile PCM音频文件（采样率16k、16bit、单声道）
     * @param userUuid  业务侧用户标识，可为空
     * @return 语音转写结果
     */
    @PostMapping("/transcribe")
    public BaseResponse<SpeechTranscriptionVO> transcribe(
            @RequestParam("audioFile") MultipartFile audioFile,
            @RequestParam(value = "userUuid", required = false) String userUuid) {
        log.info("接收到语音转文字请求, fileName={}, size={}", audioFile != null ? audioFile.getOriginalFilename() : "null",
                audioFile != null ? audioFile.getSize() : 0);
        SpeechTranscriptionVO result = speechToTextService.transcribePcmAudio(audioFile, userUuid);
        return ResultUtils.success(result);
    }
}

