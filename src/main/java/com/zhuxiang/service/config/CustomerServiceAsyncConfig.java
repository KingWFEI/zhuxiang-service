package com.zhuxiang.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class CustomerServiceAsyncConfig {

    @Bean(name = "customerServiceExecutor", destroyMethod = "shutdown")
    public ExecutorService customerServiceExecutor() {
        return Executors.newFixedThreadPool(8, Thread.ofPlatform()
                .name("customer-service-chat-", 0)
                .factory());
    }
}
