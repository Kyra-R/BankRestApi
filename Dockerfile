FROM eclipse-temurin:17-jdk-alpine
VOLUME /tmp
ARG JAR_FILE=bankapp-1.0.0.jar
COPY ${JAR_FILE} bankapp.jar
ENTRYPOINT ["java","-jar","/bankapp.jar"]