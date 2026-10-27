# 通过现有 OpenResty 统一入口部署

本方案在通用独立部署模板上增加网关配置，继续使用已经构建的两个应用镜像。
域名、密码和网络名称通过环境变量配置，模板不绑定某台服务器或个人域名。

```text
浏览器 → OpenResty :80/:443（域名跳转、HTTPS、证书）
           ↓ 共享 Docker 网络，或宿主机回环端口
         topic-selection-web:3000（内部 HTTP、静态网页、/api 转发）
           ↓ 项目 Docker 网络
         topic-selection-server:8000
           ├─ topic-selection-mysql:3306
           └─ topic-selection-redis:6379
```

这里只复用 OpenResty 网关。MySQL 和 Redis 仍由项目创建，数据库初始化和备份沿用通用方案。
服务器已有的其他数据库不会自动被复用或迁移。

## 涉及文件

| 文件 | 职责 |
| --- | --- |
| `docker-compose.yml` | 通用完整应用编排 |
| `docker-compose.openresty.yml` | 清除前端的宿主机端口映射、挂载内部 HTTP 配置、接入网关网络 |
| `docker-compose.openresty-host.yml` | 供宿主机/host 模式网关使用，仅绑定 `127.0.0.1:18080` |
| `Caddyfile.openresty` | 监听内部 3000 端口，提供网页和 API/WebSocket 转发 |
| `.env` | 域名、密码和网关网络名称；从 `.env.production.example` 复制后填写 |
| `openresty/site.conf.example` | OpenResty 配置参考；由网关侧管理，不由项目 Compose 自动安装 |

单独使用 `docker-compose.yml` 时，仍按独立部署方式运行。
网关模式每次启动或更新都必须同时指定两个 Compose 文件；否则会恢复基础模板的 80/443 映射。
两个网关覆盖文件只选一个；共享网络版用 `!reset []` 清除端口，host 版用 `!override` 替换端口。
需要 Docker Compose v2.24.4 或更新版本。

## 1. 准备镜像、环境变量和表结构

按 [生产部署说明](README.md)构建、上传并导入应用镜像，同时上传整个 `deploy/` 目录和 `schema.sql`。
在服务器的 `deploy/` 中执行：

```bash
cp .env.production.example .env
```

仅在尚无 `.env` 时复制，已有配置时直接编辑。至少填写：

```dotenv
SITE_DOMAIN=example.com
OPENRESTY_NETWORK=topic-selection-gateway
OPENRESTY_HTTP_PORT=18080
MYSQL_ROOT_PASSWORD=replace-with-a-long-random-password
SPRING_DATASOURCE_USERNAME=wts
SPRING_DATASOURCE_PASSWORD=replace-with-a-long-random-password
SPRING_REDIS_PASSWORD=replace-with-a-long-random-password
```

将示例域名和密码替换为真实配置。首次创建管理员时填写 `APP_BOOTSTRAP_ADMIN_*`，已有管理员时可留空。
`SITE_DOMAIN` 填主域名，不带 `https://`、路径或末尾斜杠；主域名和 `www` 域名都要解析到网关。
`.env` 保存在服务器，不提交到 Git，不公开发送。

## 2. 确认网关网络模式并选择覆盖文件

先查看已有容器和网络：

```bash
docker ps --format 'table {{.Names}}\t{{.Image}}\t{{.Ports}}'
docker network ls
```

将 `YOUR_OPENRESTY_CONTAINER` 替换为真实容器名，检查它的网络模式：

```bash
docker inspect --format '{{.HostConfig.NetworkMode}}' YOUR_OPENRESTY_CONTAINER
```

| OpenResty 运行方式 | 选择的覆盖文件 | OpenResty 代理目标 |
| --- | --- | --- |
| 直接安装在宿主机，或容器网络模式为 `host` | `docker-compose.openresty-host.yml` | `http://127.0.0.1:18080` |
| 可加入共享 bridge 网络的容器 | `docker-compose.openresty.yml` | `http://topic-selection-web:3000` |

如果是 host 模式，在当前终端设置：

```bash
GATEWAY_COMPOSE_FILE=docker-compose.openresty-host.yml
```

然后直接进入第 3 步，无需创建或连接额外网络。`18080` 可通过 `.env` 的 `OPENRESTY_HTTP_PORT` 改为其他空闲端口；
OpenResty 代理目标中的端口也要同步修改。

如果是共享 bridge 网络，在当前终端设置：

```bash
GATEWAY_COMPOSE_FILE=docker-compose.openresty.yml
```

后续命令使用这个 shell 变量；新开终端后需重新设置。随后准备网络：

默认使用专用网络 `topic-selection-gateway`。若不存在，先创建：

```bash
docker network create topic-selection-gateway
```

将下方 `YOUR_OPENRESTY_CONTAINER` 替换为真实的 OpenResty 容器名；如果已连接，则跳过：

```bash
docker network connect topic-selection-gateway YOUR_OPENRESTY_CONTAINER
docker network inspect topic-selection-gateway
```

也可以使用 OpenResty 已经加入的、受信任的 bridge 网络，将 `.env` 的 `OPENRESTY_NETWORK` 改成该网络名。
`external: true` 表示项目使用这个现有网络，项目的 `docker compose down` 不会删除它。

前端信任内网代理提供的 `X-Forwarded-*` 头，因此网关网络只应接入受信任的服务。
通过 `docker network connect` 手工接入的网络，需要在 OpenResty 容器被面板重建后再次确认；
如果面板支持在应用编排中声明网络，可在那里持久化同一网络连接。

## 3. 校验编排并初始化数据库

以下命令全部在服务器 `deploy/` 中执行。

```bash
docker compose -f docker-compose.yml -f "${GATEWAY_COMPOSE_FILE:?请先按第 2 步选择覆盖文件}" config --quiet
docker compose -f docker-compose.yml -f "$GATEWAY_COMPOSE_FILE" up -d topic-selection-mysql topic-selection-redis
docker compose -f docker-compose.yml -f "$GATEWAY_COMPOSE_FILE" ps
```

等 MySQL 显示 `healthy`，只对全新数据库导入表结构：

```bash
docker compose -f docker-compose.yml -f "$GATEWAY_COMPOSE_FILE" exec -T topic-selection-mysql \
  sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" nfu_topic_selection' \
  < ../nfu-graduation-topic-selection-backend/src/main/resources/sql/schema.sql
```

表结构文件按基础部署文档的相对目录上传；不要对已有业务数据库重复导入完整基线。

## 4. 启动项目

```bash
docker compose -f docker-compose.yml -f "${GATEWAY_COMPOSE_FILE:?请先按第 2 步选择覆盖文件}" up -d
docker compose -f docker-compose.yml -f "$GATEWAY_COMPOSE_FILE" ps
docker compose -f docker-compose.yml -f "$GATEWAY_COMPOSE_FILE" logs --tail=100 topic-selection-server
```

共享网络版不发布任何项目宿主机端口；host 版仅将前端映射到 `127.0.0.1:18080`。
MySQL、Redis 和后端均不发布宿主机端口。
`ps` 中可能仍显示镜像声明的容器端口，这是容器内部端口，不是宿主机端口映射。

## 5. 配置 OpenResty 网站

由现有 OpenResty 独占公网 80/443。主站代理目标按第 2 步的运行方式选择。

将整个站点的请求转发到该地址，保留原 URI，包含 `/api` 前缀；由内部 Caddy 统一去掉 `/api` 后再转发给后端。
不要在 OpenResty 再次剥离 `/api`。bridge 模式中，OpenResty 容器的 `127.0.0.1` 指向自身；
host 模式中，OpenResty 可以通过宿主机的回环端口访问项目。

### 使用 1Panel 管理网站

在现有 OpenResty 的网站管理中，为主域名创建反向代理网站，设置上述代理目标；
开启 WebSocket 转发，配置覆盖主域名和 `www` 域名的证书，并由面板管理证书续期。
站点请求体限制至少容纳后端的 10 MB 文件及 multipart 开销（参考模板使用 12 MB）。

确保转发以下头：`Host`、`X-Forwarded-For`、`X-Real-IP`、`X-Forwarded-Proto`、`X-Forwarded-Host`；
WebSocket 还需 `Upgrade`、`Connection` 和足够长的读取超时。
公网第一层 OpenResty 应用实际连接地址覆盖入站的 `X-Forwarded-For`，避免直接信任浏览器伪造的来源。
如果网关前面还有 CDN 或其他代理，应另行配置对应的受信任代理范围。

`www` 站点应返回 `301 https://主域名$request_uri`，HTTP 请求统一跳转到 HTTPS 主域名。
`www` 的 HTTPS 跳转同样需要有效证书，因为 TLS 握手发生在返回 301 之前。
不要在面板已有网站配置中重复声明 `listen`、`server_name` 或粘贴完整的第二组 `server` 块。

### 手工管理 OpenResty 配置

参考 [openresty/site.conf.example](openresty/site.conf.example)。该文件应在 OpenResty 的 `http {}` 上下文中包含，
其中 `map` 不能放入 `server` 或 `location` 内。
修改所有 `example.com` 和证书路径；证书路径必须是 OpenResty 容器内部可以读取的真实路径。
模板默认代理目标适用于共享网络版；host 版请将代理目标改为 `http://127.0.0.1:18080` 并删除 `resolver` 行。
共享网络版的 Docker DNS `127.0.0.11` 能在前端容器重建后重新解析服务名。
先在网关容器中运行 `nginx -t`，通过后再按现有运维方式重载。

## 6. 验证与更新

将下面示例域名改成自己的主域名：

```bash
curl -I https://example.com/user/login
curl -I 'https://www.example.com/user/login?from=test'
curl https://example.com/api/actuator/health
```

主域名页面应正常返回；`www` 应返回 301，`Location` 保留路径和查询参数。
再在浏览器验证登录、刷新子页面、文件上传和 WebSocket 实时通知。
遇到 502，先检查共享网络、代理目标和后端日志。

更新前先备份数据库，加载新应用镜像后，用相同的双文件命令更新；标签不变时显式重建应用容器：

```bash
bash ./backup.sh
docker compose -f docker-compose.yml -f "${GATEWAY_COMPOSE_FILE:?请先按第 2 步选择覆盖文件}" up -d --force-recreate topic-selection-server topic-selection-web
```

`backup.sh` 仍能使用，因为 MySQL 服务、数据卷及项目目录保持一致。不要用 `down -v` 作为升级步骤。

网关模式解决入口端口共享，不减少数据库和后端的内存需求。
小内存服务器仍需独立评估资源；本机配置校验不能代替云服务器实际运行验证。

## 官方参考

- [Compose 合并规则与清除覆盖值](https://docs.docker.com/reference/compose-file/merge/)
- [Caddy 的代理头和受信任代理](https://caddyserver.com/docs/caddyfile/directives/reverse_proxy#headers)
- [Nginx WebSocket 转发](https://nginx.org/en/docs/http/websocket.html)
