# 指明构建的新镜像是来自于 xxx 基础镜像
ARG JAVA_VERSION=8
FROM eclipse-temurin:${JAVA_VERSION}-jdk-focal as stage-jar-builder
ARG JAR_FILE=app.jar
COPY ${JAR_FILE} app.jar
RUN java -Djarmode=layertools -jar app.jar extract && mkdir -p /data/tmp && mkdir -p /data/logs && mkdir -p /config

FROM eclipse-temurin:8-jre

ENV JAVA_OPTS="-server"

WORKDIR /app

COPY --from=stage-jar-builder --chown=nonroot:nonroot /data /data
COPY --from=stage-jar-builder --chown=nonroot:nonroot /config /config
COPY --from=stage-jar-builder --chown=nonroot:nonroot dependencies/ ./
COPY --from=stage-jar-builder --chown=nonroot:nonroot snapshot-dependencies/ ./
COPY --from=stage-jar-builder --chown=nonroot:nonroot spring-boot-loader/ ./
COPY --from=stage-jar-builder --chown=nonroot:nonroot application/ ./

ENV TZ="Asia/Shanghai"

VOLUME ["/data", "/config", "/logs"]
EXPOSE 8080
# https://blog.csdn.net/kingwinstar/article/details/112648368 平滑关机
# https://spring.io/guides/topicals/spring-boot-docker/
ENTRYPOINT ["/bin/sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.JarLauncher"]