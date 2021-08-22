FROM openjdk:11.0.11-slim-buster
RUN addgroup spring && adduser --ingroup spring --disabled-password --home /home/spring --gecos "" spring
USER spring:spring
WORKDIR /home/spring
RUN mkdir config
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar", "app.jar", "--spring.config.location ./config"]
