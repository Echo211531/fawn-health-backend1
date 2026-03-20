package com.zr.health;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@SpringBootApplication
@EnableScheduling
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan({"com.zr.health.mapper"})
public class FawnHealthBackendApplication {

    public static void main(String[] args) {
        // 禁用MVEL JIT，避免JDK17下对java.lang.Compiler的依赖
        System.setProperty("mvel2.disable.jit", "true");
        SpringApplication.run(FawnHealthBackendApplication.class, args);
    }

}
