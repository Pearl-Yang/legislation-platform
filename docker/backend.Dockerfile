# 后端镜像(基于 Eclipse Temurin JDK 17,war 部署到外置 Tomcat)
# 也可以用 mvn spring-boot:run 跑,镜像仅用于生产或团队 CI 一致性
#
# 构建:
#   docker build -f docker/backend.Dockerfile -t legislation-backend legislation-edition/backend
# 运行:
#   docker run -p 8083:8083 --env-file .env legislation-backend

FROM eclipse-temurin:17-jdk AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -e -DskipTests dependency:go-offline || true
COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/target/legislation-edition-backend.war /app/app.war
EXPOSE 8083
ENV JAVA_OPTS="-Xms256m -Xmx768m"
# 用 spring-boot 自带 tomcat 跑 war(简化部署)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.war --spring.profiles.active=dev"]
