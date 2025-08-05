package com.ljh.fawnhealth.ai.tool;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

/**
 * 智谱AI Web Search工具
 * 提供基于智谱AI的网络搜索功能
 */
@Slf4j
@Component
public class ZhipuWebSearchTool {

    private static final String API_URL = "https://open.bigmodel.cn/api/paas/v4/web_search";
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    @Autowired
    public ZhipuWebSearchTool(ObjectMapper objectMapper,  @Value("${zhipu.api-key}") String apiKey) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }

    /**
     * 执行网络搜索
     *
     * @param searchQuery 搜索内容
     * @return 搜索结果
     */
    @Tool(description = "使用智谱AI提供的Web Search功能进行网络搜索，获取最新的互联网信息")
    public String webSearch(
            @ToolParam(description = "搜索内容，建议不超过78个字符")
            String searchQuery) {
        // 默认使用智谱高阶版搜索引擎
        String searchEngine = "search_pro";
        // 自动生成请求ID
        String requestId = UUID.randomUUID().toString();
        // 用户ID设为null
        String userId = null;

        try {
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);


            // 构建请求体
            WebSearchRequest request = WebSearchRequest.builder()
                    .searchEngine(searchEngine)
                    .searchQuery(searchQuery)
                    .requestId(requestId != null ? requestId : UUID.randomUUID().toString())
                    .userId(userId)
                    .build();

            String requestBody = objectMapper.writeValueAsString(request);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            // 发送请求
            ResponseEntity<String> response = restTemplate.postForEntity(API_URL, entity, String.class);

            // 解析响应
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.debug("智谱AI Web Search API响应: {}", response.getBody());
                WebSearchResponse webSearchResponse = objectMapper.readValue(response.getBody(), WebSearchResponse.class);
                return formatSearchResults(webSearchResponse);
            } else {
                log.error("智谱AI Web Search API请求失败: {}", response.getStatusCode());
                return "搜索请求失败: " + response.getStatusCode();
            }
        } catch (RestClientException e) {
            log.error("Web搜索API调用失败", e);
            return "搜索API调用失败: " + e.getMessage();
        } catch (JsonProcessingException e) {
            log.error("JSON处理异常", e);
            return "JSON处理异常: " + e.getMessage();
        } catch (Exception e) {
            log.error("执行Web搜索时发生未知错误", e);
            return "执行搜索时发生错误: " + e.getMessage();
        }
    }

    /**
     * 解析智谱AI返回的 JSON 数据,格式化搜索结果为可读的字符串
     */
    private String formatSearchResults(WebSearchResponse response) {
        if (response == null || response.getSearchResult() == null || response.getSearchResult().isEmpty()) {
            return "未找到相关搜索结果";
        }

        StringBuilder result = new StringBuilder();
        result.append("搜索结果：\n\n");

        // 添加搜索意图信息
        if (response.getSearchIntent() != null && !response.getSearchIntent().isEmpty()) {
            SearchIntentResp intent = response.getSearchIntent().get(0);
            result.append("搜索意图: ").append(intent.getIntent()).append("\n");
            result.append("关键词: ").append(intent.getKeywords()).append("\n\n");
        }

        int count = 1;
        for (SearchResult searchResult : response.getSearchResult()) {
            result.append(count++).append(". ");
            result.append("标题: ").append(searchResult.getTitle()).append("\n");
            if (searchResult.getContent() != null && !searchResult.getContent().isEmpty()) {
                result.append("   内容: ").append(searchResult.getContent()).append("\n");
            }
            if (searchResult.getLink() != null && !searchResult.getLink().isEmpty()) {
                result.append("   链接: ").append(searchResult.getLink()).append("\n");
            }
            if (searchResult.getRefer() != null && !searchResult.getRefer().isEmpty()) {
                result.append("   来源: ").append(searchResult.getRefer()).append("\n");
            }
            result.append("\n");
        }

        return result.toString();
    }

    /**
     *  内部类定义:Web搜索请求模型
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class WebSearchRequest {
        @JsonProperty("search_engine")
        private String searchEngine;

        @JsonProperty("search_query")
        private String searchQuery;

        @JsonProperty("request_id")
        private String requestId;

        @JsonProperty("user_id")
        private String userId;
    }

    /**
     * Web搜索响应模型
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class WebSearchResponse {
        private String id;
        private Long created;

        @JsonProperty("request_id")
        private String requestId;

        @JsonProperty("search_intent")
        private List<SearchIntentResp> searchIntent;

        @JsonProperty("search_result")
        private List<SearchResult> searchResult;
    }

    /**
     * 搜索结果模型
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class SearchResult {
        private String title;
        private String content;
        private String link;
        private String media;
        @JsonProperty("publish_date")
        private String publishDate;
        private String icon;
        private String refer;
    }

    /**
     * 搜索意图响应模型
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class SearchIntentResp {
        private String query;
        private String intent;
        private String keywords;
    }
}
