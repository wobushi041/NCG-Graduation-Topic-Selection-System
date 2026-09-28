#!/usr/bin/env bash

set -e

# 定义颜色
GREEN='\033[0;32m[info] ' # 提示
RED='\033[0;31m[erro] ' # 错误
YELLOW='\033[1;33m[wrin] ' # 警告
NC='\033[0m' # 重置

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "${ROOT_DIR}"

# 核心脚本
echo -e "${GREEN} 编译后端项目 ${NC}"
./mvnw -pl nfu-graduation-topic-selection-backend -am clean package -DskipTests

echo -e "${GREEN} 构建后端镜像 ${NC}"
docker build -t nfu-topic-selection-server:local nfu-graduation-topic-selection-backend

echo -e "${GREEN} 编译前端项目 ${NC}"
cd "${ROOT_DIR}/nfu-graduation-topic-selection-frontend"
pnpm install --frozen-lockfile
pnpm build

echo -e "${GREEN} 构建前端镜像 ${NC}"
docker build -t nfu-topic-selection-web:local .
