# 文件导入导出与 AI 模块（Wave 5：阶段 6）架构重构完成报告

## 一、重构范围与目标

本次 **Wave 5（阶段 6：文件导入导出与 AI 模块重构）** 覆盖 `FileController` 的 **9 个文件批量导入与统计导出接口（`FILE-001`～`FILE-009`）** 与 `AIController` 的 **1 个 AI 问答接口（`AI-001`）**，共 **10 个 HTTP 接口**。

重构严格遵循“**仅做上层逻辑架构迁移，零业务语义改动**”原则：
1. 100% 保持 `/file/**` 与 `/ai/send` 路由、HTTP 方法、请求参数、CSV 表头与编码格式、CSV 公式注入防御（`sanitizeCsvCell`）及业务状态码兼容。
2. 将 `FileController` 中最后 1 处活跃的 `TransactionTemplate` 下沉至 `FileApplicationServiceImpl#uploadFile` 的声明式 `@Transactional(rollbackFor = Exception.class)`，**实现全仓 Controller 层 `TransactionTemplate` 100% 清零（达成 `ARCH-04`）**。
3. 移除 `FileController` 与 `AIController` 中剩余的 5 处手写 `sentineManager.initFlowRules()` + `SphU.entry()` 限流样板代码，并为全部 10 个接口接入 `@SentinelRateLimit` 声明式切面限流，**实现全仓所有 79 个 HTTP 接口的声明式限流迁移闭环（达成 `RATE-001`～`RATE-004`）**。

---

## 二、接口迁移清单（`FILE-001`～`FILE-009` + `AI-001`）

| 编号 | HTTP 方法与路径 | 控制器方法 | 目标应用服务方法 | 事务策略 | Sentinel 资源名 | 默认 QPS |
|---|---|---|---|---|---|---:|
| `FILE-001` | `POST /file/upload` | `FileController#uploadFile` | `FileApplicationService#uploadFile` | `@Transactional(rollbackFor = Exception.class)` | `file.user.import` | `10` |
| `FILE-002` | `POST /file/upload/topic` | `FileController#uploadFileTopic` | `FileApplicationService#uploadFileTopic` | 无写事务（`notyet` 占位） | `file.topic.import` | `10` |
| `FILE-003` | `POST /file/get/select/topic/student/list` | `FileController#getSelectTopicStudentListCsv` | `FileApplicationService#listSelectedStudentTopicCsvRows` | 无写事务（只读查询） | `file.selection.selected-export` | `20` |
| `FILE-004` | `POST /file/get/unselect/topic/student/list` | `FileController#getUnSelectTopicStudentListCsv` | `FileApplicationService#listUnselectedStudentCsvUsers` | 无写事务（只读查询） | `file.selection.unselected-export` | `20` |
| `FILE-005` | `POST /file/export/user_list` | `FileController#exportUserList` | `FileApplicationService#exportUserListRows` | 无写事务（只读查询） | `file.export.user-list` | `20` |
| `FILE-006` | `POST /file/export/topic_list` | `FileController#exportTopicList` | `FileApplicationService#exportTopicListRows` | 无写事务（只读查询） | `file.export.topic-list` | `20` |
| `FILE-007` | `POST /file/export/surplus_topic_list` | `FileController#exportSurplusTopicList` | `FileApplicationService#exportSurplusTopicListRows` | 无写事务（只读查询） | `file.export.surplus-topic-list` | `20` |
| `FILE-008` | `POST /file/export/student_topic_list/en_select` | `FileController#exportStudentTopicListEnSelect` | `FileApplicationService#exportSelectedStudentRows` | 无写事务（只读查询） | `file.export.student-en-select` | `20` |
| `FILE-009` | `POST /file/export/student_topic_list/un_select` | `FileController#exportStudentTopicListUnSelect` | `FileApplicationService#exportUnselectedStudentRows` | 无写事务（只读查询） | `file.export.student-un-select` | `20` |
| `AI-001` | `POST /ai/send` | `AIController#aiSend` | `AIApplicationService#aiSend` | 无写事务（`notyet` 占位） | `ai.chat.send` | `30` |

---

## 三、核心架构交付成果

### 1. 请求模型规范化归位（`model.dto.*` -> `model.request.*`）
- 迁移 `UploadFileRequest` 至 `cn.com.edtechhub.worktopicselection.model.request.file.UploadFileRequest`。
- 迁移 `AiSendRequest` 至 `cn.com.edtechhub.worktopicselection.model.request.ai.AiSendRequest`。
- 移除 `model/dto/file` 与 `model/dto/ai` 下的旧请求类。

### 2. 应用服务下沉与协议解耦（达成 `ARCH-04` 与 `ARCH-05`）
- 新增 `FileApplicationService` / `FileApplicationServiceImpl` 与 `AIApplicationService` / `AIApplicationServiceImpl`。
- `FileApplicationService` 不依赖 `HttpServletRequest` / `HttpServletResponse`：
  - `uploadFile` 使用方法级 `@Transactional(rollbackFor = Exception.class)` 替代原 `FileController` 中的 `TransactionTemplate`。
  - 7 个导出查询方法均为无写事务只读查询，返回结构化数据行，由 `FileController` 统一负责设置 `Content-Type`、`Content-Disposition` 响应头并执行 `sanitizeCsvCell` 安全转义写出 CSV 流。

### 3. 全仓 79 个 HTTP 接口 Sentinel 限流与架构解耦 100% 闭环
- `FileController` 与 `AIController` 均仅保留单一应用服务构造注入依赖，0 `TransactionTemplate`、0 `SentineManager`。
- 全仓 11 个 Controller 共 **79 个 HTTP 接口**全部完成应用服务下沉与 `@SentinelRateLimit` 声明式切面限流。

---

## 四、验证结果

1. **Java 编码规范校验（`verify-style.ps1`）**：Wave 5 新增及修改的 12 个 Java 源码与测试文件全部通过校验（`0 violations found`）。
2. **契约与单元测试覆盖**：
   - 新增 `FileAndAIControllerContractTest` 与 `FileAndAIApplicationServiceImplTest`。
   - 保留并通过原有 `FileControllerTest`（CSV 公式注入转义测试）。
   - 全量后端 **207 个单元测试 100% 通过**。
