package com.zr.health.ai.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 智能健康助手文档加载器
 */
@Component
@Slf4j
public class HealthAppDocumentLoader {

    private final ResourcePatternResolver resourcePatternResolver;
    // 定义文件名关键词到类别的映射
    private static final Map<String, String> CATEGORY_MAP = new HashMap<>();

    static {
        CATEGORY_MAP.put("医院", "医疗机构");
        CATEGORY_MAP.put("科室", "医疗机构");
        CATEGORY_MAP.put("口腔科", "医疗机构");
        CATEGORY_MAP.put("神经内科", "医疗机构");
        CATEGORY_MAP.put("儿科", "医疗机构");
        CATEGORY_MAP.put("心血管内科", "医疗机构");

        CATEGORY_MAP.put("健康食物", "饮食");
        CATEGORY_MAP.put("高蛋白食物", "饮食");
        CATEGORY_MAP.put("高纤维蔬菜", "饮食");
        CATEGORY_MAP.put("低糖食物", "饮食");
        CATEGORY_MAP.put("健康早餐搭配", "饮食");
        CATEGORY_MAP.put("糖尿病饮食建议", "饮食");
        CATEGORY_MAP.put("饮食计划模板", "饮食");

        CATEGORY_MAP.put("热量估算指南", "营养");
        CATEGORY_MAP.put("维生素来源", "营养");
        CATEGORY_MAP.put("热量摄入推荐", "营养");
        CATEGORY_MAP.put("营养素需求表", "营养");

        CATEGORY_MAP.put("BMI计算指南", "健康知识");
        CATEGORY_MAP.put("血压管理常识", "健康知识");
        CATEGORY_MAP.put("睡眠质量提升", "健康知识");
        CATEGORY_MAP.put("运动健康指南", "健康知识");
    }

    public HealthAppDocumentLoader(
            @Qualifier("webApplicationContext") ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    /**
     * 加载所有 Markdown 文档并转换为 Document 对象
     * @return 所有文档列表
     */
    public List<Document> loadMarkdowns() {
        List<Document> allDocuments = new ArrayList<>();
        try {
            Resource[] resources = resourcePatternResolver.getResources("classpath:document/**/*.md");
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null || !filename.endsWith(".md")) continue;

                String baseName = filename.substring(0, filename.length() - 3); // 去掉 .md 后缀

                // 自动识别文档类别
                String category = CATEGORY_MAP.entrySet().stream()
                        .filter(entry -> baseName.contains(entry.getKey()))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElse("通用");

                // 构建配置
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeCodeBlock(false)
                        .withIncludeBlockquote(false)
                        .withAdditionalMetadata("filename", filename)
                        .withAdditionalMetadata("category", category)
                        .build();

                MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
                allDocuments.addAll(reader.get());
            }
        } catch (IOException e) {
            log.error("Markdown 文档加载失败", e);
        }
        return allDocuments;
    }
}