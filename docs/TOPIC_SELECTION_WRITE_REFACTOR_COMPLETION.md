# 选题写用例重构与三层架构解耦完成记录

> 完成日期：2026-09-27  
> 实施状态：已完成  
> 接口基线：项目共 79 个 HTTP 接口，本次完成选题写用例 4 个（`SEL-001`～`SEL-004`）  
> 关联台账：[HTTP_API_REFACTOR_STATUS.md](./HTTP_API_REFACTOR_STATUS.md)

## 1. 完成结论

本次重构已将学生预选/取消预选、学生确认最终选题、教师双选确认学生及教师/学生退选 4 个高风险悲观锁写用例从原 `UserController` 中完整剥离，并完成以下三层架构解耦与横切升级：

- 新建独立的 `TopicSelectionController`（`@RequestMapping("/user")`），100% 保持既有 `/user/**` HTTP 路径、请求字段、响应格式与业务错误码不变，前端零改动（严格符合 `ARCH-06`）。
- 原 `UserController` 中的 4 个写接口方法及专属内部辅助方法（`confirmStudentSelection`、`assignStudentSelection`、`withdrawSelection`、`validateStudentTopicGroup`、`isStudentAllowedCrossSelect`）已物理删除。
- 复用认证模块建立的三层 Spring AOP 横切基础设施：
  - `SentinelRateLimitAspect`（`order = -300`）：以稳定资源名包裹完整 Controller 方法并统计 RT 与异常。
  - Sa-Token 官方鉴权 Advisor（`order = -200`）：执行 `@SaCheckLogin` 与 `@SaCheckRole` 鉴权。
  - `RequestDtoValidationAspect`（`order = -100`）：通过 `@ValidateRequest` 触发 `@RequestBody` 请求模型的基础字段约束校验。
- 新建 `TopicSelectionApplicationService` 与 `TopicSelectionApplicationServiceImpl`：
  - 将原 `UserController` 中的 4 处 `TransactionTemplate` 编程式事务下沉为 `TopicSelectionApplicationServiceImpl` 公开写方法上的 `@Transactional(rollbackFor = Exception.class)`（不使用类级 `@Transactional`，落实 `ARCH-05`）。
  - 零业务逻辑变更，严格保持原有的数据库悲观行锁顺序（`User -> Topic -> StudentTopicSelection`）与事务原子性不变。
- 请求模型从 `model.dto` 迁移至 `model.request.selection`（`SelectTopicByIdRequest`、`SelectStudentRequest`、`WithdrawRequest`），并按 `java-coding-conventions` 规范补全 Javadoc 与校验注解。

## 2. 接口迁移结果

### 2.1 迁移后的选题写用例接口

| ID | 方法 | 路径（保持兼容） | 鉴权要求 | 请求模型 | 主要响应数据 |
|---|---|---|---|---|---|
| `SEL-001` | POST | `/user/preselect/topic/by/id` | 登录 + `student` 角色 | `SelectTopicByIdRequest {id,status}` | `Long`（操作关联题目 ID） |
| `SEL-002` | POST | `/user/select/topic/by/id` | 登录 + `student` 角色 | `SelectTopicByIdRequest {id,status}` | `Long`（选题关联记录 ID） |
| `SEL-003` | POST | `/user/select/student` | 登录 + `teacher` 角色 | `SelectStudentRequest {userAccount,topic}` | `String`（选题关联记录 ID 字符串） |
| `SEL-004` | POST | `/user/withdraw` | 登录 + `teacher` 或 `student` 角色 | `WithdrawRequest {id,userAccount?}` | `Boolean`（是否退选成功） |

### 2.2 Controller 职责对比

| 维度 | 迁移前（`UserController`） | 迁移后（`TopicSelectionController`） |
|---|---|---|
| HTTP 路由 | `/user/preselect/topic/by/id` 等 4 个路由 | 保持完全一致（前端零感知） |
| Sentinel 限流 | 方法内手写 `initFlowRules` + 空 `try (Entry ...) {}` | `@SentinelRateLimit(resource = "topic.selection.*")` 切面完整包围 |
| 参数基礎校验 | 方法内手写 `request == null`、`StringUtils.isBlank` | `@ValidateRequest` 切面 + Service 防御性校验 |
| 事务管理 | Controller 直接调用 `transactionTemplate.execute(...)` | 下沉至 `TopicSelectionApplicationServiceImpl` 公开方法 `@Transactional` |
| 数据访问 | Controller 直接调用 `userMapper`、`topicMapper`、`studentTopicSelectionMapper` | Controller 零 Mapper 依赖，全部收敛至 `TopicSelectionApplicationServiceImpl` |

## 3. 最终请求执行链

选题写请求进入应用后的执行顺序如下：

```text
HTTP 请求
  → Servlet Filter 链
  → DispatcherServlet
  → RequestLoggingInterceptor.preHandle
  → SentinelRateLimitAspect              order = -300
  → Sa-Token 官方鉴权 Advisor             order = -200
  → RequestDtoValidationAspect           order = -100
  → TopicSelectionController
  → TopicSelectionApplicationService 事务代理（@Transactional）
  → TopicSelectionApplicationServiceImpl
  → 悲观行锁（UserMapper → TopicMapper → StudentTopicSelectionMapper）
  → TopicService / StudentTopicSelectionService / RedisManager / MailService
  → Controller 返回 BaseResponse
  → Jackson 序列化为 JSON
```

## 4. Sentinel 限流配置

### 4.1 实现结果

- 在 `SentinelRateLimitProperties` 与 `SentinelRuleRegistry` 中集中注册 4 个选题写用例稳定资源名。
- 支持通过 `application.yaml` 的 `app.sentinel.auth.topic-selection-*` 及环境变量覆盖默认阈值。
- 消除了 `UserController` 中 4 处手写 `sentineManager.initFlowRules(entryName)` 与无效的空 `SphU.entry()` 样板代码。

### 4.2 当前选题写用例资源阈值

| Sentinel 资源 | 对应接口 | 默认单实例 QPS | 环境变量 |
|---|---|---:|---|
| `topic.selection.preselect` | `POST /user/preselect/topic/by/id` | 200 | `APP_SENTINEL_TOPIC_SELECTION_PRESELECT` |
| `topic.selection.confirm` | `POST /user/select/topic/by/id` | 100 | `APP_SENTINEL_TOPIC_SELECTION_CONFIRM` |
| `topic.selection.assign-student` | `POST /user/select/student` | 100 | `APP_SENTINEL_TOPIC_SELECTION_ASSIGN_STUDENT` |
| `topic.selection.withdraw` | `POST /user/withdraw` | 100 | `APP_SENTINEL_TOPIC_SELECTION_WITHDRAW` |

## 5. 事务原子性与悲观锁并发边界

`TopicSelectionApplicationServiceImpl` 100% 保持了原有事务原子性与行锁顺序，确保高并发抢题与退选场景下的数据一致性：

1. **固定加锁顺序防死锁**：
   - 所有写用例统一遵循：先锁学生行 `userMapper.selectByIdForUpdate(studentId)` $\rightarrow$ 再锁课题行 `topicMapper.selectByIdForUpdate(topicId)` $\rightarrow$ 最后锁学生选题记录 `studentTopicSelectionMapper.selectByUserForUpdate(...)`（或 `selectByUserAndTopicForUpdate`）。
   - 避免“学生确认选题”与“教师确认/退选同一学生或同一课题”并发执行时出现循环等待死锁。
2. **方法级 `@Transactional(rollbackFor = Exception.class)`**：
   - 仅在 `preselectTopicById`、`selectTopicById`、`selectStudent`、`withdraw` 四个公开写方法上开启物理事务，内部调用 `TopicService` 和 `StudentTopicSelectionService` 时通过 Spring 默认的 `Propagation.REQUIRED` 加入同一事务，任一步骤抛出 `BusinessException` 均整体回滚。
3. **余量计数与状态机守恒**：
   - **预选（`EN_PRESELECT`）**：递增 `selectAmount`，不扣减 `surplusQuantity`；取消预选（`UN_PRESELECT`）时递减 `selectAmount`（下限为 0），不膨胀 `surplusQuantity`。
   - **学生确认（`EN_SELECT`）**：要求已存在预选记录，将状态由 `EN_PRESELECT` 升级为 `EN_SELECT` 并扣减 1 次 `surplusQuantity`。
   - **教师确认（`selectStudent`）**：若学生尚未预选该题，则新建 `EN_SELECT` 记录、递增 `selectAmount` 并扣减 `surplusQuantity`；若学生已预选该题，则复用原预选记录升级为 `EN_SELECT` 并仅扣减 `surplusQuantity`。
   - **退选（`withdraw`）**：通过 `restoreTopicCounters` 递减 `selectAmount` 并恢复 1 次 `surplusQuantity`，删除关联记录；若为教师操作且学生配置了邮箱，在事务末尾同步发送退选通知邮件。

## 6. 请求模型与编码规范合规

1. **模型归属迁移**：
   - `SelectTopicByIdRequest`、`SelectStudentRequest`、`WithdrawRequest` 统一迁移至 `cn.com.edtechhub.worktopicselection.model.request.selection` 包，旧 `model.dto` 下的重复类已清理。
2. **Javadoc 与代码规范（`java-coding-conventions`）**：
   - `TopicSelectionApplicationService` 接口方法 Javadoc 描述 **WHAT**（对外业务契约，隐藏底层表与锁细节）。
   - `TopicSelectionApplicationServiceImpl` 实现类方法 Javadoc 描述 **HOW**（写明悲观锁顺序、Redis 开关与计数更新机制）。
   - 所有类、方法、字段、`serialVersionUID` 分隔线及单元测试场景/步骤注释均通过 `verify-style.ps1` 零违规校验。

## 7. 测试与验证结果

### 7.1 代码规范校验

执行命令：

```powershell
pwsh "C:/Users/abc/.codex/skills/java-coding-conventions/scripts/verify-style.ps1" -TargetPath "<本次改动文件>"
```

结果：全部 10 个新增与修改的 Java 文件 `VERIFY_PASSED: 0 violations found`。

### 7.2 后端单元测试

执行命令：

```powershell
.\mvnw.cmd test
```

结果：

```text
Tests run: 162
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

新增与更新测试：
- `TopicSelectionControllerContractTest`：验证 4 个选题写接口的路由、HTTP 方法、`@SaCheckLogin`/`@SaCheckRole`、`@SentinelRateLimit`、`@ValidateRequest` 契约，并验证 `UserController` 已不再声明这 4 个路由。
- `UserControllerTopicSelectionTest`：验证 `TopicSelectionApplicationServiceImpl` 的专业选题组匹配、预选与确认分组拦截、悲观锁加锁顺序（`User -> Topic -> StudentTopicSelection`）、重复最终选题拦截、教师复用预选记录及退选余量恢复。

## 8. 完成标准核对

| 完成标准 | 状态 | 结果 |
|---|---|---|
| `SEL-001`～`SEL-004` 移出 `UserController` | 完成 | 4 个接口及专属辅助方法已物理删除 |
| `TopicSelectionController` 不包含业务实现 | 完成 | 仅接收请求、调用 `TopicSelectionApplicationService`、包装响应 |
| `TopicSelectionController` 不访问 Mapper、Redis、`SphU`、事务模板 | 完成 | 零基础设施依赖 |
| 保持 `/user/**` 路由与请求/响应契约兼容 | 完成 | 前端无需改动 |
| 事务下沉至 Service 方法级 `@Transactional` | 完成 | 消除 `UserController` 中 4 处 `TransactionTemplate` 调用 |
| 悲观锁加锁顺序与事务原子性保持不变 | 完成 | 通过 `InOrder` 锁顺序与容量守恒单元测试验证 |
| 遵循 `java-coding-conventions` 规范 | 完成 | `verify-style.ps1` 全部通过（0 violations） |
