# syntax=docker/dockerfile:1

# ===== Build stage: TypeScript 프론트 + Spring Boot JAR =====
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 빌드 설정 먼저 복사 (레이어 캐시 활용)
COPY gradlew settings.gradle build.gradle package.json tsconfig.json ./
COPY gradle ./gradle
RUN chmod +x gradlew

# 소스 복사
COPY src ./src
COPY frontend ./frontend

# bootJar = 프론트(node-gradle가 Node 자동설치 → tsc) + 백엔드 컴파일 + 패키징
RUN ./gradlew bootJar --no-daemon

# ===== Runtime stage =====
FROM eclipse-temurin:21-jre
WORKDIR /app

# 헬스체크용 curl + 비루트 실행 사용자
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd -r -u 1001 appuser

COPY --from=build /app/build/libs/*.jar app.jar
RUN mkdir -p /app/logs && chown -R appuser:appuser /app

USER appuser
EXPOSE 9090

# 컨테이너 메모리 한도에 맞춰 힙 자동 조정
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
# exec form: SIGTERM 이 JVM 에 전달되어 graceful shutdown 동작
ENTRYPOINT ["java", "-jar", "app.jar"]
