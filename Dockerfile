# Build Container
FROM maven:3-amazoncorretto-15

WORKDIR /home/maven/backend

# Copy Files
COPY backend/pom.xml backend/formatter.xml ./
COPY backend/business-partner-agent ./business-partner-agent
COPY backend/business-partner-agent-core ./business-partner-agent-core
COPY frontend ../frontend

# Cache Maven Artefacts
RUN mvn dependency:go-offline || true

# Build .jar
RUN mvn clean install -P build-frontend -DskipTests=true -Dspotbugs.skip=true -Dpmd.skip=true

# Runtime Container
FROM amazoncorretto:15-alpine
COPY --from=0 /home/maven/backend/business-partner-agent/target/business-partner-agent*SNAPSHOT.jar business-partner-agent.jar
COPY --from=0 /home/maven/backend/business-partner-agent/src/main/resources/3rdPartyLicenses ./3rdPartyLicenses

EXPOSE 8080
CMD java -XX:+UnlockExperimentalVMOptions -Dcom.sun.management.jmxremote -noverify ${JAVA_OPTS} -jar business-partner-agent.jar