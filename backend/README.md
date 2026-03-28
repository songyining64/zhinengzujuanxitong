# 课程智能组卷系统 · 后端

## 恢复说明（2026）

仓库内已恢复完整业务模块（课程 / 题库 / 试卷 / 考试 / 阅卷 / 成绩分析 / 错题本 / 文件 / 菜单等），并与 `doc/schema.sql` 对齐。

- 统一异常：`ErrorCode` + `BizException` + `GlobalExceptionHandler`（HTTP 状态与 body.code）
- 分页：`PaginationValidationInterceptor`，`size` 上限 200
- 考试：答题暂存、自动收卷定时任务、交卷幂等、开考并发处理
- 依赖：`pom.xml` 含 EasyExcel

### 本地编译

```bash
mvn -q compile -DskipTests
```

### 数据库

在 MySQL 中创建库 `exam_system` 后执行 `doc/schema.sql`。

### 与旧版对话代码的差异

若你曾使用「数字状态 + `paper_question_id` 作答行」的旧表结构，本仓库采用**另一套 DDL**（见 `doc/schema.sql`），部署前请**以本仓库 DDL 为准**或自行迁移数据。

### API 路径提示

- 成绩分析前缀：`/api/exam/analytics/...`（非 `/api/analytics/exam/...`）
- 主观题阅卷：`POST /api/exam/grading/subjective`

详见 `doc/ERROR_CODES.md`（若存在）。
