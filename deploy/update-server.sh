#!/usr/bin/env bash
# 兼容入口：旧命令 ./deploy/update-server.sh 保留可用，参数原样透传给 deploy.sh。
# 现在首次部署与更新都由 deploy/deploy.sh 负责（会自动区分）。
exec "$(cd "$(dirname "$0")" && pwd)/deploy.sh" "$@"
