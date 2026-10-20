package com.communityfreshmarket.wechat;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 调微信开放接口的 HTTP 客户端。
 *
 * <p>JDK 默认 HTTP/2 在 Docker 里直连 api.weixin.qq.com 时，TLS 握手经常被对端掐断并挂住，
 * 登录接口会一直不返回。这里强制 HTTP/1.1，并限制连接/读取时间。
 */
public final class WechatHttp {

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(8);

  private static final RestClient CLIENT = build();

  private WechatHttp() {}

  public static RestClient client() {
    return CLIENT;
  }

  private static RestClient build() {
    HttpClient http = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(CONNECT_TIMEOUT)
        .build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
    factory.setReadTimeout(READ_TIMEOUT);
    return RestClient.builder().requestFactory(factory).build();
  }
}
