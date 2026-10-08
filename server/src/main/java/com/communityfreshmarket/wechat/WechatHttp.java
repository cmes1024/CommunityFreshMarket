package com.communityfreshmarket.wechat;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * 调微信开放接口的 HTTP 客户端。
 *
 * <p>JDK 默认 HTTP/2 在 Docker 里直连 api.weixin.qq.com 时，TLS 握手经常被对端掐断并挂住，
 * 登录接口会一直不返回。这里强制 HTTP/1.1，并限制连接/读取时间。若设置了 HTTPS_PROXY
 * （本机代理，如 {@code http://host.docker.internal:7890}），则走代理。
 */
public final class WechatHttp {

  private static final Logger log = LoggerFactory.getLogger(WechatHttp.class);
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(8);

  private static final RestClient CLIENT = build();

  private WechatHttp() {}

  public static RestClient client() {
    return CLIENT;
  }

  private static RestClient build() {
    HttpClient.Builder http = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(CONNECT_TIMEOUT);
    ProxySelector proxy = proxyFromEnv();
    if (proxy != null) {
      http.proxy(proxy);
    }
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http.build());
    factory.setReadTimeout(READ_TIMEOUT);
    return RestClient.builder().requestFactory(factory).build();
  }

  private static ProxySelector proxyFromEnv() {
    String raw = first(
        System.getenv("HTTPS_PROXY"),
        System.getenv("https_proxy"),
        System.getenv("HTTP_PROXY"),
        System.getenv("http_proxy"));
    if (!StringUtils.hasText(raw)) {
      log.info("wechat http direct");
      return null;
    }
    URI uri = URI.create(raw.trim());
    String host = uri.getHost();
    if (!StringUtils.hasText(host)) {
      log.warn("wechat http proxy ignored, bad url");
      return null;
    }
    int port = uri.getPort() > 0 ? uri.getPort() : 80;
    log.info("wechat http via proxy {}:{}", host, port);
    return ProxySelector.of(new InetSocketAddress(host, port));
  }

  private static String first(String... values) {
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value;
      }
    }
    return null;
  }
}
