# 全新环境部署

本文面向**一台新的 Linux 云主机**和**一个新注册的微信小程序**。按顺序做完，可以得到：可公网访问的 API、能登录的小程序、以及第一位店主。

本地开发者工具联调仍以 [deploy/README.md](deploy/README.md) 为准。本文默认目标是**体验版 / 正式版**（必须 HTTPS、必须配置服务器域名）。

仓库里的 `miniprogram/project.config.json` 带有现有开发号 AppID，`miniprogram/utils/config.js` 里的接口地址和订阅模板 ID 也是开发环境的值。新环境必须全部换成你自己的，不要沿用。

---

## 1. 跑起来之后的结构

```text
顾客 / 店员手机
    │  HTTPS（微信要求）
    ▼
宿主机 Caddy 或 Nginx（证书、80/443）
    │  HTTP 127.0.0.1:8088
    ▼
容器 cfm-nginx ── /api/ ──▶ 容器 cfm-api（Spring Boot :8080）──▶ 容器 cfm-mysql
              └── /uploads/ ──▶ 卷 cfm_uploads（商品图、头像）
```

- 业务在 Java 服务里，不用微信云开发、不用云函数。
- 数据库结构由 Flyway 在 API 第一次启动时自动建好，并写入默认店（`id = 1`，店名「绿意鲜生」）和一批示例分类、商品、自提时段。
- 仓库内的 Nginx（`deploy/nginx/nginx.conf`）只监听 HTTP，负责反代 `/api/` 和托管 `/uploads/`。正式环境的 HTTPS 加在宿主机前面，不改这段容器配置也能上线。
- 收款在线下完成，部署时不要接微信支付。

---

## 2. 事先准备

| 项 | 要求 |
|----|------|
| 云主机 | Linux，建议 2 核 4 GB 及以上。第一次构建要在容器里跑 Maven，内存太小会失败。磁盘建议 40 GB 以上（镜像、依赖、数据库、图片） |
| 系统 | 能安装 Docker Engine 与 Docker Compose 插件。出方向要能访问 Docker Hub、Maven Central、`https://api.weixin.qq.com` |
| 域名 | 一条 A 记录指到这台机器，例如 `api.example.com`。下文用这个名字代替你的域名 |
| 备案 | 服务器在中国大陆时，域名须完成 ICP 备案。未备案的域名不能配进小程序服务器域名 |
| 微信小程序 | 新注册一个小程序。生鲜经营建议用企业或个体工商户主体；个人主体很多类目、订阅消息和客服开不了 |
| 本机工具 | 安装 [微信开发者工具](https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html)，并用小程序管理员或开发者微信扫码登录 |

小程序管理员在公众平台「成员管理」里把上传代码的人加为开发者。

---

## 3. 注册小程序并记下密钥

1. 打开 [微信公众平台](https://mp.weixin.qq.com/) 注册小程序，按经营范围选择类目（后续订阅消息模板必须落在已选类目里）。
2. 「开发管理 → 开发设置」复制 **AppID**。
3. 同一页生成 **AppSecret**。它只显示一次，立刻记到密码管理器里，不要写进 Git、不要发到聊天记录。
4. 先不要发布。服务器域名、隐私指引、订阅模板都配好再上传代码。

---

## 4. 域名

在域名服务商添加：

```text
api.example.com    A    <云主机公网 IP>
```

等解析生效后再申请证书。可用 `dig api.example.com` 或 `ping api.example.com` 确认已经指到这台机器。

邀请店员用的普通链接（下一节的 `INVITE_LINK_BASE`）必须是这个域名下的 HTTPS 地址，微信不允许 IP、不允许带非标准端口。

---

## 5. 安装 Docker

以下以 Ubuntu / Debian 为例。其他发行版用 Docker 官方文档安装 Engine 与 Compose 插件即可。

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo ${VERSION_CODENAME}) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
sudo usermod -aG docker "$USER"
```

重新登录后确认：

```bash
docker version
docker compose version
```

防火墙只对公网开放 SSH、HTTP、HTTPS。数据库端口和管理端口不要暴露。

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
sudo ufw status
```

不要执行 `ufw allow 3306` 或 `ufw allow 8088`。

---

## 6. 放置代码

```bash
sudo mkdir -p /opt/community-fresh-market
sudo chown "$USER" /opt/community-fresh-market
cd /opt/community-fresh-market
git clone <你的仓库地址> .
```

生产机只需要能构建 `server/` 和运行 `deploy/`。小程序代码在你自己的电脑上用开发者工具打开，不必在服务器上跑。

---

## 7. 填写 `deploy/.env`

```bash
cd /opt/community-fresh-market/deploy
cp .env.example .env
```

生成两段随机口令（各执行一次，分别用作数据库密码和 JWT）：

```bash
openssl rand -base64 24
openssl rand -base64 48
```

编辑 `deploy/.env`，按下面填写。`.env` 已在忽略列表里，不要提交。

```bash
MYSQL_ROOT_PASSWORD=<第一段随机口令>
DB_PASSWORD=<再生成一段，与 root 不同>
JWT_SECRET=<openssl rand -base64 48 的结果>

WX_APPID=<小程序 AppID>
WX_SECRET=<小程序 AppSecret>

# 全新机器能直接访问微信时留空。只有服务器必须走 HTTP 代理才能访问外网时才填。
# HTTPS_PROXY=

DEFAULT_STORE_ID=1
# 店主还没登录过，这里先留空。第 12 节再写。
BOOTSTRAP_OWNER_OPENID=

# 必须是手机能打开的 HTTPS 地址，末尾不要加斜杠以外的路径
UPLOAD_PUBLIC_BASE=https://api.example.com/uploads

# 四个模板还没申请就先留空。申请后把模板 ID 填进来，并与小程序 config.js 保持一致。
WX_SUBSCRIBE_RESTOCK_TEMPLATE_ID=
WX_SUBSCRIBE_NEW_ORDER_TEMPLATE_ID=
WX_SUBSCRIBE_CANCEL_TEMPLATE_ID=
WX_SUBSCRIBE_ARRIVED_TEMPLATE_ID=

# 与公众平台「扫普通链接二维码打开小程序」的链接前缀一致，不要带 ?code=
INVITE_LINK_BASE=https://api.example.com/cfm/join

# 只给本机反代用。不要改成对公网发布。
MYSQL_PORT=3306
HTTP_PORT=8088
```

说明：

| 变量 | 作用 |
|------|------|
| `WX_APPID` / `WX_SECRET` | 用 `wx.login` 的 code 向微信换 openid。空着或填错时，小程序登录失败 |
| `JWT_SECRET` | 签发登录令牌。换掉它会使所有人重新登录 |
| `BOOTSTRAP_OWNER_OPENID` | 该微信号登录后成为默认店店主。空着时任何人登录都只是顾客 |
| `UPLOAD_PUBLIC_BASE` | 图片 URL 前缀。必须和 `downloadFile` 合法域名一致，否则商品图、头像在手机上打不开 |
| `INVITE_LINK_BASE` | 店员邀请二维码里的链接前缀。服务端把邀请码拼成 `{前缀}?code=邀请码` |
| `HTTPS_PROXY` | 容器访问微信和构建时访问 Maven 的代理。正常云主机留空 |
| `JWT_EXPIRE_SECONDS` | 可选，默认 604800（7 天），一般不用写进 `.env` |

四个订阅模板可以先空着上线：主流程（登录、下单、接单）仍可用，服务端只打日志、不发微信通知。订单台红点不依赖模板。

---

## 8. 让管理端口只监听本机

编辑服务器上的 `deploy/docker-compose.yml`，把两个 `ports` 改成只绑定回环地址，避免 3306 / 8088 绕过防火墙：

```yaml
# mysql.ports
- "127.0.0.1:${MYSQL_PORT:-3306}:3306"

# nginx.ports
- "127.0.0.1:${HTTP_PORT:-8088}:80"
```

这是这台生产机上的收紧，不是功能改动。容器之间仍走 Docker 网络，不受影响。

---

## 9. 用 Caddy 提供 HTTPS

小程序体验版和正式版只接受 HTTPS，且不能写端口号。在宿主机安装 Caddy，由它申请并续期证书，再转到本机 `8088`。

```bash
sudo apt-get install -y debian-keyring debian-archive-keyring apt-transport-https curl
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' | sudo gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' | sudo tee /etc/apt/sources.list.d/caddy-stable.list
sudo apt-get update
sudo apt-get install -y caddy
```

写入 `/etc/caddy/Caddyfile`（把域名换成你的）：

```caddyfile
api.example.com {
	encode gzip

	# 微信「扫普通链接二维码」的校验文件。文件名以公众平台下载的为准。
	handle /WW_verify_*.txt {
		root * /var/www/cfm-verify
		file_server
	}

	handle {
		reverse_proxy 127.0.0.1:8088
	}
}
```

```bash
sudo mkdir -p /var/www/cfm-verify
sudo systemctl reload caddy
```

先确认证书和反代（此时后面的 API 还没启动，`/api/v1/health` 会是 502，这是正常的；首页应返回容器 Nginx 的纯文本，等第 10 节起来之后才有）：

```bash
curl -sI https://api.example.com
```

若证书申请失败，先查：80/443 是否放行、域名是否已经解析到本机、大陆机器是否已备案。

也可以改用你熟悉的宿主机 Nginx + certbot，反代目标同样是 `127.0.0.1:8088`。校验文件同样放在宿主机上直接返回，不要指望容器里的 `location /`（它只返回一行固定文字）。

---

## 10. 启动

在 `deploy/` 目录：

```bash
cd /opt/community-fresh-market/deploy
docker compose up -d --build
```

第一次会拉取 MySQL、Nginx、JDK 镜像，并在容器内执行 `mvn package`，可能要十几分钟。完成后：

```bash
docker compose ps
curl -s http://127.0.0.1:8088/api/v1/health
curl -s https://api.example.com/api/v1/health
```

两边都应类似：

```json
{"code":0,"message":"ok","data":{"status":"UP","db":"UP"}}
```

API 起不来时：

```bash
docker compose logs -f api
```

常见原因：`.env` 里数据库密码和 MySQL 首次初始化时不一致（改密码后旧数据卷仍用旧密码）、内存不足导致 Maven 被杀掉、构建时访问不了 Maven Central。

确认这台机器能访问微信（登录依赖它）：

```bash
docker run --rm curlimages/curl -sI https://api.weixin.qq.com | head -n 5
```

---

## 11. 公众平台配置

全部在 [微信公众平台](https://mp.weixin.qq.com/) 完成。域名每月可修改次数有限，确认 `https://api.example.com` 可用后再保存。

### 11.1 服务器域名

「开发管理 → 开发设置 → 服务器域名」：

| 类型 | 填写 |
|------|------|
| request 合法域名 | `https://api.example.com` |
| uploadFile 合法域名 | `https://api.example.com` |
| downloadFile 合法域名 | `https://api.example.com` |

不要填 IP，不要带端口，不要带路径。三个都要填：接口走 request，传图走 uploadFile，商品图和头像走 downloadFile。

socket 合法域名、业务域名留空即可。本项目没有 websocket，也没有 web-view。

### 11.2 用户隐私保护指引

「设置 → 服务内容声明 → 用户隐私保护指引」按代码实际用到的接口声明，否则真机调用会被微信拦住：

| 接口 | 代码里的用途 |
|------|----------------|
| 收集你选中的照片或视频信息 | 商品图、店铺头图、实景图、头像（`wx.chooseMedia`） |
| 访问你的摄像头 | 拍照上传；商家扫会员码（`wx.scanCode`） |
| 收集你的昵称、头像 | 「我的」里编辑资料（`chooseAvatar`、昵称输入框） |

隐私弹窗组件已在小程序里。指引内容用真实用途说明，例如「用于上传商品图片和设置头像」。

### 11.3 客服

订单详情和「联系门店」使用按钮 `open-type="contact"`，没有自建客服后端。

「功能 → 客服」开通，并添加至少一名客服微信。未开通时，用户点「微信客服」没有会话。

### 11.4 订阅消息模板

「功能 → 订阅消息」里选用**已选类目**下的公共模板，各申请一个（也可以四个变量填同一个模板 ID）。添加后复制模板 ID。

服务端写死了字段名，模板的两个关键词必须正好是：

| 字段 | 类型 | 内容 |
|------|------|------|
| `thing1` | 事物 | 摘要（商品名或订单摘要，最长 20 字） |
| `thing2` | 事物 | 提示语（最长 20 字） |

关键词名字不是 `thing1` / `thing2` 时，微信会拒绝发送，业务本身不会回滚。

| 环境变量 | 小程序 `config.js` | 谁授权 | 何时发送 | 发给谁 |
|----------|---------------------|--------|----------|--------|
| `WX_SUBSCRIBE_RESTOCK_TEMPLATE_ID` | `restockTemplateId` | 顾客点「到货提醒我」 | 商品改回有货 | 该顾客 |
| `WX_SUBSCRIBE_NEW_ORDER_TEMPLATE_ID` | `newOrderTemplateId` | 店员点「开启订单通知」 | 顾客提交预约 | 已授权的本店成员 |
| `WX_SUBSCRIBE_CANCEL_TEMPLATE_ID` | `cancelTemplateId` | 顾客提交/取消，或店员取消前 | 订单取消 | 对端里已授权的人 |
| `WX_SUBSCRIBE_ARRIVED_TEMPLATE_ID` | `arrivedTemplateId` | 顾客「我到店了」或店员「开启订单通知」 | 顾客报到 | 已授权的店员 |

填进服务器 `deploy/.env` 后执行 `docker compose up -d api` 使 API 读到新值。小程序侧在下一节改 `config.js`，两边字符串必须相同。

一次性订阅：用户点一次，大约只能发一条。发完需要再点一次授权。未授权时订单台红点仍然有效。

### 11.5 邀请店员（扫普通链接二维码）

店员邀请不是小程序太阳码。服务端用本地二维码编码这条链接：

```text
https://api.example.com/cfm/join?code=<邀请码>
```

「开发管理 → 开发设置 → 扫普通链接二维码打开小程序」新增规则：

| 项 | 值 |
|----|-----|
| 二维码规则 / 链接前缀 | `https://api.example.com/cfm/join` |
| 小程序功能页面 | `packageCustomer/pages/join-store/join-store` |
| 测试范围 | 先选体验版；正式发布后再改为正式版 |

保存前微信会让你下载校验文件（文件名类似 `WW_verify_xxxx.txt`）：

```bash
sudo cp ~/WW_verify_xxxx.txt /var/www/cfm-verify/
curl -sI https://api.example.com/WW_verify_xxxx.txt
```

必须返回 200，且内容与下载文件一致，然后回到公众平台点校验。`INVITE_LINK_BASE` 要和这条前缀完全一致，前面不要多写 `www`，后面不要多写斜杠（与 `.env` 里写的保持同一个字符串）。

测试链接、测试范围、发布这条规则，都在该配置页完成。只上传小程序代码并不会让微信识别这个链接。

---

## 12. 改小程序工程

在**你自己的电脑**上改仓库里的小程序，改完用开发者工具打开 `miniprogram/` 目录。

### 12.1 AppID

`miniprogram/project.config.json` 的 `appid` 改成新小程序的 AppID。开发者工具导入时如果提示 AppID 不一致，以公众平台上的为准。

### 12.2 接口地址与模板

编辑 `miniprogram/utils/config.js`：

```js
module.exports = {
  version: "0.1.0",
  env: "prod",
  apiHost: "https://api.example.com",
  defaultStoreId: 1,
  restockTemplateId: "<与 WX_SUBSCRIBE_RESTOCK_TEMPLATE_ID 相同>",
  newOrderTemplateId: "<与 WX_SUBSCRIBE_NEW_ORDER_TEMPLATE_ID 相同>",
  cancelTemplateId: "<与 WX_SUBSCRIBE_CANCEL_TEMPLATE_ID 相同>",
  arrivedTemplateId: "<与 WX_SUBSCRIBE_ARRIVED_TEMPLATE_ID 相同>",
};
```

`apiHost` 不要加 `/api`，不要末尾斜杠，不要写 `http://`，不要写端口。未申请的模板保持 `""`。

`project.config.json` 里的 `urlCheck: false` 只影响开发者工具。体验版和正式版仍校验第 11.1 节的合法域名。

### 12.3 上传

1. 开发者工具打开 `miniprogram/`，确认详情里的 AppID 是新号。
2. 右上角「上传」，填写版本号（与 `config.js` 的 `version` 对齐即可，例如 `0.1.0`）。
3. 公众平台「管理 → 版本管理」把该版本设为体验版，用店主微信扫体验版二维码。
4. 第 13 节绑定店主并走通冒烟后，再「提交审核」。审核说明可写：预约下单，收款在店内完成，小程序内无支付。
5. 审核通过后点「发布」。邀请链接规则的测试范围改成正式版并发布该规则。

---

## 13. 绑定店主

冷启动没有后台账号密码。店主就是某个微信用户。

1. 店主用体验版打开小程序，等待登录完成（能看到首页即可）。此时他还不是店员，「我的」里没有商家工作台。
2. 在服务器查出 openid：

```bash
cd /opt/community-fresh-market/deploy
# 把密码换成 .env 里的 DB_PASSWORD
docker exec cfm-mysql mysql -ucfm -p'<DB_PASSWORD>' cfm \
  -e "SELECT id, openid, nickname FROM user ORDER BY id;"
```

3. 把店主那一行的 `openid` 写入 `deploy/.env`：

```bash
BOOTSTRAP_OWNER_OPENID=oXXXXXXXX
```

4. 只重启 API：

```bash
docker compose up -d api
```

5. 店主完全退出小程序再打开（或下拉触发重新登录）。「我的」出现「商家工作台」。

若 openid 已经写对但仍无入口，可直接插入成员（`user_id` 用上面查询的 `id`）：

```bash
docker exec cfm-mysql mysql -ucfm -p'<DB_PASSWORD>' cfm -e \
  "INSERT INTO store_member (store_id, user_id, role, created_at, updated_at)
   VALUES (1, <user_id>, 'OWNER', NOW(3), NOW(3));"
```

店主登录后，在商家设置里改掉种子数据：店名、电话（库里初始是 `13800000000`）、购买须知、自提时段，并下架或删掉示例商品（小白菜、生菜、土豆、香蕉、土鸡蛋）。

之后新增店员：商家端生成邀请二维码，对方用微信扫一扫（须已发布第 11.5 节的规则）。不要把顾客 openid 再写进 `BOOTSTRAP_OWNER_OPENID`；该项只用于默认店第一位店主。

---

## 14. 上线冒烟

用体验版或正式版，在手机上走一遍（开发者工具勾选「不校验合法域名」不算数）：

1. 顾客打开即登录，首页能看到商品。
2. 商家上传一张商品图，顾客端能显示（验证 `UPLOAD_PUBLIC_BASE` 与 downloadFile 域名）。
3. 顾客加购、填地址、提交一笔配送预约；再提交一笔自提预约。
4. 店主在订单台看到新单，把状态推到完成；未完成单可以取消。
5. 订单详情「微信客服」能进会话。
6. 若已配订阅模板：顾客提交时出现订阅弹窗；店员点「开启订单通知」后，再下一单能收到服务通知。
7. 商家生成邀请二维码，另一个微信号扫码后「我的」出现商家工作台。

接口抽查（令牌从小程序登录响应里取，仅排错时用）：

```bash
curl -s https://api.example.com/api/v1/health
curl -s https://api.example.com/api/v1/me -H "Authorization: Bearer <JWT>"
```

---

## 15. 更新、备份、日志

### 更新代码

```bash
cd /opt/community-fresh-market
git pull
cd deploy
docker compose up -d --build
```

Flyway 会在 API 启动时补迁移。不要删除 Docker 卷，否则数据库和图片一起没了。

小程序有界面或 `config.js` 改动时，需要重新上传并发布，只重启服务器不会更新手机上的正式版。

### 备份

卷名以 `docker volume ls` 为准，Compose 项目目录是 `deploy` 时通常是 `deploy_cfm_mysql_data`、`deploy_cfm_uploads`。

```bash
mkdir -p /var/backups/cfm
docker exec cfm-mysql mysqldump -ucfm -p'<DB_PASSWORD>' \
  --single-transaction --routines cfm \
  > /var/backups/cfm/cfm-$(date +%F).sql

docker run --rm \
  -v deploy_cfm_uploads:/data:ro \
  -v /var/backups/cfm:/backup \
  alpine tar czf /backup/uploads-$(date +%F).tgz -C /data .
```

把这两份文件拷离这台机器。恢复数据库前先停 API：`docker compose stop api`，导入后再启动。

### 日志

```bash
cd /opt/community-fresh-market/deploy
docker compose logs -f api
docker compose logs --tail=100 nginx
```

登录失败看 API 日志里访问 `api.weixin.qq.com` 的报错。订阅发送失败会有 `subscribe ... send failed`，主流程仍应成功。

---

## 16. 对照清单

上线前逐项打勾：

- [ ] 域名已解析并备案（大陆服务器）
- [ ] `https://api.example.com/api/v1/health` 返回 `db: UP`
- [ ] 3306 与 8088 只监听 `127.0.0.1`，防火墙未放行
- [ ] `JWT_SECRET`、数据库密码、AppSecret 已换成随机值，且不在 Git 里
- [ ] `WX_APPID` / `WX_SECRET` 与公众平台一致
- [ ] `UPLOAD_PUBLIC_BASE=https://api.example.com/uploads`
- [ ] request、uploadFile、downloadFile 三个合法域名都已保存
- [ ] 隐私指引已声明相册、摄像头、昵称头像
- [ ] 客服已添加
- [ ] 订阅模板字段为 `thing1`、`thing2`，ID 已同时写入 `.env` 与 `config.js`（或明确留空）
- [ ] 邀请链接校验文件可通过 HTTPS 打开，规则已发布
- [ ] `project.config.json` 的 AppID、`config.js` 的 `apiHost` 已换成新环境
- [ ] `BOOTSTRAP_OWNER_OPENID` 已填并重启过 API，店主能进商家工作台
- [ ] 种子店名、电话、示例商品已在商家端改掉
- [ ] 手机体验版走通：登录、看图、下单、接单、客服
