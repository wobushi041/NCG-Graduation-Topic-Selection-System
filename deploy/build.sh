#!/usr/bin/env bash

set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="${ROOT_DIR}/nfu-graduation-topic-selection-backend"
FRONTEND_DIR="${ROOT_DIR}/nfu-graduation-topic-selection-frontend"
SERVER_IMAGE="nfu-topic-selection-server:local"
WEB_IMAGE="nfu-topic-selection-web:local"

info() {
  printf '[info] %s\n' "$1"
}

fail() {
  printf '[error] %s\n' "$1" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "缺少 $1，请先安装后再构建。"
}

require_command java
require_command javac
require_command node
require_command pnpm
require_command docker

java_version="$(java -version 2>&1 | head -n 1)"
[[ "${java_version}" == *'version "1.8.'* || "${java_version}" == *'version "8.'* ]] \
  || fail "后端需要 JDK 8，当前为：${java_version}"

node_version="$(node --version)"
[[ "${node_version}" == v24.* ]] \
  || fail "前端需要 Node.js 24，当前为：${node_version}"

pnpm_version="$(pnpm --version)"
[[ "${pnpm_version}" == 11.19.0 ]] \
  || fail "前端需要 pnpm 11.19.0，当前为：${pnpm_version}"

docker info >/dev/null 2>&1 || fail "无法连接 Docker，请先启动 Docker。"

cd "${ROOT_DIR}"
info "编译后端项目"
sh ./mvnw -pl nfu-graduation-topic-selection-backend -am clean package -DskipTests
compgen -G "${BACKEND_DIR}/target/*-exec.jar" >/dev/null \
  || fail "后端构建完成，但未找到 target/*-exec.jar。"

info "构建后端镜像：${SERVER_IMAGE}"
docker build -t "${SERVER_IMAGE}" "${BACKEND_DIR}"

cd "${FRONTEND_DIR}"
info "安装前端依赖并构建"
pnpm install --frozen-lockfile
pnpm build
[[ -f dist/index.html ]] || fail "前端构建完成，但未找到 dist/index.html。"

info "构建前端镜像：${WEB_IMAGE}"
docker build -t "${WEB_IMAGE}" .

info "构建完成：${SERVER_IMAGE}、${WEB_IMAGE}"
