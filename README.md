# 广州南方学院毕设选题管理系统

<div align="center">
  <strong>按客户端角色开放毕业设计选题、审核、确认与管理能力。</strong>
  <br />
  React 18 · Umi Max · Spring Boot · MyBatis-Plus · Sa-Token
</div>

<p align="center">
  <a href="#产品能力">产品能力</a>
  ·
  <a href="#技术架构">技术架构</a>
  ·
  <a href="#本地启动">本地启动</a>
  ·
  <a href="#项目结构">项目结构</a>
  ·
  <a href="https://github.com/wobushi041/NCG-Graduation-Topic-Selection-System/issues">问题反馈</a>
</p>

## 项目简介

广州南方学院毕设选题管理系统（NCG Graduation Topic Selection System）是一套面向高校院系毕业设计选题场景的前后端分离管理系统。系统为学生、教师、选题负责人和系统管理员提供独立工作台，覆盖账号与组织维护、课题发布与审核、学生预选与确认、退选、统计导出和系统策略配置。

项目采用 Maven 多模块与前端独立工程组成的单体仓库。前端通过 Umi Max 路由和权限模型开放角色页面，后端通过 Spring MVC、Sa-Token、Service 事务和 MyBatis-Plus 承载业务规则，并使用 MySQL、Redis、Caffeine 与 WebSocket 提供数据、会话、缓存和实时消息能力。

![系统首页预览](./nfu-graduation-topic-selection-frontend/public/home.png)

## 产品能力

| 客户端角色 | 开放功能 |
| --- | --- |
| 学生端 | 浏览可选教师与课题，预选或取消预选，在开放阶段确认最终课题，查看选题状态并按规则退选。 |
| 教师端 | 新增、编辑、删除和提交课题，设置课题选题组与人数，查看已选学生并执行教师侧选题操作。 |
| 选题负责人端 | 审核所属范围内的课题，填写通过或退回意见，查看选题统计与明细，导出结果并按资格切换教师角色。 |
| 系统管理端 | 管理学院、专业、选题组和账号，设置出题与预选额度，控制课题发布、选题时间和系统策略，执行批量导入导出。 |

系统同时提供以下公共能力：

- Sa-Token 登录、角色和权限校验，BCrypt 密码存储与历史密码平滑迁移；
- Sentinel 接口限流、统一异常响应和请求 DTO 基础校验；
- Spring 事务、数据库锁和唯一约束共同保护选题名额与状态一致性；
- CSV 批量导入导出、WebSocket 系统消息及可选的 SMTP 邮件通知；
- 可选接入腾讯云智能体 API，为教师提供课题内容辅助检查。

## 技术架构

```text
React 18 + TypeScript + Umi Max + Ant Design Pro
  ├─ HTTP / Sa-Token Cookie ──> Spring Boot :8000
  └─ WebSocket ───────────────> 系统消息与状态通知

Spring MVC Controller
  └─ Service / Transaction
      ├─ MyBatis-Plus ──> MySQL 8
      ├─ Redis / Caffeine
      ├─ SMTP 邮件服务（可选）
      └─ 腾讯云智能体 API（可选）
```

| 层次 | 主要技术 |
| --- | --- |
| 前端 | React 18、TypeScript 5、Umi Max 4、Ant Design 5、ECharts 6 |
| 后端 | Java 8、Spring Boot 2.5.6、Spring MVC、MyBatis-Plus、Sa-Token、Sentinel |
| 数据与缓存 | MySQL 8、Redis 7、Caffeine |
| 接口与文件 | Knife4j / OpenAPI 2、Apache Commons CSV、EasyExcel |
| 测试 | JUnit 5、Spring Boot Test、H2、Testcontainers、Jest |
| 构建与部署 | Maven Wrapper、pnpm 11.19.0、Docker Compose、Caddy |

## 本地启动

以下命令以 Windows PowerShell 和仓库根目录为起点。请先准备 JDK 8、Node.js 24、pnpm 11.19.0、MySQL 8 和 Redis 6/7。运行完整集成测试时还需要 Docker Desktop。

1. 获取代码：

   ```powershell
   git clone https://github.com/wobushi041/NCG-Graduation-Topic-Selection-System.git
   Set-Location .\NCG-Graduation-Topic-Selection-System
   ```

2. 创建数据库并导入表结构：

   ```sql
   CREATE DATABASE IF NOT EXISTS nfu_topic_selection
     CHARACTER SET utf8mb4
     COLLATE utf8mb4_0900_ai_ci;
   ```

   ```powershell
   Get-Content -Raw ".\nfu-graduation-topic-selection-backend\src\main\resources\sql\schema.sql" |
     mysql -u YOUR_MYSQL_USER -p nfu_topic_selection
   ```

   如需本地演示数据，可继续导入同目录下的 `demo-data.sql`。演示数据只适用于开发环境。

3. 配置本地环境变量：

   ```powershell
   Copy-Item .env.example .env
   notepad .env
   ```

   至少填写 MySQL 与 Redis 连接信息。数据库尚无管理员时，可在 `.env` 中设置一次性的 `APP_BOOTSTRAP_ADMIN_ACCOUNT`、`APP_BOOTSTRAP_ADMIN_NAME` 和 `APP_BOOTSTRAP_ADMIN_PASSWORD`；首次登录后应删除这些值。

4. 启动后端：

   ```powershell
   .\mvnw.cmd -pl nfu-graduation-topic-selection-backend spring-boot:run
   ```

5. 在另一个终端启动前端：

   ```powershell
   pnpm --dir nfu-graduation-topic-selection-frontend install --frozen-lockfile
   $env:PORT = '3000'
   $env:HOST = '127.0.0.1'
   pnpm --dir nfu-graduation-topic-selection-frontend start:dev
   ```

默认访问地址：

| 服务 | 地址 |
| --- | --- |
| 前端 | <http://127.0.0.1:3000> |
| 后端 API | <http://127.0.0.1:8000> |
| Knife4j | <http://127.0.0.1:8000/doc.html> |
| OpenAPI 2 JSON | <http://127.0.0.1:8000/v2/api-docs> |

本地开发请求会携带名为 `nfu-topic-selection` 的 Sa-Token Cookie。若修改前端来源或后端端口，请同步调整 `.env` 中的 `APP_CORS_ALLOWED_ORIGINS` 和 `APP_WEBSOCKET_ALLOWED_ORIGINS`。

## 项目结构

```text
NCG-Graduation-Topic-Selection-System/
├─ nfu-graduation-topic-selection-frontend/   # React + Umi Max 前端
│  ├─ config/                                 # 路由、构建与布局配置
│  ├─ public/                                 # 校徽、模板和角色使用手册
│  └─ src/                                    # 页面、组件、权限与接口调用
├─ nfu-graduation-topic-selection-backend/    # Spring Boot 后端
│  ├─ src/main/java/cn/edu/nfu/topicselection/
│  └─ src/main/resources/                     # 配置、Mapper XML 与 SQL
├─ integration-tests/                         # Testcontainers 集成测试
├─ deploy/                                    # Docker Compose、Caddy 与备份脚本
├─ docs/                                      # 架构计划与重构记录
├─ .env.example                               # 本地环境变量模板
├─ pom.xml                                    # Maven 父工程
├─ TODO.md                                    # 后续开发计划
└─ README.md
```

## 常用接口

以下路径均以本地后端地址 `http://127.0.0.1:8000` 为基础；除公开接口外，请携带有效登录会话。

| 用途 | 方法与路径 |
| --- | --- |
| 登录 / 退出 | `POST /auth/login` · `POST /auth/logout` |
| 获取当前用户 | `GET /user/get/login` |
| 课题分页查询 | `POST /user/get/topic/page` |
| 预选 / 正式选题 | `POST /user/preselect/topic/by/id` · `POST /user/select/topic/by/id` |
| 退选 | `POST /user/withdraw` |
| 系统连通性检查 | `GET /user/test` |
| 文件导入导出 | `/file/**` |
| AI 辅助检查 | `POST /ai/send` |

完整接口及参数请在后端启动后查看 Knife4j，或阅读 [HTTP 接口重构DOC](./docs/phase-00-planning/HTTP_API_REFACTOR_STATUS.md)。

## 检查与构建

```powershell
# 后端单元测试
.\mvnw.cmd test

# 后端单元测试、Testcontainers 集成测试和接口契约测试
.\mvnw.cmd verify

# 前端代码检查与类型检查
pnpm --dir nfu-graduation-topic-selection-frontend lint

# 前端测试与生产构建
pnpm --dir nfu-graduation-topic-selection-frontend test
pnpm --dir nfu-graduation-topic-selection-frontend build
```

生产环境部署、Caddy 反向代理、数据库备份和恢复流程见 [deploy/README.md](./deploy/README.md)。

## 使用手册与文档

- [学生使用手册](./nfu-graduation-topic-selection-frontend/public/steps/1.学生使用手册.md)
- [教师使用手册](./nfu-graduation-topic-selection-frontend/public/steps/2.教师使用手册.md)
- [选题负责人使用手册](./nfu-graduation-topic-selection-frontend/public/steps/3.专业负责人使用手册.md)
- [项目文档索引](./docs/README.md)
- [开发计划](./TODO.md)
- [项目协作规则](./AGENTS.md)

仓库不提供可直接登录的公开账号，也不应提交真实名单、账号密码、生产数据库或包含密钥的 `.env` 文件。

## 许可证

项目采用 [MIT License](./LICENSE)。

本项目仅用于学习、研究、教学演示和二次开发。用于实际学校或院系环境前，请依据组织的数据安全、账号管理和运维规范完成评估与加固。
