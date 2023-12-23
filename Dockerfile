FROM eclipse-temurin:17-jdk-focal
COPY target/meetingAI-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
EXPOSE 8002