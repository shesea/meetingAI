FROM eclipse-temurin:17-jdk-focal
COPY target/meetingAI-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar", "-Djavax.net.ssl.certificate=ssl/private_key.pem", "-Djavax.net.ssl.certificate-private-key=ssl/certs.pem","app.jar"]
EXPOSE 8002