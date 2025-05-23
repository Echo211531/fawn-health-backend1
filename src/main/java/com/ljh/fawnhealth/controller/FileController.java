package com.ljh.fawnhealth.controller;

import com.ljh.fawnhealth.commen.BaseResponse;
import com.ljh.fawnhealth.config.ResultUtils;
import com.ljh.fawnhealth.exception.ErrorCode;
import com.ljh.fawnhealth.utils.AliOssUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * 文件上传模块
 * 提供文件上传至阿里云OSS的接口
 */
@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {

    @Resource
    private AliOssUtil aliOssUtil;

    /**
     * 文件上传接口
     *
     * @param file 客户端上传的文件对象
     * @return 上传成功返回文件访问URL，失败返回错误信息
     */
    @PostMapping("/upload")
    public BaseResponse<String> upload(MultipartFile file) {
        log.info("文件上传请求: {}", file.getOriginalFilename());

        try {
            // 获取原始文件名
            String originalFilename = file.getOriginalFilename();

            // 提取文件扩展名（包括.）
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));

            // 生成唯一文件名（UUID+扩展名），避免文件名冲突
            String objectName = UUID.randomUUID().toString() + extension;

            // 调用阿里云OSS工具类上传文件字节流，并返回访问路径
            String filePath = aliOssUtil.upload(file.getBytes(), objectName);

            log.info("文件上传成功: {}", filePath);
            return ResultUtils.success(filePath);

        } catch (IOException e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            return ResultUtils.error(ErrorCode.UPLOAD_FAILED);
        }
    }
}