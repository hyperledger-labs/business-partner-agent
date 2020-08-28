FROM maven:3-amazoncorretto-11

WORKDIR /home/maven
COPY pom.xml spotbugs-exclude.xml ../formatter.xml ./
COPY src ./src

RUN mvn package -DskipTests=true

FROM adoptopenjdk/openjdk11-openj9:alpine-slim
COPY --from=0 /home/maven/target/agent-registry-*.jar agent-registry.jar

EXPOSE 8080
CMD java -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -Dcom.sun.management.jmxremote -noverify ${JAVA_OPTS} -jar agent-registry.jar