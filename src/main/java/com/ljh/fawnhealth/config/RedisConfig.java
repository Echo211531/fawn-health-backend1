package com.ljh.fawnhealth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis配置类
 * 提供两种序列化策略：
 * 1. StringRedisTemplate - 用于简单类型（字符串、数字）和Lua脚本操作
 * 2. objectRedisTemplate - 用于复杂Java对象存储（使用JSON序列化）
 *
 * @author 27105
 */
@Configuration
public class RedisConfig {

    /**
     * 字符串Redis模板（用于简单类型和Lua脚本操作）
     * 使用场景：
     * - 库存操作（Lua脚本）
     * - 计数器、限流等数值操作
     * - Hash结构存储简单键值对
     * - BitMap操作
     *
     * 注入方式：
     * - 直接注入：@Resource private StringRedisTemplate stringRedisTemplate;
     * - 或：@Resource private RedisTemplate<String, String> redisTemplate;
     *
     * @param connectionFactory Redis连接工厂
     * @return StringRedisTemplate实例
     */
    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 字符串Redis模板（泛型版本，与stringRedisTemplate功能相同）
     * 用于需要明确指定泛型类型的场景
     *
     * @param connectionFactory Redis连接工厂
     * @return RedisTemplate<String, String>实例
     */
    @Bean
    public RedisTemplate<String, String> stringRedisTemplateGeneric(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 对象Redis模板（用于复杂Java对象存储）
     * 使用场景：
     * - 存储VO对象（如CommunityPostsVO、CommentVO）
     * - 存储复杂实体对象
     * - 需要保留对象类型信息的场景
     *
     * @param connectionFactory Redis连接工厂
     * @return RedisTemplate<String, Object>实例
     */
    @Bean
    public RedisTemplate<String, Object> objectRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 配置ObjectMapper，支持类型信息
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        // 使用Jackson2JsonRedisSerializer序列化值
        Jackson2JsonRedisSerializer<Object> valueSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        // Key使用String序列化（统一使用字符串作为key）
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(valueSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(valueSerializer);

        // 设置默认序列化器
        template.setDefaultSerializer(valueSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Spring Session默认序列化器
     * 用于Spring Session的Redis存储
     *
     * @return GenericJackson2JsonRedisSerializer实例
     */
    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        // 让 Spring Session 使用 JSON 方式存储
        return new GenericJackson2JsonRedisSerializer();
    }

}
