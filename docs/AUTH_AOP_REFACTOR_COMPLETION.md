# 认证模块重构与 AOP 架构升级完成记录

> 完成日期：2026-09-27  
> 实施状态：已完成  
> 接口基线：项目共 79 个 HTTP 接口，其中认证域 10 个  
> 关联台账：[HTTP_API_REFACTOR_STATUS.md](./HTTP_API_REFACTOR_STATUS.md)

## 1. 完成结论

本次重构已经将认证、密码和验证码能力从原 `UserController` 中完整迁出，并完成以下架构调整：

- 新建独立的 `AuthController`，统一使用 `/auth/**` 路径。
- 原有 9 个 `/user/**` 认证接口已删除，不保留兼容别名；密码修改拆分后形成 10 个新接口。
- Sa-Token 使用官方 Spring AOP 执行登录、角色和权限鉴权，原 MVC `SaInterceptor` 已移除。
- Sentinel 使用自定义注解和 Spring AOP 包围完整 Controller 方法。
- DTO 保留 Bean Validation 约束注解，但 Controller 不使用 `@Valid`，改由统一 DTO 校验切面触发。
- 账号状态、密码真实性、身份切换资格、验证码和邮箱凭证等业务规则均保留在 Service。
- Controller 不再直接访问认证相关 Mapper、Redis、Sentinel API 或事务模板。
- 前端登录、退出、角色切换、密码修改、验证码和管理员重置密码调用均已迁移。
- HTTP 响应继续保持 `BaseResponse {code,message,data}` 和既有业务错误码体系。

## 2. 接口迁移结果

### 2.1 新认证接口

| ID | 方法 | 新路径 | 鉴权要求 | 请求 | 主要响应数据 |
|---|---|---|---|---|---|
| AUTH-001 | POST | `/auth/login` | 公开 | `{account,password}` | `LoginUserVO` |
| AUTH-002 | POST | `/auth/logout` | 登录 | 无 | `boolean` |
| AUTH-003 | POST | `/auth/role-switch` | 教师或系负责人 | `{targetRole}` | `LoginUserVO` |
| AUTH-004 | GET | `/auth/role-switch/availability` | 登录 | 无 | `{available,targetRole?}` |
| AUTH-005 | POST | `/auth/password/admin-reset` | 管理员 | `{account,name}` | `{account,temporaryPassword}` |
| AUTH-006 | POST | `/auth/password/change` | 公开 | `{account,currentPassword,newPassword,email?,emailProofToken?}` | 用户 ID |
| AUTH-007 | POST | `/auth/password/reset` | 公开 | `{account,resetCode,newPassword}` | 用户 ID |
| AUTH-008 | POST | `/auth/password/reset-code/send` | 公开 | `{account}` | 通用发送结果 |
| AUTH-009 | POST | `/auth/email-verification/code/send` | 公开 | `{email}` | 发送结果 |
| AUTH-010 | POST | `/auth/email-verification/code/verify` | 公开 | `{email,code}` | `{proofToken,expiresInSeconds}` |

### 2.2 已移除旧接口

| 原路径 | 替代接口 |
|---|---|
| `POST /user/login` | `POST /auth/login` |
| `POST /user/logout` | `POST /auth/logout` |
| `POST /user/toggle/login` | `POST /auth/role-switch` |
| `GET /user/toggle/available` | `GET /auth/role-switch/availability` |
| `POST /user/reset/password` | `POST /auth/password/admin-reset` |
| `POST /user/updata/password` | `POST /auth/password/change`、`POST /auth/password/reset` |
| `POST /user/send/code` | `POST /auth/password/reset-code/send` |
| `POST /user/send/captcha` | `POST /auth/email-verification/code/send` |
| `POST /user/check/captcha` | `POST /auth/email-verification/code/verify` |

旧方法及其辅助实现已从 `UserController` 物理删除，不是仅移除 `@RequestMapping`。

## 3. 最终请求执行链

认证请求进入应用后的主要执行顺序如下：

```text
HTTP 请求
  → Servlet Filter 链
  → DispatcherServlet
  → RequestLoggingInterceptor.preHandle
  → SentinelRateLimitAspect              order = -300
  → Sa-Token 官方鉴权 Advisor             order = -200
  → RequestDtoValidationAspect           order = -100
  → AuthController
  → AuthenticationService / PasswordService / VerificationCodeService
  → Service 事务 AOP（写操作）
  → Mapper / Redis / Sa-Token / MailService
  → Controller 返回 BaseResponse
  → Jackson 序列化为 JSON
```

职责边界：

- `RequestLoggingInterceptor`：请求日志及原有访问检查，属于 Spring MVC `HandlerInterceptor`，不属于 AOP。
- `SentinelRateLimitAspect`：路由级流量准入、RT 和异常统计。
- Sa-Token 官方 Advisor：登录、角色和权限鉴权。
- `RequestDtoValidationAspect`：请求 DTO 的空值、长度和格式等基础约束。
- Service：账号状态、密码真实性、角色配对、验证码、邮箱凭证、事务等业务规则。
- 全局异常处理器：统一转换业务异常、Sa-Token 异常、Sentinel 异常和 JSON 解析异常。

## 4. Sa-Token 鉴权升级

### 4.1 已完成内容

- 引入 `sa-token-spring-aop:1.42.0`。
- 删除 `SaInterceptor` 的 Spring MVC 注册，避免同一请求重复鉴权。
- 保留并继续使用官方注解：
  - `@SaIgnore`
  - `@SaCheckLogin`
  - `@SaCheckRole`
  - `@SaCheckPermission`
- 通过 `SaTokenAopOrderConfigurer` 将官方 Advisor 顺序设置为 `-200`。
- 会话主动操作统一封装到 `AuthSessionManager`。

### 4.2 当前鉴权分配

| 接口类别 | 注解策略 |
|---|---|
| 登录、当前密码改密、重置码改密、验证码 | `@SaIgnore` |
| 退出登录、角色可用性 | `@SaCheckLogin` |
| 角色切换 | `@SaCheckLogin` + 教师/系负责人角色校验 |
| 管理员重置密码 | `@SaCheckLogin` + 管理员角色校验 |

本项目没有再封装一层重复执行 `StpUtil.check*` 的自定义鉴权切面；自定义切面仅负责 Sentinel 和 DTO 校验。

## 5. Sentinel 限流升级

### 5.1 实现结果

- 新增 `@SentinelRateLimit(resource = "...")`。
- 新增 `SentinelRateLimitAspect`，顺序为 `-300`。
- 切面使用 `SphU.entry()` 包围完整 Controller 方法，并在 `finally` 中关闭 `Entry`。
- 业务执行异常通过 `Tracer.trace()` 记录后继续向外抛出。
- `BlockException` 继续由全局异常处理器转换为 `FLOW_RULES`。
- 新增 `SentinelRuleRegistry`，认证规则仅在启动或显式注册时集中发布。
- 旧 `SentineManager` 已改为委托新注册器，避免覆盖认证模块规则。

### 5.2 当前认证资源阈值

| Sentinel 资源 | 默认单实例 QPS | 环境变量 |
|---|---:|---|
| `auth.login` | 100 | `APP_SENTINEL_AUTH_LOGIN` |
| `auth.logout` | 300 | `APP_SENTINEL_AUTH_LOGOUT` |
| `auth.role-switch` | 100 | `APP_SENTINEL_AUTH_ROLE_SWITCH` |
| `auth.role-switch-availability` | 300 | `APP_SENTINEL_AUTH_ROLE_SWITCH_AVAILABILITY` |
| `auth.password-admin-reset` | 20 | `APP_SENTINEL_AUTH_PASSWORD_ADMIN_RESET` |
| `auth.password-change` | 50 | `APP_SENTINEL_AUTH_PASSWORD_CHANGE` |
| `auth.password-reset` | 50 | `APP_SENTINEL_AUTH_PASSWORD_RESET` |
| `auth.password-reset-code-send` | 20 | `APP_SENTINEL_AUTH_PASSWORD_RESET_CODE_SEND` |
| `auth.email-code-send` | 20 | `APP_SENTINEL_AUTH_EMAIL_CODE_SEND` |
| `auth.email-code-verify` | 100 | `APP_SENTINEL_AUTH_EMAIL_CODE_VERIFY` |

Sentinel 解决接口资源的单实例流量控制，不承担身份鉴权，也不能替代 Redis 的账号、邮箱和 IP 业务防刷。

## 6. DTO 校验 AOP

### 6.1 已完成内容

- DTO 字段继续使用 `@NotBlank`、`@Size`、`@Email`、`@Pattern` 等标准约束。
- 新密码使用自定义 `@Utf8ByteLength(min = 8, max = 72)`，按 UTF-8 字节数校验 BCrypt 输入边界。
- Controller 方法使用 `@ValidateRequest`，不使用 `@Valid` 或 `@Validated`。
- `RequestDtoValidationAspect` 只检查方法参数中标记为 `@RequestBody` 的对象。
- 请求体为 `null` 时统一返回 `PARAMS_ERROR`。
- 多个字段失败时按属性路径排序后返回第一个错误，保证结果稳定。
- JSON 语法错误和字段类型转换失败由 `HttpMessageNotReadableException` 全局处理器转换为 `PARAMS_ERROR`。

### 6.2 明确边界

DTO 校验切面不执行以下逻辑：

- 不查询数据库或 Redis。
- 不判断当前登录状态。
- 不校验密码是否正确。
- 不判断账号是否封禁。
- 不判断教师与系负责人是否存在配对账号。
- 不校验验证码或 `proofToken` 的真实性。

这些规则全部由对应 Service 处理。

## 7. Service 拆分结果

### 7.1 AuthenticationService

负责：

- 账号密码登录和退出登录。
- 未知账号伪散列校验，降低账号枚举与计时差异风险。
- 封禁角色、初始密码状态检查。
- 旧 MD5 密码登录成功后升级为 BCrypt。
- 教师与系负责人身份切换及可用性查询。
- 调用 `AuthSessionManager` 创建和注销 Sa-Token 会话。

### 7.2 PasswordService

负责：

- BCrypt 加密、密码匹配和旧 MD5 兼容迁移。
- 8～72 个 UTF-8 字节密码边界。
- 当前密码修改、重置码改密、管理员重置。
- 临时密码安全随机生成。
- 超级管理员保护。
- 数据库写事务和凭证更新事件发布。
- 事务提交成功后通过 `CredentialsChangedListener` 注销相关会话。

原 `UserService` 中的密码辅助方法已迁移并删除，`FileController`、`AdminBootstrap` 和用户创建流程统一使用 `PasswordService`。

### 7.3 VerificationCodeService

负责：

- 邮箱规范化和允许域名检查。
- 密码重置码与邮箱验证码的生成、发送、消费和过期处理。
- 未知账号发送重置码时返回通用成功信息，避免直接暴露账号是否存在。
- 邮箱验证码通过后生成一次性 `proofToken`。

邮箱域名白名单默认值为：

```text
qq.com,gmail.com,nfu.edu.cn
```

可通过 `APP_ALLOWED_EMAIL_DOMAINS` 覆盖。

## 8. 邮箱 proofToken 与验证码

- 邮箱验证码有效期：2 分钟。
- `proofToken` 有效期：5 分钟。
- Token 使用 32 字节安全随机数生成，并采用 URL-safe Base64 编码返回前端。
- Redis 键只包含 Token 的 SHA-256 摘要，不保存明文 Token。
- Redis 值保存规范化后的邮箱，用于绑定关系校验。
- `consumeEmailProof()` 使用 Redis Lua 脚本原子校验并删除，成功、失败或重复使用均不能再次消费同一凭证。
- 前端只在当前密码页面内存中保存 `proofToken`。
- 邮箱改变、校验失败和密码提交成功时清除前端凭证状态。
- 不再使用 `HttpSession` 保存“邮箱已验证”状态。

## 9. Redis 业务限频保留情况

| 维度 | 当前限制 |
|---|---:|
| 单登录账号 | 8 次 / 5 分钟 |
| 登录 IP | 300 次 / 5 分钟 |
| 单邮箱或重置目标 | 1 次 / 分钟 |
| 邮件 IP | 100 次 / 10 分钟 |
| 全局邮件 | 200 次 / 小时 |

以上限制由 `SecurityRateLimitManager` 和 Redis Lua 脚本处理，与 Sentinel QPS 限流并行存在。

## 10. 前端迁移情况

新增统一认证客户端 `authController.ts`，已迁移以下位置：

- 登录页：请求字段由旧字段映射为 `{account,password}`。
- 头像菜单：退出登录、角色切换和切换可用性查询。
- 密码页面：区分“当前密码修改”和“重置码改密”两个接口。
- 管理员列表、系负责人列表、教师列表和学生列表：管理员重置密码。
- 管理员重置成功后从响应 VO 的 `temporaryPassword` 字段展示临时密码，不再解析 `message`。
- 旧前端 `userController.ts` 中的 9 个认证请求函数已删除。

前端认证服务中已经不存在旧 `/user/**` 认证 API 引用；页面路由 `/user/login` 仍是前端页面地址，不是后端接口。

## 11. 配置与构建兼容

- Sentinel 认证阈值已加入 `application.yaml`，全部支持环境变量覆盖。
- 邮箱域名白名单已加入 `app.security.allowed-email-domains`。
- Sa-Token 依赖统一为 `1.42.0`。
- Lombok 显式升级为 `1.18.32`，解决默认 JDK 21 编译时的 `JCTree.qualid` 兼容问题，同时仍以 Java 8 字节码为构建目标。

## 12. 测试与验证结果

### 12.1 后端

执行命令：

```powershell
.\mvnw.cmd clean test
```

结果：

```text
Tests run: 159
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

新增或调整的重点测试覆盖：

- 10 个认证接口路径、HTTP 方法和 Sentinel 注解契约。
- 旧 9 个认证路径已不存在。
- Controller 未使用 `@Valid`，请求体接口均使用 `@ValidateRequest`。
- AOP 顺序：Sentinel `-300`、Sa-Token `-200`、DTO 校验 `-100`。
- DTO 请求体为空、字段约束失败、稳定错误顺序和正常放行。
- 正常登录、未知账号伪散列和封禁账号。
- BCrypt、旧 MD5 迁移、临时密码和 UTF-8 字节边界。
- 未知账号重置码通用响应、邮箱域名限制和邮箱凭证生成。
- Redis 登录限频键规范化和部分配额回收。

### 12.2 前端

| 命令 | 结果 |
|---|---|
| `pnpm tsc` | 通过 |
| `pnpm lint:js` | 通过 |
| `pnpm test -- --runInBand` | 1 个测试套件、2 个测试通过 |
| `pnpm build` | 生产构建通过 |

前端契约测试确认：

- 10 个 `/auth/**` 路径均已登记。
- 9 个旧 `/user/**` 认证 API 均已从前端 Service 中移除。

## 13. 完成标准核对

| 完成标准 | 状态 | 结果 |
|---|---|---|
| 原认证方法移出 `UserController` | 完成 | 方法与辅助实现已物理删除 |
| `AuthController` 不包含业务实现 | 完成 | 只接收请求、提取 IP/设备、调用 Service、包装响应 |
| 认证 Controller 不访问 Mapper、Redis、`SphU`、事务模板 | 完成 | 未发现相关依赖或调用 |
| DTO 基础校验由 AOP 触发 | 完成 | 使用 `@ValidateRequest`，未使用 `@Valid` |
| 认证接口由 Sentinel AOP 限流 | 完成 | 10 个接口均配置稳定资源名 |
| Sa-Token 官方 AOP 负责鉴权 | 完成 | `SaInterceptor` 已移除 |
| 业务规则保留在 Service | 完成 | 账号、凭证、角色配对、验证码均在 Service |
| Redis 业务防刷保留 | 完成 | 账号、邮箱、IP、全局邮件限频均保留 |
| 前端移除旧认证 API | 完成 | 已通过前端契约测试 |
| 接口台账更新为 79 | 完成 | `59 + 10 + 9 + 1 = 79` |

## 14. 本阶段未包含的范围

以下内容不属于本次认证域重构，仍按后续阶段处理：

- 其余 69 个接口的 Controller/Service 分层重构。
- 其余接口中手写 Sentinel 样板代码的迁移。
- 选题、退选、课题审核、文件导入等高成本 Service 资源保护。
- 全局角色数据模型调整。
- HTTP 业务错误统一迁移为标准 HTTP 状态码或 429。
- `CacheSearchOptimizationAOP` 的保留、重写或删除。
- `@EnableAspectJAutoProxy(exposeProxy = true)` 的最终评估。

认证模块已经具备后续业务模块可复用的三类横切模板：Sa-Token 官方鉴权 AOP、Sentinel 限流 AOP、DTO 基础校验 AOP。
