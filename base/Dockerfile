FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

COPY gradlew .
COPY gradle gradle/
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY src src/

RUN chmod +x gradlew && ./gradlew bootJar -x test

FROM eclipse-temurin:17-jdk-alpine

ENV TZ=Europe/Moscow
RUN apk add --no-cache tzdata && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone

ENV JAVA_CACERTS=/opt/java/openjdk/lib/security/cacerts
ENV JAVA_OPTS="-Djavax.net.ssl.trustStore=/opt/java/openjdk/lib/security/cacerts -Djavax.net.ssl.trustStorePassword=changeit"

WORKDIR /app

COPY ROOT.crt /tmp/root.crt
COPY ISS.crt /tmp/iss.crt
RUN keytool -importcert -alias "tagras-rootca" -file /tmp/root.crt -keystore "$JAVA_CACERTS" -storepass changeit -noprompt && \
    keytool -importcert -alias "tagras-intermediate" -file /tmp/iss.crt -keystore "$JAVA_CACERTS" -storepass changeit -noprompt && \
    rm /tmp/root.crt /tmp/iss.crt

COPY --from=builder /app/build/libs/*.jar /app/app.jar

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=prod -jar /app/app.jar"]