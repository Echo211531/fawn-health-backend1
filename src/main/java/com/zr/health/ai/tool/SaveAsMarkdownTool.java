package com.zr.health.ai.tool;

import com.zr.health.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
@Component
public class SaveAsMarkdownTool {

    private static final String FILE_SAVE_DIR = FileConstant.FILE_SAVE_DIR + "/file";

    /**
     * 将给定的内容保存到Markdown文件中。
     *
     * @param fileName 文件名（包括.md扩展名）
     * @param content  要保存到Markdown文件中的内容
     */
    @Tool(description = "将内容保存markdown文件中")
    public void saveContentAsMarkdown(@ToolParam(description = "保存文件名称") String fileName,@ToolParam(description = "需要保存的内容")  String content) {
        // 确保目录存在
        File dir = new File(FILE_SAVE_DIR);
        if (!dir.exists()) {
            dir.mkdirs(); // 创建目录
        }

        String filePath = FILE_SAVE_DIR + "/" + fileName;
        try (FileWriter fileWriter = new FileWriter(filePath)) {
            fileWriter.write(content);
            System.out.println("文件已成功保存至: " + filePath);
        } catch (IOException e) {
            System.err.println("发生错误时尝试保存文件: " + e.getMessage());
            e.printStackTrace();
        }
    }
}