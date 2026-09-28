FROM openjdk:8-jdk-slim
WORKDIR /app
ARG JAR_FILE=target/*-exec.jar
COPY ${JAR_FILE} app.jar
ENV SERVER_ADDRESS=0.0.0.0
EXPOSE 8000
CMD ["java", "-Duser.timezone=Asia/Shanghai", "-jar", "app.jar"]
