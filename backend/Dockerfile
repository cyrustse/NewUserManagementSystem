FROM bellsoft/liberica-openjdk-alpine:17 as builder
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

FROM bellsoft/liberica-openjdk-alpine:17
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

RUN addgroup -S ums && adduser -S ums -G ums
USER ums

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
