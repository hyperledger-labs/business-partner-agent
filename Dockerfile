FROM maven:3-amazoncorretto-11

WORKDIR /home/maven/backend
COPY backend/pom.xml backend/formatter.xml ./
COPY backend/company-agent/pom.xml/ company-agent/pom.xml
COPY backend/registry/pom.xml/ registry/pom.xml
RUN mvn dependency:go-offline

COPY backend/company-agent ./company-agent
COPY frontend ../frontend

WORKDIR /home/maven/backend/company-agent
RUN mvn package -P build-frontend -DskipTests=true

FROM adoptopenjdk/openjdk11-openj9:alpine-slim
COPY --from=0 /home/maven/backend/company-agent/target/organizational-agent*SNAPSHOT.jar organizational-agent.jar

EXPOSE 8080
CMD java -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -Dcom.sun.management.jmxremote -noverify ${JAVA_OPTS} -jar organizational-agent.jar