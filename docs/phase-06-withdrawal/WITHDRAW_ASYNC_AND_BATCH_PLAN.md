# 退选异步通知与批量退选实施计划

> 状态：计划已确认，尚未实施  
> 制定日期：2026-09-30  
> 适用范围：毕业选题管理系统后端退选链路及教师端“查看已选学生”页面

## 一、背景与目标

当前 `POST /user/withdraw` 在数据库事务中同步调用 SMTP 邮件服务。教师退选已绑定邮箱的学生时，请求线程必须等待邮件发送完成，期间学生、题目和选题记录的数据库行锁不能释放。因此，SMTP 响应慢会直接拖慢接口；同一道题目的并发退选还可能因等待题目行锁进一步放大延迟。

当前教师端“查看已选学生”页面只支持逐行退选，后端也只有单条退选接口。若前端循环调用单条接口实现批量操作，会产生多次 HTTP 请求、多次锁定同一道题目以及部分成功、部分失败的状态。

本计划包含两个目标：

1. 将退选通知从事务内同步发送改为事务提交后的异步发送。
2. 新增教师批量退选接口，并在“查看已选学生”页面提供多选及批量退选操作。

## 二、当前实现基线

### 2.1 单条退选接口

```http
POST /user/withdraw
Content-Type: application/json

{
  "id": 123,
  "userAccount": "2023001"
}
```

- 接口允许教师或学生调用。
- 学生只能退选自己的题目，服务端忽略空的 `userAccount` 并使用当前登录账号。
- 教师必须提供 `userAccount`，且只能退选本人题目下的学生。
- 单条接口的 `id` 字段保持不变，不在本次改造中重命名，避免破坏现有前端契约。
- 现有集成测试错误地向该接口传递 `topicId`，实施时需要修正为 `id`。

### 2.2 当前事务和外部调用

单条退选当前执行以下步骤：

1. 查询当前登录用户。
2. 按学生账号查询学生。
3. 使用 `FOR UPDATE` 锁定学生记录。
4. 使用 `FOR UPDATE` 锁定题目记录。
5. 使用 `(userAccount, topicId)` 联合条件锁定最终选题记录。
6. 更新题目余量与选题人数。
7. 删除学生选题记录。
8. 教师操作且学生已绑定邮箱时，同步调用 `mailSender.send(...)`。
9. 提交事务并返回 HTTP 响应。

现有查询均有主键、唯一索引或联合唯一索引支持。常规单请求下，主要延迟风险是同步 SMTP 调用；并发场景下还存在行锁等待。

## 三、确定的设计决策

### 3.1 异步通知语义

1. 数据库退选是核心业务，邮件是提交后的附加通知。
2. 只有数据库事务成功提交后才允许创建异步邮件任务。
3. 邮件发送失败只记录错误日志，不回滚已经完成的退选。
4. 学生自主退选继续不发送邮件，保持当前行为。
5. 教师退选且学生未绑定邮箱时不创建邮件任务。
6. 邮件事件携带收件地址、题目名称和操作人名称，异步线程不再查询业务数据库。
7. 单条退选发布包含一个收件人的通知事件；批量退选发布一个包含多个收件人的通知事件，避免一次批量操作创建大量独立线程任务。

本方案使用进程内线程池，属于尽力通知。应用在任务执行前退出时，尚未发送的邮件可能丢失。如果将来要求可靠投递，需要新增 Outbox 表或消息队列，不在本次范围内。

### 3.2 批量退选语义

1. 批量接口只允许教师角色调用。
2. 一次请求只处理同一道题目下的学生。
3. 单批最多接收 100 个学生账号。
4. 对账号进行去空格、去重检查和稳定排序。
5. 批量操作采用单个原子事务。
6. 任一学生不存在、不是学生角色、未最终选择该题目或题目不属于当前教师时，整批回滚。
7. 全部校验通过后统一恢复题目计数，并批量删除选题记录。
8. 接口成功只表示退选事务成功，不表示所有通知邮件已经发送成功。

选择原子事务而非部分成功，是为了避免前端无法准确判断哪些学生已退选，也避免重试时产生复杂的幂等与计数恢复问题。

### 3.3 并发和锁顺序

单条和批量实现必须采用一致的锁顺序：

```text
按账号排序锁定学生
  -> 锁定题目
  -> 按账号排序锁定选题记录
  -> 更新题目计数
  -> 删除选题记录
```

批量查询必须使用稳定的 `ORDER BY`，降低两个重叠批次以不同顺序锁定学生造成死锁的风险。同一道题目的并发请求仍会串行等待题目行锁，这是维护余量一致性的预期行为。

## 四、目标接口契约

### 4.1 请求

```http
POST /user/withdraw/batch
Content-Type: application/json

{
  "topicId": 123,
  "userAccounts": [
    "2023001",
    "2023002"
  ]
}
```

字段规则：

| 字段 | 类型 | 规则 |
|---|---|---|
| `topicId` | `Long` | 必填且为正整数 |
| `userAccounts` | `List<String>` | 必填，数量为 1 至 100 |
| `userAccounts[]` | `String` | 去除首尾空格后不能为空，不允许重复 |

### 4.2 成功响应

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "topicId": 123,
    "withdrawnCount": 2
  }
}
```

### 4.3 失败语义

- 使用现有 `BaseResponse` 和业务状态码，不将业务错误强制映射为 HTTP 4xx/5xx。
- 任一目标账号校验失败时返回明确消息，并保证数据库没有部分提交。
- 邮件异步发送失败不改变成功响应，也不修改数据库事务结果。

## 五、目标异步通知流程

```text
POST /user/withdraw 或 POST /user/withdraw/batch
  -> Controller 鉴权、限流、DTO 校验
  -> TopicSelectionApplicationService 开启事务
  -> 锁定并校验学生、题目和选题记录
  -> 更新题目计数并删除选题记录
  -> 发布 TopicWithdrawalNotificationEvent
  -> 提交数据库事务
  -> HTTP 返回成功
  -> AFTER_COMMIT 监听器提交到 mailTaskExecutor
  -> 异步调用 MailService 逐个发送通知
  -> 发送失败记录日志，不影响退选结果
```

## 六、文件级实施方案

### 6.1 异步执行基础设施

新增：

```text
nfu-graduation-topic-selection-backend/src/main/java/cn/edu/nfu/topicselection/config/AsyncConfig.java
```

职责：

- 使用 `@EnableAsync` 开启 Spring 异步代理。
- 注册名称为 `mailTaskExecutor` 的独立 `ThreadPoolTaskExecutor`。
- 建议初始配置：核心线程 2、最大线程 4、队列容量 200。
- 配置可识别的线程名前缀和异步异常日志处理。
- 明确拒绝策略；默认建议记录拒绝并保护主业务响应，不让已经提交的退选被表现为失败。

修改：

```text
nfu-graduation-topic-selection-backend/src/main/resources/application.yaml
```

为 SMTP 增加有限超时，建议初始值：

```yaml
spring:
  mail:
    properties:
      mail:
        smtp:
          connectiontimeout: 5000
          timeout: 10000
          writetimeout: 10000
```

### 6.2 事务提交后通知事件

新增：

```text
nfu-graduation-topic-selection-backend/src/main/java/cn/edu/nfu/topicselection/event/TopicWithdrawalNotificationEvent.java
nfu-graduation-topic-selection-backend/src/main/java/cn/edu/nfu/topicselection/event/TopicWithdrawalNotification.java
nfu-graduation-topic-selection-backend/src/main/java/cn/edu/nfu/topicselection/event/TopicWithdrawalNotificationListener.java
```

其中：

- `TopicWithdrawalNotification` 保存单个收件人的邮箱、题目名称和操作人名称。
- `TopicWithdrawalNotificationEvent` 保存本次事务产生的通知列表。
- Listener 使用 `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`。
- Listener 使用 `@Async("mailTaskExecutor")`，在独立线程中调用现有 `MailService.sendTopicMail(...)`。
- 单个收件人发送失败时记录收件账号或脱敏邮箱及异常，继续处理本批次后续通知。
- 不使用 `fallbackExecution = true`，防止无事务调用时绕过提交边界。

`MailService` 和 `MailServiceImpl` 继续保留同步邮件发送原语；异步边界放在事件监听器，而不是直接给 `sendTopicMail` 添加 `@Async`。这样可以明确控制事务提交顺序，也便于测试和复用。

### 6.3 请求与响应模型

新增：

```text
nfu-graduation-topic-selection-backend/src/main/java/cn/edu/nfu/topicselection/model/request/selection/BatchWithdrawRequest.java
nfu-graduation-topic-selection-backend/src/main/java/cn/edu/nfu/topicselection/model/vo/BatchWithdrawResultVO.java
```

要求：

- DTO 使用 Bean Validation 约束基础字段。
- Service 层再次执行去空格、重复账号、角色和关联关系校验。
- 遵守项目 Java 编码规范，补齐类、字段和序列化字段 Javadoc。

### 6.4 Controller 和 Sentinel

修改：

```text
nfu-graduation-topic-selection-backend/src/main/java/cn/edu/nfu/topicselection/controller/TopicSelectionController.java
nfu-graduation-topic-selection-backend/src/main/java/cn/edu/nfu/topicselection/manager/sentinel/SentinelRateLimitProperties.java
nfu-graduation-topic-selection-backend/src/main/java/cn/edu/nfu/topicselection/manager/sentinel/SentinelRuleRegistry.java
nfu-graduation-topic-selection-backend/src/main/resources/application.yaml
```

新增 Controller 方法：

```java
POST /user/withdraw/batch
@SaCheckLogin
@SaCheckRole(value = {"teacher"}, mode = SaMode.OR)
@ValidateRequest
@SentinelRateLimit(resource = "topic.selection.withdraw-batch")
```

新增 Sentinel 配置项：

```yaml
topic-selection-withdraw-batch: ${APP_SENTINEL_TOPIC_SELECTION_WITHDRAW_BATCH:20}
```

批量接口阈值低于单条接口，因为单次请求会处理多名学生。

### 6.5 Service 和持久层

修改：

```text
nfu-graduation-topic-selection-backend/src/main/java/cn/edu/nfu/topicselection/service/TopicSelectionApplicationService.java
nfu-graduation-topic-selection-backend/src/main/java/cn/edu/nfu/topicselection/service/impl/TopicSelectionApplicationServiceImpl.java
nfu-graduation-topic-selection-backend/src/main/java/cn/edu/nfu/topicselection/mapper/UserMapper.java
nfu-graduation-topic-selection-backend/src/main/java/cn/edu/nfu/topicselection/mapper/StudentTopicSelectionMapper.java
nfu-graduation-topic-selection-backend/src/main/resources/mapper/UserMapper.xml
nfu-graduation-topic-selection-backend/src/main/resources/mapper/StudentTopicSelectionMapper.xml
```

Service 接口新增：

```java
BatchWithdrawResultVO batchWithdraw(BatchWithdrawRequest request);
```

实现要求：

1. 抽取单条和批量都可复用的权限、锁定、选题记录校验和计数恢复辅助逻辑。
2. 不通过同类内部调用 `withdraw(...)` 实现批量操作，避免重复事务边界和 Spring 代理失效问题。
3. 批量锁定学生和选题记录，禁止循环执行 N 次单条 SQL。
4. 锁定结果数量必须与请求账号数量一致，否则整批失败。
5. 所有选题记录必须是最终确认状态。
6. 题目的 `selectAmount` 和 `surplusQuantity` 按实际退选数量一次更新。
7. 使用批量逻辑删除选题记录。
8. 事务内仅组装并发布通知事件，不执行 SMTP I/O。

### 6.6 前端 API 和类型

修改：

```text
nfu-graduation-topic-selection-frontend/src/services/topic-selection/topicSelectionController.ts
nfu-graduation-topic-selection-frontend/src/services/topic-selection/typings.d.ts
```

新增：

```typescript
batchWithdrawUsingPost(body: API.BatchWithdrawRequest)
```

类型包括：

```text
BatchWithdrawRequest
BatchWithdrawResultVO
BaseResponseBatchWithdrawResultVO_
```

保持现有 `withdrawUsingPost` 不变。

### 6.7 教师端页面

修改：

```text
nfu-graduation-topic-selection-frontend/src/pages/SelectStudentList/index.tsx
```

页面调整：

1. 为 `ProTable` 增加 `rowSelection`。
2. 使用学生 `id` 作为稳定 `rowKey`，使用 `userAccount` 组装接口参数。
3. 工具栏新增“批量退选”按钮，未选择学生时禁用。
4. 点击后弹出确认框，明确显示题目和选中人数，并提示操作不可恢复。
5. 确认后只发送一次 `POST /user/withdraw/batch`。
6. 成功后清空已选行并重新加载表格。
7. 失败时保留已选行并展示后端错误信息，便于用户修正或重试。
8. 保留每行原有单条退选入口。
9. 移除当前工具栏中无业务作用的示例下拉菜单。
10. 开启跨分页保留选择时，只保留当前题目查询结果中的有效学生账号。

## 七、测试计划

### 7.1 后端单元测试

覆盖以下场景：

1. 教师单条退选成功后发布一个通知事件，不直接调用邮件服务。
2. 学生自主退选不发布通知事件。
3. 学生未绑定邮箱时不发布通知事件。
4. 事务回滚时 Listener 不执行邮件发送。
5. 事务提交后 Listener 在 `mailTaskExecutor` 线程执行。
6. 某个收件人发送失败时继续发送后续通知。
7. 批量请求为空、超过 100 项、包含空账号或重复账号时失败。
8. 批量请求包含不存在学生时整批回滚。
9. 批量请求包含非最终选题记录时整批回滚。
10. 非题目所属教师调用时失败。
11. 成功批量退选后题目计数和删除数量正确。

### 7.2 Controller 契约测试

修改：

```text
nfu-graduation-topic-selection-backend/src/test/java/cn/edu/nfu/topicselection/controller/TopicSelectionControllerContractTest.java
```

验证：

- 选题 Controller 接口数量由 9 个更新为 10 个。
- 新接口存在 `@SaCheckLogin`、教师角色限制、`@ValidateRequest` 和 Sentinel 注解。
- 旧单条接口契约保持不变。

### 7.3 集成测试

修改或新增：

```text
integration-tests/src/test/java/cn/edu/nfu/topicselection/integration/selection/TopicSelectionFlowIT.java
integration-tests/src/test/java/cn/edu/nfu/topicselection/integration/selection/TopicBatchWithdrawIT.java
integration-tests/src/test/java/cn/edu/nfu/topicselection/integration/documentation/Knife4jDocumentationIT.java
```

验证：

1. 修正现有单条退选测试，将请求字段从错误的 `topicId` 改为 `id`。
2. 教师成功批量退选两个已选学生。
3. 批量成功后两条选题记录均删除，题目计数恢复正确。
4. 一个账号无效时整批没有任何数据库变化。
5. 两个重叠批次并发执行时不会造成题目余量超出容量或重复恢复。
6. Knife4j 文档包含 `POST /user/withdraw/batch`。
7. 集成测试 Mock 邮件服务，不连接真实 SMTP。

### 7.4 前端测试

验证：

1. 未选择学生时批量按钮禁用。
2. 选择多名学生后请求只发送一次，并包含正确 `topicId` 和账号数组。
3. 用户取消确认时不发送请求。
4. 成功后清空选择并刷新列表。
5. 失败后保留选择并显示错误信息。
6. 原有单条退选仍可正常使用。

## 八、实施顺序

### 阶段一：锁定现有行为

1. 补充单条退选事件发布和计数恢复测试。
2. 修正集成测试中的 `id/topicId` 字段错误。
3. 确认单条退选现有权限和锁定行为全部通过。

### 阶段二：异步邮件改造

1. 增加线程池配置和 SMTP 超时。
2. 增加退选通知事件及监听器。
3. 将单条退选中的同步邮件调用替换为事件发布。
4. 验证事务提交、回滚和邮件失败边界。

### 阶段三：批量后端接口

1. 增加请求和响应模型。
2. 增加批量加锁 Mapper。
3. 重构单条与批量共享的领域辅助逻辑。
4. 增加 Service、Controller 和 Sentinel 配置。
5. 完成单元测试、契约测试和集成测试。

### 阶段四：前端批量操作

1. 增加 API 方法和 TypeScript 类型。
2. 增加表格选择、批量按钮和确认流程。
3. 删除示例工具栏菜单。
4. 完成前端测试、类型检查和构建验证。

### 阶段五：性能与并发验收

1. 使用绑定邮箱的学生测量改造前后单条接口耗时。
2. 将邮件服务模拟延迟设为 3 秒，确认退选 HTTP 响应不等待邮件完成。
3. 对同一道题目执行并发单条和批量退选，确认计数正确且没有死锁。
4. 检查线程池队列、拒绝日志和邮件失败日志。

## 九、验收标准

- 单条退选的 URL、请求字段、权限和业务状态码保持兼容。
- 教师退选的 HTTP 响应不再等待 SMTP 发送完成。
- 数据库回滚时不发送退选邮件。
- 邮件失败不回滚或伪装成退选失败。
- 批量接口一次请求可以原子退选 1 至 100 名学生。
- 批量失败时数据库不存在部分更新。
- 题目余量和选题人数在单条、批量及并发场景下保持一致。
- 前端支持跨当前列表选择学生、确认批量退选和结果刷新。
- Knife4j 文档、Controller 契约、前端类型和实际接口一致。
- Java 规范检查、后端测试、集成测试、前端检查和生产构建全部通过。

## 十、验证命令

```powershell
pwsh "C:/Users/abc/.codex/skills/java-coding-conventions/scripts/verify-style.ps1" `
  -TargetPath "nfu-graduation-topic-selection-backend/src/main/java"

pwsh "C:/Users/abc/.codex/skills/java-coding-conventions/scripts/verify-style.ps1" `
  -TargetPath "nfu-graduation-topic-selection-backend/src/test/java"

pwsh "C:/Users/abc/.codex/skills/java-coding-conventions/scripts/verify-style.ps1" `
  -TargetPath "integration-tests/src/test/java"

.\mvnw.cmd test
.\mvnw.cmd verify

pnpm --dir nfu-graduation-topic-selection-frontend tsc
pnpm --dir nfu-graduation-topic-selection-frontend lint:js
pnpm --dir nfu-graduation-topic-selection-frontend test
pnpm --dir nfu-graduation-topic-selection-frontend build
```

## 十一、建议提交序列

```text
test: lock current withdrawal behavior
refactor: send withdrawal notifications after commit
feat: add atomic batch withdrawal API
feat(frontend): add batch withdrawal action
test: verify withdrawal performance and concurrency
```

每个提交完成后运行与该阶段匹配的测试。不得通过循环调用单条接口代替批量后端实现，不得把 SMTP 调用重新放入数据库事务，也不得删除现有单条退选入口。

## 十二、非本次范围

- 不引入 RabbitMQ、Kafka 等消息中间件。
- 不实现持久化邮件重试和管理后台补发。
- 不修改学生自主退选规则和退选锁定策略。
- 不修改题目容量、选题状态枚举或教师题目归属模型。
- 不将批量接口开放给学生、选题负责人或管理员。
