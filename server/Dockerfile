# 基础镜像可由 compose 的 build.args 覆盖，国内机器可指到镜像站。
ARG MAVEN_IMAGE=maven:3.9.9-eclipse-temurin-17
ARG JRE_IMAGE=eclipse-temurin:17-jre-alpine

FROM ${MAVEN_IMAGE} AS build
WORKDIR /app

# 国内服务器访问 Maven Central 经常超时，构建走阿里云镜像。
COPY docker-maven-settings.xml /root/.m2/settings.xml

# 先只拷 pom，依赖层可复用：只改 src 时不必重新下载依赖。
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -q -B -DskipTests dependency:go-offline || true

COPY src ./src
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -q -B -DskipTests package

FROM ${JRE_IMAGE}
WORKDIR /app
ENV TZ=Asia/Shanghai
ENV JAVA_TOOL_OPTIONS=-Duser.timezone=Asia/Shanghai
RUN addgroup -S app && adduser -S app -G app
USER app
COPY --from=build /app/target/server-*.jar /app/app.jar
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
