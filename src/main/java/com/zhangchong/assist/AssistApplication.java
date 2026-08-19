package com.zhangchong.assist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.zhangchong.assist.config.AssistProperties;

@SpringBootApplication
@EnableConfigurationProperties(AssistProperties.class)
public class AssistApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssistApplication.class, args);
    }
}
