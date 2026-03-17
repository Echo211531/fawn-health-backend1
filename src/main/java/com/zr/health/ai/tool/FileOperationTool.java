package com.zr.health.ai.tool;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import com.zr.health.utils.AliOssUtil;
import jakarta.annotation.Resource;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 文件操作工具类（基于阿里云OSS）
 */
@Slf4j
@Component
public class FileOperationTool {

    @Resource
    private AliOssUtil aliOssUtil;
    private final String objectNamePrefix="oss/files/";   // 对象名称前缀，例如："oss/files/"


    /**
     * 从OSS读取文件内容
     *
     * @param fileName 文件名
     * @return 文件内容字符串
     */
    @Tool(description = "从OSS中读取文件内容")
    public String readFileFromOSS(@ToolParam(description = "要读取的文件名") String fileName) {
        try {
            OSS ossClient = new OSSClientBuilder().build(aliOssUtil.getEndpoint(), aliOssUtil.getAccessKeyId(), aliOssUtil.getAccessKeySecret());
            OSSObject ossObject = ossClient.getObject(aliOssUtil.getBucketName(), getObjectKey(fileName));
            StringBuilder content = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(ossObject.getObjectContent()));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        } catch (Exception e) {
            log.error("读取OSS文件失败：{}", fileName, e);
            return "读取文件时出错：" + e.getMessage();
        }
    }

    /**
     * 写入内容到OSS文件
     *
     * @param fileName 文件名
     * @param content  文件内容
     * @return 操作结果
     */
    @Tool(description = "写入内容到OSS文件")
    public String writeFileToOSS(
            @ToolParam(description = "要写入的文件名") String fileName,
            @ToolParam(description = "要写入的内容") String content
    ) {
        try {
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            String url = aliOssUtil.upload(bytes, getObjectKey(fileName));
            return "文件已成功上传至OSS：" + url;
        } catch (Exception e) {
            log.error("写入OSS文件失败：{}", fileName, e);
            return "写入文件时出错：" + e.getMessage();
        }
    }

    /**
     * 构造OSS中的对象名称
     */
    private String getObjectKey(String fileName) {
        return objectNamePrefix + fileName;
    }
}