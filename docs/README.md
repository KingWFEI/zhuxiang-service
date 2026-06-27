# 筑享接口文档

- `openapi.yaml`：推荐用于导入 Apifox、Postman、YApi 等接口工具。
- `openapi.json`：与 YAML 内容等价，适合程序读取和校验。
- 本地启动服务后，Swagger UI 地址为 `http://localhost:8000/api/swagger-ui.html`。
- OpenAPI 在线地址为 `http://localhost:8000/api/v3/api-docs`，YAML 地址为 `http://localhost:8000/api/v3/api-docs.yaml`。

导出的文档包含 14 个接口分组、53 个路径和 57 个操作，并标明请求参数、请求体、响应模型、通用错误响应及 Bearer Token 认证要求。

如接口发生变化，启动服务后重新请求上述 JSON 和 YAML 地址并覆盖本目录中的文件。
