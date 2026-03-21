# 课程智能组卷系统 — HTTP 接口文档

> **Base URL**：`http://{host}:{port}`，默认后端端口 `8080`。  
> **统一前缀**：业务接口均为 `/api/**`。  
> **自动生成文档**：启动后端后可访问 [OpenAPI / Swagger](./API与Swagger.md)（`/swagger-ui.html`、`/v3/api-docs`）。

---

## 1. 通用约定

### 1.1 鉴权

| 项 | 说明 |
|----|------|
| 登录 | `POST /api/auth/login`（匿名） |
| 其它接口 | 请求头 `Authorization: Bearer {token}` |
| 权限 | 使用 Spring `@PreAuthorize`，按 **角色**（`ROLE_ADMIN` 等）与 **权限字符串**（如 `question:read`）控制 |

匿名放行（无需 Token）：`/api/auth/login`、`/v3/api-docs/**`、`/swagger-ui/**`、`/swagger-ui.html`、`/actuator/health`。

### 1.2 统一响应体 `ApiResponse<T>`

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | int | `0` 表示成功；非 `0` 为业务/校验错误 |
| `msg` | string | 提示信息 |
| `data` | T | 业务数据，成功时返回 |

**成功示例：**

```json
{ "code": 0, "msg": "success", "data": { } }
```

### 1.3 业务错误码 `ErrorCode`（节选）

| code | 含义 |
|------|------|
| `0` | 成功 |
| `40000` | 请求参数错误 |
| `40001` | 校验失败 |
| `40002` | 分页等参数越界 |
| `40100` | 未认证 / Token 无效 |
| `40300` | 无权限 |
| `40400` | 资源不存在 |
| `40900` / `40901` | 冲突 / 重复 |
| `50000` | 服务内部错误 |

### 1.4 分页

列表类接口多使用 MyBatis-Plus `Page`，`data` 中常见字段：`records`、`total`、`size`、`current` 等。

### 1.5 文件类响应

部分接口直接输出 **二进制流**（Excel 导出、文件下载），**不**包装为 `ApiResponse`，需使用 `Authorization` 头或浏览器携带 Cookie（本项目以 Bearer 为主）。

---

## 2. 认证 `/api/auth`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/auth/login` | 登录，Body：`{ "username", "password" }` | 匿名 |
| | | 成功 `data`：`token`、`username`、`realName`、`role` | |

---

## 3. 系统 `/api/system`

### 3.1 菜单

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/system/menu/tree` | 当前用户可见菜单树 | 已登录 |

### 3.2 用户

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/system/user` | 用户分页，`keyword`、`page`、`size` | `system:user:manage` 或 ADMIN |

---

## 4. 课程 `/api/course`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/course` | 创建课程 | `course:manage` |
| PUT | `/api/course/{id}` | 更新课程 | `course:manage` |
| GET | `/api/course/{id}` | 课程详情 | `course:read` |
| GET | `/api/course` | 分页列表，`keyword`、`page`、`size` | `course:read` |
| POST | `/api/course/{courseId}/students/{studentId}` | 添加学生 | `course:manage` |
| DELETE | `/api/course/{courseId}/students/{studentId}` | 移除学生 | `course:manage` |
| GET | `/api/course/{courseId}/students` | 课程学生列表 | `course:read` |

---

## 5. 知识点 `/api/knowledge-point`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/knowledge-point` | 创建 | `knowledge:manage` |
| PUT | `/api/knowledge-point/{id}` | 更新 | `knowledge:manage` |
| DELETE | `/api/knowledge-point/{id}` | 删除 | `knowledge:manage` |
| GET | `/api/knowledge-point` | 列表，`courseId` 必填 | `knowledge:read` |

---

## 6. 试题 `/api/question`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/question` | 创建试题 | `question:manage` |
| PUT | `/api/question/{id}` | 更新（有变更会归档历史版本） | `question:manage` |
| POST | `/api/question/save` | 创建或更新（带 `id` 为更新） | `question:manage` |
| DELETE | `/api/question/{id}` | 删除 | `question:manage` |
| GET | `/api/question` | 分页，`courseId` 必填；可选 `knowledgePointId`、`type`、`keyword`、`reviewStatus`、`page`、`size` | `question:read` |
| GET | `/api/question/{id}` | 详情 | `question:read` |
| POST | `/api/question/batch` | 批量改知识点/难度 | `question:manage` |
| POST | `/api/question/import` | Excel 导入，`courseId` + `multipart file` | `question:manage` |
| GET | `/api/question/export` | Excel 导出（二进制） | `question:read` |
| GET | `/api/question/import/template` | 下载导入模板（二进制） | `question:manage` |
| POST | `/api/question/dedup-check` | 查重，Body 见下表 | `question:read` |
| GET | `/api/question/{id}/versions` | 历史版本列表 | `question:read` |
| GET | `/api/question/{id}/versions/{versionNo}` | 某一历史快照 | `question:read` |
| POST | `/api/question/{id}/submit-review` | 提交审核 | `question:manage` |
| POST | `/api/question/{id}/approve` | 审核通过 | **仅 ADMIN** |
| POST | `/api/question/{id}/reject` | 驳回 | **仅 ADMIN** |

**查重请求体 `QuestionDedupCheckRequest`（主要字段）**

| 字段 | 类型 | 说明 |
|------|------|------|
| `courseId` | long | 必填 |
| `stem` | string | 必填，待比对题干 |
| `optionsJson` | string | 可选 |
| `excludeQuestionId` | long | 编辑时排除本题 ID |
| `knowledgePointId` | long | 可选，仅同知识点下比对 |
| `threshold` | double | 可选，默认约 `0.78` |

---

## 7. 试卷 `/api/paper`、组卷模板 `/api/paper-template`

### 7.1 试卷

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/paper/manual` | 手工组卷 | `paper:manage` |
| POST | `/api/paper/auto-generate` | 自动组卷 | `paper:manage` |
| POST | `/api/paper/from-template/{templateId}` | 按模板生成，`Body`：`title`、`randomSeed` | `paper:manage` |
| GET | `/api/paper/generation-logs` | 组卷审计分页，`courseId`、`page`、`size` | `paper:read` |
| GET | `/api/paper` | 试卷分页，`courseId`、`page`、`size` | `paper:read` |
| GET | `/api/paper/{id}` | 试卷详情（含题目行） | `paper:read` |
| DELETE | `/api/paper/{id}` | 删除 | `paper:manage` |

### 7.2 组卷模板

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/paper-template` | 创建模板，`courseId`、`name`、`rules`（同自动组卷规则） | `paper:manage` |
| PUT | `/api/paper-template/{id}` | 更新 | `paper:manage` |
| DELETE | `/api/paper-template/{id}` | 删除 | `paper:manage` |
| GET | `/api/paper-template` | 列表，`courseId` | `paper:read` |
| GET | `/api/paper-template/{id}` | 详情 | `paper:read` |

---

## 8. 考试 `/api/exam` 与阅卷 `/api/exam/grading`

### 8.1 考试

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/exam` | 创建考试 | `exam:manage` |
| PUT | `/api/exam/{id}` | 更新 | `exam:manage` |
| POST | `/api/exam/{id}/publish` | 发布 | `exam:manage` |
| POST | `/api/exam/{id}/end` | 结束考试 | `exam:manage` |
| POST | `/api/exam/{id}/publish-score` | 发布成绩 | `exam:manage` |
| POST | `/api/exam/{id}/unpublish-score` | 撤销成绩发布 | `exam:manage` |
| GET | `/api/exam/teacher` | 教师端考试分页，`courseId` | `exam:manage` |
| GET | `/api/exam/student` | 学生端可参加考试分页 | **STUDENT** |
| GET | `/api/exam/{id}` | 考试详情 | `exam:manage` 或 `exam:student` |
| POST | `/api/exam/{id}/start` | 开始答题，返回 `recordId` 等 | **STUDENT** |
| GET | `/api/exam/record/{recordId}/questions` | 答题题目列表 | `exam:manage` 或 `exam:student` |
| GET | `/api/exam/record/{recordId}/summary` | 答卷摘要（分数、名次、及格、切屏等） | `exam:manage` 或 `exam:student` |
| POST | `/api/exam/record/{recordId}/save-answers` | 保存答案 | **STUDENT** |
| POST | `/api/exam/record/{recordId}/submit` | 交卷 | **STUDENT** |
| POST | `/api/exam/record/{recordId}/blur` | 切屏/失焦上报 | **STUDENT** |

### 8.2 阅卷

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/exam/grading/subjective` | 主观题评分，`SubjectiveGradeRequest` | `exam:manage` |

---

## 9. 成绩统计 `/api/exam/analytics`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/exam/analytics/overview` | 考试总览，`examId` | `exam:analytics` |
| GET | `/api/exam/analytics/rank` | 排名分页，`examId`、`page`、`size` | `exam:analytics` |
| GET | `/api/exam/analytics/question-stats` | 逐题统计，`examId` | `exam:analytics` |
| GET | `/api/exam/analytics/knowledge-point-stats` | 知识点统计，`examId` | `exam:analytics` |
| GET | `/api/exam/analytics/export-rank` | 导出排名（二进制） | `exam:manage` |

---

## 10. 错题本 `/api/wrong-book`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/wrong-book` | 分页，`courseId`、`page`、`size` | `wrongbook:read` |

---

## 11. 文件 `/api/file`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/file/upload` | 上传，`multipart/form-data` 字段名 `file` | 已登录 |
| GET | `/api/file/download/{id}` | 下载（二进制，`Content-Disposition`） | 已登录 |

成功上传时 `data` 含 `id`、`downloadUrl` 等（见 `FileUploadVO`）。

---

## 12. 运维

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/actuator/health` | 健康检查（匿名） |

---

## 13. 数据库与功能依赖说明

部分能力依赖数据库字段/表，需按 `backend/doc/` 下迁移脚本升级（如 `review_status`、`version_no`、`question_version`、`paper_template`、`paper_generation_log` 等）。详见 **`backend/doc/优化说明.md`**。

---

## 14. 与 Swagger 的关系

- 本文档为 **人工维护的总览**，便于答辩与交付。  
- **字段级**、**DTO 级**细节以代码与 **Swagger UI** 为准；复杂请求体请在后端 DTO 或 Swagger 中查看。
