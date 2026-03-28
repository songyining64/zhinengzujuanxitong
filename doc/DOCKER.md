# Docker 部署说明

## 一键启动（项目根目录）

```bash
docker compose up -d --build
```

- 前端：<http://localhost>（Nginx 将 `/api` 转发到后端）
- 后端：<http://localhost:8080>
- MySQL：`localhost:3306`（root/root，库 `exam_system`）
- Redis：`localhost:6379`

首次启动需等待 MySQL healthy 后再启动后端；**请自行执行** `backend/doc/schema.sql`（及迁移 SQL）初始化表结构，或挂载初始化脚本。

## 单独构建镜像

```bash
docker build -t exam-backend:latest ./backend
docker build -t exam-frontend:latest ./frontend
```

## 环境变量

后端可通过环境变量覆盖 JWT、MySQL、Redis 等，见 `application.yml` / `application-docker.yml`。
