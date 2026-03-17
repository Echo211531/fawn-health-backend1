package com.zr.health.ai.rag.factory;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;

/**
 * 创建上下文查询增强器的工厂
 */
public class HealthAppContextualQueryAugmenterFactory {

    /**
     * 创建基础版上下文查询增强器（直接返回兜底提示语）
     * 当检索结果为空时，完全跳过模型调用，直接返回预设提示语
     */
    public static ContextualQueryAugmenter createInstance() {
        PromptTemplate emptyContextPromptTemplate = new PromptTemplate("""
                你应该输出下面的内容：
                抱歉，我只能回答健康的问题，别的没办法帮到您哦，
                有问题可以联系kongshuo 666
                """);
        return ContextualQueryAugmenter.builder()
                .allowEmptyContext(false) // 禁止空上下文传递，强制要求有效检索结果
                .emptyContextPromptTemplate(emptyContextPromptTemplate)
                .build();
    }

    /**
     * 创建增强版上下文查询增强器（调用大模型生成兜底回答）
     * 当检索结果为空时，调用大模型基于通用知识生成回答，并明确告知信息来源
     * 
     * 关键点：传入用户原始消息，让AI基于通用知识回答，但明确说明知识库检索为空
     */
    public static ContextualQueryAugmenter createEnhancedInstance() {
        PromptTemplate emptyContextPromptTemplate = new PromptTemplate("""
                重要提示：当前查询在知识库中未找到相关文档，检索结果为空。
                
                请基于你的通用知识回答用户问题，但必须在回答开头明确说明：
                "虽然我没找到相关信息，但基于知识库的通用知识，我可以为您提供以下建议："
                
                然后给出你的回答。如果问题与健康无关，请礼貌地告知用户你只能回答健康相关问题。
                
                用户问题：{query}
                """);
        return ContextualQueryAugmenter.builder()
                .allowEmptyContext(false) // 禁止空上下文传递
                .emptyContextPromptTemplate(emptyContextPromptTemplate) // 传入用户消息{query}，触发大模型调用
                .build();
    }
}