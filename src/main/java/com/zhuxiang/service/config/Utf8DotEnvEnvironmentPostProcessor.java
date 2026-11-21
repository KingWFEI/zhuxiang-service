package com.zhuxiang.service.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

/**
 * 在配置绑定前按 UTF-8 加载本地 .env，允许直接填写中文配置值。
 */
public class Utf8DotEnvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        FileSystemResource resource = new FileSystemResource(".env");
        if (!resource.exists()) {
            return;
        }
        Properties properties;
        try {
            properties = PropertiesLoaderUtils.loadProperties(new EncodedResource(resource, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalStateException("无法按 UTF-8 读取本地 .env 配置文件", exception);
        }
        PropertiesPropertySource source = new PropertiesPropertySource("localUtf8DotEnv", properties);
        if (environment.getPropertySources().contains(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
            environment.getPropertySources().addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, source);
        } else {
            environment.getPropertySources().addLast(source);
        }
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 1;
    }
}
