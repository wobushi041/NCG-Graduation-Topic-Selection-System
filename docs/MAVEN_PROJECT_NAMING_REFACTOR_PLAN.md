# Maven 工程命名与模块化重构实施计划

> 状态：计划已确认，尚未实施
> 目标项目：广州南方学院毕业选题管理系统
> 英文名称：NCG Graduation Topic Selection System
> 适用基线：Wave 1 ～ Wave 6 解耦重构完成后的当前仓库

> 2026-09-28 目录命名修订：后端目录由 `server/` 调整为 `nfu-graduation-topic-selection-backend/`，前端目录由 `web/` 调整为 `nfu-graduation-topic-selection-frontend/`。下文阶段步骤记录的是当时的迁移路径；执行剩余验证命令时须将目录参数替换为新路径。Maven `artifactId`、前端 `package.json` 名称与 Docker 镜像名仍使用下文所列值，目录名不等于产物名。

## 0. 给执行 Agent 的交接入口

本文件可交给能访问本仓库的编码 Agent 执行，包括 Antigravity。执行者先从仓库根目录阅读 `AGENTS.md`、本计划及 `docs/HTTP_API_REFACTOR_STATUS.md`，再核对当前文件与计划中的基线是否一致。本计划中的相对路径均以仓库根目录为基准；不要把示例中的原机器盘符当作项目路径。每个阶段执行完成后记录改动文件、验证命令和结果，再进入下一阶段；命令返回非零退出码时先排查，不跳过失败继续迁移。

运行环境需有 JDK 8、Node.js 24、pnpm 11.19.0、Docker Desktop/Compose v2（集成测试阶段）、PowerShell 和可用的 Maven Wrapper。前端依赖若缺失，按锁文件执行 `pnpm --dir web install --frozen-lockfile`（目录迁移前将 `web` 替换为旧前端目录）；后端依赖由 Maven Wrapper 解析。涉及 Java 文件时，先遵守仓库 `AGENTS.md` 指向的 Java 编码规范。该规范目前位于本机 `C:/Users/abc/.codex/skills/java-coding-conventions/SKILL.md`；若执行环境无法访问它，需先解决规范可访问性，不能假定 Antigravity 自带同名技能。

以下事项不由重构 Agent 从仓库内推断：远端 Git 仓库目前未配置，`nfu-graduation-topic-selection` 是将来创建或更名远端时的目标；本机仓库目录是否更名不影响 Maven 构建，也不作为阶段验收门槛。数据库与 Docker 卷仅在确认属于本项目且没有需保留的数据时清理，清理不是构建与测试的前置条件。

可直接交给 Antigravity 的任务指令：

> 请阅读仓库根目录 `AGENTS.md`、`docs/MAVEN_PROJECT_NAMING_REFACTOR_PLAN.md` 和 `docs/HTTP_API_REFACTOR_STATUS.md`，从阶段 0 起按顺序实施到阶段 9。每阶段先核对当前文件和未提交修改，再实施、运行该阶段验收命令并记录结果；通过后继续下一阶段。保留现有用户修改，不用跳过测试、删除有效测试或改用本机数据库绕过集成测试。若 Java 规范文件、Docker、必要凭据或其他前置条件缺失，完成不依赖该条件的工作后，报告准确阻塞点、已完成阶段和可复现命令。最终给出改动摘要、测试结果、剩余风险和 `git status --short`。不要创建或更名远端 Git 仓库。

## 1. 目标与原则

本次重构统一仓库、Maven 坐标、Java 根包、前后端工程、运行时服务和部署产物的命名，并建立独立的后端集成测试模块。

重构完成后的核心命名如下：

| 对象 | 目标名称 |
|---|---|
| 中文项目名 | 广州南方学院毕业选题管理系统 |
| 英文项目名 | NCG Graduation Topic Selection System |
| 项目简称 | NCG Topic Selection |
| Git 仓库名 | `nfu-graduation-topic-selection` |
| Maven `groupId` | `cn.edu.nfu` |
| Java 根包名 | `cn.edu.nfu.topicselection` |
| Maven 父工程 | `graduation-topic-selection-parent` |
| 后端可执行模块 | `graduation-topic-selection-server` |
| 集成测试模块 | `graduation-topic-selection-integration-tests` |
| 前端工程名 | `graduation-topic-selection-web` |
| Spring 应用名 | `nfu-topic-selection` |
| Docker 镜像名 | `nfu-topic-selection-server`、`nfu-topic-selection-web` |
| 数据库名 | `nfu_topic_selection` |
| Java 启动类 | `TopicSelectionApplication` |

实施时遵循以下原则：

1. 只改变工程组织和技术标识，不改变 79 个 HTTP 接口路径、请求字段、响应字段、业务状态码和提示文案。
2. 目录迁移、Maven 坐标迁移、Java 包迁移、运行时标识迁移和集成测试建设分阶段实施；提交时保留各阶段边界。
3. 使用 `git mv` 保留文件历史，不删除或覆盖当前工作区已有修改。
4. 当前重构完成报告作为历史记录保留；只修复其中失效的文件链接，不改写历史结论。
5. 项目尚未发布、上线或公网部署，也没有需要保留的数据库、会话和缓存数据，因此 Sa-Token Cookie、Redis/Caffeine前缀、数据库名、代理前缀和Docker卷可以一次性统一命名并重建。
6. 每一阶段完成后必须独立编译和验证，通过后才能进入下一阶段。
7. 现有测试按价值清理，不按是否使用 Mock 一刀切；只有在新测试形成等价覆盖后，才删除对应的旧回归测试。

## 2. 当前基线与已知边界

### 2.1 当前工程事实

- 后端目录：`work-topic-selection-backend/`
- 前端目录：`work-topic-selection-frontend/`
- 当前 Maven 坐标：`com.Lzh:work-topic-selection:1.0.0`
- 当前 Java 根包：`cn.com.edtechhub.worktopicselection`
- 当前启动类：`WorkTopicSelectionApplication`
- 当前 Spring 应用名：`work-topic-selection`
- 当前 Sa-Token Cookie 名：`work-topic-selection`
- 当前 Redis/Caffeine 前缀：`work-topic-selection:`
- 当前数据库：`work_topic_selection`
- 当前仓库没有配置 Git remote，远端仓库改名需要在代码迁移完成后单独执行。
- 当前仓库中约有 278 个文件包含旧包名或旧工程名，需要区分运行时代码、构建配置、文档历史和上游项目署名。

### 2.2 当前未提交修改

开始实施前必须重新执行：

```powershell
git status --short
```

制定本计划时已存在以下用户修改，后续迁移必须保留：

```text
TODO.md
work-topic-selection-backend/src/main/java/cn/com/edtechhub/worktopicselection/annotation/SentinelRateLimit.java
work-topic-selection-backend/src/main/java/cn/com/edtechhub/worktopicselection/aop/SentinelRateLimitAspect.java
```

这些文件随目录和包一起迁移，但不得回滚其内容。
本计划文件 `docs/MAVEN_PROJECT_NAMING_REFACTOR_PLAN.md` 也是当前新建、尚未跟踪的文件；执行者应将它纳入计划文档提交，不把它当作可清理的临时文件。以执行当天的 `git status --short` 为准，保留之后新增的其他用户修改。

### 2.3 重建边界

项目当前没有发布环境、线上会话、Redis缓存或需要保留的数据库数据，因此不实施旧技术标识兼容层，直接切换到目标名称：

| 标识 | 当前值 | 目标值 | 处理方式 |
|---|---|---|---|
| HTTP Controller路径 | `/auth/**`、`/user/**`、`/file/**`、`/ai/**` | 保持不变 | 继续作为79个接口的兼容契约 |
| 外部反向代理前缀 | `/work_topic_selection_api` | `/api` | 同步修改Caddy和前端生产基础地址 |
| Sa-Token Cookie | `work-topic-selection` | `nfu-topic-selection` | 直接切换，不兼容旧Cookie |
| Redis/Caffeine全局前缀 | `work-topic-selection:` | `nfu:topic-selection:` | 直接切换并清空旧Redis数据 |
| 查询缓存相对前缀 | `work-topic-selection:search:` | `search:` | 由RedisManager统一添加全局前缀，最终键为 `nfu:topic-selection:search:*` |
| 数据库名 | `work_topic_selection` | `nfu_topic_selection` | 删除本地旧库/卷后按schema重建 |
| Docker数据卷 | `mysql-data`、`redis-data`等 | 使用新Compose项目下的新卷 | 允许删除旧卷，不迁移数据 |
| 上游项目署名 | `limou3434/work-topic-selection` | 保持不变 | 永久保留 |

HTTP Controller路径属于业务API契约，仍保持不变；`/api` 只是部署层反向代理前缀，不改变后端Controller映射。

## 3. 目标目录结构

```text
nfu-graduation-topic-selection/
├─ pom.xml
├─ mvnw
├─ mvnw.cmd
├─ .mvn/
│  └─ wrapper/
├─ server/
│  ├─ pom.xml
│  ├─ Dockerfile
│  └─ src/
├─ integration-tests/
│  ├─ pom.xml
│  └─ src/
│     └─ test/
│        ├─ java/cn/edu/nfu/topicselection/integration/
│        └─ resources/
├─ web/
│  ├─ package.json
│  ├─ Dockerfile
│  └─ src/
├─ deploy/
├─ docs/
├─ .env.example
├─ env.sh
├─ env.ps1
├─ build.sh
└─ README.md
```

前端不是 Maven 模块，不加入根 POM 的 `<modules>`。

## 4. Maven 目标结构

### 4.1 父工程坐标

根目录 `pom.xml` 使用：

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.5.6</version>
    <relativePath/>
</parent>

<groupId>cn.edu.nfu</groupId>
<artifactId>graduation-topic-selection-parent</artifactId>
<version>1.0.0-SNAPSHOT</version>
<packaging>pom</packaging>

<name>NCG Graduation Topic Selection :: Parent</name>
<description>Graduation topic selection management system for Nanfang College, Guangzhou</description>

<modules>
    <module>server</module>
    <module>integration-tests</module>
</modules>
```

父工程负责：

- Java 8 与 UTF-8 编码；
- 公共版本属性；
- Maven Compiler、Surefire、Failsafe 的版本与公共参数；
- Testcontainers 版本管理；
- 子模块版本继承；
- License、URL 等项目元数据。

开发阶段统一使用 `1.0.0-SNAPSHOT`，发布首个稳定版本时再切换为 `1.0.0`。

### 4.2 后端模块坐标

`server/pom.xml` 使用：

```xml
<parent>
    <groupId>cn.edu.nfu</groupId>
    <artifactId>graduation-topic-selection-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <relativePath>../pom.xml</relativePath>
</parent>

<artifactId>graduation-topic-selection-server</artifactId>
<packaging>jar</packaging>

<name>NCG Graduation Topic Selection :: Server</name>
<description>Spring Boot backend server</description>
```

### 4.3 可执行 Jar 与模块依赖

集成测试模块需要把后端普通 Jar 作为测试依赖；Spring Boot 可执行 Jar 的类位于 `BOOT-INF/classes`，不能直接作为普通 Maven 依赖使用。因此后端模块保留普通 Jar，并把可执行 Jar 附加为 `exec` classifier：

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <classifier>exec</classifier>
        <mainClass>cn.edu.nfu.topicselection.TopicSelectionApplication</mainClass>
        <includeSystemScope>true</includeSystemScope>
        <excludes>
            <exclude>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </exclude>
        </excludes>
    </configuration>
</plugin>
```

预期产物：

```text
server/target/graduation-topic-selection-server-1.0.0-SNAPSHOT.jar
server/target/graduation-topic-selection-server-1.0.0-SNAPSHOT-exec.jar
```

普通 Jar 供 `integration-tests` 依赖；`-exec.jar` 供 Docker 和直接运行使用。

### 4.4 集成测试模块坐标

`integration-tests/pom.xml` 使用：

```xml
<parent>
    <groupId>cn.edu.nfu</groupId>
    <artifactId>graduation-topic-selection-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <relativePath>../pom.xml</relativePath>
</parent>

<artifactId>graduation-topic-selection-integration-tests</artifactId>
<packaging>jar</packaging>

<name>NCG Graduation Topic Selection :: Integration Tests</name>
<description>Backend integration and API documentation tests</description>
```

测试模块依赖：

```xml
<dependency>
    <groupId>cn.edu.nfu</groupId>
    <artifactId>graduation-topic-selection-server</artifactId>
    <version>${project.version}</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <scope>test</scope>
</dependency>
```

Testcontainers 固定使用仍兼容 Java 8 的 `1.19.8`，Redis 使用 `GenericContainer<?>` 启动 `redis:7-alpine`。

集成测试类统一以 `IT` 结尾并由 Failsafe 执行：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>integration-test</goal>
                <goal>verify</goal>
            </goals>
            <configuration>
                <systemPropertyVariables>
                    <spring.profiles.active>integration</spring.profiles.active>
                </systemPropertyVariables>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## 5. 分阶段实施步骤

## 阶段 0：冻结基线

### 目标

确认当前代码可以构建，记录用户未提交修改，建立重构前可比较基线。

### 操作

```powershell
git status --short

Push-Location "work-topic-selection-backend"
.\mvnw.cmd test
Pop-Location

pnpm --dir work-topic-selection-frontend tsc
pnpm --dir work-topic-selection-frontend lint:js
pnpm --dir work-topic-selection-frontend exec prettier --check "src/**/*.{js,jsx,ts,tsx,less}"
pnpm --dir work-topic-selection-frontend exec jest --runInBand
pnpm --dir work-topic-selection-frontend build
```

记录：

- 后端测试总数、失败数和错误数；
- 前端类型检查和构建结果；
- `git status --short` 输出；
- 当前 79 个 HTTP 接口台账状态。

### 完成标准

- 现有失败全部记录，不把存量失败误判为迁移引入。
- 用户未提交修改仍在工作区且内容未被覆盖。

## 阶段 0.5：清理伪测试并建立可信测试基线

### 目标

在工程与包名迁移前删除不调用生产代码、只断言测试局部变量的伪测试，保留能够保护安全、架构和核心业务行为的测试。此阶段不清空 `src/test/java`。

本阶段会删除和修改 Java 测试，执行前须按仓库 `AGENTS.md` 完整阅读 Java 编码规范。先用 `rg --files work-topic-selection-backend/src/test` 核对下列文件仍存在；路径或测试内容若已变化，先重新评估分类，不能按旧清单盲删。

### 立即删除

以下测试没有实际调用对应生产实现，可以直接删除：

```text
server/src/test/java/cn/com/edtechhub/worktopicselection/service/DeptServiceTest.java
server/src/test/java/cn/com/edtechhub/worktopicselection/service/MailServiceTest.java
server/src/test/java/cn/com/edtechhub/worktopicselection/service/SqlExportServiceTest.java
server/src/test/java/cn/com/edtechhub/worktopicselection/service/StudentTopicSelectionServiceTest.java
server/src/test/java/cn/com/edtechhub/worktopicselection/service/SwitchServiceTest.java
server/src/test/java/cn/com/edtechhub/worktopicselection/service/TopicServiceTest.java
server/src/test/java/cn/com/edtechhub/worktopicselection/response/BaseResponseTest.java
server/src/test/java/cn/com/edtechhub/worktopicselection/response/TheResultTest.java
```

本阶段发生在目录迁移之前时，上述路径中的 `server/` 对应当前 `work-topic-selection-backend/`。

### 拆分后删除

`ProjectServiceTest` 是混合测试：保留其中非法排序字段和非法排序方向两个查询安全场景，将它们迁入 `SqlUtilsTest` 或新的查询安全单元测试；其余只断言局部变量的场景删除。迁移完成后删除 `ProjectServiceTest`。

### 迁移期间保留

以下测试用于证明目录、Maven坐标和Java包迁移没有破坏关键行为，先保留并随Java根包一起迁移：

```text
aop/AopOrderTest.java
aop/RequestDtoValidationAspectTest.java
controller/FileControllerTest.java
manager/security/SecurityRateLimitManagerTest.java
manager/websocket/WebSocketEditHandlerTest.java
model/entity/UserSerializationTest.java
service/AuthenticationServiceTest.java
service/PasswordServiceTest.java
service/VerificationCodeServiceTest.java
service/TopicSelectionQueryServiceTest.java
service/impl/ArchitectureTransactionBoundaryGuardTest.java
service/impl/FileAndAIApplicationServiceImplTest.java
service/impl/OrganizationApplicationServiceImplTest.java
service/impl/SelectionReportServiceImplTest.java
service/impl/TopicApplicationServiceImplTest.java
service/impl/UserAndPolicyServiceTest.java
controller/UserControllerTopicSelectionTest.java
utils/DeviceUtilsTest.java
utils/IpUtilsTest.java
utils/SpringContextUtilsTest.java
utils/SqlUtilsTest.java
utils/ThrowUtilsTest.java
```

保留理由包括：

- 密码、邮箱、日志和JSON脱敏；
- CSV公式注入与SQL排序字段防护；
- AOP顺序、DTO校验和事务边界；
- 登录、密码、验证码、限流和WebSocket权限；
- 选题状态、调用顺序和异常分支；
- 关键工具类的确定性行为。

Mock本身不是删除理由。能够验证生产分支、失败路径、调用顺序和安全不变量的Mock单元测试，仍可提供比集成测试更快、更精确的故障定位。

### 等价覆盖后删除

Controller反射契约测试在 `Knife4jDocumentationIT` 完成运行时路由和文档集合比对后删除：

```text
controller/AuthControllerContractTest.java
controller/FileAndAIControllerContractTest.java
controller/OrganizationAndGroupControllerContractTest.java
controller/SelectionPolicyAndSystemControllerContractTest.java
controller/TopicControllerContractTest.java
controller/TopicQueryControllerContractTest.java
controller/TopicSelectionControllerContractTest.java
```

业务Mock测试按照以下退出条件逐个处理：

| 旧测试类型 | 删除条件 |
|---|---|
| 认证、密码、验证码Mock测试 | Sa-Token、Redis、密码和一次性凭证集成测试通过 |
| 选题写用例Mock测试 | MySQL悲观锁、并发容量、退选恢复和事务回滚集成测试通过 |
| 查询与统计Mock测试 | 真实MySQL写入后查询、统计和权限过滤集成测试通过 |
| 组织、用户和策略Mock测试 | MySQL与Redis跨域业务旅程集成测试通过 |
| 文件与AIMock测试 | Multipart导入、真实导出和外部服务替身集成测试通过 |

即使集成测试已覆盖，也继续保留小而快的安全、序列化、工具和架构守护测试，除非它们已经变成对实现细节的重复断言。

### 验证

删除和拆分完成后执行：

```powershell
Push-Location "work-topic-selection-backend"
.\mvnw.cmd test
Pop-Location
```

记录新的有效测试数量，后续不再以“210个测试”为质量目标，而以关键行为覆盖矩阵和全量通过率为准。

### 建议提交

```text
test: remove placeholder tests and preserve behavioral coverage
```

## 阶段 1：建立父工程与物理目录

### 目标

只迁移目录和 Maven Wrapper，不修改 Java 包名和业务代码。

### 操作顺序

先检查目标目录不存在：

```powershell
Test-Path -LiteralPath "server"
Test-Path -LiteralPath "web"
Test-Path -LiteralPath "integration-tests"
Test-Path -LiteralPath ".mvn"
```

所有结果均应为 `False`，再执行：

```powershell
git mv "work-topic-selection-backend" "server"
git mv "work-topic-selection-frontend" "web"

git mv "server/.mvn" ".mvn"
git mv "server/mvnw" "mvnw"
git mv "server/mvnw.cmd" "mvnw.cmd"
```

随后：

1. 在根目录创建父 `pom.xml`。
2. 将原后端 POM 改为 `server/pom.xml` 子工程。
3. 创建 `integration-tests/pom.xml` 和空测试目录。
4. 暂不移动任何后端测试到集成测试模块。

### 验证

```powershell
.\mvnw.cmd -pl server -am test
pnpm --dir web tsc
```

### 建议提交

```text
build: establish Maven parent and module directories
```

## 阶段 2：统一 Maven、前端和运行时工程名称

### 目标

修改构建坐标和运行时技术标识，不迁移 Java package。涉及Java中的前缀常量和外部调用标识时，必须先读取 `java-coding-conventions` 技能，并且只修改字符串常量，不调整业务逻辑。

### 修改范围

- 根 `pom.xml`
- `server/pom.xml`
- `integration-tests/pom.xml`
- `web/package.json`
- `server/src/main/resources/application.yaml`
- `server/src/main/resources/application-release.yaml`
- `server/src/main/resources/sql/schema.sql`
- `server/src/main/java/cn/com/edtechhub/worktopicselection/manager/redis/RedisConfig.java`
- `server/src/main/java/cn/com/edtechhub/worktopicselection/manager/caffeine/CaffeineConfig.java`
- `server/src/main/java/cn/com/edtechhub/worktopicselection/constant/RedisConstant.java`
- `server/src/main/java/cn/com/edtechhub/worktopicselection/service/impl/TopicApplicationServiceImpl.java`
- `server/Dockerfile`
- `web/Caddyfile`
- `build.sh`
- `deploy/docker-compose.yml`
- `deploy/Caddyfile`
- `deploy/backup.sh`
- `deploy/README.md`

### 命名变化

```text
com.Lzh:work-topic-selection
  -> cn.edu.nfu:graduation-topic-selection-server

graduation-topic-selection-frontend
  -> graduation-topic-selection-web

spring.application.name=work-topic-selection
  -> spring.application.name=nfu-topic-selection

sa-token.token-name=work-topic-selection
  -> sa-token.token-name=nfu-topic-selection

RedisConfig.keyPrefix=work-topic-selection:
  -> RedisConfig.keyPrefix=nfu:topic-selection:

CaffeineConfig.keyPrefix=work-topic-selection:
  -> CaffeineConfig.keyPrefix=nfu:topic-selection:

RedisConstant.SEARCH_KEY_PREFIX=work-topic-selection:search:
  -> RedisConstant.SEARCH_KEY_PREFIX=search:

TopicApplicationServiceImpl中的外部调用factor=work-topic-selection-backend
  -> factor=nfu-topic-selection-server

数据库名work_topic_selection
  -> nfu_topic_selection
```

`RedisConstant.SEARCH_KEY_PREFIX` 改为相对键名是为了避免与 `RedisManager` 的全局前缀重复。最终Redis查询缓存键格式统一为：

```text
nfu:topic-selection:search:<hash>
```

### Dockerfile调整

`server/Dockerfile` 改为只复制可执行 Jar：

```dockerfile
FROM openjdk:8-jdk-slim
WORKDIR /app
ARG JAR_FILE=target/*-exec.jar
COPY ${JAR_FILE} app.jar
ENV SERVER_ADDRESS=0.0.0.0
EXPOSE 8000
CMD ["java", "-Duser.timezone=Asia/Shanghai", "-jar", "app.jar"]
```

镜像目标：

```text
nfu-topic-selection-server:local
nfu-topic-selection-web:local
```

仓库有两份有效的 Caddyfile：`web/Caddyfile` 由 `web/Dockerfile` 复制进独立运行的前端镜像，`deploy/Caddyfile` 由 Compose 挂载到容器。两者的代理前缀都要改为 `/api/*`；`web/Dockerfile` 的 `COPY ./Caddyfile ./Caddyfile` 保留。分别验证 `docker build -t nfu-topic-selection-server:local server` 和 `docker build -t nfu-topic-selection-web:local web`，不能仅以 `docker compose config` 代替镜像构建。

### 验证

```powershell
.\mvnw.cmd -pl server -am clean package -DskipTests
Test-Path "server/target/graduation-topic-selection-server-1.0.0-SNAPSHOT.jar"
Test-Path "server/target/graduation-topic-selection-server-1.0.0-SNAPSHOT-exec.jar"
pnpm --dir web build
docker build -t nfu-topic-selection-server:local server
docker build -t nfu-topic-selection-web:local web
docker compose --env-file deploy/.env.production.example -f deploy/docker-compose.yml config
```

### 建议提交

```text
build: align project and runtime artifact names
```

## 阶段 3：迁移 Java 根包与启动类

### 目标

将所有生产和测试 Java 源码从：

```text
cn.com.edtechhub.worktopicselection
```

迁移到：

```text
cn.edu.nfu.topicselection
```

将：

```text
WorkTopicSelectionApplication
```

改为：

```text
TopicSelectionApplication
```

### 执行要求

本阶段涉及 Java 文件。执行者必须先完整读取：

```text
C:/Users/abc/.codex/skills/java-coding-conventions/SKILL.md
```

将物理目录、`package` 声明、Java `import`、反射引用和配置引用作为同一次迁移。可用 IntelliJ IDEA 的 `Refactor -> Rename/Move`，也可使用 Agent 可用的文件移动与精确替换工具；无论采用哪种工具，都必须检查目录与声明一致、旧类名和旧包名搜索无结果。不要依赖 IDE 的交互界面作为唯一可执行路径。

### 必须迁移的位置

1. `server/src/main/java` 下全部生产代码。
2. `server/src/test/java` 下全部单元测试。
3. `server/src/main/resources/mapper/*.xml` 中的 Mapper namespace 和实体类型。
4. `server/src/main/resources/application.yaml` 中的日志包名和 Knife4j 扫描包。
5. 测试中对 `WorkTopicSelectionApplication` 的导入和架构断言。
6. `server/pom.xml` 中 Spring Boot main class。

目标配置：

```yaml
logging:
  level:
    cn.edu.nfu.topicselection: INFO

knife4j:
  openapi:
    group:
      default:
        api-rule: package
        api-rule-resources:
          - cn.edu.nfu.topicselection.controller
```

### 不做的事情

- 不重命名 Controller、Service、DTO、Entity 和数据库字段。
- 不调整分层和依赖注入。
- 不修改 HTTP 路径。
- 不顺便修改业务逻辑。
- 不批量删除原作者 `@author` 标注。

### 验证

```powershell
rg -n "cn\.com\.edtechhub\.worktopicselection|WorkTopicSelectionApplication" `
  server integration-tests `
  -g "!**/target/**"

pwsh "C:/Users/abc/.codex/skills/java-coding-conventions/scripts/verify-style.ps1" `
  -TargetPath "server/src/main/java"

pwsh "C:/Users/abc/.codex/skills/java-coding-conventions/scripts/verify-style.ps1" `
  -TargetPath "server/src/test/java"

.\mvnw.cmd -pl server -am test
```

第一条 `rg` 命令应无输出。

若执行机器没有 `pwsh` 或仓库 `AGENTS.md` 指向的规范脚本，应先解决规范入口，不得把规范校验视为通过；Maven 测试和旧包名搜索仍须照常执行。

### 建议提交

```text
refactor: migrate Java namespace to cn.edu.nfu.topicselection
```

## 阶段 4：本地配置加载与测试环境隔离

### 目标

落实 `.env` 自动加载、Windows PowerShell 支持和测试 Profile 隔离。

### `application.yaml`

在非 Profile 专属的主配置中加入：

```yaml
spring:
  config:
    import:
      - optional:file:../.env[.properties]
      - optional:file:./.env[.properties]
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:develop}
```

当前目录 `.env` 放在最后，使其在两个候选文件同时存在时优先。

### `.env`约束

从 `.env.example` 中移除 `SPRING_PROFILES_ACTIVE=develop`。Profile来源统一为：

- 本地默认：`application.yaml` 回退到 `develop`；
- Docker：Compose 显式设置 `release`；
- 单元测试：Surefire 设置 `test`；
- 集成测试：Failsafe 和 `@ActiveProfiles` 设置 `integration`。

`.env` 每行只使用 `KEY=value`，禁止行内注释；脚本必须按第一个 `=` 分割并保留值中的后续 `=`。仓库根目录 `.env` 与 `deploy/.env` 用途不同：前者供本地 Spring 启动读取，后者供 Docker Compose 插值；不能把本地数据库地址直接复制进容器配置。

`.env.example` 和本地 `.env` 的数据库URL统一改为：

```properties
SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3306/nfu_topic_selection
```

测试环境仍由H2或Testcontainers动态属性覆盖，不使用此地址。

### `application-develop.yaml`

```yaml
management:
  health:
    mail:
      enabled: false

knife4j:
  enable: true
```

### `application-test.yml`

删除其中的：

```yaml
spring:
  profiles:
    active: test
```

Profile 专属文件不得再次声明 `spring.profiles.active`。

保留 H2 配置，但将 MyBatis 配置与生产配置保持一致：

```yaml
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: false
```

### Surefire隔离

`server/pom.xml` 的 Surefire 配置固定：

```xml
<systemPropertyVariables>
    <spring.profiles.active>test</spring.profiles.active>
</systemPropertyVariables>
```

### PowerShell脚本

新增根目录 `env.ps1`，要求：

- 必须通过 `. .\env.ps1` 点调用；
- 读取根目录 `.env`；
- 忽略空行和以 `#` 开头的注释；
- 按第一个 `=` 分割；
- 写入 Process 级环境变量；
- 不输出密码和值。

### 验证

```powershell
# 全新PowerShell窗口，从根目录执行
.\mvnw.cmd -pl server -am test

# 点调用脚本后只检查变量是否存在，不打印值
. .\env.ps1
Test-Path Env:SPRING_DATASOURCE_URL
Test-Path Env:SPRING_REDIS_HOST

# 验证父工程目录启动
.\mvnw.cmd -pl server spring-boot:run
```

还需从 `server` 目录验证 `../.env`：

```powershell
Push-Location server
..\mvnw.cmd spring-boot:run
Pop-Location
```

两次 `spring-boot:run` 仅在本机 MySQL、Redis 和 `.env` 凭据已就绪时执行；否则先用不连接外部服务的配置加载测试验证 `../.env` 与 `./.env` 的属性值，并在交接记录中注明运行时启动尚未验证。验证时只报告属性是否按预期解析，不输出密钥。

### 建议提交

```text
build: isolate local and test configuration loading
```

## 阶段 5：建立集成测试模块

### 目标

让单元测试和真实基础设施集成测试使用不同生命周期和不同配置。

### 测试环境

- MySQL：Testcontainers `MySQLContainer`，版本与部署的 MySQL 8 系列一致。
- Redis：Testcontainers `GenericContainer`，镜像 `redis:7-alpine`。
- Spring：`@SpringBootTest(webEnvironment = RANDOM_PORT)`。
- Profile：`@ActiveProfiles("integration")`。
- 动态地址：`@DynamicPropertySource` 注入 MySQL 和 Redis 连接参数。
- 邮件：Mock `JavaMailSender`，禁止连接真实 SMTP。
- AI：Mock `AIManager`，禁止访问真实外部服务。
- 数据库结构：使用 `server/src/main/resources/sql/schema.sql` 初始化。
- MySQL容器数据库名明确设置为 `nfu_topic_selection`，在容器启动时只执行一次 `sql/schema.sql`；确认该资源通过后端普通Jar进入测试类路径，若未进入则显式复制到 `integration-tests/src/test/resources/sql/`。不运行 `demo-data.sql`。
- 集成测试必须明确使用 `@SpringBootTest(classes = TopicSelectionApplication.class, webEnvironment = RANDOM_PORT)`，并由 `@DynamicPropertySource` 覆盖 DataSource URL、账号、密码和 Redis host、port、password、database；测试断言实际 JDBC 连接元数据和 Redis 连接目标，而非只检查配置字符串。
- `integration-tests/src/test/resources/application-integration.yml` 仅放集成测试专用的非密钥配置，例如禁用邮件健康检查和外部服务调用；外部地址由容器动态注入。测试之间清理自己插入的表记录和 Redis 业务键，不能依赖执行顺序。

### 目录

```text
integration-tests/src/test/java/cn/edu/nfu/topicselection/integration/
├─ support/
│  ├─ IntegrationTestBase.java
│  ├─ TestApiClient.java
│  ├─ TestFixtureFactory.java
│  ├─ DatabaseCleaner.java
│  └─ IntegrationAssertions.java
├─ smoke/
│  └─ ApplicationSmokeIT.java
├─ documentation/
│  └─ Knife4jDocumentationIT.java
├─ auth/
│  └─ AuthenticationFlowIT.java
├─ topic/
│  └─ TopicLifecycleIT.java
└─ selection/
   ├─ TopicSelectionFlowIT.java
   ├─ TopicSelectionConcurrencyIT.java
   └─ TopicSelectionRollbackIT.java
```

### 首批测试

1. `ApplicationSmokeIT`
   - Spring 上下文启动；
   - 激活 Profile 仅包含 `integration`；
   - DataSource 指向容器 MySQL；
   - 当前数据库名为 `nfu_topic_selection`；
   - Redis 指向容器 Redis；
   - Redis业务键以 `nfu:topic-selection:` 开头，查询缓存键不出现重复项目前缀；
   - 不读取本地 `.env` 中的数据库和 Redis 地址。

2. `AuthenticationFlowIT`
   - 登录返回名为 `nfu-topic-selection` 的 Sa-Token Cookie；
   - Cookie 可访问登录接口；
   - 退出后会话失效；
   - 未登录和角色错误返回当前业务码。

3. `TopicLifecycleIT`
   - 教师出题；
   - 系部主任审核；
   - 管理员发布；
   - 学生查询到已发布课题。

4. `TopicSelectionFlowIT`
   - 预选、最终确认、查询和退选完整链路；
   - 数据库状态和课题余量与响应一致。

5. `TopicSelectionConcurrencyIT`
   - 两名学生争抢最后一个名额；
   - 只能一人成功；
   - 余量不能为负；
   - 关联记录满足唯一约束。

6. `TopicSelectionRollbackIT`
   - 制造多步写操作中的受控异常；
   - 断言用户、课题、选题记录和额度不存在部分提交。

### 验证命令

```powershell
# 只执行快速单元测试
.\mvnw.cmd test

# 执行完整构建与集成测试，需要Docker Desktop可用
.\mvnw.cmd verify

# 构建但跳过集成测试
.\mvnw.cmd verify -DskipITs
```

### 旧业务Mock测试收口

每个集成测试首次通过后，按照阶段0.5中的退出条件检查对应Mock测试：

1. 若旧测试只验证了已被真实基础设施测试覆盖的相同行为，删除旧测试。
2. 若旧测试仍覆盖集成测试难以稳定制造的异常分支，保留并收窄断言。
3. 若旧测试验证调用顺序，而调用顺序本身不是业务不变量，删除该实现细节断言。
4. 删除后重新执行 `mvn test` 和 `mvn verify`，不能只运行新增测试类。

### 建议提交

```text
test: establish isolated backend integration test module
```

## 阶段 6：Knife4j接口文档测试

### 目标

验证运行时文档入口、OpenAPI2 JSON和实际 Controller映射一致。

`Knife4jDocumentationIT` 覆盖：

1. `GET /doc.html` 返回 200、HTML内容类型且响应非空。
2. `GET /v2/api-docs` 返回 200。
3. JSON 中 `swagger` 等于 `2.0`。
4. `info.title` 等于 `接口文档`。
5. 文档包含当前79个HTTP操作，而不是只比较 `paths.size()`。
6. 关键重构接口存在：

```text
POST /auth/login
POST /auth/logout
POST /user/add/topic
POST /user/preselect/topic/by/id
POST /user/select/topic/by/id
POST /user/withdraw
POST /file/upload
POST /ai/send
```

7. 废弃认证路由不存在：

```text
POST /user/login
POST /user/logout
POST /user/toggle/login
POST /user/reset/password
POST /user/send/code
POST /user/send/captcha
```

8. 从 `RequestMappingHandlerMapping` 收集项目 Controller 的方法和路径，与 `/v2/api-docs` 的业务操作集合比较，排除：

```text
/error
/actuator/**
/doc.html
/v2/api-docs
Knife4j和Spring框架自身端点
```

9. `release` Profile 保持 `knife4j.enable=false`，部署环境不公开接口文档。

### 完成标准

- Knife4j UI和JSON端点都可用；
- 79个业务操作全部进入文档；
- 不出现已废弃路由；
- 新增接口若未进入文档，测试会失败。

### 旧Controller契约测试收口

上述完成标准全部满足后，删除阶段0.5列出的7个反射式Controller契约测试。删除后必须重新运行：

```powershell
.\mvnw.cmd test
.\mvnw.cmd verify
```

Knife4j运行时集合比对成为HTTP路由和文档完整性的唯一契约来源，避免同时维护反射测试和运行时测试。

### 建议提交

```text
test: verify Knife4j documentation against runtime mappings
```

## 阶段 7：前端内部命名迁移

### 目标

统一前端包名和API服务目录；Controller请求路径保持不变，生产反向代理基础前缀从 `/work_topic_selection_api` 简化为 `/api`。

### 修改

```text
web/package.json
  name: graduation-topic-selection-web

web/pnpm-lock.yaml
  若锁文件记录根包名，同步更新并用 --frozen-lockfile 验证

web/src/services/work-topic-selection/
  -> web/src/services/topic-selection/
```

更新所有：

```typescript
@/services/work-topic-selection/...
```

为：

```typescript
@/services/topic-selection/...
```

API方法名称、URL、请求类型和响应类型全部保持不变。

同步修改：

```text
web/src/app.tsx
web/src/components/WebSocket/index.tsx
web/Caddyfile
```

生产HTTP和WebSocket地址分别变为：

```text
${window.location.origin}/api
${window.location.origin}/api/global/message
```

### 验证

```powershell
rg -n "services/work-topic-selection|graduation-topic-selection-frontend|work_topic_selection_api" web `
  -g "!node_modules/**" `
  -g "!dist/**"

pnpm --dir web install --frozen-lockfile
pnpm --dir web tsc
pnpm --dir web lint:js
pnpm --dir web exec prettier --check "src/**/*.{js,jsx,ts,tsx,less}"
pnpm --dir web exec jest --runInBand
pnpm --dir web build
```

`rg` 应无输出。

### 建议提交

```text
refactor: align frontend project and service names
```

## 阶段 8：部署、脚本和文档收口

### 修改范围

- `build.sh`
- `deploy/docker-compose.yml`
- `deploy/README.md`
- `README.md`
- `.env.example`
- `deploy/.env.production.example`
- `.gitignore`
- `web/Caddyfile`
- 仍引用旧目录的当前有效文档

### 部署命名

```text
work-backend  -> topic-selection-server
work-frontend -> topic-selection-web
work-mysql    -> topic-selection-mysql
work-redis    -> topic-selection-redis
```

镜像：

```text
nfu-topic-selection-server:local
nfu-topic-selection-web:local
```

数据库和代理配置同步改为：

```text
MYSQL_DATABASE=nfu_topic_selection
jdbc:mysql://topic-selection-mysql:3306/nfu_topic_selection
SPRING_REDIS_HOST=topic-selection-redis
BACKEND_UPSTREAM=topic-selection-server:8000
Caddy handle_path=/api/*
```

Docker卷统一改名：

```text
topic-selection-mysql-data
topic-selection-redis-data
topic-selection-caddy-data
topic-selection-caddy-config
```

Compose 卷实际名称通常带 Compose 项目前缀；上表是 Compose 文件中的卷键。更改卷键后新服务会创建新卷，旧卷可以待新环境验收后清理。若要清理，先在修改旧 Compose 配置之前记录旧容器和卷的实际名称、Compose 项目标识及卷标签；在新环境验收后只删除经过核对、确属本项目的旧资源。不要在已改成新卷名的 Compose 文件上执行 `down --volumes` 来清理旧卷。

只读检查命令：

```powershell
docker compose --env-file deploy/.env.production.example -f deploy/docker-compose.yml config
docker volume ls --format "{{.Name}}"
docker ps -a --filter "label=com.docker.compose.project" --format "{{.Names}}"
```

镜像构建、集成测试和新 Compose 服务验收均不依赖旧卷清理。实际清理时先对每个待删卷执行 `docker volume inspect <精确卷名>`，确认其标签和挂载容器后再使用精确卷名删除；不能通过通配符或当前新 Compose 项目批量删除。

### 数据库重建

如果使用本机MySQL而不是Docker，确认旧库无数据后执行：

```sql
DROP DATABASE IF EXISTS `work_topic_selection`;
CREATE DATABASE `nfu_topic_selection`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
```

PowerShell执行方式：

```powershell
mysql -u $env:SPRING_DATASOURCE_USERNAME -p `
  -e "DROP DATABASE IF EXISTS ``work_topic_selection``; CREATE DATABASE ``nfu_topic_selection`` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"

Get-Content -Raw "server/src/main/resources/sql/schema.sql" |
  mysql -u $env:SPRING_DATASOURCE_USERNAME -p nfu_topic_selection

# 仅在需要演示数据时执行
Get-Content -Raw "server/src/main/resources/sql/demo-data.sql" |
  mysql -u $env:SPRING_DATASOURCE_USERNAME -p nfu_topic_selection
```

如果使用Docker，先启动新数据库和Redis：

```powershell
docker compose -f deploy/docker-compose.yml up -d topic-selection-mysql topic-selection-redis

Get-Content -Raw "server/src/main/resources/sql/schema.sql" |
  docker compose -f deploy/docker-compose.yml exec -T topic-selection-mysql `
    sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" nfu_topic_selection'
```

上述 `up` 命令要求已准备 `deploy/.env`，并在 Compose 文件中将四项必填变量配置好；没有真实环境文件时仅执行前述基于 `.env.production.example` 的 `config` 语法检查。

schema与演示数据必须分开执行；集成测试只加载schema和自己的Fixture，不加载 `demo-data.sql`。

### README更新

必须同步：

- 新项目名称；
- 新目录树；
- 根目录 Maven Wrapper 命令；
- `server/` 和 `web/` 路径；
- `.env` 自动加载与 `env.ps1`；
- 单元测试和集成测试命令；
- Knife4j文档验证地址；
- Docker镜像和Compose服务名。
- 新数据库名、备份文件名和 `/api` 代理前缀。

`deploy/backup.sh` 中的数据库名和备份文件名前缀同步改为：

```text
nfu_topic_selection
nfu_topic_selection-<timestamp>.sql.gz
```

永久保留上游项目署名：

```text
limou3434/work-topic-selection
```

### 验证

```powershell
rg -n "work-topic-selection|work_topic_selection|graduation-topic-selection-(backend|frontend)|com\.Lzh|work-(mysql|redis|backend|frontend)" `
  pom.xml server integration-tests web deploy build.sh .env.example `
  -g "!**/target/**" `
  -g "!**/node_modules/**"

docker compose --env-file deploy/.env.production.example -f deploy/docker-compose.yml config
```

允许旧名称继续存在的位置：

- 上游项目署名与链接；
- 历史重构记录中明确描述旧实现的文字；

运行时代码、配置、前端和部署文件中不再允许出现旧Cookie名、旧缓存前缀、旧数据库名或旧代理前缀。

### 建议提交

```text
docs: align build deployment and usage documentation
```

## 阶段 9：最终验证与发布准备

### Java规范校验

```powershell
pwsh "C:/Users/abc/.codex/skills/java-coding-conventions/scripts/verify-style.ps1" `
  -TargetPath "server/src/main/java"

pwsh "C:/Users/abc/.codex/skills/java-coding-conventions/scripts/verify-style.ps1" `
  -TargetPath "server/src/test/java"

pwsh "C:/Users/abc/.codex/skills/java-coding-conventions/scripts/verify-style.ps1" `
  -TargetPath "integration-tests/src/test/java"
```

### 后端验证

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd verify
```

### 前端验证

```powershell
pnpm --dir web tsc
pnpm --dir web lint:js
pnpm --dir web exec prettier --check "src/**/*.{js,jsx,ts,tsx,less}"
pnpm --dir web exec jest --runInBand
pnpm --dir web build
```

### 部署验证

```powershell
docker compose --env-file deploy/.env.production.example -f deploy/docker-compose.yml config
docker build -t nfu-topic-selection-server:local server
docker build -t nfu-topic-selection-web:local web
```

构建镜像后验证：

```powershell
docker image inspect nfu-topic-selection-server:local
docker image inspect nfu-topic-selection-web:local
```

### 运行时验收

```text
GET http://127.0.0.1:8000/user/test
GET http://127.0.0.1:8000/doc.html
GET http://127.0.0.1:8000/v2/api-docs
```

验收标准：

- 根目录 `mvn test` 通过；
- 根目录 `mvn verify` 通过；
- 前端类型检查、测试和生产构建通过；
- 79个HTTP操作仍存在；
- Knife4j文档操作集合与运行时Controller一致；
- 容器配置可解析；
- 旧包名不再出现在运行时代码；
- Sa-Token Cookie名为 `nfu-topic-selection`；
- Redis/Caffeine业务前缀为 `nfu:topic-selection:`，不存在重复项目前缀；
- 本地、容器和文档统一使用数据库名 `nfu_topic_selection`；
- 前端和Caddy统一使用 `/api` 反向代理前缀；
- 用户已有未提交修改完整保留。

## 6. 提交序列

建议按以下顺序形成小提交：

```text
docs: add Maven naming refactor plan
test: remove placeholder tests and preserve behavioral coverage
build: establish Maven parent and module directories
build: align project and runtime artifact names
refactor: migrate Java namespace to cn.edu.nfu.topicselection
build: isolate local and test configuration loading
test: establish isolated backend integration test module
test: verify Knife4j documentation against runtime mappings
refactor: align frontend project and service names
docs: align build deployment and usage documentation
```

每个提交必须能够单独说明目的，禁止把业务逻辑优化混入这些提交。

## 7. 后续非本次事项

Sa-Token Cookie、Redis/Caffeine前缀、数据库名、外部代理前缀、Docker服务和数据卷重命名已经纳入本计划，不再建立旧名称兼容层。

本计划暂不实施的架构拆分只有：

1. 将单体后端继续拆分为 `domain/application/infrastructure/web/boot` Maven模块。
   - 当前Controller和Application Service解耦不等于已经具备零环依赖的物理模块边界；
   - 应先用集成测试锁定行为，再分析依赖图。

## 8. 常见失败与检查

### 父POM找不到子模块

检查：

```powershell
Test-Path server/pom.xml
Test-Path integration-tests/pom.xml
.\mvnw.cmd help:effective-pom
```

### 集成测试无法引用后端类

检查后端是否同时生成普通Jar和 `-exec.jar`：

```powershell
Get-ChildItem server/target -Filter "*.jar"
```

若只有可执行Jar，检查 `spring-boot-maven-plugin` 的 `classifier=exec`。

### Spring找不到Mapper

检查旧包名和Mapper XML：

```powershell
rg -n "cn\.com\.edtechhub\.worktopicselection" server/src/main/resources/mapper server/src/main/java
```

### Knife4j文档为空

检查：

```text
knife4j.openapi.group.default.api-rule-resources
```

必须指向：

```text
cn.edu.nfu.topicselection.controller
```

### 测试误连本地数据库或Redis

检查测试进程实际激活Profile及动态属性，测试失败日志不得出现 `.env` 中的本地连接地址。Surefire固定 `test`，Failsafe固定 `integration`，集成测试使用 `@DynamicPropertySource` 覆盖容器地址。

### Docker复制到错误Jar

检查 `server/Dockerfile` 只匹配：

```text
target/*-exec.jar
```

不得继续使用可能同时匹配两个Jar的：

```text
target/*.jar
```

## 9. 调试交接模板

执行中遇到问题时，请提供以下信息：

```text
执行阶段：阶段 X
执行命令：完整命令
错误类型：编译 / 单元测试 / 集成测试 / Spring启动 / 数据库 / Redis / Knife4j / 前端 / Docker
错误输出：从第一条 ERROR 或 Caused by 开始的完整日志
刚修改的文件：文件路径列表
git status --short：完整输出
是否能通过上一阶段验证：是 / 否
```

不要通过删除测试、跳过失败模块、回滚用户已有修改或改用本地真实数据库来绕过问题。
