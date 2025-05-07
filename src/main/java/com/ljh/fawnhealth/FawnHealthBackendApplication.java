package com.ljh.fawnhealth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.ljh.fawnhealth.mapper")
public class FawnHealthBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(FawnHealthBackendApplication.class, args);
    }

}
