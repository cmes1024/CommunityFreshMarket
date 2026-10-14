#!/usr/bin/env bash
# 在 Mac 上执行，更新服务器上的社区生鲜接口。
# 同步 server/ 源码和 deploy/nginx、deploy/.env，只重建并重启 api。
# 数据库留在原数据卷里，图片在已有 MinIO。新的 Flyway 脚本会在接口启动时自动执行。
#
# 会用本机 deploy/.env 覆盖服务器上的同名文件（微信密钥、MinIO、订阅模板等）。
# 不会覆盖服务器上已有的 deploy/docker-compose.yml（端口只绑 127.0.0.1 等改动留在服务器）。
# 服务器上还没有这份 compose 时，会先拷过去。
#
# 数据库和 nginx 容器需要已经在跑。第一次请先在服务器的 deploy 目录执行：
#   docker compose up -d
#
# 用法：
#   ./deploy/update-server.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
HOST="${CFM_HOST:-root@47.100.199.249}"
REMOTE_DIR="${CFM_REMOTE_DIR:-/www/wwwroot/cfm}"
HEALTH_URL="${CFM_HEALTH_URL:-https://cfm.zfqwjs.com/api/v1/health}"

echo "同步源码到 ${HOST}:${REMOTE_DIR}"
ssh -o ServerAliveInterval=30 "$HOST" "mkdir -p '${REMOTE_DIR}/server' '${REMOTE_DIR}/deploy/nginx'"

rsync -av \
  --exclude 'target' \
  "${ROOT}/server/" "${HOST}:${REMOTE_DIR}/server/"

rsync -av \
  "${ROOT}/deploy/nginx/" "${HOST}:${REMOTE_DIR}/deploy/nginx/"

rsync -av \
  "${ROOT}/deploy/.env" "${HOST}:${REMOTE_DIR}/deploy/.env"

if ! ssh -o ServerAliveInterval=30 "$HOST" "test -f '${REMOTE_DIR}/deploy/docker-compose.yml'"; then
  echo "服务器上还没有 docker-compose.yml，上传一份"
  rsync -av \
    "${ROOT}/deploy/docker-compose.yml" "${HOST}:${REMOTE_DIR}/deploy/docker-compose.yml"
fi

echo "重建并重启 api"
ssh -o ServerAliveInterval=30 "$HOST" "bash -s" -- "$REMOTE_DIR" "$HEALTH_URL" <<'EOF'
set -euo pipefail
REMOTE_DIR="$1"
HEALTH_URL="$2"
cd "${REMOTE_DIR}/deploy"
docker compose up -d --build --no-deps --force-recreate --pull never api
if docker compose ps --status running --services | grep -qx nginx; then
  docker compose exec -T nginx nginx -s reload
fi
for _ in $(seq 1 24); do
  if curl -fsS "$HEALTH_URL"; then
    echo
    exit 0
  fi
  sleep 5
done
echo "健康检查超时：${HEALTH_URL}" >&2
exit 1
EOF
