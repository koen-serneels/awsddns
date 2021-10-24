FROM openjdk:11.0.11-slim-buster
RUN set -eux; \
	apt-get update; \
	apt-get install -y gosu; \
	rm -rf /var/lib/apt/lists/*; \
	gosu nobody true
RUN addgroup spring && adduser --ingroup spring --disabled-password --home /home/spring --gecos "" spring
USER spring:spring
WORKDIR /home/spring
RUN mkdir config
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

USER root
COPY entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
CMD ["java", "-jar", "/home/spring/app.jar", "--spring.config.location /home/spring/config/"]
