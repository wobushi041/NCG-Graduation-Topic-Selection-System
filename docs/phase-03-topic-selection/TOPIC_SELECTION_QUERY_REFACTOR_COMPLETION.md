# 选题查询用例重构与选题域全量闭环完成记录

> 完成日期：2026-09-27  
> 实施状态：已完成  
> 接口基线：项目共 79 个 HTTP 接口，本次完成选题读用例 5 个（`SEL-005`～`SEL-009`），使整个“学生选题与教师确认域（9 个接口）” 100% 闭环  
> 关联文档：[HTTP_API_REFACTOR_STATUS.md](./HTTP_API_REFACTOR_STATUS.md)、[TOPIC_SELECTION_WRITE_REFACTOR_COMPLETION.md](./TOPIC_SELECTION_WRITE_REFACTOR_COMPLETION.md)

## 1. 完成结论

继完成 `SEL-001`～`SEL-004` 四个高风险悲观锁写用例重构后，本次重构将剩余 5 个选题查询接口（`SEL-005`～`SEL-009`）从原 `UserController` 中完整剥离至 `TopicSelectionController` 与新建的只读服务 `TopicSelectionQueryService`，达成整个“学生选题与教师确认”业务域（共 9 个接口）的 100% 三层架构解耦闭环：

- `TopicSelectionController` 现已完整承载全部 9 个选题域 HTTP 接口（4 个写用例 + 5 个读用例），保持 `/user/**` 路由、JSON 请求字段与 `BaseResponse` 响应契约 100% 兼容，前端零改动（严格符合 `ARCH-06`）。
- 顺带修复原 `UserController.getSelectTopic` 上 `@PostMapping("get/select/topic")` 缺少前导 `/` 的规范问题，统一为 `@PostMapping("/get/select/topic")`。
- 新建 `TopicSelectionQueryService` 与 `TopicSelectionQueryServiceImpl`：
  - 严格落实读写分离与 `ARCH-05` 原则：读用例服务**不标注**类级或方法级 `@Transactional` 写事务，避免纯查询请求无差别进入写事务代理。
  - 零业务逻辑变更，完整保留教师课题归属权校验（`isTopicOwner`）、学生预选/最终选题查询及秒级确认时间戳转换逻辑。
- 查询请求模型统一迁移至 `model.request.selection`（`GetSelectTopicByIdRequest`、`GetSelectTopicRequest`、`GetStudentByTopicIdRequest`），消除了原 `model.dto` 下非实体类误加 `@TableField(exist = false)` 的遗留问题。
- 5 个查询接口全部接入 `@SentinelRateLimit`（`order = -300`）、Sa-Token 官方鉴权 Advisor（`order = -200`）与 `@ValidateRequest` DTO 校验切面（`order = -100`），从 `UserController` 进一步清除了 5 处手写 Sentinel 样板代码。

## 2. 接口迁移结果

### 2.1 本次迁移的选题查询接口（`SEL-005`～`SEL-009`）

| ID | 方法 | 路径（保持兼容） | 鉴权要求 | 请求模型 | 主要响应数据 |
|---|---|---|---|---|---|
| `SEL-005` | POST | `/user/get/select/topic/by/id` | 登录 + `teacher` 角色 | `GetSelectTopicByIdRequest {id}` | `List<User>`（已选该题目的学生列表） |
| `SEL-006` | POST | `/user/get/preselect/topic` | 登录 + `student` 角色 | 无 | `List<Topic>`（当前学生预选题目列表） |
| `SEL-007` | POST | `/user/get/select/topic` | 登录 + `student` 角色 | 无 | `List<Topic>`（当前学生最终确认题目列表） |
| `SEL-008` | POST | `/user/get/select/topic/choice_time` | 登录 + `student` 角色 | `GetSelectTopicRequest {topicId}` | `String`（最终选题确认秒级时间戳） |
| `SEL-009` | POST | `/user/get/student/by/topicId` | 登录 + `teacher` 角色 | `GetStudentByTopicIdRequest {id}` | `List<User>`（已选择该题目的学生列表） |

### 2.2 选题域（`SEL-001`～`SEL-009`）全量闭环总览

| 子模块 | 接口 ID | 承载 Controller | 承载 Service | 事务策略 |
|---|---|---|---|---|
| 选题写用例（4） | `SEL-001`～`SEL-004` | `TopicSelectionController` | `TopicSelectionApplicationService` | 公开写方法级 `@Transactional(rollbackFor = Exception.class)` + 固定顺序悲观锁 |
| 选题读用例（5） | `SEL-005`～`SEL-009` | `TopicSelectionController` | `TopicSelectionQueryService` | 无写事务代理（纯只读查询） |

## 3. 最终请求执行链（读用例）

```text
HTTP 请求
  → Servlet Filter 链
  → DispatcherServlet
  → RequestLoggingInterceptor.preHandle
  → SentinelRateLimitAspect              order = -300
  → Sa-Token 官方鉴权 Advisor             order = -200
  → RequestDtoValidationAspect           order = -100（有 @RequestBody 时触发）
  → TopicSelectionController
  → TopicSelectionQueryServiceImpl（无写事务包装）
  → TopicService / StudentTopicSelectionService / UserService
  → Controller 返回 BaseResponse
  → Jackson 序列化为 JSON
```

## 4. Sentinel 限流配置扩展

### 4.1 当前选题读用例资源阈值

| Sentinel 资源 | 对应接口 | 默认单实例 QPS | 环境变量 |
|---|---|---:|---|
| `topic.selection.query-selected-students` | `POST /user/get/select/topic/by/id` | 300 | `APP_SENTINEL_TOPIC_SELECTION_QUERY_SELECTED_STUDENTS` |
| `topic.selection.query-preselected` | `POST /user/get/preselect/topic` | 300 | `APP_SENTINEL_TOPIC_SELECTION_QUERY_PRESELECTED` |
| `topic.selection.query-selected` | `POST /user/get/select/topic` | 300 | `APP_SENTINEL_TOPIC_SELECTION_QUERY_SELECTED` |
| `topic.selection.query-choice-time` | `POST /user/get/select/topic/choice_time` | 300 | `APP_SENTINEL_TOPIC_SELECTION_QUERY_CHOICE_TIME` |
| `topic.selection.query-students-by-topic` | `POST /user/get/student/by/topicId` | 300 | `APP_SENTINEL_TOPIC_SELECTION_QUERY_STUDENTS_BY_TOPIC` |

至此，`SentinelRuleRegistry` 启动时共集中注册并发布 **19 条** 稳定资源规则（认证域 10 条 + 选题写用例 4 条 + 选题读用例 5 条）。

## 5. 测试与验证结果

### 5.1 代码规范校验（`java-coding-conventions`）

执行命令：

```powershell
pwsh "C:/Users/abc/.codex/skills/java-coding-conventions/scripts/verify-style.ps1" -TargetPath "<本次改动文件>"
```

结果：全部 13 个新增与修改的 Java 文件 `VERIFY_PASSED: 0 violations found`。

### 5.2 后端单元测试

执行命令：

```powershell
.\mvnw.cmd test
```

结果：

```text
Tests run: 165
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

新增与更新测试覆盖：
- `TopicSelectionControllerContractTest`：验证 `TopicSelectionController` 暴露全部 9 个选题接口（4 写 + 5 读），全部配备 `@SentinelRateLimit`、`@SaCheckLogin`/`@SaCheckRole`，带 `@RequestBody` 的接口均配备 `@ValidateRequest`，且 `UserController` 已不再声明这 9 个路由。
- `TopicSelectionQueryServiceTest`：验证教师查询本人课题已选学生、非归属教师越权访问拦截、学生查询预选课题及秒级确认时间戳转换。

## 6. 完成标准核对

| 完成标准 | 状态 | 结果 |
|---|---|---|
| `SEL-005`～`SEL-009` 移出 `UserController` | 完成 | 5 个读方法已物理删除，`UserController` 进一步瘦身 |
| 选题域 9 个接口全量归口 `TopicSelectionController` | 完成 | 4 写 + 5 读全部收敛完成 |
| 读用例不开启无差别写事务（`ARCH-05`） | 完成 | `TopicSelectionQueryServiceImpl` 无 `@Transactional` 代理 |
| 请求模型统一规范命名并迁入 `model.request.selection` | 完成 | 6 个选题请求模型全部位于 `model.request.selection` |
| 遵循 `java-coding-conventions` 规范 | 完成 | `verify-style.ps1` 全部通过（0 violations） |
