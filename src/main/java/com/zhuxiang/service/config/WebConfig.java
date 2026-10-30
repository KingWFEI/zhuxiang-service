package com.zhuxiang.service.config;

import com.zhuxiang.service.auth.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final Path uploadDirectory;
    private final String publicPath;

    public WebConfig(
            AuthInterceptor authInterceptor,
            ObjectStorageProperties storageProperties
    ) {
        this.authInterceptor = authInterceptor;
        ObjectStorageProperties.Local local = storageProperties.getLocal();
        this.uploadDirectory = Path.of(local.getDirectory()).toAbsolutePath().normalize();
        this.publicPath = normalizePublicPath(local.getPublicPath());
    }

    /**
     * 配置所有接口的跨域访问规则。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE")
                .allowedHeaders("Content-Type", "Authorization")
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor).addPathPatterns("/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = uploadDirectory.toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler(publicPath + "/**").addResourceLocations(location);
        // 兼容沉浸式旧文件路径 /files/immersive/**，仍映射到本地公开文件目录。
        registry.addResourceHandler("/files/immersive/**")
                .addResourceLocations(location);
    }

    private static String normalizePublicPath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("必须配置本地公开文件 URL 路径");
        }
        String normalized = path.trim().replace('\\', '/');
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if ("/".equals(normalized) || normalized.contains("..")) {
            throw new IllegalArgumentException("本地公开文件 URL 路径无效");
        }
        return normalized;
    }
}
