# 查询与统计域（Wave 4）HTTP 接口与上层架构重构完成报告

## 一、重构范围与目标

本次 **Wave 4（Phase 3.4 & 3.5：查询与统计域重构）** 覆盖 `UserController` 中剩余的 **8 个只读查询与统计接口（`QRY-001` ~ `QRY-008`）**。重构严格遵循“**仅做上层逻辑架构迁移，零业务语义改动**”原则，在保持前端路由（`/user/**`）、请求体 JSON 字段、响应结构（`BaseResponse<T>`）、业务状态码与提示文案 100% 兼容的前提下，完成控制器拆分、只读服务下沉、请求模型归位与声明式 Sentinel 限流收口，并实现 `UserController` 历史技术债的彻底清零。

---

## 二、接口迁移清单（QRY-001 ~ QRY-008）

| 编号 | HTTP 方法与路径 | 原控制器方法 | 目标控制器 | 目标应用服务方法 | 请求模型（`model.request.*`） | Sentinel 资源名 | 默认 QPS |
|---|---|---|---|---|---|---|---|---|
| `QRY-001` | `POST /user/get/topic/page` | `UserController#getTopicList` | `TopicQueryController#getTopicList` | `SelectionReportService#getTopicList` | `TopicQueryRequest` | `query.topic.page` | `200` |
| `QRY-002` | `POST /user/get/select/topic/situation` | `UserController#getSelectTopicSituation` | `TopicQueryController#getSelectTopicSituation` | `SelectionReportService#getSelectTopicSituation` | - | `query.selection.situation` | `120` |
| `QRY-003` | `POST /user/get/dept/teacher` | `UserController#getTeacher` | `TopicQueryController#getTeacher` | `SelectionReportService#getTeacher` | `DeptTeacherQueryRequest` | `query.dept.teacher` | `150` |
| `QRY-004` | `POST /user/get/unselect/topic/student/list` | `UserController#getUnSelectTopicStudentList` | `TopicQueryController#getUnSelectTopicStudentList` | `SelectionReportService#getUnSelectTopicStudentList` | - | `query.selection.unselected-students` | `120` |
| `QRY-005` | `POST /user/get/topic/list/by/admin` | `UserController#getTopicListByAdmin` | `TopicQueryController#getTopicListByAdmin` | `SelectionReportService#getTopicListByAdmin` | `TopicQueryByAdminRequest` | `query.topic.admin-page` | `150` |
| `QRY-006` | `POST /user/list/page/vo` | `UserController#listUserVOByPage` | `TopicQueryController#listUserVOByPage` | `SelectionReportService#listUserVOByPage` | `UserQueryRequest` | `query.user.vo-page` | `120` |
| `QRY-007` | `POST /user/get/user/list` | `UserController#getUserList` | `TopicQueryController#getUserList` | `SelectionReportService#getUserList` | `GetUserListRequest` | `query.user.name-list` | `150` |
| `QRY-008` | `POST /user/get/dept/teacher/by/admin` | `UserController#getTeacherByAdmin` | `TopicQueryController#getTeacherByAdmin` | `SelectionReportService#getTeacherByAdmin` | `DeptTeacherQueryRequest` | `query.dept.pending-teacher` | `120` |

---

## 三、核心架构交付成果

### 1. 请求模型规范化归位（`model.dto.*` -> `model.request.*`）
- 迁移并规范化 4 个前端 HTTP 请求模型：
  - `cn.com.edtechhub.worktopicselection.model.request.topic.TopicQueryRequest`
  - `cn.com.edtechhub.worktopicselection.model.request.topic.TopicQueryByAdminRequest`
  - `cn.com.edtechhub.worktopicselection.model.request.user.DeptTeacherQueryRequest`
  - `cn.com.edtechhub.worktopicselection.model.request.user.GetUserListRequest`
- 移除 `model/dto/topic` 与 `model/dto/user` 下的旧请求类，同步更新 `TopicService` 与 `TopicServiceImpl` 的引用包路径。

### 2. 只读查询服务下沉与无写事务保障（`ARCH-05`）
- 新增 `SelectionReportService` 接口与 `SelectionReportServiceImpl` 实现类。
- **零写事务代理开销**：`SelectionReportServiceImpl` 类与方法层面均不添加 `@Transactional` 注解，彻底消除纯查询与统计报表的事务连接占锁开销。
- **100% 业务行为保真**：
  - 完整保留 `getTopicList` 按 `ADMIN` / `DEPT` / `TEACHER` / `STUDENT` 角色与 `VIEW_TOPIC_SWITCH`、`CROSS_TOPIC_SWITCH` 开关的数据范围过滤。
  - 完整保留 `getTeacher` 与 `getTeacherByAdmin` 针对系部教师题目余量（`surplusQuantity`）、已选数量（`selectAmount`）的内存聚合与内存分页逻辑。
  - 完整保留 `listUserVOByPage` 每页大小 `1 ~ 20` 的校验规则及错误提示文案。

### 3. 控制器拆分与 `UserController` 彻底净化（`ARCH-01` ~ `ARCH-04`）
- 新增 `TopicQueryController`（`@RequestMapping("/user")`），承接 `QRY-001` ~ `QRY-008` 共 8 个接口，统一使用 `@SentinelRateLimit(resource = "query.*")` 声明式切面限流。
- `UserController` 移除全部 8 个手工 `sentineManager.initFlowRules(entryName)` + `SphU.entry(entryName)` 代码块及 `SentineManager`、`UserService`、`ProjectService`、`TopicService`、`StudentTopicSelectionService`、`SwitchService` 等冗余依赖，仅保留 `USR-001` ~ `USR-008` 8 个用户管理接口与 `UserApplicationService` 单一构造注入依赖。

---

## 四、验证结果

1. **Java 编码规范校验（`verify-style.ps1`）**：Wave 4 新增及修改的 14 个 Java 源码与测试文件全部通过校验（`0 violations found`）。
2. **契约与单元测试覆盖**：
   - 新增 `TopicQueryControllerContractTest`：验证 8 个查询统计接口的 HTTP 路由、Sentinel 资源名、Sa-Token 鉴权注解以及 `UserController` 单一依赖净化。
   - 新增 `SelectionReportServiceImplTest`：验证 `SelectionReportServiceImpl` 无 `@Transactional` 架构红线（`ARCH-05`）及 8 个查询与统计方法的业务分支。
   - 全量后端测试套件 100% 通过。
