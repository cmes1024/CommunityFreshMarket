# 社区生鲜（cfm）站点模板。由 deploy/deploy.sh 渲染后写入宝塔 vhost 目录。
# 占位符：__SERVER_NAME__ __HTTP_PORT__ __MINIO_API_PORT__
#         __SSL_CERT__ __SSL_CERT_KEY__ __ACME_ROOT__
#
# 约定（与 deploy.md 一致）：
#   /                       -> 127.0.0.1:__HTTP_PORT__  容器 cfm-api
#   /communityfreshmarket/  -> 127.0.0.1:__MINIO_API_PORT__  容器 cfm-minio（路径即桶名）
#   /.well-known/           -> 宿主机目录，供 ACME 做 HTTP-01 校验
server {
    listen 80;
    listen 443 ssl;
    server_name __SERVER_NAME__;

    # 商品图上传走 multipart，接口侧限制更小，这里留 10m 冗余
    client_max_body_size 10m;

    ssl_certificate __SSL_CERT__;
    ssl_certificate_key __SSL_CERT_KEY__;
    ssl_protocols TLSv1.2 TLSv1.3;

    location ^~ /.well-known/acme-challenge/ {
        default_type "text/plain";
        alias __ACME_ROOT__/.well-known/acme-challenge/;
    }

    location ^~ /communityfreshmarket/ {
        proxy_pass http://127.0.0.1:__MINIO_API_PORT__;
        proxy_set_header Host $http_host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        proxy_pass http://127.0.0.1:__HTTP_PORT__;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
