# 项目结构、可维护性、测试与安全指南

## 一、如何优化项目结构以提高可维护性？

### 1. 原则（Java / 前端通用）

| 原则 | 说明 |
|------|------|
| **按业务域分包** | 同一聚合（题库、试卷、考试、用户）的 controller / service / entity / mapper 放在一起，修改影响面清晰。 |
| **分层清晰** | 接口层只做参数校验与编排；领域规则在 service；持久化在 mapper；避免 Controller 写业务 SQL。 |
| **公共与业务分离** | 全局异常、统一返回、工具类放在 `common`；与业务无关的配置在 `config`。 |
| **DTO 与实体分离** | 入参/出参用 Request/Response/VO，实体仅映射表结构，避免 API 与表结构强耦合。 |

### 2. 目标结构 vs 当前仓库（映射）

你期望的「精细化」目录与当前实现对应关系如下，**不必一次性大搬迁**，可按迭代逐步迁移。

| 目标目录 | 当前位置 | 说明 |
|----------|----------|------|
| `module/questionBank/` | `module/question/` + `module/course/`（知识点） | 试题、知识点、难度、题型枚举 |
| `module/paperGenerate/` | `module/paper/service` 中 `generateAuto` + `paper/support/PaperDifficultyAllocator` | 抽题算法与规则可继续抽到独立包 |
| `module/paperManage/` | `module/paper/`（除生成外的 CRUD、详情） | 试卷列表、预览、删除 |
| `module/user/` | `module/system/` + `security/` | 用户、菜单、角色、JWT |
| `module/statistics/` | `module/analytics/` + `module/wrongbook/` | 考试统计、错题本 |
| **顶层 `mapper/`** | 各 `module/*/mapper/` | MyBatis 习惯「与领域同包」；若统一迁到 `mapper/`，需同步改 `@MapperScan` 与 XML 路径 |
| **顶层 `model/`** | 各 `module/*/entity` + `dto` | 聚合到 `model` 需大量改 import，建议**新模块**再采用 |

**建议迁移顺序**：① 抽离纯算法（已做 `PaperDifficultyAllocator`）→ ② 文档与接口契约稳定 → ③ 按模块复制移动并保留旧包一层 `@Deprecated` 委托 → ④ 删除旧代码。

### 3. 前端结构（当前与建议）

| 目标 | 当前 | 说明 |
|------|------|------|
| `api/` 按模块 | `api/http.ts` + 可扩展 `api/modules/*.ts` | 按业务拆分请求，便于复用与 Mock |
| `hooks/` | `hooks/useExamAntiCheat.ts` | 业务组合逻辑（原 `composables` 已迁移） |
| `store/` | 可新增 Pinia `store/user.ts` 等 | 登录态、组卷草稿缓存 |
| `router/` | 按角色扩展 `meta` | 与后端权限一致 |

---

## 二、如何进行单元测试与集成测试？

### 1. 单元测试（JUnit 5）

- **对象**：**纯函数**、**无 Spring 的算法**（如 `PaperDifficultyAllocator`）、工具类、校验逻辑。
- **位置**：`backend/src/test/java/...` 与源码包同结构。
- **运行**：`mvn test`（或 IDE 运行单个类）。
- **示例**：`PaperDifficultyAllocatorTest` — 验证「按权重拆分题量」与「总和等于 need」。

### 2. 集成测试

| 类型 | 说明 | 依赖 |
|------|------|------|
| **@SpringBootTest** | 启动完整 Spring 上下文，测 Controller→Service→Mapper | 需数据库：可用 **H2 内存库**、**Testcontainers MySQL**、或**独立测试库** |
| **@WebMvcTest** | 只测 Web 层，Mock Service | 无真实 DB |
| **@DataJpaTest / MyBatis** | 偏持久层 | 需配置 |

**建议**：本地/CI 用 **Testcontainers** 或 **固定测试库**；在 `src/test/resources/application-test.yml` 中覆盖数据源。

### 3. 组卷规则专项测试建议

- **已覆盖**：难度权重拆分（`PaperDifficultyAllocatorTest`）。
- **可补充**：在集成测试中向 H2 灌入题目数据，调用 `PaperService.generateAuto`，断言**题型数量**、**去重**、**难度占比**（需 Mock 或真实 Mapper）。

---

## 三、如何保证项目安全性？

### 1. 已具备（本仓库）

- **Spring Security + JWT**：无 Token 时除白名单外拒绝访问。
- **方法级权限**：`@PreAuthorize` + 菜单 `perms` 与 `UserDetails` 权限对齐。
- **课程数据权限**：`CoursePermissionService` 限制教师/学生仅能访问本课程数据。
- **参数校验**：JSR-380（`@Valid`、`@NotNull` 等）在 DTO 上。
- **MyBatis 参数绑定**：`#{}` 预编译，避免拼接 SQL 导致注入。
- **生产**：JWT 密钥、数据库密码、Redis 密码使用环境变量（见 `application.yml` / `docker-compose`）。

### 2. 建议加强

| 项 | 说明 |
|----|------|
| **HTTPS** | 生产反向代理（Nginx）终止 TLS |
| **CORS** | 生产限制 `allowedOrigin` 为前端域名 |
| **限流** | 网关或 Bucket4j 限制登录、组卷接口 |
| **审计日志** | 记录关键操作人、时间、资源 ID |
| **依赖扫描** | `mvn dependency-check` 或 GitHub Dependabot |

### 3. Redis 与容器

- **Redis**：默认关闭自动配置；`app.redis.enabled=true` 且 `RedisConfig` 生效时连接缓存；Docker 中 `application-docker.yml` 已示例开启。
- **Docker**：根目录 `docker-compose.yml` 编排 MySQL、Redis、后端、前端；前端 Nginx 将 `/api` 反代到后端。

---

## 四、相关文档索引

| 文档 | 内容 |
|------|------|
| [组卷算法设计.md](./组卷算法设计.md) | 配比规则、抽题流程 |
| [核心业务深化与路线图.md](./核心业务深化与路线图.md) | 批量导入/审核/版本/查重、多模式组卷、导出与审计等分阶段规划 |
| [数据库表设计说明.md](./数据库表设计说明.md) | 表与关系（与 `schema.sql` 一致） |
| [API接口文档.md](./API接口文档.md) | HTTP 接口总览（路径、权限、模块） |
| [API与Swagger.md](./API与Swagger.md) | OpenAPI / Swagger UI 访问方式 |
