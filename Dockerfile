FROM eclipse-temurin:17-jdk-focal
COPY /home/runner/work/meetingAI/meetingAI/target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
EXPOSE 8002