FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
ARG HTTPS_PROXY
ARG HTTP_PROXY
ENV HTTPS_PROXY=${HTTPS_PROXY} HTTP_PROXY=${HTTP_PROXY}
COPY pom.xml .
COPY src ./src
# 容器直连 Maven Central 会 TLS 握手失败。Maven 3.9 默认传输不看 MAVEN_OPTS 里的 proxyHost，要写 settings.xml。
RUN if [ -n "$HTTPS_PROXY" ]; then \
      rest="${HTTPS_PROXY#*://}"; \
      rest="${rest%%/*}"; \
      host="${rest%%:*}"; \
      port="${rest##*:}"; \
      if [ "$port" = "$rest" ]; then port=80; fi; \
      echo "maven via proxy ${host}:${port}"; \
      mkdir -p /root/.m2; \
      printf '%s\n' \
        '<settings>' \
        '  <proxies>' \
        '    <proxy><id>http</id><active>true</active><protocol>http</protocol>' \
        "      <host>${host}</host><port>${port}</port>" \
        '    </proxy>' \
        '    <proxy><id>https</id><active>true</active><protocol>https</protocol>' \
        "      <host>${host}</host><port>${port}</port>" \
        '    </proxy>' \
        '  </proxies>' \
        '</settings>' > /root/.m2/settings.xml; \
    fi; \
    mvn -q -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
ENV TZ=Asia/Shanghai
ENV JAVA_TOOL_OPTIONS=-Duser.timezone=Asia/Shanghai
RUN addgroup -S app && adduser -S app -G app
USER app
COPY --from=build /app/target/server-*.jar /app/app.jar
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
