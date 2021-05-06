FROM folioci/alpine-jre-openjdk11:latest

# Copy your fat jar to the container
ENV APP_FILE mod-data-export-spring-fat.jar

# - should be a single jar file
ARG JAR_FILE=./target/*-exec.jar
# - copy
COPY ${JAR_FILE} ${JAVA_APP_DIR}/${APP_FILE}

# Expose this port locally in the container.
EXPOSE 8081
