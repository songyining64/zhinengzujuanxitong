# 接口文档（OpenAPI / Swagger）

## 文字版总览

- **[API接口文档.md](./API接口文档.md)**：按模块整理的 HTTP 路径、方法、权限与说明（便于交付/答辩）。

## 启动后端后访问

- **Swagger UI**：`http://localhost:8080/swagger-ui.html`  
- **OpenAPI JSON**：`http://localhost:8080/v3/api-docs`

## 安全说明

- 上述接口文档路径在 `SecurityConfig` 中已配置为 **匿名可访问**（便于开发联调）。
- **生产环境**建议：
  - 关闭对外 Swagger，或
  - 仅内网/VPN 访问，或
  - 增加 Basic 认证 / 网关鉴权。

## 与「接口文档」课程要求对应

- 使用 **springdoc-openapi**（OpenAPI 3）自动生成文档，与 Controller 注解保持同步。
- 复杂 DTO 可在类上补充 `@Schema` 描述（按需逐步完善）。
