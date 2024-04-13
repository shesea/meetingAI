FROM eclipse-temurin:17-jdk-focal
WORKDIR /

ARG api_key
ARG cluster_id
ARG db_name
ARG db_username
ARG db_password

ENV API_KEY=${api_key}
ENV CLUSTER_ID=${cluster_id}
ENV DB_NAME=${db_name}
ENV DB_USERNAME=${db_username}
ENV DB_PASSWORD=${db_password}

RUN apt-get update && \
    apt-get install wget postgresql-client --yes && \
    mkdir --parents ~/.postgresql && \
    wget "https://storage.yandexcloud.net/cloud-certs/CA.pem" \
         --output-document ~/.postgresql/root.crt && \
    chmod 0600 ~/.postgresql/root.crt
COPY ssl/certs.pem ssl/certs.pem
COPY ssl/private_key.pem ssl/private_key.pem
RUN mkdir /root/.aws
COPY .aws/credentials /root/.aws/credentials
COPY target/meetingAI-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
EXPOSE 8002