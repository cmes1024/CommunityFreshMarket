FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
ARG HTTPS_PROXY
ARG HTTP_PROXY
ENV HTTPS_PROXY=${HTTPS_PROXY} HTTP_PROXY=${HTTP_PROXY}
COPY pom.xml .
COPY src ./src
# 构建阶段与运行时一样：容器直连 Maven Central 会 TLS 握手失败，有代理则走代理。
RUN if [ -n "$HTTPS_PROXY" ]; then \
      host=$(printf '%s' "$HTTPS_PROXY" | sed -E 's#https?://([^:/]+).*#\1#'); \
      port=$(printf '%s' "$HTTPS_PROXY" | sed -E 's#https?://[^:/]+:([0-9]+).*#\1#'); \
      case "$port" in ''|http*) port=80;; esac; \
      export MAVEN_OPTS="-Dhttps.proxyHost=$host -Dhttps.proxyPort=$port -Dhttp.proxyHost=$host -Dhttp.proxyPort=$port"; \
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
