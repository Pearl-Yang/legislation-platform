# 后端镜像(基于 Eclipse Temurin JDK 17 JRE)
# 复用宿主机的 legislation-edition-backend.jar,避免容器内 mvn 编译
#
# 构建前置(在宿主机执行一次):
#   cd legislation-edition/backend && mvn -DskipTests clean package
#
# 构建镜像:
#   docker build -f docker/backend.Dockerfile -t legislation-backend legislation-edition/backend
# 运行:
#   docker run -p 8083:8083 --env-file docker/.env legislation-backend
#
# 为什么不用多阶段 mvn 构建:
#   - eclipse-temurin:17-jdk 的 PATH 中没有 mvn,需要额外装 Maven 镜像,体积 +800MB
#   - 容器内 mvn 编译在 CI 之外不可重现(无法调试)
#   - 复用宿主机 jar 体积小、启动快、调试方便

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/legislation-edition-backend.jar /app/app.jar
EXPOSE 8083
ENV JAVA_OPTS="-Xms256m -Xmx768m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar --spring.profiles.active=dev"]
