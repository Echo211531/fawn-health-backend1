package com.zr.health.ai.tool;

import org.jsoup.Jsoup;

import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 网页抓取工具类
 */
public class WebScrapingTool {

    /**
     * 抓取指定URL的网页内容
     */
    @Tool(description = "抓取网页内容")
    public String scrapeWebPage(@ToolParam(description = "要抓取的网页URL") String url) {
        try {
            // 使用Jsoup获取页面内容
            Document document = Jsoup.connect(url).get();
            return document.html();
        } catch (Exception e) {
            return "抓取网页时发生错误: " + e.getMessage();
        }
    }
}