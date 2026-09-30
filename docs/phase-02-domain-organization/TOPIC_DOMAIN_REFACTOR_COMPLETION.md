# 课题维护、审核、发布、配额管理与 AI 查重重构完成报告（Wave 1：阶段 3.1 + 阶段 5.4）

## 1. 重构概述

本阶段（**Wave 1**）完成了毕业选题管理系统 **阶段 3.1（课题维护、审核与发布，`TOPIC-001` ～ `TOPIC-009`，共 9 个接口）** 与 **阶段 5.4（高成本外部资源防护）** 的三层架构解耦。

在 **100% 保持业务逻辑、数据库悲观锁加锁顺序、状态流转矩阵、错误提示文案、Redis 限流键及 HTTP 路由契约不变** 的前提下，将原本内联在 [`UserController`](file:///e:/Demos/graduation-topic-selection-system/work-topic-selection-backend/src/main/java/cn/com/edtechhub/worktopicselection/controller/UserController.java) 中的 9 个课题生命周期接口下沉至独立的 [`TopicController`](file:///e:/Demos/graduation-topic-selection-system/work-topic-selection-backend/src/main/java/cn/com/edtechhub/worktopicselection/controller/TopicController.java) 与 [`TopicApplicationService`](file:///e:/Demos/graduation-topic-selection-system/work-topic-selection-backend/src/main/java/cn/com/edtechhub/worktopicselection/service/TopicApplicationService.java)（[`TopicApplicationServiceImpl`](file:///e:/Demos/graduation-topic-selection-system/work-topic-selection-backend/src/main/java/cn/com/edtechhub/worktopicselection/service/impl/TopicApplicationServiceImpl.java)）。

---

## 2. 接口迁移与契约映射表

| 接口编号 | HTTP 方法与路径 | 原入口 | 新入口 | 应用服务方法 | 鉴权注解 | Sentinel 资源标识 | 事务与锁策略 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `TOPIC-001` | `POST /user/add/topic` | `UserController#addTopic` | `TopicController#addTopic` | `TopicApplicationService#addTopic` | `@SaCheckLogin` + `@SaCheckRole("teacher")` | `topic.add` (100 QPS) | 方法级 `@Transactional` + `userMapper.selectByIdForUpdate` |
| `TOPIC-002` | `POST /user/delete/topic` | `UserController#deleteTopic` | `TopicController#deleteTopic` | `TopicApplicationService#deleteTopic` | **补齐 `@SaCheckLogin`** + `@SaCheckRole("teacher")` | `topic.delete` (100 QPS) | 方法级 `@Transactional` + `User -> Topic` 悲观锁 |
| `TOPIC-003` | `POST /user/get/teacher/topicAmount` | `UserController#getTeacherTopicAmount` | `TopicController#getTeacherTopicAmount` | `TopicApplicationService#getTeacherTopicAmount` | `@SaCheckLogin` + `@SaCheckRole("admin")` | `topic.quota.get` (200 QPS) | 无事务代理（只读查询） |
| `TOPIC-004` | `POST /user/set/teacher/topicAmount` | `UserController#setTeacherTopicAmount` | `TopicController#setTeacherTopicAmount` | `TopicApplicationService#setTeacherTopicAmount` | `@SaCheckLogin` + `@SaCheckRole("admin")` | `topic.quota.set` (100 QPS) | 方法级 `@Transactional` + `teacherId` 同步锁 |
| `TOPIC-005` | `POST /user/check/topic` | `UserController#checkTopic` | `TopicController#checkTopic` | `TopicApplicationService#checkTopic` | `@SaCheckLogin` + `@SaCheckRole({"dept", "teacher"})` | `topic.review.check` (100 QPS) | 方法级 `@Transactional` + `topicMapper.selectByIdForUpdate` |
| `TOPIC-006` | `POST /user/set/time/by/id` | `UserController#setTimeById` | `TopicController#setTimeById` | `TopicApplicationService#setTimeById` | `@SaCheckLogin` + `@SaCheckRole("admin")` | `topic.publication.publish` (100 QPS) | 方法级 `@Transactional` + 批量 `topicMapper.selectByIdForUpdate` |
| `TOPIC-007` | `POST /user/unset/time/by/id` | `UserController#unsetTimeById` | `TopicController#unsetTimeById` | `TopicApplicationService#unsetTimeById` | `@SaCheckLogin` + `@SaCheckRole("admin")` | `topic.publication.unpublish` (100 QPS) | 方法级 `@Transactional` + 批量 `topicMapper.selectByIdForUpdate` |
| `TOPIC-008` | `POST /user/update/topic` | `UserController#updateTopic` | `TopicController#updateTopic` | `TopicApplicationService#updateTopic` | `@SaCheckLogin` + `@SaCheckRole("teacher")` | `topic.update` (100 QPS) | 方法级 `@Transactional` + `User -> Topic` 悲观锁 |
| `TOPIC-009` | `POST /user/get/topic/review_level` | `UserController#getTopicReviewLevel` | `TopicController#getTopicReviewLevel` | `TopicApplicationService#getTopicReviewLevel` | `@SaCheckLogin` + `@SaCheckRole({"admin", "teacher"})` | `topic.review.ai-level` (20 QPS) | 无数据库长事务 + Redis 每日 30 次限流（`ai-review-rate:{id}`） |

---

## 3. 核心架构改进点

### 3.1 请求模型包规范化（`model.request.topic`）
将分散在 `model.dto.topic`、`model.dto.schedule`、`model.dto.user` 下的 9 个前端 HTTP 请求模型统一迁移至 `cn.com.edtechhub.worktopicselection.model.request.topic` 包，并严格按照 `java-coding-conventions` 规范补齐三行字段 Javadoc 与底部 `/// 序列化字段 ///` 分区：
- [`AddTopicRequest`](file:///e:/Demos/graduation-topic-selection-system/work-topic-selection-backend/src/main/java/cn/com/edtechhub/worktopicselection/model/request/topic/AddTopicRequest.java)
- [`DeleteTopicRequest`](file:///e:/Demos/graduation-topic-selection-system/work-topic-selection-backend/src/main/java/cn/com/edtechhub/worktopicselection/model/request/topic/DeleteTopicRequest.java)
- [`GetTeacherTopicAmountRequest`](file:///e:/Demos/graduation-topic-selection-system/work-topic-selection-backend/src/main/java/cn/com/edtechhub/worktopicselection/model/request/topic/GetTeacherTopicAmountRequest.java)
- [`SetTeacherTopicAmountRequest`](file:///e:/Demos/graduation-topic-selection-system/work-topic-selection-backend/src/main/java/cn/com/edtechhub/worktopicselection/model/request/topic/SetTeacherTopicAmountRequest.java)
- [`CheckTopicRequest`](file:///e:/Demos/graduation-topic-selection-system/work-topic-selection-backend/src/main/java/cn/com/edtechhub/worktopicselection/model/request/topic/CheckTopicRequest.java)
- [`SetTimeRequest`](file:///e:/Demos/graduation-topic-selection-system/work-topic-selection-backend/src/main/java/cn/com/edtechhub/worktopicselection/model/request/topic/SetTimeRequest.java)
- [`UnSetTimeRequest`](file:///e:/Demos/graduation-topic-selection-system/work-topic-selection-backend/src/main/java/cn/com/edtechhub/worktopicselection/model/request/topic/UnSetTimeRequest.java)
- [`UpdateTopicRequest`](file:///e:/Demos/graduation-topic-selection-system/work-topic-selection-backend/src/main/java/cn/com/edtechhub/worktopicselection/model/request/topic/UpdateTopicRequest.java)
- [`GetTopicReviewLevelRequest`](file:///e:/Demos/graduation-topic-selection-system/work-topic-selection-backend/src/main/java/cn/com/edtechhub/worktopicselection/model/request/topic/GetTopicReviewLevelRequest.java)

### 3.2 事务边界收窄与锁顺序保持（`ARCH-03` & `ARCH-05`）
- 移除了 `UserController` 中的 7 处 `transactionTemplate.execute(...)` 编程式事务块，下沉为 [`TopicApplicationServiceImpl`](file:///e:/Demos/graduation-topic-selection-system/work-topic-selection-backend/src/main/java/cn/com/edtechhub/worktopicselection/service/impl/TopicApplicationServiceImpl.java) 中 7 个写方法上的方法级 `@Transactional(rollbackFor = Exception.class)`。
- 只读查询 `getTeacherTopicAmount` 与外部高耗时 AI 调用 `getTopicReviewLevel` **不标注 `@Transactional`**，避免外部 HTTP/AI I/O 占用数据库连接池。
- 严格保留 `userMapper.selectByIdForUpdate` $\rightarrow$ `topicMapper.selectByIdForUpdate` 的悲观锁获取顺序。

### 3.3 鉴权缺口修复（`AUTH-02`）与高成本资源双层限流（阶段 5.4）
- 为 `POST /user/delete/topic`（`TOPIC-002`）补齐显式 `@SaCheckLogin` 注解，与全站接口鉴权声明保持一致。
- 在 [`SentinelRuleRegistry`](file:///e:/Demos/graduation-topic-selection-system/work-topic-selection-backend/src/main/java/cn/com/edtechhub/worktopicselection/manager/sentinel/SentinelRuleRegistry.java) 与 [`SentinelRateLimitProperties`](file:///e:/Demos/graduation-topic-selection-system/work-topic-selection-backend/src/main/java/cn/com/edtechhub/worktopicselection/manager/sentinel/SentinelRateLimitProperties.java) 中集中注册 9 条 `topic.*` QPS 规则，其中 `topic.review.ai-level` 设为 20 QPS 低阈值，配合 `TopicApplicationServiceImpl#getTopicReviewLevel` 内的 Redis 用户级日限流（`ai-review-rate:{userId}`，30 次/24h）构成双层防护，并消除了 `UserController` 中 9 处手写 Sentinel 样板代码。

---

## 4. 验证结果

1. **Java 编码规范校验（`verify-style.ps1`）**：
   - 对 9 个新增 `Request` 类、`SentinelRateLimitProperties`、`SentinelRuleRegistry`、`TopicApplicationService`、`TopicApplicationServiceImpl`、`TopicController` 及 3 个测试类运行 `verify-style.ps1`，结果全部为 **`VERIFY_PASSED: 0 violations found`**。
2. **单元测试与全量回归测试（`.\mvnw.cmd test`）**：
   - 新增 [`TopicControllerContractTest`](file:///e:/Demos/graduation-topic-selection-system/work-topic-selection-backend/src/test/java/cn/com/edtechhub/worktopicselection/controller/TopicControllerContractTest.java)（验证 9 个接口路由、鉴权、Sentinel 注解与委托调用）；
   - 新增 [`TopicApplicationServiceImplTest`](file:///e:/Demos/graduation-topic-selection-system/work-topic-selection-backend/src/test/java/cn/com/edtechhub/worktopicselection/service/impl/TopicApplicationServiceImplTest.java)（验证添加课题扣减配额、删除课题悲观锁顺序与恢复配额、配额下限校验、系主任退回邮件通知、AI 查重 Redis 日限流）；
   - 全量 **172 个测试全部通过（0 Failures, 0 Errors）**。
