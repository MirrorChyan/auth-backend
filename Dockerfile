FROM amd64/eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/mirrorc-cdk-backend.jar .

EXPOSE 9768

ENTRYPOINT ["java", "-jar", "-XX:+UseZGC", "-XX:+ZGenerational", "mirrorc-cdk-backend.jar"]
