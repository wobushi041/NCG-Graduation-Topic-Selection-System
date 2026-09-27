# 系部、专业与教师选题组域重构完成报告（Wave 2：阶段 3.2 + 阶段 5 切片）

## 1. 重构背景与目标

本次重构对应《HTTP 接口与 AOP 重构台账》（`docs/HTTP_API_REFACTOR_STATUS.md`）中的 **Wave 2（阶段 3.2：系部、专业与教师选题组域，`ORG-001` ～ `ORG-009` + `GRP-001` ～ `GRP-003`，共 12 个接口）**。

遵循“仅做上层逻辑架构迁移、100% 保持底层业务实现与对外协议不变”的原则，完成以下目标：

1. **控制层解耦**：将系部与专业管理 9 个接口迁移至独立 [`OrganizationController`](file:///e:/Demos/graduation-topic-selection-system/work-topic-selection-backend/src/main/java/cn/com/edtechhub/worktopicselection/controller/OrganizationController.java)，将教师选题组与额度查询 3 个接口迁移至独立 [`TeacherGroupController`](file:///e:/Demos/graduation-topic-selection-system/work-topic-selection-backend/src/main/java/cn/com/edtechhub/worktopicselection/controller/TeacherGroupController.java)，两者均保持 `@RequestMapping("/user")` 与原有子路径不变，前端零改动。
2. **事务边界下沉与收窄（`ARCH-03` & `ARCH-05`）**：移除 `UserController` 中该域的 5 处 `transactionTemplate.execute(...)`，在 [`OrganizationApplicationServiceImpl`](file:///e:/Demos/graduation-topic-selection-system/work-topic-selection-backend/src/main/java/cn/com/edtechhub/worktopicselection/service/impl/OrganizationApplicationServiceImpl.java) 的 5 个写方法（`addDept`、`addProject`、`updateProjectGroup`、`deleteDept`、`deleteProject`）上声明方法级 `@Transactional(rollbackFor = Exception.class)`，只读分页、下拉列表与选题组查询方法不开启数据库写事务。
3. **鉴权与限流规范化（`AUTH-02` & 阶段 5）**：
   - 为原先仅有 `@SaCheckRole` 而缺失登录校验注解的 5 个接口（`ORG-004` `/user/delete/dept`、`ORG-005` `/user/delete/project`、`GRP-001` `/user/teacher/groups`、`GRP-002` `/user/teacher/groups/batch`、`GRP-003` `/user/group/list`）显式补齐 `@SaCheckLogin`。
   - 移除 `UserController` 中 9 处手写 `initFlowRules()` + `SphU.entry()` 样板代码（剩余 41 $\rightarrow$ 32 处），并为全部 12 个接口（含原先无 Sentinel 保护的 `GRP-001` ～ `GRP-003`）接入 `@SentinelRateLimit` 注解切面与启动期规则注册。
4. **请求模型归档**：将 8 个 HTTP 请求模型从 `model.dto.dept` / `model.dto.project` 迁移至 `model.request.organization` 包，并清理已腾空的 `model/dto/project` 目录。

---

## 2. 接口迁移明细（12 个接口）

| 台账 ID | HTTP 方法与路径 | 迁出类 | 迁入 Controller | 目标服务方法 | 鉴权注解 | Sentinel 资源名 | 事务策略 |
|---|---|---|---|---|---|---|---|
| `ORG-001` | `POST /user/add/dept` | `UserController` | `OrganizationController` | `OrganizationApplicationService.addDept` | `@SaCheckLogin` + `@SaCheckRole("admin")` | `organization.dept.add` | `@Transactional` |
| `ORG-002` | `POST /user/add/project` | `UserController` | `OrganizationController` | `OrganizationApplicationService.addProject` | `@SaCheckLogin` + `@SaCheckRole("admin")` | `organization.project.add` | `@Transactional` |
| `ORG-003` | `POST /user/update/project/group` | `UserController` | `OrganizationController` | `OrganizationApplicationService.updateProjectGroup` | `@SaCheckLogin` + `@SaCheckRole("admin")` | `organization.project.update-group` | `@Transactional` |
| `ORG-004` | `POST /user/delete/dept` | `UserController` | `OrganizationController` | `OrganizationApplicationService.deleteDept` | 补齐 `@SaCheckLogin` + `@SaCheckRole("admin")` | `organization.dept.delete` | `@Transactional` |
| `ORG-005` | `POST /user/delete/project` | `UserController` | `OrganizationController` | `OrganizationApplicationService.deleteProject` | 补齐 `@SaCheckLogin` + `@SaCheckRole("admin")` | `organization.project.delete` | `@Transactional` |
| `ORG-006` | `POST /user/get/dept/page` | `UserController` | `OrganizationController` | `OrganizationApplicationService.getDeptPage` | `@SaCheckLogin` + `@SaCheckRole("admin")` | `organization.dept.query-page` | 无事务（只读） |
| `ORG-007` | `POST /user/get/dept/list` | `UserController` | `OrganizationController` | `OrganizationApplicationService.getDeptList` | `@SaCheckLogin` | `organization.dept.query-list` | 无事务（只读） |
| `ORG-008` | `POST /user/get/project/page` | `UserController` | `OrganizationController` | `OrganizationApplicationService.getProjectPage` | `@SaCheckLogin` | `organization.project.query-page` | 无事务（只读） |
| `ORG-009` | `POST /user/get/project/list` | `UserController` | `OrganizationController` | `OrganizationApplicationService.getProjectList` | `@SaCheckLogin` | `organization.project.query-list` | 无事务（只读） |
| `GRP-001` | `GET /user/teacher/groups` | `UserController` | `TeacherGroupController` | `OrganizationApplicationService.getTeacherGroups` | 补齐 `@SaCheckLogin` + `@SaCheckRole("teacher")` | `teacher-group.query-self` | 无事务（只读） |
| `GRP-002` | `POST /user/teacher/groups/batch` | `UserController` | `TeacherGroupController` | `OrganizationApplicationService.getTeacherGroupsBatch` | 补齐 `@SaCheckLogin` + `@SaCheckRole({"admin", "dept"})` | `teacher-group.query-batch` | 无事务（只读） |
| `GRP-003` | `GET /user/group/list` | `UserController` | `TeacherGroupController` | `OrganizationApplicationService.getGroupList` | 补齐 `@SaCheckLogin` + `@SaCheckRole({"admin", "dept"})` | `teacher-group.query-all` | 无事务（只读） |

---

## 3. 请求模型迁移对照

全部 8 个前端入参模型已统一归档至 `cn.com.edtechhub.worktopicselection.model.request.organization` 包，字段名与反序列化行为 100% 保持不变：

- `DeptAddRequest`
- `DeleteDeptRequest`
- `DeptQueryRequest`
- `ProjectAddRequest`
- `DeleteProjectRequest`
- `ProjectGroupUpdateRequest`
- `ProjectQueryRequest`
- `TeacherGroupsBatchRequest`

> 注：`model/dto/dept/SetDeptConfigRequest.java` 属于系部跨选策略配置（`CFG-010` `/user/set/dept/config`），将在 Wave 3 随系统开关与配置域一并迁移。

---

## 4. 质量验证结果

1. **Java 编码规范校验（`verify-style.ps1`）**：
   - 校验范围：`model/request/organization`（8 个文件）、`SentinelRateLimitProperties.java`、`SentinelRuleRegistry.java`、`DeptService.java`、`DeptServiceImpl.java`、`ProjectService.java`、`ProjectServiceImpl.java`、`OrganizationApplicationService.java`、`OrganizationApplicationServiceImpl.java`、`OrganizationController.java`、`TeacherGroupController.java`、`OrganizationAndGroupControllerContractTest.java`、`OrganizationApplicationServiceImplTest.java`（共 20 个文件）。
   - 结果：**0 违规（`VERIFY_PASSED`）**。
2. **后端全量单元测试（`.\mvnw.cmd test`）**：
   - 新增 `OrganizationAndGroupControllerContractTest`（4 个契约测试）与 `OrganizationApplicationServiceImplTest`（5 个应用服务与事务边界测试）。
   - 全量测试结果：**181 个测试全部通过（`Failures: 0, Errors: 0`）**。
