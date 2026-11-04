#!/usr/bin/env bash
# 社区生鲜（cfm）部署脚本：首次部署与日常更新共用一条命令。
#
#   ./deploy/deploy.sh              # 自动识别首次 / 更新
#   ./deploy/deploy.sh --dry-run    # 只打印会同步什么，不改服务器
#   ./deploy/deploy.sh --first      # 强制按首次部署处理
#   ./deploy/deploy.sh --push-env   # 更新时也用本机 deploy/.env 覆盖服务器配置
#   ./deploy/deploy.sh --skip-nginx # 只动容器，不碰宝塔站点配置
#
# 做的事：
#   1. 校验本机 deploy/.env 是不是生产取值（挡住「本地开发配置误推生产」）
#   2. 探测服务器上是否已有 docker-compose.yml，据此区分首次 / 更新
#   3. 同步 server/ 源码、compose、MinIO Dockerfile、nginx 模板；首次额外同步 .env
#   4. 重建 api 并等容器就绪；就绪后渲染并安装 nginx 站点配置
#   5. 最后用公网地址验一遍反代，失败时打印日志与常见原因
#
# 注意：deploy/.env 里的 DB_PASSWORD 只在首次部署时落地。改动它不会改已有数据卷里
#      的 Postgres 角色密码，API 会以 password authentication failed 起不来。
#      确实要改，先在库里 ALTER USER，再更新 .env。
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEPLOY_DIR="${ROOT}/deploy"

HOST="${CFM_HOST:-root@47.100.199.249}"
REMOTE_DIR="${CFM_REMOTE_DIR:-/www/wwwroot/cfm}"
ENV_FILE="${CFM_ENV_FILE:-${DEPLOY_DIR}/.env}"

# 只在本机推导默认值，可用 CFM_* 覆盖。
NGINX_CONF_DIR="${CFM_NGINX_CONF_DIR:-/www/server/panel/vhost/nginx}"
SSL_CERT="${CFM_SSL_CERT:-/www/server/panel/vhost/cert/hs-cfm.zfqwjs.com.pem}"
SSL_CERT_KEY="${CFM_SSL_CERT_KEY:-/www/server/panel/vhost/cert/hs-cfm.zfqwjs.com.key}"
ACME_ROOT="${CFM_ACME_ROOT:-/www/wwwroot/acme-webroot}"

DRY_RUN=0
FORCE_FIRST=0
PUSH_ENV=0
SKIP_NGINX=0

usage() {
  awk 'NR>1 && /^#/ {sub(/^# ?/, ""); print; next} NR>1 {exit}' "$0"
}

while [ $# -gt 0 ]; do
  case "$1" in
    --dry-run) DRY_RUN=1 ;;
    --first) FORCE_FIRST=1 ;;
    --push-env) PUSH_ENV=1 ;;
    --skip-nginx) SKIP_NGINX=1 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "未知参数：$1（--help 查看用法）" >&2; exit 1 ;;
  esac
  shift
done

# ssh/rsync 各自建连接会反复要密码，复用同一条。
# ControlPath 必须短：macOS 的 unix socket 路径上限约 104 字节，%C 是固定的 40 位 hash。
SSH_OPTS=(
  -o ControlMaster=auto
  -o "ControlPath=/tmp/cfm-%C"
  -o ControlPersist=120
  -o ConnectTimeout=10
)

die() { echo "✗ $*" >&2; exit 1; }

# 末尾的 || true 不能省：脚本开了 pipefail，key 不存在时 grep 返回 1
# 会让整个赋值语句失败，set -e 直接静默退出。
env_val() {
  local key="$1"
  grep -E "^[[:space:]]*${key}=" "$ENV_FILE" 2>/dev/null | head -1 \
    | cut -d= -f2- | tr -d '\r' | sed "s/^\"//; s/\"\$//; s/^'//; s/'\$//" || true
}

# 本机 .env 会整体覆盖服务器配置，这里挡住明显是开发环境的取值。
assert_prod_env() {
  if [ ! -f "$ENV_FILE" ]; then
    echo "先照 deploy/README.md 复制一份：cp deploy/.env.example deploy/.env" >&2
    die "缺少 ${ENV_FILE}，拒绝部署（服务器会继续用旧配置）"
  fi
  if [ "${CFM_ALLOW_DEV_ENV:-0}" = "1" ]; then
    echo "⚠️  已用 CFM_ALLOW_DEV_ENV=1 跳过生产配置校验"
    return
  fi

  local base host_part bad=0 warn=0
  base="$(env_val UPLOAD_PUBLIC_BASE)"

  if [ -z "$base" ]; then
    echo "UPLOAD_PUBLIC_BASE 为空，拒绝部署（小程序拿不到图片地址）。" >&2
    bad=1
  else
    if [ "${base#https://}" = "$base" ]; then
      echo "UPLOAD_PUBLIC_BASE 不是 https（当前 ${base}），拒绝部署。" >&2
      echo "小程序 downloadFile 合法域名只接受 443 上的 https。" >&2
      bad=1
    fi
    host_part="$(printf '%s' "$base" | sed -E 's|^https://([^/]+).*|\1|')"
    if printf '%s' "$host_part" | grep -Eq '^(localhost|127\.0\.0\.1|0\.0\.0\.0)$|:[0-9]+$'; then
      echo "UPLOAD_PUBLIC_BASE 指向本机或带了端口（当前 ${base}），拒绝部署。" >&2
      bad=1
    fi
  fi

  local invite
  invite="$(env_val INVITE_LINK_BASE)"
  if [ -n "$invite" ]; then
    if [ "${invite#https://}" = "$invite" ] || printf '%s' "$invite" | grep -q 'your\.example\.com'; then
      echo "INVITE_LINK_BASE 仍是模板值或不是 https（当前 ${invite}），拒绝部署。" >&2
      echo "店员邀请二维码会指向无效链接，先改成 https://<本站域名>/cfm/join。" >&2
      bad=1
    fi
  fi

  local appid secret dbpw pair k v
  appid="$(env_val WX_APPID)"
  secret="$(env_val WX_SECRET)"
  dbpw="$(env_val DB_PASSWORD)"

  for pair in "WX_APPID:${appid}" "WX_SECRET:${secret}" "DB_PASSWORD:${dbpw}"; do
    k="${pair%%:*}"
    v="${pair#*:}"
    if [ -z "$v" ]; then
      echo "${k} 为空，拒绝部署。" >&2
      bad=1
    elif printf '%s' "$v" | grep -Eq '^(change-me|your_|xxx)$'; then
      echo "${k} 仍是模板占位值，拒绝部署。" >&2
      bad=1
    fi
  done

  # 数据卷里的角色密码在首次建库时就固定了，改 .env 不会同步过去。
  # 所以这里只警告，不阻断：真要换密码得先 ALTER USER。
  if [ "$dbpw" = "cfm_dev_password" ]; then
    echo "⚠️  DB_PASSWORD 仍是默认的 cfm_dev_password。" >&2
    echo "    换密码必须先改库（在服务器 deploy/ 下）：docker compose exec -T postgres psql -U cfm -d cfm -c \"ALTER USER cfm WITH PASSWORD '<新口令>';\"" >&2
    echo "    再把新口令写进本机 deploy/.env 并重跑。只改 .env 会让 API 以 password authentication failed 起不来。" >&2
    warn=1
  fi

  if [ "$bad" -ne 0 ]; then
    echo >&2
    echo "本机 deploy/.env 当前不是生产配置。确实要推，用 CFM_ALLOW_DEV_ENV=1。" >&2
    exit 1
  fi
  if [ "$warn" -ne 0 ] && [ "${CFM_STRICT_ENV:-0}" = "1" ]; then
    die "存在弱口令 / 缺配置告警，且已开启 CFM_STRICT_ENV=1，中止部署"
  fi
}

assert_prod_env

# 健康检查地址默认由图片前缀推导，换域名时不用改脚本。
HEALTH_URL="${CFM_HEALTH_URL:-}"
if [ -z "$HEALTH_URL" ]; then
  base_origin="$(env_val UPLOAD_PUBLIC_BASE | sed -E 's|^(https?://[^/]+).*|\1|')"
  HEALTH_URL="${base_origin}/api/v1/health"
fi
SITE_HOST="$(printf '%s' "$HEALTH_URL" | sed -E 's|^https?://([^/:]+).*|\1|')"
SITE_ORIGIN="$(printf '%s' "$HEALTH_URL" | sed -E 's|(^https?://[^/]+).*|\1|')"
HTTP_PORT="$(env_val HTTP_PORT)"; HTTP_PORT="${HTTP_PORT:-8088}"
MINIO_API_PORT="$(env_val MINIO_API_PORT)"; MINIO_API_PORT="${MINIO_API_PORT:-9002}"

echo "部署目标   : ${HOST}:${REMOTE_DIR}"
echo "站点地址   : ${SITE_ORIGIN}"
echo "健康检查   : ${HEALTH_URL}"
echo "宿主机端口 : api ${HTTP_PORT} / minio ${MINIO_API_PORT}（都只绑 127.0.0.1）"
[ "$DRY_RUN" -eq 1 ] && echo "模式       : dry-run（只打印，不改服务器）"

# 先探一次连通：网络不通 / 没配免密会在这一步就说清楚，而不是 rsync 到一半失败。
# 首次允许输一次密码，之后由 ControlMaster 复用。
echo "==> 检查 SSH 连接"
ssh "${SSH_OPTS[@]}" -o ConnectTimeout=15 "$HOST" true \
  || die "无法 SSH 到 ${HOST}，检查网络，或先配置免密：ssh-copy-id ${HOST}"

if [ "$DRY_RUN" -eq 0 ]; then
  echo "==> 准备远端目录"
  ssh "${SSH_OPTS[@]}" "$HOST" "mkdir -p '${REMOTE_DIR}/server' '${REMOTE_DIR}/deploy'"
fi

# 只有 compose 和 .env 都在才算已部署过：更新分支不推 .env，
# 少了它远端会直接拒绝，还不如按首次补齐。
FIRST_DEPLOY=1
if ssh "${SSH_OPTS[@]}" "$HOST" \
   "test -f '${REMOTE_DIR}/deploy/docker-compose.yml' && test -f '${REMOTE_DIR}/deploy/.env'" 2>/dev/null; then
  FIRST_DEPLOY=0
fi
if [ "$FORCE_FIRST" -eq 1 ]; then
  FIRST_DEPLOY=1
fi

# -v 不能省：不带它 rsync 什么都不打印，--dry-run 就看不到会同步哪些文件。
RSYNC=(rsync -avz --checksum -e "ssh ${SSH_OPTS[*]}"
  --exclude 'target' --exclude '.git' --exclude '*.log' --exclude '.DS_Store'
  --exclude '.env' --exclude '.env.bak.*')
# 单独推 .env 时不能用上面那套 excludes，否则 .env 自己会被过滤掉。
RSYNC_ONE=(rsync -avz -e "ssh ${SSH_OPTS[*]}")
[ "$DRY_RUN" -eq 1 ] && RSYNC+=(--dry-run) && RSYNC_ONE+=(--dry-run)

PUSH_ENV_FLAG=0
if [ "$FIRST_DEPLOY" -eq 1 ]; then
  echo "==> 首次部署：源码 + compose + MinIO 构建文件 + .env"
  # 覆盖 .env 前，服务器上若已有同名文件先留一份。
  if [ "$DRY_RUN" -eq 0 ]; then
    ssh "${SSH_OPTS[@]}" "$HOST" \
      "if [ -f '${REMOTE_DIR}/deploy/.env' ]; then cp -a '${REMOTE_DIR}/deploy/.env' \"${REMOTE_DIR}/deploy/.env.bak.\$(date +%Y%m%d%H%M%S)\"; fi"
  fi
  "${RSYNC[@]}" "${ROOT}/server/" "${HOST}:${REMOTE_DIR}/server/"
  "${RSYNC[@]}" "${DEPLOY_DIR}/docker-compose.yml" \
    "${DEPLOY_DIR}/Dockerfile.minio" "${HOST}:${REMOTE_DIR}/deploy/"
  "${RSYNC_ONE[@]}" "${ENV_FILE}" "${HOST}:${REMOTE_DIR}/deploy/.env"
else
  echo "==> 更新：同步源码、compose、MinIO 构建文件、nginx 模板"
  echo "    （服务器上的 .env 保持不动；要覆盖加 --push-env）"
  "${RSYNC[@]}" "${ROOT}/server/" "${HOST}:${REMOTE_DIR}/server/"
  # compose / Dockerfile.minio 由仓库统一管理（端口绑定、镜像源都在里面），
  # 改动必须生效，否则新加的构建参数推不上去。覆盖前留一份备份。
  if [ "$DRY_RUN" -eq 0 ]; then
    ssh "${SSH_OPTS[@]}" "$HOST" \
      "cd '${REMOTE_DIR}/deploy' && for f in docker-compose.yml Dockerfile.minio; do if [ -f \"\$f\" ]; then cp -a \"\$f\" \"\$f.bak.\$(date +%Y%m%d%H%M%S)\"; fi; done; true"
  fi
  "${RSYNC[@]}" "${DEPLOY_DIR}/docker-compose.yml" \
    "${DEPLOY_DIR}/Dockerfile.minio" "${HOST}:${REMOTE_DIR}/deploy/"
  if [ "$PUSH_ENV" -eq 1 ]; then
    echo "    --push-env：覆盖服务器 .env（覆盖前会留 .env.bak.<时间戳>）"
    "${RSYNC_ONE[@]}" "${ENV_FILE}" "${HOST}:${REMOTE_DIR}/deploy/.env"
    PUSH_ENV_FLAG=1
  fi
fi
"${RSYNC[@]}" "${DEPLOY_DIR}/nginx/" "${HOST}:${REMOTE_DIR}/deploy/nginx/"

if [ "$DRY_RUN" -eq 1 ]; then
  echo "dry-run 结束，未改动服务器"
  exit 0
fi

echo "==> 服务器侧：构建并重启"
ssh "${SSH_OPTS[@]}" -o ServerAliveInterval=30 "$HOST" "bash -s" -- \
  "$REMOTE_DIR" "$HEALTH_URL" "$SITE_HOST" "$FIRST_DEPLOY" "$PUSH_ENV_FLAG" "$SKIP_NGINX" \
  "$HTTP_PORT" "$MINIO_API_PORT" "$NGINX_CONF_DIR" "$SSL_CERT" "$SSL_CERT_KEY" "$ACME_ROOT" <<'EOF'
set -euo pipefail

REMOTE_DIR="$1"; HEALTH_URL="$2"; SITE_HOST="$3"; FIRST="$4"; PUSH_ENV="$5"; SKIP_NGINX="$6"
HTTP_PORT="$7"; MINIO_API_PORT="$8"; NGINX_CONF_DIR="$9"
SSL_CERT="${10}"; SSL_CERT_KEY="${11}"; ACME_ROOT="${12}"

cd "${REMOTE_DIR}/deploy"

if ! command -v docker >/dev/null 2>&1; then
  echo "服务器没有 docker，先装 Docker Engine 与 Compose 插件（见 deploy.md §5）" >&2
  exit 1
fi
if ! docker compose version >/dev/null 2>&1; then
  echo "服务器的 docker compose 插件不可用（老版本只有 docker-compose），先升级 Docker" >&2
  exit 1
fi
if [ ! -f .env ]; then
  echo "缺少 ${REMOTE_DIR}/deploy/.env，拒绝继续" >&2
  exit 1
fi

if [ "$PUSH_ENV" = "1" ]; then
  cp -a .env ".env.bak.$(date +%Y%m%d%H%M%S)"
fi

# 服务器没有本机 Clash 代理，置空再启动；行不存在时 sed 不动。
sed -i 's/^JAVA_TOOL_OPTIONS=.*/JAVA_TOOL_OPTIONS=/' .env

fail() {
  echo "$1" >&2
  echo "--- api 最近日志 ---" >&2
  docker compose logs --tail=60 api >&2 || true
  recent="$(docker compose logs --tail=200 api 2>/dev/null || true)"
  if printf '%s' "$recent" | grep -q 'password authentication failed'; then
    echo >&2
    echo "原因：数据库密码与数据卷不一致（.env 里的 DB_PASSWORD 被改过，或卷是旧密码建的）。" >&2
    echo "修复（不动数据）：" >&2
    echo "  PW=\$(grep -E '^DB_PASSWORD=' .env | cut -d= -f2-)" >&2
    echo "  docker compose exec -T postgres psql -U cfm -d cfm -c \"ALTER USER cfm WITH PASSWORD '\$PW';\"" >&2
    echo "  docker compose up -d --force-recreate api" >&2
  fi
  exit 1
}

docker_has() { docker image inspect "$1" >/dev/null 2>&1; }

# Docker Hub 在国内常常很慢；没配加速时先提醒一次，不阻断部署。
if [ "$FIRST" = "1" ]; then
  img_prefix="$(grep -E '^IMAGE_PREFIX=' .env | head -1 | cut -d= -f2- | tr -d '\r' || true)"
  has_mirror=0
  if [ -f /etc/docker/daemon.json ] && grep -q 'registry-mirrors' /etc/docker/daemon.json 2>/dev/null; then
    has_mirror=1
  fi
  if [ -z "$img_prefix" ] && [ "$has_mirror" = "0" ]; then
    echo "提示：这台机器没配 Docker 国内加速，基础镜像会从 Docker Hub 直拉，可能很久。"
    echo "      一次性配置（影响本机所有容器）："
    echo "        /etc/docker/daemon.json 加 {\"registry-mirrors\":[\"https://docker.m.daocloud.io\"]}"
    echo "        systemctl restart docker"
    echo "      或在本目录 .env 里设 IMAGE_PREFIX=docker.m.daocloud.io/library/ 后重跑。"
  fi
fi

# MinIO 首次构建要下载二进制，国内很慢。同机已有 hs-minio:local 就直接复用。
if ! docker_has cfm-minio:local && docker_has hs-minio:local; then
  echo "复用 hs-minio:local 作为 cfm-minio:local（省掉重新下载）"
  docker tag hs-minio:local cfm-minio:local
fi

# Dockerfile 里用了 RUN --mount=type=cache，需要 BuildKit；老版本默认关闭。
export DOCKER_BUILDKIT=1

echo "构建 api 镜像"
docker compose build api

if ! docker_has cfm-minio:local; then
  echo "没有可复用的 MinIO 镜像，本次连 MinIO 一起构建（要下载二进制，可能几分钟）"
  docker compose build minio
fi

# 依赖服务先确保在跑：compose 文件还在但容器被删过时（比如清了缓存），
# 只重建 api 会让它连不上库和 MinIO。已在跑的不会被重建。
docker compose up -d --no-build --remove-orphans postgres redis minio

if [ "$FIRST" = "1" ]; then
  docker compose up -d --no-build --remove-orphans
else
  docker compose up -d --no-build --remove-orphans --force-recreate --no-deps api
fi

docker compose ps

# ---------- 等容器就绪（走宿主机回环端口，不依赖反代）----------
LOCAL_HEALTH_URL="http://127.0.0.1:${HTTP_PORT}/api/v1/health"
if [ "$FIRST" = "1" ]; then
  tries=150   # 首次含构建与 Flyway 初始化，最多等 12.5 分钟
else
  tries=60    # 常规更新最多等 5 分钟
fi

echo "等待 api 就绪 ${LOCAL_HEALTH_URL}"
ready=0
for _ in $(seq 1 "$tries"); do
  cid="$(docker compose ps -q api 2>/dev/null || true)"
  state="$(docker inspect -f '{{.State.Status}}' "$cid" 2>/dev/null || echo unknown)"
  if [ "$state" = "restarting" ] || [ "$state" = "exited" ]; then
    fail "api 容器没有保持运行（${state}）"
  fi
  if body="$(curl -fsS "$LOCAL_HEALTH_URL" 2>/dev/null)"; then
    echo "$body"
    ready=1
    break
  fi
  sleep 5
done
[ "$ready" = "1" ] || fail "健康检查超时：${LOCAL_HEALTH_URL}"

# ---------- nginx ----------
NGINX_MANAGED=0

render_and_install_nginx() {
  local tpl="${REMOTE_DIR}/deploy/nginx/cfm.zfqwjs.com.conf.tpl"
  local conf="${NGINX_CONF_DIR}/cfm.zfqwjs.com.conf"
  local tmp="/tmp/cfm-nginx.$$.conf"
  local bak=""

  if [ "$SKIP_NGINX" = "1" ]; then
    echo "已跳过 nginx（--skip-nginx）"
    return
  fi
  if [ ! -f "$tpl" ]; then
    echo "缺少 nginx 模板 ${tpl}，跳过反代配置；公网会 502 直到手工处理" >&2
    return
  fi
  if [ ! -d "$NGINX_CONF_DIR" ]; then
    echo "未找到 ${NGINX_CONF_DIR}（不是宝塔机器？），跳过 nginx。" >&2
    echo "请自行把 ${SITE_HOST} 反代到 127.0.0.1:${HTTP_PORT}、/communityfreshmarket/ 到 127.0.0.1:${MINIO_API_PORT}" >&2
    return
  fi

  sed -e "s|__SERVER_NAME__|${SITE_HOST}|g" \
      -e "s|__HTTP_PORT__|${HTTP_PORT}|g" \
      -e "s|__MINIO_API_PORT__|${MINIO_API_PORT}|g" \
      -e "s|__SSL_CERT__|${SSL_CERT}|g" \
      -e "s|__SSL_CERT_KEY__|${SSL_CERT_KEY}|g" \
      -e "s|__ACME_ROOT__|${ACME_ROOT}|g" "$tpl" > "$tmp"

  if [ -f "$conf" ] && cmp -s "$tmp" "$conf"; then
    echo "nginx 配置无变化"
    rm -f "$tmp"
    NGINX_MANAGED=1
    return
  fi

  local nginx_bin
  nginx_bin="$(command -v nginx || true)"
  if [ -z "$nginx_bin" ] && [ -x /www/server/nginx/sbin/nginx ]; then
    nginx_bin=/www/server/nginx/sbin/nginx
  fi

  if [ -f "$conf" ]; then
    bak="${conf}.bak.$(date +%Y%m%d%H%M%S)"
    cp -a "$conf" "$bak"
  fi
  cp -f "$tmp" "$conf"
  chmod 644 "$conf"
  rm -f "$tmp"

  if [ -z "$nginx_bin" ]; then
    echo "找不到 nginx 可执行文件，已写入 ${conf} 但未 reload" >&2
    return
  fi
  if ! "$nginx_bin" -t; then
    echo "nginx -t 失败，回滚站点配置" >&2
    if [ -n "$bak" ]; then
      cp -a "$bak" "$conf"
    else
      rm -f "$conf"
    fi
    exit 1
  fi
  "$nginx_bin" -s reload
  echo "nginx 已重载 ${conf}"
  NGINX_MANAGED=1
}

render_and_install_nginx

# ---------- 公网验一遍反代 ----------
if [ "$NGINX_MANAGED" = "1" ]; then
  echo "验证公网入口 ${HEALTH_URL}"
  ok=0
  for _ in $(seq 1 6); do
    if curl -fsS "$HEALTH_URL" >/dev/null 2>&1; then
      ok=1
      break
    fi
    sleep 3
  done
  if [ "$ok" != "1" ]; then
    echo "容器已就绪，但公网 ${HEALTH_URL} 仍不可用。" >&2
    echo "多半是反代没生效：核对 ${NGINX_CONF_DIR}/cfm.zfqwjs.com.conf 的 proxy_pass 与证书路径。" >&2
    exit 1
  fi
else
  echo "未托管 nginx，跳过公网验证"
fi

echo
echo "部署完成：${HEALTH_URL}"

owner="$(grep -E '^BOOTSTRAP_OWNER_OPENID=' .env | head -1 | cut -d= -f2- | tr -d '\r' || true)"
if [ -z "$owner" ]; then
  cat <<'TIP'

提示：BOOTSTRAP_OWNER_OPENID 还是空的，店主登录后「我的」不会出现商家工作台。
      让店主先用小程序登录一次，再执行：
        docker exec cfm-postgres psql -U cfm -d cfm -c 'SELECT id, openid, nickname FROM "user" ORDER BY id;'
      把 openid 填进本机 deploy/.env 后重跑：
        ./deploy/deploy.sh --push-env
TIP
fi
EOF

echo
echo "内网（服务器上）：curl -sI http://127.0.0.1:${HTTP_PORT}/api/v1/health"
echo "外网：curl -sI ${HEALTH_URL}"

