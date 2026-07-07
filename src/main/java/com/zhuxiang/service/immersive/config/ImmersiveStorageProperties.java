package com.zhuxiang.service.immersive.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "immersive.storage")
public class ImmersiveStorageProperties {

    private Local local = new Local();

    public Local getLocal() { return local; }
    public void setLocal(Local local) { this.local = local; }

    public static class Local {
        private String rootPath = "./uploads";
        private String publicBaseUrl = "http://localhost:8000/api/files/immersive";

        public String getRootPath() { return rootPath; }
        public void setRootPath(String rootPath) { this.rootPath = rootPath; }
        public String getPublicBaseUrl() { return publicBaseUrl; }
        public void setPublicBaseUrl(String publicBaseUrl) { this.publicBaseUrl = publicBaseUrl; }
    }
}
