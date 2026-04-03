# 课程智能组卷与在线考试系统

前后端分离：Spring Boot 3 + Vue 3（Vite + Element Plus）。

## 环境要求

- JDK 17、Maven 3.8+
- Node.js 18+、npm 9+
- MySQL 8.x（本地或 Docker，见 `docker-compose.yml`）

## 数据库

1. 创建库并导入表结构：`backend/doc/schema.sql`
2. 首次启动后端若 `sys_menu` 为空，会自动写入菜单与角色绑定。
3. **若数据库已有旧菜单**，可按需执行迁移 SQL：
   - `doc/migration_menu_grading.sql`（主观题阅卷）
   - `doc/migration_menu_file_tools.sql`（文件与文本）

## 注册与上传

- **学生自助注册**：`POST /api/auth/register`（无需登录），前端路径 `/register`。库表无邮箱字段，采用「用户名 + 可选姓名 + 密码」，固定注册为 **学生** 角色（与 [WebStudy](https://github.com/songyining64/WebStudy) 中注册页思路类似，简化了邮箱验证码流程）。
- **文件上传**：登录后 `POST /api/file/upload`，工具页路径 `/tools/file`；另支持浏览器端 **纯前端** 导出 `.txt`。

## 启动后端

```bash
cd backend
mvn spring-boot:run
```

默认端口 `8080`。健康检查：`GET http://localhost:8080/actuator/health`  
Swagger UI：`http://localhost:8080/swagger-ui.html`（见 `doc/API与Swagger.md`）

## 启动前端

```bash
cd frontend
npm install
npm run dev
```

开发时 Vite 将 `/api` 代理到 `http://localhost:8080`（见 `vite.config.ts`）。

## 脚本说明

| 命令 | 说明 |
|------|------|
| `npm run dev` | 开发服务器 |
| `npm run build` | 生产构建（已配置 `manualChunks` 分包） |
| `npm run typecheck` | `vue-tsc --noEmit` 类型检查 |

## 演示账号

启动后若不存在会自动创建（见 `ExamApplication`）：

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | 管理员 |
| teacher | teacher123 | 教师 |
| student | student123 | 学生 |

## 功能说明（节选）

- **主观题阅卷**：试卷中含「简答题（SHORT）」且学生已交卷、该题尚未得分时，教师/管理员可在「主观题阅卷」页打分；接口 `GET /api/exam/grading/pending-subjective`、`POST /api/exam/grading/subjective`。
- 权限与菜单说明见 `backend/doc/权限与成绩说明.md`。

## 参考 UI

前端顶栏与登录区曾对齐 [WebStudy](https://github.com/songyining64/WebStudy) 中 `qingbaogang` 项目的风格（浅蓝导航、主色 `#3498db`）。
