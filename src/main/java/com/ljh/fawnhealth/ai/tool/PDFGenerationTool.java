package com.ljh.fawnhealth.ai.tool;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import cn.hutool.core.io.FileUtil;
import com.ljh.fawnhealth.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.IOException;

/**
 * PDF生成工具类
 */
public class PDFGenerationTool {

    /**
     * 生成包含指定内容的PDF文件
     */
    @Tool(description = "根据提供的内容生成PDF文件", returnDirect = false)
    public String generatePDF(
            @ToolParam(description = "生成的PDF文件名") String fileName,
            @ToolParam(description = "要写入PDF的内容") String content) {
        // PDF保存目录
        String fileDir = FileConstant.FILE_SAVE_DIR + "/pdf";
        String filePath = fileDir + "/" + fileName;
        
        try {
            FileUtil.mkdir(fileDir); // 创建目录
            
            // 使用try-with-resources自动关闭资源
            try (PdfWriter writer = new PdfWriter(filePath);
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf)) {
                
                // 使用内置中文字体
                PdfFont font = PdfFontFactory.createFont("STSongStd-Light", "UniGB-UCS2-H");
                document.setFont(font);
                
                // 添加文本内容
                document.add(new Paragraph(content));
            }
            return "PDF文件已成功生成至：" + filePath;
        } catch (IOException e) {
            return "生成PDF时发生错误：" + e.getMessage();
        }
    }
}