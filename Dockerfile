FROM openjdk:21

WORKDIR /app

COPY mirrorc-cdk-backend.jar mirrorc-cdk-backend.jar

EXPOSE 9768

ENTRYPOINT ["java", "-jar", "-XX:+UseZGC", "mirrorc-cdk-backend.jar"]