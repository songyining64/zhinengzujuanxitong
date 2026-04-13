# 课程智能组卷系统

前后端分离的在线组卷、考试、阅卷与成绩分析系统。业务与表结构以本仓库 `backend/doc/schema.sql` 为准。

> **关于参考仓库**  
> 曾对照 [asdfaf1458/biyesheji2289](https://github.com/asdfaf1458/biyesheji2289)：该仓库与标题不符，内容为招聘类通用模板且未提供可用组卷 SQL，**未并入任何业务代码**，以免破坏现有架构。本文档按本校/本课题要求整理本项目真实说明。

---

## 作品简介

本系统面向课程教学场景，支持教师维护课程与知识点、录入与导入试题、手动或**自动组卷**（按题型、难度权重、知识点范围选题），并发布考试、学生在线答题、客观题自动判分、主观题阅卷及成绩分析、错题本等模块。后端采用 Spring Boot 3 + MyBatis-Plus，前端采用 Vue 3 + Element Plus，数据使用 MySQL（可选用 Redis）。

---

## 开源代码与组件使用情况说明

| 类别 | 组件 | 许可证（常见） | 用途简述 |
|------|------|----------------|----------|
| 后端框架 | Spring Boot 3.2.x | Apache-2.0 | Web、安全、校验、Actuator |
| 持久层 | MyBatis-Plus 3.5.x | Apache-2.0 | ORM 与分页 |
| 数据库 | MySQL Connector/J | GPL-2.0（带例外） | JDBC 驱动 |
| 认证 | JJWT 0.11.x | Apache-2.0 | JWT 签发与解析 |
| API 文档 | springdoc-openapi | Apache-2.0 | OpenAPI / Swagger UI |
| 表格导入导出 | EasyExcel 3.x | Apache-2.0 | Excel 处理 |
| 缓存（可选） | Spring Data Redis | Apache-2.0 | 缓存等 |
| 前端框架 | Vue 3、Vue Router、Pinia | MIT | UI 与状态 |
| UI 库 | Element Plus | MIT | 组件库 |
| 构建 | Vite 5、TypeScript | MIT | 开发与打包 |
| HTTP | Axios | MIT | 接口请求 |

完整依赖以 `backend/pom.xml`、`frontend/package.json` 为准。自有业务代码为本项目作者编写；未将上述第三方源码并入版权归属。

---

## 作品安装说明

### 环境要求

- JDK 17+
- Node.js 18+（前端开发/构建）
- Maven 3.8+
- MySQL 8.x（推荐）

### 数据库

1. 创建数据库 `exam_system`（或按 `application.yml` 配置）。
2. 执行 `backend/doc/schema.sql` 初始化表结构；若有增量迁移脚本，按 `doc/数据库表设计说明.md` 说明执行。

### 后端

```bash
cd backend
# 配置 src/main/resources/application.yml 中的数据源等
mvn -q spring-boot:run
```

默认 API 端口见 `application.yml`（常见 `8080`）。

### 前端

```bash
cd frontend
npm install
npm run dev
```

构建生产包：`npm run build`。

### Docker 一键部署

见 [doc/DOCKER.md](doc/DOCKER.md)。

### 接口说明

见 [doc/API接口文档.md](doc/API接口文档.md)。

---

## 作品效果图

> 可在 `doc/images/` 下放置截图，并在下列位置替换为实际图片引用，例如：  
> `![登录页](doc/images/login.png)`

- **登录与首页**：_（待补充）_
- **题库 / 知识点**：_（待补充）_
- **组卷与试卷**：_（待补充）_
- **考试答题与成绩**：_（待补充）_

---

## 设计思路

1. **领域划分**：课程与选课、题库与知识点树、试卷（手动/自动）、考试会话与答卷、阅卷、统计分析、错题本、文件与菜单权限分模块实现，接口前缀统一在 `/api` 下。
2. **组卷**：在指定课程与知识点范围内，按题型数量或题型+难度权重从题库抽样，支持随机种子与跨题型去重；规则写入 `paper.rules_json` 便于审计与复现。算法要点见 [doc/组卷算法设计.md](doc/组卷算法设计.md)。
3. **安全**：Spring Security + JWT；菜单与接口通过角色与 `perms` 对齐。
4. **前后端分离**：前端 Vue 托管于 Nginx 或开发服务器，通过代理访问后端 API。

---

## 设计重点难点

| 重点/难点 | 说明 |
|-----------|------|
| **自动组卷约束满足** | 题型、难度档位、知识点过滤同时满足时，题库不足需明确报错；难度与 `question.difficulty` 语义统一（含空难度的归并规则）。 |
| **考试并发与一致性** | 开考、暂存、交卷幂等与收卷定时任务，避免重复提交与状态错乱。 |
| **阅卷与评分** | 客观题匹配规则、主观题流程与成绩汇总。 |
| **权限与数据范围** | 教师/学生可见课程与考试范围，与 `doc/权限与成绩说明.md` 等文档一致。 |
| **表结构演进** | 以 `schema.sql` 为基准，增量迁移在 `backend/doc/` 中维护。 |

更多表设计与接口细节见 `doc/` 目录下各 Markdown。
