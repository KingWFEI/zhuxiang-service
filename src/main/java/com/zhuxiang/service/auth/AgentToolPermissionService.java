package com.zhuxiang.service.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.config.AgentProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Loads the allowlist that controls which internal business tools the Agent may call per role. */
@Component
public class AgentToolPermissionService {

    private final AgentProperties properties;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private volatile Map<String, Set<String>> permissions = Map.of();
    private volatile Map<String, String> scopes = Map.of();

    public AgentToolPermissionService(
            AgentProperties properties,
            ResourceLoader resourceLoader,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void load() {
        String location = properties.getToolPermissionsFile();
        try {
            Resource resource = resourceLoader.getResource(location);
            if (!resource.exists()) {
                throw new IllegalStateException("Agent 工具权限文件不存在: " + location);
            }
            Map<String, Set<String>> loaded = new LinkedHashMap<>();
            try (InputStream input = resource.getInputStream()) {
                JsonNode root = objectMapper.readTree(input);
                JsonNode roles = root.path("roles");
                if (!roles.isObject()) {
                    throw new IllegalStateException("Agent 工具权限 JSON 必须包含 roles 对象");
                }
                roles.fields().forEachRemaining(entry -> {
                    if (!entry.getValue().isArray()) {
                        throw new IllegalStateException(
                                "角色权限必须是工具名称数组: " + entry.getKey());
                    }
                    Set<String> tools = new LinkedHashSet<>();
                    for (JsonNode tool : entry.getValue()) {
                        if (!tool.isTextual() || tool.asText().isBlank()) {
                            throw new IllegalStateException(
                                    "工具名称必须是非空字符串: " + entry.getKey());
                        }
                        tools.add(tool.asText().trim());
                    }
                    loaded.put(normalizeRole(entry.getKey()), Collections.unmodifiableSet(tools));
                });
                Map<String, String> loadedScopes = new LinkedHashMap<>();
                JsonNode scopeNode = root.path("scopes");
                if (!scopeNode.isMissingNode()) {
                    if (!scopeNode.isObject()) {
                        throw new IllegalStateException("Agent 工具权限 scopes 必须是对象");
                    }
                    scopeNode.fields().forEachRemaining(entry -> {
                        if (!entry.getValue().isTextual()) {
                            throw new IllegalStateException("角色数据范围必须是字符串: " + entry.getKey());
                        }
                        String scope = entry.getValue().asText().trim().toUpperCase(Locale.ROOT);
                        if (!Set.of("SELF", "ANY").contains(scope)) {
                            throw new IllegalStateException("角色数据范围只支持 SELF 或 ANY: " + entry.getKey());
                        }
                        loadedScopes.put(normalizeRole(entry.getKey()), scope);
                    });
                }
                scopes = Collections.unmodifiableMap(loadedScopes);
            }
            permissions = Collections.unmodifiableMap(loaded);
        } catch (Exception error) {
            throw new IllegalStateException("加载 Agent 工具权限配置失败: " + location, error);
        }
    }

    public Set<String> allowedTools(String role) {
        return permissions.getOrDefault(normalizeRole(role), Set.of());
    }

    public void requireAllowed(String role, String toolName) {
        if (toolName == null || !allowedTools(role).contains(toolName)) {
            throw BusinessException.forbidden("当前用户角色无权调用该智能体业务工具");
        }
    }

    public boolean mayAccessAnyUser(String role) {
        return "ANY".equals(scopes.get(normalizeRole(role)));
    }

    private String normalizeRole(String role) {
        return role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
    }
}
