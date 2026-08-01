# Bước 1: Build ứng dụng với Java 21 OpenJDK
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy toàn bộ mã nguồn vào trong container
COPY . .

# Phân quyền thực thi cho gradlew và tiến hành build (bỏ qua test)
RUN chmod +x ./gradlew
RUN ./gradlew clean build -x test

# Bước 2: Chạy ứng dụng với môi trường JRE nhẹ hơn
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy file .jar đã build từ Bước 1 sang
COPY --from=build /app/build/libs/*.jar app.jar

# Mở port 8080 
EXPOSE 8080

# Lệnh khởi chạy Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]