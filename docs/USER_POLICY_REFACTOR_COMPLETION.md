# 系统开关配置、系统诊断与用户管理域重构完成报告（Wave 3：阶段 3.3 + 阶段 5 切片）

## 1. 重构背景与目标

本次重构对应《HTTP 接口与 AOP 重构台账》（`docs/HTTP_API_REFACTOR_STATUS.md`）中的 **Wave 3（阶段 3.3：系统开关配置 `CFG-001` ～ `CFG-012` + 用户管理 `USR-001` ～ `USR-008` + 测试诊断 `TST-001`，共 21 个接口）**。

遵循“仅做上层逻辑架构迁移、100% 保持底层业务实现与对外协议不变”的原则，完成以下目标：

1. **`UserController` 彻底解耦（达成 `ARCH-02` & `ARCH-03`）**：
   - 将剩余 3 处 `transactionTemplate.execute(...)` 与 3 个直接注入的 `Mapper`（`UserMapper`、`TopicMapper`、`StudentTopicSelectionMapper`）全部下沉至 [`UserApplicationServiceImpl`](file:///e:/Demos/graduation-topic-selection-system/work-topic-selection-backend/src/main/java/cn/com/edtechhub/worktopicselection/service/impl/UserApplicationServiceImpl.java)。
   - 至此，[`UserController`](file:///e:/Demos/graduation-topic-selection-system/work-topic-selection-backend/src/main/java/cn/com/edtechhub/worktopicselection/controller/UserController.java) 内的 **`TransactionTemplate` 与直接 `Mapper` 引用 100% 清零**。
2. **控制层与应用服务拆分**：
   - 新建 [`SelectionPolicyController`](file:///e:/Demos/graduation-topic-selection-system/work-topic-selection-backend/src/main/java/cn/com/edtechhub/worktopicselection/controller/SelectionPolicyController.java)（承载 `CFG-001` ～ `CFG-011` 共 11 个选题开关与跨系规则接口）与 [`SystemController`](file:///e:/Demos/graduation-topic-selection-system/work-topic-selection-backend/src/main/java/cn/com/edtechhub/worktopicselection/controller/SystemController.java)（承载 `TST-001` 连通性诊断与 `CFG-012` 系统信息面板），统一委托给 [`SelectionPolicyService`](file:///e:/Demos/graduation-topic-selection-system/work-topic-selection-backend/src/main/java/cn/com/edtechhub/worktopicselection/service/SelectionPolicyService.java)（[`SelectionPolicyServiceImpl`](file:///e:/Demos/graduation-topic-selection-system/work-topic-selection-backend/src/main/java/cn/com/edtechhub/worktopicselection/service/impl/SelectionPolicyServiceImpl.java)）。
   - [`UserController`](file:///e:/Demos/graduation-topic-selection-system/work-topic-selection-backend/src/main/java/cn/com/edtechhub/worktopicselection/controller/UserController.java) 承载 `USR-001` ～ `USR-008` 共 8 个用户管理与查询接口，统一委托给 [`UserApplicationService`](file:///e:/Demos/graduation-topic-selection-system/work-topic-selection-backend/src/main/java/cn/com/edtechhub/worktopicselection/service/UserApplicationService.java)（[`UserApplicationServiceImpl`](file:///e:/Demos/graduation-topic-selection-system/work-topic-selection-backend/src/main/java/cn/com/edtechhub/worktopicselection/service/impl/UserApplicationServiceImpl.java)）。
3. **事务边界精细收窄（`ARCH-05`）**：
   - 仅在 `UserApplicationServiceImpl` 的 3 个数据库写方法（`addUser`、`deleteUser`、`updateUser`）上声明方法级 `@Transactional(rollbackFor = Exception.class)`。
   - 5 个用户只读查询方法与 `SelectionPolicyServiceImpl` 的全部 12 个策略/监控方法均不开启数据库写事务。
4. **鉴权与限流规范化（`AUTH-02` & 阶段 5）**：
   - 为公开连通性诊断接口 `TST-001`（`GET /user/test`）显式标注 `@SaIgnore`。
   - 移除 `UserController` 中 20 处手写 `initFlowRules()` + `SphU.entry()` 样板代码（剩余 32 $\rightarrow$ 12 处，其中 `UserController` 仅剩 Wave 4 的 8 处统计查询接口），并为全部 21 个接口接入 `@SentinelRateLimit` 与启动期规则注册。
5. **请求模型归档**：
   - 将 `SetDeptConfigRequest` 迁入 `model.request.policy`（并清理空的 `model/dto/dept` 目录），将 `UserAddRequest`、`DeleteRequest`、`UserUpdateRequest`、`UserQueryRequest`、`TeacherQueryRequest` 迁入 `model.request.user`。

---

## 2. 接口迁移明细（21 个接口）

| 台账 ID | HTTP 方法与路径 | 迁入 Controller | 目标服务方法 | 鉴权注解 | Sentinel 资源名 | 事务策略 |
|---|---|---|---|---|---|---|
| `TST-001` | `GET /user/test` | `SystemController` | 直接返回 `TheResult.notyet()` | `@SaIgnore` | `system.diagnostics.test` | 无事务 |
| `CFG-001` | `GET /user/cross_topic` | `SelectionPolicyController` | `SelectionPolicyService.getCrossTopicStatus` | `@SaCheckLogin` + `@SaCheckRole("admin")` | `policy.cross-topic.query` | 无事务 |
| `CFG-002` | `POST /user/cross_topic` | `SelectionPolicyController` | `SelectionPolicyService.setCrossTopicStatus` | `@SaCheckLogin` + `@SaCheckRole("admin")` | `policy.cross-topic.update` | 无事务 |
| `CFG-003` | `GET /user/view_topic` | `SelectionPolicyController` | `SelectionPolicyService.getViewTopicStatus` | `@SaCheckLogin` + `@SaCheckRole("admin")` | `policy.view-topic.query` | 无事务 |
| `CFG-004` | `POST /user/view_topic` | `SelectionPolicyController` | `SelectionPolicyService.setViewTopicStatus` | `@SaCheckLogin` + `@SaCheckRole("admin")` | `policy.view-topic.update` | 无事务 |
| `CFG-005` | `GET /user/switch_single_choice` | `SelectionPolicyController` | `SelectionPolicyService.getSwitchSingleChoiceStatus` | `@SaCheckLogin` + `@SaCheckRole("admin")` | `policy.single-choice.query` | 无事务 |
| `CFG-006` | `POST /user/switch_single_choice` | `SelectionPolicyController` | `SelectionPolicyService.setSwitchSingleChoiceStatus` | `@SaCheckLogin` + `@SaCheckRole("admin")` | `policy.single-choice.update` | 无事务 |
| `CFG-007` | `GET /user/topic_lock` | `SelectionPolicyController` | `SelectionPolicyService.getTopicLock` | `@SaCheckLogin` + `@SaCheckRole({"admin","teacher","student"})` | `policy.topic-lock.query` | 无事务 |
| `CFG-008` | `POST /user/topic_lock` | `SelectionPolicyController` | `SelectionPolicyService.setTopicLock` | `@SaCheckLogin` + `@SaCheckRole("admin")` | `policy.topic-lock.update` | 无事务 |
| `CFG-009` | `GET /user/get/dept/config` | `SelectionPolicyController` | `SelectionPolicyService.getDeptConfig` | `@SaCheckLogin` + `@SaCheckRole("admin")` | `policy.dept-config.query` | 无事务 |
| `CFG-010` | `POST /user/set/dept/config` | `SelectionPolicyController` | `SelectionPolicyService.setDeptConfig` | `@SaCheckLogin` + `@SaCheckRole("admin")` | `policy.dept-config.update` | 无事务 |
| `CFG-011` | `POST /user/del/dept/config` | `SelectionPolicyController` | `SelectionPolicyService.delDeptConfig` | `@SaCheckLogin` + `@SaCheckRole("admin")` | `policy.dept-config.delete` | 无事务 |
| `CFG-012` | `GET /user/get/system/info` | `SystemController` | `SelectionPolicyService.getSystemInfo` | `@SaCheckLogin` + `@SaCheckRole("admin")` | `system.info.query` | 无事务 |
| `USR-001` | `POST /user/add` | `UserController` | `UserApplicationService.addUser` | `@SaCheckLogin` + `@SaCheckRole("admin")` | `user.manage.add` | `@Transactional` |
| `USR-002` | `POST /user/delete` | `UserController` | `UserApplicationService.deleteUser` | `@SaCheckLogin` + `@SaCheckRole("admin")` | `user.manage.delete` | `@Transactional` |
| `USR-003` | `POST /user/update` | `UserController` | `UserApplicationService.updateUser` | `@SaCheckLogin` + `@SaCheckRole("admin")` | `user.manage.update` | `@Transactional` |
| `USR-004` | `GET /user/get/login` | `UserController` | `UserApplicationService.getLoginUser` | `@SaCheckLogin` | `user.query.current` | 无事务 |
| `USR-005` | `POST /user/get/user/page` | `UserController` | `UserApplicationService.listUserByPage` | `@SaCheckLogin` + `@SaCheckRole({"admin","teacher"})` | `user.query.page` | 无事务 |
| `USR-006` | `POST /user/get/teacher` | `UserController` | `UserApplicationService.getTeacher` | `@SaCheckLogin` + `@SaCheckRole("teacher")` | `user.query.teacher-list` | 无事务 |
| `USR-007` | `GET /user/get` | `UserController` | `UserApplicationService.getUserById` | `@SaCheckLogin` + `@SaCheckRole("admin")` | `user.query.by-id` | 无事务 |
| `USR-008` | `GET /user/get/vo` | `UserController` | `UserApplicationService.getUserVOById` | `@SaCheckLogin` + `@SaCheckRole("admin")` | `user.query.vo-by-id` | 无事务 |

---

## 3. 质量验证结果

1. **Java 编码规范校验（`verify-style.ps1`）**：
   - 校验范围：19 个新增/修改的 Java 源码与测试文件。
   - 结果：**0 违规（`VERIFY_PASSED`）**。
2. **后端全量单元测试（`.\mvnw.cmd test`）**：
   - 新增 `SelectionPolicyAndSystemControllerContractTest` 与 `UserAndPolicyServiceTest`（含反射断言 `UserController` 零 `TransactionTemplate` 与零 `Mapper` 字段）。
   - 全量测试结果：**188 个测试全部通过（`Failures: 0, Errors: 0`）**。
