# 部署

## 服务器：一条命令部署与更新

首次部署和后续迭代都用同一个脚本，会自动判断是哪一种：

```bash
./deploy/deploy.sh             # 自动识别首次 / 更新
./deploy/deploy.sh --dry-run   # 只打印会同步什么，不改服务器
```

脚本负责：生产配置校验 → 同步源码 → 构建并重启 api → 等健康检查 → 渲染并安装宝塔 nginx 站点配置 → 公网验一遍反代。
新机器上的人工前置（装 Docker、域名解析、备案、微信公众平台配置）见 [../deploy.md](../deploy.md)。

| 场景 | 行为 |
|------|------|
| 首次（服务器上没有 `deploy/docker-compose.yml`） | 推源码 + `docker-compose.yml` + `Dockerfile.minio` + `deploy/.env` |
| 更新 | 推源码 + compose + MinIO Dockerfile + nginx 模板（覆盖前留 `.bak.<时间戳>`）；服务器上的 `.env` 保持不动 |
| `--push-env` | 更新时也用本机 `.env` 覆盖服务器配置，覆盖前留 `.env.bak.<时间戳>` |
| `--skip-nginx` | 只动容器，不安装站点配置 |
| `--first` | 强制按首次部署处理 |

常用覆盖项：`CFM_HOST`、`CFM_REMOTE_DIR`、`CFM_ENV_FILE`、`CFM_HEALTH_URL`、`CFM_ALLOW_DEV_ENV=1`（跳过配置校验）、`CFM_STRICT_ENV=1`（把弱口令告警升级为拒绝部署）、`CFM_NGINX_CONF_DIR` / `CFM_SSL_CERT` / `CFM_SSL_CERT_KEY` / `CFM_ACME_ROOT`（非默认站点路径时用）。

`./deploy/update-server.sh` 仍然可用，等价于 `./deploy/deploy.sh`。

**DB_PASSWORD 是「写一次」变量。** 数据卷里的 Postgres 角色密码在首次建库时就固定了，之后只改 `.env` 不会同步过去，
API 会以 `password authentication failed` 起不来。换密码要走：

```bash
# 在服务器 /www/wwwroot/cfm/deploy 下
docker compose exec -T postgres psql -U cfm -d cfm -c "ALTER USER cfm WITH PASSWORD '<新口令>';"
# 再把新口令写进本机 deploy/.env，重跑 ./deploy/deploy.sh --push-env
```

## 国内加速（服务器首次构建慢时看）

| 环节 | 现状 | 怎么加速 |
|------|------|----------|
| Maven 依赖 | 已固定走 `maven.aliyun.com`（`server/docker-maven-settings.xml`） | 不用配 |
| Docker 基础镜像 | Docker Hub 官方 tag（postgres / redis / alpine / maven / jre） | 服务器配 `registry-mirrors`，或 `.env` 里设 `IMAGE_PREFIX` |
| MinIO 二进制 | `dl.min.io`（已从 GitHub releases 换掉） | 慢就设 `MINIO_DOWNLOAD_BASE` 指到镜像站 |

**推荐做法：给 Docker 守护进程配镜像站**，一次配置对本机所有项目生效：

```bash
sudo tee /etc/docker/daemon.json >/dev/null <<'JSON'
{
  "registry-mirrors": ["https://docker.m.daocloud.io"]
}
JSON
sudo systemctl restart docker
docker info | grep -A2 'Registry Mirrors'
```

公共镜像站地址常变，用阿里云控制台给的专属加速地址最稳。**注意 `systemctl restart docker` 会让本机所有容器重启**
（`restart: unless-stopped` 的会自动起来，homestocker / shop 这些会短暂中断几秒）。

不想动 `daemon.json` 时，在 `deploy/.env` 里写前缀代替：

```bash
IMAGE_PREFIX=docker.m.daocloud.io/library/
```

留空 = 用官方镜像；配了 `registry-mirrors` 就不要再填它。**前缀必须带 `library/` 段**，否则 `postgres` 这种官方镜像会 404。

`docker system prune -a` 会连 BuildKit 的依赖缓存一起清掉，下次构建是全量下载 Maven 依赖（走阿里云镜像，通常 1～3 分钟）。

## 本地 / 单机

在本目录执行：

```bash
cp .env.example .env
# 填入 WX_APPID / WX_SECRET；店主 openid 见下方「绑店主」
docker compose up -d --build
```

验证：

```bash
curl -s http://127.0.0.1:8088/api/v1/health
# {"code":0,"message":"ok","data":{"status":"UP","db":"UP"}}
```

## 绑店主（本地与生产相同）

登录必须走真实微信 `code`（需已配置 AppID/Secret）。

1. 用店主微信打开小程序，完成一次登录  
2. 查 openid：

```bash
docker exec -it cfm-postgres psql -U cfm -d cfm \
  -c 'SELECT id, openid, nickname FROM "user" ORDER BY id;'
```

3. 写入 `deploy/.env`：

```bash
BOOTSTRAP_OWNER_OPENID=从上面查到的openid
```

4. 重启 API：`docker compose up -d api`  
5. 店主再进一次小程序（触发登录）→「我的」出现商家工作台  

也可直接刷库：

```sql
INSERT INTO store_member (store_id, user_id, role, created_at, updated_at)
VALUES (1, <user_id>, 'OWNER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
```

## 图片地址

正式环境 `UPLOAD_PUBLIC_BASE=https://cfm.zfqwjs.com/communityfreshmarket`。路径就是桶名，和 HomeStocker 的 `/homestocker/` 一样。宝塔反向代理目录 `/communityfreshmarket`，目标 `http://127.0.0.1:9002`。

开发者工具把同一项改成 `http://127.0.0.1:9002/communityfreshmarket`，图片直接打开 MinIO，不经过接口。

## 接口抽查（需小程序登录后的令牌）

```bash
TOKEN=...
curl -s http://127.0.0.1:8088/api/v1/me -H "Authorization: Bearer $TOKEN"
curl -s http://127.0.0.1:8088/api/v1/categories -H "Authorization: Bearer $TOKEN"
curl -s http://127.0.0.1:8088/api/v1/products -H "Authorization: Bearer $TOKEN"

# 商家新建商品（须已是店铺成员）
curl -s http://127.0.0.1:8088/api/v1/merchant/products \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"categoryId":1,"name":"油麦菜","unit":"JIN","priceFen":450,"inStock":true,"onShelf":true}'
```

履约设置 / 上传 / 到货提醒：

- `PUT /api/v1/merchant/store`（含 `heroImageUrl`）
- `POST /api/v1/merchant/pickup-slots`
- `POST /api/v1/upload/image`（multipart file）
- `POST /api/v1/products/{id}/restock-watch`

环境变量说明见 `.env.example`（含四类订阅消息模板；以及邀请店员 `INVITE_LINK_BASE`）。

**邀请店员 · 公众平台配置（扫普通链接二维码打开小程序）**

| 项 | 值 |
|----|-----|
| 链接前缀 | 与 `INVITE_LINK_BASE` 一致，例 `https://your.example.com/cfm/join` |
| 完整示例 | `https://your.example.com/cfm/join?code=AB12CD` |
| 小程序页面 | `packageCustomer/pages/join-store/join-store` |
| 参数名 | `code`（邀请码） |

生产务必更换数据库密码，并配置真实微信密钥与订阅模板 ID；小程序 `utils/config.js` 须与 `.env` 中模板 ID 一致。登录态在 `cfm-redis`，不映射宿主机端口。
