package com.ljh.fawnhealth.ai.tools;
import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.ljh.fawnhealth.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.File;

/**
 * 资源下载工具类
 */
public class ResourceDownloadTool {

    /**
     * 从指定URL下载资源（默认路径）
     */
    @Tool(description = "从指定URL下载资源（默认路径）")
    public String downloadResource(
            @ToolParam(description = "资源的下载地址") String url,
            @ToolParam(description = "保存资源的文件名") String fileName) {
        String fileDir = FileConstant.FILE_SAVE_DIR +"/download";
        return download(url, fileName, fileDir);
    }

    /**
     * 从指定URL下载资源（自定义路径）
     */
    @Tool(description = "从指定URL下载资源，并指定保存路径")
    public String downloadResourceWithCustomPath(
            @ToolParam(description = "资源的下载地址") String url,
            @ToolParam(description = "保存资源的文件名") String fileName,
            @ToolParam(description = "自定义保存路径") String customPath) {
        return download(url, fileName, customPath);
    }

    /**
     * 实际下载逻辑
     */
    private String download(String url, String fileName, String targetDir) {
        String filePath = targetDir + "/" + fileName;
        try {
            FileUtil.mkdir(targetDir); // 确保目录存在
            HttpUtil.downloadFile(url, new File(filePath));
            return "资源已成功下载至：" + filePath;
        } catch (Exception e) {
            return "下载资源时发生错误：" + e.getMessage();
        }
    }
}