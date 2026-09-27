# HTTP API 与架构重构状态

> 最后核对日期：2026-09-27  
> 当前基线：79 个 HTTP 接口  
> 适用范围：后端 Controller/Service 解耦、AOP 整理、Sentinel 限流迁移

认证模块本轮实施详情见：[认证模块重构与 AOP 架构升级完成记录](./AUTH_AOP_REFACTOR_COMPLETION.md)。

## 1. 文档目的

本文件是本轮架构重构的状态台账，用于回答以下问题：

1. 当前有哪些 HTTP 接口，接口路径、方法和权限是否保持兼容。
2. 每个接口当前属于哪个业务域，重构后应由哪个应用服务承接。
3. Controller 中的业务编排、事务和 Mapper 调用迁移到了什么阶段。
4. AOP、MVC Interceptor 和 Sentinel 限流分别处于什么状态。
5. 每次重构后验证了哪些行为，是否允许进入下一阶段。

重构期间不得直接删除接口记录。接口废弃时将状态改为“已废弃”，同时记录替代接口和兼容截止时间。

## 2. 状态定义

接口使用以下状态流转：

```text
已登记
  -> 已分析
  -> 已补测试
  -> 已下沉 Service
  -> 已迁移横切逻辑
  -> 已验证
```

| 状态 | 含义 |
|---|---|
| 已登记 | 已确认路由、HTTP 方法、权限和当前处理方法 |
| 已分析 | 已梳理请求 DTO、业务规则、事务、Mapper、Redis 与外部依赖 |
| 已补测试 | 已使用测试锁定接口当前行为与兼容契约 |
| 已下沉 Service | Controller 只保留协议转换、参数入口和响应组装 |
| 已迁移横切逻辑 | 限流、日志、缓存、事务等已迁移至目标机制 |
| 已验证 | 相关测试、完整后端测试及必要的前端调用检查通过 |
| 已废弃 | 接口不再使用，已记录替代方案和兼容期限 |

## 3. 当前架构基线

### 3.1 接口数量

| Controller | 接口数 | 当前职责 |
|---|---:|---|
| `UserController` | 59 | 用户、组织、课题、选题、统计、系统配置、教师组 |
| `AuthController` | 10 | 登录会话、角色切换、密码与验证码 |
| `FileController` | 9 | 用户/课题导入与统计数据导出 |
| `AIController` | 1 | AI 问答占位接口 |
| **总计** | **79** |  |

### 3.2 已确认的耦合问题

| 编号 | 当前事实 | 风险 | 目标方向 | 状态 |
|---|---|---|---|---|
| ARCH-01 | `UserController` 约 4042 行并承载 68 个接口 | 修改影响面过大，业务域边界不清晰 | 按认证、用户、组织、课题、选题、配置、报表拆分 Controller | 已登记 |
| ARCH-02 | `UserController` 直接注入 3 个 Mapper，并存在 23 处直接调用 | Controller 越过 Service，事务与业务规则难复用 | Mapper 仅由 Service/Repository 访问 | 已登记 |
| ARCH-03 | `UserController` 存在 20 处 `TransactionTemplate` 调用 | 事务边界位于 Web 层 | 事务下沉至应用服务公开方法 | 已登记 |
| ARCH-04 | `FileController` 存在 2 处 `TransactionTemplate` 调用 | 文件协议处理与批量业务事务耦合 | 导入用例下沉至 Import Service | 已登记 |
| ARCH-05 | 5 个 Service 实现类使用类级别 `@Transactional` | 查询和纯计算方法也可能无差别进入事务代理 | 将事务收窄至写用例和确需一致性的公开方法 | 已登记 |
| ARCH-06 | HTTP 路径、DTO 和前端生成 Service 已形成契约 | 拆分类时容易造成前端破坏性变更 | 第一阶段保持 URL、请求字段、响应结构与业务码不变 | 已登记 |

### 3.3 目标调用方向

```text
HTTP / JSON
  -> Web Filter / HandlerInterceptor
  -> Controller
  -> Application Service（用例编排与事务边界）
  -> Domain Service / Infrastructure Manager
  -> Mapper
  -> MySQL / Redis / 外部服务
```

目标约束：

- Controller 不直接调用 Mapper。
- Controller 不创建或控制数据库事务。
- Controller 不负责 Sentinel 规则注册。
- Controller 只处理 HTTP 协议、DTO 入口、认证上下文提取和统一响应。
- Service 返回业务结果或 VO 所需数据，不依赖 `HttpServletRequest`、`HttpServletResponse`。
- Mapper 只负责数据访问，不承载权限、限流和 HTTP 语义。

## 4. 业务域迁移目标

| 业务域 | 当前入口 | 接口数 | 建议目标 Controller | 建议目标应用服务 | 状态 |
|---|---|---:|---|---|---|
| 测试诊断 | `UserController` | 1 | `DiagnosticsController` | 无或 `DiagnosticsService` | 已登记 |
| 用户管理 | `UserController` | 8 | `UserController` | `UserApplicationService`、`UserQueryService` | 已登记 |
| 认证与密码 | `AuthController` | 10 | `AuthController` | `AuthenticationService`、`PasswordService`、`VerificationCodeService` | 已完成 |
| 系部与专业 | `UserController` | 9 | `OrganizationController` | `OrganizationApplicationService` | 已登记 |
| 课题维护与审核 | `UserController` | 9 | `TopicController` | `TopicApplicationService`、`TopicReviewService` | 已登记 |
| 学生选题 | `UserController` | 9 | `TopicSelectionController` | `TopicSelectionApplicationService` | 已登记 |
| 查询与统计 | `UserController` | 8 | `TopicQueryController`、`ReportController` | `TopicQueryService`、`SelectionReportService` | 已登记 |
| 系统开关与配置 | `UserController` | 12 | `SelectionPolicyController`、`SystemController` | `SelectionPolicyService`、`SystemQueryService` | 已登记 |
| 教师选题组 | `UserController` | 3 | `TeacherGroupController` | 现有 `TeacherGroupService` | 已登记 |
| 文件导入导出 | `FileController` | 9 | `FileController` 或拆分 `ImportController`/`ExportController` | `UserImportService`、`TopicImportService`、现有 `SqlExportService` | 已登记 |
| AI | `AIController` | 1 | `AIController` | `AIApplicationService` | 已登记 |

上述名称是目标边界建议，不表示必须一次性创建全部类。应以纵向业务切片逐步迁移。

## 5. HTTP 接口详细台账

权限说明：公开、登录、管理员（`admin`）、系部主任（`dept`）、教师（`teacher`）、学生（`student`）。

### 5.1 测试诊断（1）

| ID | 方法 | 路径 | 权限 | 用途 | 状态 |
|---|---|---|---|---|---|
| TST-001 | GET | `/user/test` | 公开 | 后端连通性测试 | 已登记 |

### 5.2 用户管理（8）

| ID | 方法 | 路径 | 权限 | 用途 | 目标服务 | 状态 |
|---|---|---|---|---|---|---|
| USR-001 | POST | `/user/add` | 管理员 | 创建用户 | `UserApplicationService` | 已登记 |
| USR-002 | POST | `/user/delete` | 管理员 | 删除用户及关联数据 | `UserApplicationService` | 已登记 |
| USR-003 | POST | `/user/update` | 管理员 | 更新用户 | `UserApplicationService` | 已登记 |
| USR-004 | GET | `/user/get/login` | 登录 | 获取当前登录用户 | `UserQueryService` | 已登记 |
| USR-005 | POST | `/user/get/user/page` | 管理员、教师 | 分页查询用户 | `UserQueryService` | 已登记 |
| USR-006 | POST | `/user/get/teacher` | 教师 | 查询教师脱敏列表 | `UserQueryService` | 已登记 |
| USR-007 | GET | `/user/get` | 管理员 | 按 ID 查询用户实体 | `UserQueryService` | 已登记 |
| USR-008 | GET | `/user/get/vo` | 管理员 | 按 ID 查询用户 VO | `UserQueryService` | 已登记 |

### 5.3 认证、密码与验证码（10）

| ID | 方法 | 路径 | 权限 | 用途 | 目标服务 | 状态 |
|---|---|---|---|---|---|---|
| AUTH-001 | POST | `/auth/login` | 公开 | 账号密码登录 | `AuthenticationService` | 已完成；替代 `/user/login` |
| AUTH-002 | POST | `/auth/logout` | 登录 | 退出登录 | `AuthenticationService` | 已完成；替代 `/user/logout` |
| AUTH-003 | POST | `/auth/role-switch` | 系部主任、教师 | 切换可用身份 | `AuthenticationService` | 已完成；替代 `/user/toggle/login` |
| AUTH-004 | GET | `/auth/role-switch/availability` | 登录 | 查询是否可切换身份 | `AuthenticationService` | 已完成；替代 `/user/toggle/available` |
| AUTH-005 | POST | `/auth/password/admin-reset` | 管理员 | 管理员重置用户密码 | `PasswordService` | 已完成；替代 `/user/reset/password` |
| AUTH-006 | POST | `/auth/password/change` | 公开 | 使用当前密码修改密码 | `PasswordService` | 已完成；拆分自 `/user/updata/password` |
| AUTH-007 | POST | `/auth/password/reset` | 公开 | 使用重置码修改密码 | `PasswordService` | 已完成；拆分自 `/user/updata/password` |
| AUTH-008 | POST | `/auth/password/reset-code/send` | 公开 | 发送密码重置码 | `VerificationCodeService` | 已完成；替代 `/user/send/code` |
| AUTH-009 | POST | `/auth/email-verification/code/send` | 公开 | 发送邮箱验证码 | `VerificationCodeService` | 已完成；替代 `/user/send/captcha` |
| AUTH-010 | POST | `/auth/email-verification/code/verify` | 公开 | 换取一次性邮箱凭证 | `VerificationCodeService` | 已完成；替代 `/user/check/captcha` |

### 5.4 系部、专业与选题组归属（9）

| ID | 方法 | 路径 | 权限 | 用途 | 目标服务 | 状态 |
|---|---|---|---|---|---|---|
| ORG-001 | POST | `/user/add/dept` | 管理员 | 添加系部 | `OrganizationApplicationService` | 已登记 |
| ORG-002 | POST | `/user/add/project` | 管理员 | 添加专业 | `OrganizationApplicationService` | 已登记 |
| ORG-003 | POST | `/user/update/project/group` | 管理员 | 设置专业所属选题组 | `OrganizationApplicationService` | 已登记 |
| ORG-004 | POST | `/user/delete/dept` | 管理员 | 删除系部 | `OrganizationApplicationService` | 已登记 |
| ORG-005 | POST | `/user/delete/project` | 管理员 | 删除专业 | `OrganizationApplicationService` | 已登记 |
| ORG-006 | POST | `/user/get/dept/page` | 管理员 | 分页查询系部 | `OrganizationQueryService` | 已登记 |
| ORG-007 | POST | `/user/get/dept/list` | 登录 | 查询可见系部列表 | `OrganizationQueryService` | 已登记 |
| ORG-008 | POST | `/user/get/project/page` | 登录 | 分页查询专业 | `OrganizationQueryService` | 已登记 |
| ORG-009 | POST | `/user/get/project/list` | 登录 | 查询可见专业列表 | `OrganizationQueryService` | 已登记 |

### 5.5 课题维护、审核与发布（9）

| ID | 方法 | 路径 | 权限 | 用途 | 目标服务 | 状态 |
|---|---|---|---|---|---|---|
| TOPIC-001 | POST | `/user/add/topic` | 教师 | 添加课题 | `TopicApplicationService` | 已登记 |
| TOPIC-002 | POST | `/user/delete/topic` | 教师 | 删除课题 | `TopicApplicationService` | 已登记 |
| TOPIC-003 | POST | `/user/get/teacher/topicAmount` | 管理员 | 查询教师课题额度 | `TopicQuotaService` | 已登记 |
| TOPIC-004 | POST | `/user/set/teacher/topicAmount` | 管理员 | 设置教师课题额度 | `TopicQuotaService` | 已登记 |
| TOPIC-005 | POST | `/user/check/topic` | 系部主任、教师 | 审核或重新审核课题 | `TopicReviewService` | 已登记 |
| TOPIC-006 | POST | `/user/set/time/by/id` | 管理员 | 发布课题并设置开放时间 | `TopicPublicationService` | 已登记 |
| TOPIC-007 | POST | `/user/unset/time/by/id` | 管理员 | 取消发布并清空时间 | `TopicPublicationService` | 已登记 |
| TOPIC-008 | POST | `/user/update/topic` | 教师 | 更新课题 | `TopicApplicationService` | 已登记 |
| TOPIC-009 | POST | `/user/get/topic/review_level` | 管理员、教师 | 获取 AI 审核等级 | `TopicReviewService` | 已登记 |

### 5.6 学生选题与教师确认（9）

| ID | 方法 | 路径 | 权限 | 用途 | 目标服务 | 状态 |
|---|---|---|---|---|---|---|
| SEL-001 | POST | `/user/preselect/topic/by/id` | 学生 | 预选或取消预选 | `TopicSelectionApplicationService` | 已登记 |
| SEL-002 | POST | `/user/select/topic/by/id` | 学生 | 提交最终选题 | `TopicSelectionApplicationService` | 已登记 |
| SEL-003 | POST | `/user/select/student` | 教师 | 教师为学生确认课题 | `TopicSelectionApplicationService` | 已登记 |
| SEL-004 | POST | `/user/withdraw` | 教师、学生 | 退选并恢复课题余量 | `TopicSelectionApplicationService` | 已登记 |
| SEL-005 | POST | `/user/get/select/topic/by/id` | 教师 | 查询选择本人课题的学生 | `TopicSelectionQueryService` | 已登记 |
| SEL-006 | POST | `/user/get/preselect/topic` | 学生 | 查询当前学生预选课题 | `TopicSelectionQueryService` | 已登记 |
| SEL-007 | POST | `/user/get/select/topic` | 学生 | 查询当前学生最终选题 | `TopicSelectionQueryService` | 已登记；源码注解无前导 `/` |
| SEL-008 | POST | `/user/get/select/topic/choice_time` | 学生 | 查询选中时间 | `TopicSelectionQueryService` | 已登记 |
| SEL-009 | POST | `/user/get/student/by/topicId` | 教师 | 按课题查询学生 | `TopicSelectionQueryService` | 已登记 |

### 5.7 查询与统计（8）

| ID | 方法 | 路径 | 权限 | 用途 | 目标服务 | 状态 |
|---|---|---|---|---|---|---|
| QRY-001 | POST | `/user/get/topic/page` | 登录 | 分页查询可见课题 | `TopicQueryService` | 已登记 |
| QRY-002 | POST | `/user/get/select/topic/situation` | 管理员、系部主任 | 查询系部选题统计 | `SelectionReportService` | 已登记 |
| QRY-003 | POST | `/user/get/dept/teacher` | 登录 | 分页查询系部教师 | `UserQueryService` | 已登记 |
| QRY-004 | POST | `/user/get/unselect/topic/student/list` | 系部主任 | 查询本系未选题学生 | `SelectionReportService` | 已登记 |
| QRY-005 | POST | `/user/get/topic/list/by/admin` | 管理员 | 管理员分页查询课题 | `TopicQueryService` | 已登记 |
| QRY-006 | POST | `/user/list/page/vo` | 管理员 | 分页查询用户 VO | `UserQueryService` | 已登记 |
| QRY-007 | POST | `/user/get/user/list` | 管理员 | 查询用户名称列表 | `UserQueryService` | 已登记 |
| QRY-008 | POST | `/user/get/dept/teacher/by/admin` | 系部主任 | 查询待审核课题相关教师 | `UserQueryService` | 已登记 |

### 5.8 系统开关与配置（12）

| ID | 方法 | 路径 | 权限 | 用途 | 目标服务 | 状态 |
|---|---|---|---|---|---|---|
| CFG-001 | GET | `/user/cross_topic` | 管理员 | 查询跨系选题开关 | `SelectionPolicyService` | 已登记 |
| CFG-002 | POST | `/user/cross_topic` | 管理员 | 设置跨系选题开关 | `SelectionPolicyService` | 已登记 |
| CFG-003 | GET | `/user/view_topic` | 管理员 | 查询学生查看课题开关 | `SelectionPolicyService` | 已登记 |
| CFG-004 | POST | `/user/view_topic` | 管理员 | 设置学生查看课题开关 | `SelectionPolicyService` | 已登记 |
| CFG-005 | GET | `/user/switch_single_choice` | 管理员 | 查询单选模式 | `SelectionPolicyService` | 已登记 |
| CFG-006 | POST | `/user/switch_single_choice` | 管理员 | 设置单选模式 | `SelectionPolicyService` | 已登记 |
| CFG-007 | GET | `/user/topic_lock` | 管理员、教师、学生 | 查询退选锁定状态 | `SelectionPolicyService` | 已登记 |
| CFG-008 | POST | `/user/topic_lock` | 管理员 | 设置退选锁定状态和时间 | `SelectionPolicyService` | 已登记 |
| CFG-009 | GET | `/user/get/dept/config` | 管理员 | 查询系部跨选配置 | `SelectionPolicyService` | 已登记 |
| CFG-010 | POST | `/user/set/dept/config` | 管理员 | 设置系部跨选配置 | `SelectionPolicyService` | 已登记 |
| CFG-011 | POST | `/user/del/dept/config` | 管理员 | 清除系部跨选配置 | `SelectionPolicyService` | 已登记 |
| CFG-012 | GET | `/user/get/system/info` | 管理员 | 查询系统信息面板 | `SystemQueryService` | 已登记 |

### 5.9 教师选题组（3）

| ID | 方法 | 路径 | 权限 | 用途 | 目标服务 | 状态 |
|---|---|---|---|---|---|---|
| GRP-001 | GET | `/user/teacher/groups` | 教师 | 查询当前教师选题组与额度 | `TeacherGroupService` | 已登记 |
| GRP-002 | POST | `/user/teacher/groups/batch` | 管理员、系部主任 | 批量查询教师选题组额度 | `TeacherGroupService` | 已登记 |
| GRP-003 | GET | `/user/group/list` | 管理员、系部主任 | 查询选题组名称列表 | `TeacherGroupService` | 已登记 |

### 5.10 文件导入导出（9）

| ID | 方法 | 路径 | 权限 | 用途 | 目标服务 | 状态 |
|---|---|---|---|---|---|---|
| FILE-001 | POST | `/file/upload` | 管理员 | 批量导入用户 | `UserImportService` | 已登记 |
| FILE-002 | POST | `/file/upload/topic` | 教师 | 批量导入课题 | `TopicImportService` | 已登记；当前功能关闭 |
| FILE-003 | POST | `/file/get/select/topic/student/list` | 管理员、系部主任 | 下载已选学生课题列表 | `SelectionExportService` | 已登记 |
| FILE-004 | POST | `/file/get/unselect/topic/student/list` | 管理员、系部主任 | 下载未选学生列表 | `SelectionExportService` | 已登记 |
| FILE-005 | POST | `/file/export/user_list` | 管理员 | 导出全部账号 | `SqlExportService` | 已登记 |
| FILE-006 | POST | `/file/export/topic_list` | 管理员 | 导出全部课题 | `SqlExportService` | 已登记 |
| FILE-007 | POST | `/file/export/surplus_topic_list` | 管理员 | 导出剩余课题 | `SqlExportService` | 已登记 |
| FILE-008 | POST | `/file/export/student_topic_list/en_select` | 管理员 | 导出已选学生 | `SqlExportService` | 已登记 |
| FILE-009 | POST | `/file/export/student_topic_list/un_select` | 管理员 | 导出未选学生 | `SqlExportService` | 已登记 |

### 5.11 AI（1）

| ID | 方法 | 路径 | 权限 | 用途 | 目标服务 | 状态 |
|---|---|---|---|---|---|---|
| AI-001 | POST | `/ai/send` | 学生 | AI 问答 | `AIApplicationService` | 已登记；当前功能未开放 |

## 6. AOP 与拦截器状态

### 6.1 当前组件

| ID | 组件 | 实际机制 | 当前状态 | 问题 | 目标 |
|---|---|---|---|---|---|
| AOP-001 | `CacheSearchOptimizationAOP` | Spring `@Aspect` + `@Around` | Bean 已启用，但使用注解均被注释 | 无有效切入点，缓存契约和失效策略未验证 | 决定保留、重写或删除；启用前补充缓存命中、序列化、隔离和失效测试 |
| AOP-002 | `CacheSearchOptimization` | 自定义方法注解 | 未实际使用 | 只支持第一个参数和 `Page<?>` 返回结构 | 若保留，明确 Key 生成器、适用返回类型与失效事件 |
| AOP-003 | `TypeBuilder` | 缓存反序列化工具 | 仅被缓存切面使用 | 位于 `aop` 包但本质是类型工具 | 随缓存方案一起迁移到缓存基础设施包 |
| AOP-004 | `RequestLoggingInterceptor` | Spring MVC `HandlerInterceptor` | 已正确命名并全路径启用 | 仍同时承担日志和封禁 | 后续拆出独立访问频控组件 |
| AOP-007 | Sa-Token 官方 AOP | Spring Advisor | 已启用 | 替代原 `SaInterceptor`，避免重复鉴权 | 保持官方注解契约 |
| AOP-008 | `RequestDtoValidationAspect` | Spring `@Aspect` | 认证接口已启用 | 只校验 `@RequestBody` DTO | 后续按业务切片推广 |
| AOP-009 | `SentinelRateLimitAspect` | Spring `@Aspect` | 认证接口已启用 | 以稳定资源名包围完整 Controller 调用 | 后续逐步替换其余接口的手写 `SphU.entry()` |
| AOP-005 | `@Transactional` | Spring 事务 AOP | 5 个 Service 类级启用 | 事务范围过宽；Controller 又额外使用 `TransactionTemplate` | 写用例事务统一下沉至应用服务公开方法 |
| AOP-006 | `@EnableAspectJAutoProxy` | Spring AOP 配置 | `proxyTargetClass=true, exposeProxy=true` | 当前未发现 `AopContext.currentProxy()` 使用 | 迁移完成后评估是否移除不必要的 `exposeProxy=true` |

### 6.2 AOP 重构原则

- AOP 只处理真正的横切关注点，不承载核心选题状态机和领域规则。
- MVC 请求日志优先使用 `HandlerInterceptor`；不要仅为名称统一而强行改成 Aspect。
- 事务放在应用服务公开方法，避免 Controller 管理事务和同类内部调用绕过代理。
- 缓存切面必须先定义缓存键、租户/角色隔离、TTL、失效事件和序列化失败策略。
- 权限由 Sa-Token 官方 AOP 处理；业务数据范围校验仍属于应用服务。
- 认证请求的切面顺序固定为 Sentinel `-300` → Sa-Token `-200` → DTO 校验 `-100`，事务仍由 Service 代理管理。
- 每一个新切面必须有正常执行、异常执行、代理失效和重复调用相关测试。

## 7. Sentinel 与限流状态

### 7.1 当前事实

| ID | 当前事实 | 风险 | 状态 |
|---|---|---|---|
| RATE-001 | 68 个 Controller 方法重复调用 `initFlowRules()` 和 `SphU.entry()` | 大量样板代码，Controller 被基础设施污染 | 已登记 |
| RATE-002 | `try (Entry ...) {}` 为空，业务代码在 Entry 关闭后执行 | QPS 准入仍可发生，但 RT、异常和熔断统计不能覆盖真实业务 | 已登记 |
| RATE-003 | 规则在请求期间按资源名称动态注册 | 规则生命周期与请求处理耦合 | 已登记 |
| RATE-004 | 资源名通过 Controller 方法名生成 | 不同 Controller 同名方法可能冲突，重命名方法会改变资源标识 | 已登记 |
| RATE-005 | 默认阈值由 `2000 * 0.75 * 2 * 1` 计算为 3000 QPS | 阈值与具体接口成本无关，难以保护高成本资源 | 已登记 |
| RATE-006 | 登录另有 Redis 账号/IP 防爆破限流 | 与 Sentinel 目标不同，迁移时可能被误删 | 已登记 |
| RATE-007 | `BlockException` 由全局异常处理器转换为 `BaseResponse` | 需保持前端业务码兼容 | 已登记 |

### 7.2 目标限流分层

| 层级 | 保护目标 | 推荐机制 | 示例 |
|---|---|---|---|
| Caddy/Nginx/API Gateway | 系统总流量、单 IP 粗粒度流量 | 网关限流 | 单 IP 每分钟请求数、全站突发流量 |
| MVC 接入层 | HTTP 路由级 QPS | 统一 Interceptor 或围绕 Controller 的注解切面 | `/user/login`、文件导出接口 |
| Application Service | 高成本业务能力 | Sentinel 资源包裹完整 Service 用例 | 最终选题、AI 审核、邮件发送、批量导入 |
| Redis 业务限流 | 账号/IP 防爆破、验证码频控 | 保留原子 Lua 计数 | 登录失败、邮箱验证码发送 |

### 7.3 推荐迁移方向

1. 先建立稳定资源名，不再使用 Java 方法名隐式生成，例如：

   ```text
   auth.login
   topic.query.page
   topic.selection.confirm
   topic.review.ai
   file.user.import
   ```

2. 将规则加载移至启动配置或外部规则源，不在每个请求中初始化。
3. 接口级限流统一放在 MVC 接入层，删除 Controller 中重复的 Sentinel 样板代码。
4. 对最终选题、退选、AI、邮件、批量导入等高成本用例，在 Application Service 层包裹完整业务执行过程。
5. 保留登录和验证码的 Redis 防爆破限流，因为它解决的是用户维度的业务安全问题，不等价于全局 QPS 限流。
6. 保持现有 `CodeBindMessageEnums.FLOW_RULES` 响应兼容，再考虑统一 HTTP 429 状态码。

限流方案最终选型状态：**认证域已采用注解 AOP**。其他业务域仍按切片逐步迁移。

## 8. 建议实施顺序

### 阶段 0：锁定行为

- [x] 建立接口基线台账，认证域迁移后更新为 79 个接口。
- [x] 记录 Controller/Mapper/事务/Sentinel 当前耦合数据。
- [ ] 为核心接口补充 Controller 集成测试。
- [ ] 为选题并发、退选、课题余量补充事务回归测试。
- [ ] 保存 Knife4j/OpenAPI 接口快照或生成接口契约基线。

### 阶段 1：认证域试点

- [x] 将登录、退出、身份切换迁移到 `AuthController` 与 `AuthenticationService`。
- [x] 移除旧 `/user/**` 认证路由并同步迁移前端调用。
- [x] 将密码和验证码迁移到 `PasswordService` 与 `VerificationCodeService`。
- [x] 保留 Redis 防爆破与验证码限流。
- [x] 移除新认证 Controller 中的事务、Mapper、Redis 和 Sentinel 手写调用。

### 阶段 2：选题写用例

- [ ] 迁移预选、最终选题、教师确认和退选。
- [ ] 将悲观锁 Mapper 调用封装进事务应用服务。
- [ ] 一个公开用例方法对应一个清晰事务边界。
- [ ] 验证并发下课题余量、重复选题和退选恢复。

### 阶段 3：课题、组织与查询

- [ ] 迁移课题维护、审核、发布和额度管理。
- [ ] 迁移系部、专业和选题组归属。
- [ ] 创建查询服务，避免查询接口继续堆积在 `UserController`。
- [ ] 评估查询事务的 `readOnly=true` 或无事务策略。

### 阶段 4：AOP 与 MVC 横切组件

- [x] 更正 `RequestLogAOP` 命名和包归属，迁移为 `RequestLoggingInterceptor`。
- [ ] 决定缓存切面的保留、重写或删除。
- [ ] 收窄 `@Transactional` 范围。
- [ ] 评估 `exposeProxy=true` 是否仍有必要。

### 阶段 5：限流迁移

- [x] 为认证域定义稳定 Sentinel 资源名和接口分级阈值。
- [x] 将认证域规则加载移出请求方法。
- [x] 认证域接入层统一使用注解 AOP 处理路由级限流。
- [ ] Service 层保护高成本业务资源。
- [ ] 删除 68 处 Controller Sentinel 样板代码。
- [x] 验证认证域限流响应、完整方法包围和异常记录实现。

### 阶段 6：文件与剩余模块

- [ ] 将文件导入事务下沉至 Import Service。
- [ ] 保持文件流响应和 CSV 安全处理行为不变。
- [ ] 明确未开放的课题导入和 AI 接口是继续实现还是正式废弃。
- [ ] 全量验证 79 个接口并更新本台账状态。

## 9. 每次变更的更新格式

完成一个接口或一个纵向切片后，在对应接口行更新状态，并在此追加记录：

```markdown
### YYYY-MM-DD：变更主题

- 接口 ID：AUTH-001、AUTH-002
- 原 Controller：UserController
- 新 Controller：AuthController
- 新 Service：AuthenticationService
- 路径兼容：是
- 请求/响应兼容：是
- 横切逻辑变化：限流由 Controller 手写迁移至统一组件
- 验证：相关测试、mvn test、前端调用检查
- 遗留问题：无 / 问题说明
```

## 10. 变更记录

### 2026-09-27：完成认证模块重构与 AOP 架构升级

- 接口 ID：AUTH-001～AUTH-010。
- 原 Controller：`UserController`。
- 新 Controller：`AuthController`。
- 新 Service：`AuthenticationService`、`PasswordService`、`VerificationCodeService`。
- 路径兼容：不保留旧路径；前后端已同步迁移。
- 请求/响应兼容：保持 `BaseResponse` 与业务错误码；管理员临时密码改为响应 VO 字段。
- 横切逻辑变化：Sa-Token 官方 AOP 鉴权、Sentinel 注解 AOP 限流、DTO 校验 AOP。
- 业务安全：保留 Redis 账号/IP/邮箱限频，引入一次性邮箱 `proofToken`。
- 验证：后端 159 个测试通过；前端类型检查、ESLint、2 个契约测试和生产构建通过。
- 详细记录：[AUTH_AOP_REFACTOR_COMPLETION.md](./AUTH_AOP_REFACTOR_COMPLETION.md)。

### 2026-09-27：建立重构基线

- 登记全部 78 个 HTTP 接口。
- 记录 Controller 与 Service 未完全解耦的现状。
- 记录当前 AOP、MVC Interceptor、事务和 Sentinel 限流状态。
- 所有接口初始状态设为“已登记”。
- 尚未修改任何运行时代码或接口行为。
