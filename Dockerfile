# Build Container
FROM maven:3-amazoncorretto-15

WORKDIR /home/maven/backend

# Copy Files
COPY backend/pom.xml backend/formatter.xml ./
COPY backend/company-agent ./company-agent
COPY backend/company-agent-core ./company-agent-core
COPY frontend ../frontend

# Cache Maven Artefacts
RUN mvn dependency:go-offline || true

# Generate License Info
RUN mvn -f company-agent-core/pom.xml clean install -Dspotbugs.skip=true -Dpmd.skip=true
RUN mvn attribution:generate-attribution-file
RUN cp ./company-agent/target/attribution.xml ../frontend/licenses/attribution.xml

# Build .jar
RUN mvn clean install -P build-frontend -DskipTests=true -Dspotbugs.skip=true -Dpmd.skip=true

# Runtime Container
FROM amazoncorretto:15-alpine
COPY --from=0 /home/maven/backend/company-agent/target/organizational-agent*SNAPSHOT.jar organizational-agent.jar

EXPOSE 8080
CMD java -XX:+UnlockExperimentalVMOptions -Dcom.sun.management.jmxremote -noverify ${JAVA_OPTS} -jar organizational-agent.jar