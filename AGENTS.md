# 项目级上下文与协作规则

本文件是仓库级 AI 协作入口，适用于本目录及其全部子目录。Java 编码规范采用渐进式加载：本文件只保存触发条件和规范入口，不复制完整技能正文。

## 一、Java 编码规范的渐进式加载(没有说明情况下始终禁用)

### 1. 规范入口

- Java 编码规范的唯一来源是 `C:/Users/abc/.codex/skills/java-coding-conventions/SKILL.md`。
- 涉及新增或修改 Java 代码、Java 注释与 Javadoc、Java 格式化、Java 重构、Java 单元测试时，必须在行动前完整读取上述 `SKILL.md`。
- 仅处理前端、Markdown 文档、Git 操作、需求分析或其他不涉及 Java 文件的任务时，不加载该技能，避免无关规范占用上下文。
- 用户当前任务中的明确要求优先于技能；本文件中的快速索引不能替代技能正文。

### 2. 按需加载顺序

1. 先根据目标文件判断任务是否涉及 Java。
2. 若涉及 Java，完整读取 `java-coding-conventions/SKILL.md`，再阅读和修改目标代码。
3. 只在需要校验 Java 变更时，按需读取并执行 `java-coding-conventions/scripts/verify-style.ps1`，不为非 Java 任务加载校验脚本。
4. 校验时将范围限制在本次改动涉及的 Java 文件或源码目录，不用无关存量问题扩大任务范围。

Windows PowerShell 校验命令：

```powershell
pwsh "C:/Users/abc/.codex/skills/java-coding-conventions/scripts/verify-style.ps1" -TargetPath "<Java 文件或源码目录>"
```

### 3. 规范快速索引

| Java 任务 | 加载后重点检查 |
|---|---|
| 新增或重构生产代码 | 类、方法、字段 Javadoc，分层职责，命名、导入、缩进和模块分隔线 |
| 仅调整注释或格式 | 不修改字符串字面量、业务逻辑、查询键、缓存键和配置键 |
| 修改 DTO、VO、Entity、Properties、常量类 | 字段 Javadoc、模型命名、`serialVersionUID` 位置及相关注解 |
| 修改 Service 或实现类 | 接口说明 WHAT，实现类说明 HOW，业务方法和私有辅助方法均有 Javadoc |
| 修改 Java 测试 | 每个测试使用场景说明，并按技能约定组织测试步骤注释 |
| 完成 Java 变更 | 运行规范校验脚本，再运行与改动风险相匹配的测试 |

## 二、沟通与技能调用

- 默认使用简体中文回复；代码标识符、命令、日志和报错信息保持原始语言。
- 用户没有特别说明时，不调用 `java-spring-mentor` 技能。
- 用户没有特别说明时，不调用任何 `superpowers` 系列技能，包括但不限于 `using-superpowers`、`brainstorming`、`systematic-debugging`、`test-driven-development`、`writing-plans` 和 `verification-before-completion`。
- 只有用户在当前任务中明确要求使用上述技能时，才允许调用。
- 讲解代码时优先依据仓库中的真实实现，不把通用 Spring 理论描述成项目当前行为。
- 发现项目实现与标准实践不同，应明确区分“当前实现”和“推荐实现”，不得擅自重构。

## 三、项目概览

本项目是前后端分离的毕业选题管理系统（广州南方学院毕设选题管理系统 / NCG Graduation Topic Selection System），采用单体仓库与 Maven 多模块架构：

- `nfu-graduation-topic-selection-backend/`：Java 8、Spring Boot 2.5.6、Spring MVC、MyBatis-Plus、MySQL、Redis、Sa-Token、Sentinel、Caffeine、WebSocket（根包 `cn.edu.nfu.topicselection`）。
- `integration-tests/`：基于 Testcontainers（MySQL 8 + Redis 7）的后端端到端集成测试与 Knife4j 接口文档契约测试模块。
- `nfu-graduation-topic-selection-frontend/`：React、TypeScript、Umi Max、Ant Design Pro，使用 pnpm 管理依赖。
- `deploy/`：Docker Compose、Caddy 与部署相关文件。
- `AGENTS.md`：项目上下文、协作边界与 Java 规范的渐进式加载入口。
- `C:/Users/abc/.codex/skills/java-coding-conventions/`：Java 编码规范正文与校验脚本。
- `README.md`：环境配置、启动、构建与部署说明。

本地默认地址：

- 前端：`http://127.0.0.1:3000`
- 后端：`http://127.0.0.1:8000`
- Knife4j：`http://127.0.0.1:8000/doc.html`

## 四、后端架构事实

后端主要包职责如下：

- `controller`：HTTP 接口、请求参数接收与业务编排。
- `service`、`service.impl`：业务服务与事务边界。
- `mapper`：MyBatis-Plus 数据访问层。
- `model.entity`：数据库实体。
- `model.request`：前端 HTTP 请求模型，统一使用 `XxxRequest` 后缀。
- `model.dto`：内部传输或查询模型，统一使用 `XxxDTO`、`XxxQuery` 等后缀。
- `model.vo`：响应视图模型，统一使用 `XxxVO` 后缀。
- `manager`：Redis、Sa-Token、Sentinel、WebSocket、MyBatis-Plus 等基础设施封装。
- `response`：`BaseResponse<T>` 与 `TheResult` 统一响应封装。
- `exception`：业务异常与全局异常处理。
- `aop`：Sentinel 限流、DTO 基础校验及缓存优化等 Spring AOP 切面。
- `interceptor`：Spring MVC 请求日志拦截器。

普通 HTTP 请求的实际主链路为：

```text
客户端
  -> Tomcat / Servlet Filter Chain
  -> DispatcherServlet
  -> Spring MVC HandlerInterceptor preHandle
  -> JSON 反序列化与参数绑定
  -> Sentinel / Sa-Token / DTO 校验 Spring AOP
  -> Controller
  -> Service / Transaction AOP
  -> MyBatis-Plus Mapper
  -> MySQL
  -> BaseResponse
  -> Jackson JSON 序列化
  -> 客户端
```

阅读和修改后端时注意以下现状：

- `RequestLoggingInterceptor` 是 `HandlerInterceptor`，负责请求日志，不是 `@Aspect` 切面。
- Sa-Token 使用官方 Spring AOP 处理 `@SaCheckLogin`、`@SaCheckRole`、`@SaCheckPermission` 和 `@SaIgnore`；项目不再注册 `SaInterceptor`，避免重复鉴权。
- 认证接口按 Sentinel `-300`、Sa-Token `-200`、DTO 校验 `-100` 的顺序执行横切逻辑。
- `RequestDtoValidationAspect` 只负责请求体的基础字段约束；账号状态、凭证真实性、验证码匹配和角色切换资格保留在 Service。
- `CacheSearchOptimizationAOP` 是项目自定义的真正 Spring AOP 切面；当前 Controller 中的使用注解被注释，默认没有实际切入点。
- 多个 Service 实现类使用类级别 `@Transactional`，事务通过 Spring AOP 代理实现。
- 认证模块通过 `@SentinelRateLimit` 和 `SentinelRateLimitAspect` 限流；尚未迁移的其他模块仍可能在 Controller 中手动调用 `SphU.entry(...)`，分析时以真实实现为准。
- 认证相关 10 个接口位于 `AuthController`，业务拆分到认证、密码和验证码服务；`UserController` 的其他存量业务仍可能直接调用 Mapper 或 Manager，不得假定所有接口都已完成标准分层。
- `GlobalExceptionHandler` 将业务异常、Sa-Token 异常、Sentinel 异常和校验异常转换为统一的 `BaseResponse`。
- 项目主要使用响应体中的 `code` 表示业务成功或失败，不要假定所有业务错误都对应 HTTP 4xx/5xx。

## 五、前端架构事实

- API 调用主要位于 `nfu-graduation-topic-selection-frontend/src/services/topic-selection/`。
- 全局请求配置位于 `nfu-graduation-topic-selection-frontend/src/app.tsx`，本地开发时后端基础地址指向 `8000` 端口，生产反向代理基础地址为 `/api`。
- 请求开启 `withCredentials`，用于携带 Sa-Token Cookie（`nfu-topic-selection`）。
- 全局业务错误处理位于 `nfu-graduation-topic-selection-frontend/src/requestErrorConfig.ts`，按照 `{ code, message, data }` 结构处理响应。
- 修改后端接口路径、请求 DTO 或响应 VO 时，应同步检查前端 service、类型定义和调用页面。

## 六、修改边界

- 修改代码前先阅读目标文件及其直接调用者、被调用者；涉及后端接口时同时检查前端调用。
- 保持现有 API 路径、请求字段、响应字段和业务状态码兼容，除非用户明确要求破坏性变更。
- 不因顺手优化而扩大修改范围，不擅自拆分大型 Controller、替换鉴权框架或迁移数据模型。
- 不把 MVC Interceptor、Servlet Filter、Spring AOP、Controller Advice 和 Sentinel 混为同一种机制。
- 命中 Java 任务触发条件后，必须完整加载 `java-coding-conventions` 技能并遵守其注释、Javadoc、命名、依赖注入和格式规范。
- 优先使用构造清晰、范围最小的变更，并为行为变化补充或更新测试。
- 保留用户已有的未提交修改，不覆盖、不回滚与当前任务无关的文件。

## 七、验证命令

Windows PowerShell 下优先使用以下命令（在仓库根目录执行）。

后端：

```powershell
.\mvnw.cmd test
.\mvnw.cmd verify
.\mvnw.cmd -pl nfu-graduation-topic-selection-backend spring-boot:run
```

前端：

```powershell
pnpm --dir nfu-graduation-topic-selection-frontend install --frozen-lockfile
pnpm --dir nfu-graduation-topic-selection-frontend lint
pnpm --dir nfu-graduation-topic-selection-frontend test
pnpm --dir nfu-graduation-topic-selection-frontend build
```

- 只修改文档时，无需运行完整构建，但应检查 Markdown 内容和 Git diff。
- 修改后端业务逻辑时至少运行相关测试；修改公共接口或持久层时优先运行完整后端测试。
- 修改前端 TypeScript 时至少运行 `pnpm tsc` 和相关测试；涉及构建配置时运行 `pnpm build`。
