package com.ljh.fawnhealth.config;

import com.ljh.fawnhealth.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@Slf4j
public class ossConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public AliOssUtil aliOssUtil(AliOssConfig aliOssConfig){
        log.info("开始创建阿里云文件上传工具类对象:{}",aliOssConfig);
        return new AliOssUtil(aliOssConfig.getEndpoint(),
                aliOssConfig.getAccessKeyId(),
                aliOssConfig.getAccessKeySecret(),
                aliOssConfig.getBucketName());
    }
}
