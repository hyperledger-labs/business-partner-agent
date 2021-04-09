# Vue Build Container
FROM node:lts-alpine as VUE
WORKDIR /frontend
COPY frontend .
RUN npm install && npm run license-check && npm run license && npm run build

# Micronaut build
FROM maven:3-amazoncorretto-15 as MAVEN

WORKDIR /home/maven/backend

# Copy Files
COPY backend/pom.xml backend/formatter.xml ./
COPY backend/business-partner-agent ./business-partner-agent
COPY backend/business-partner-agent-core ./business-partner-agent-core
# Copy Vue App
COPY --from=VUE ["/frontend/licenses/[^attribution]*", "./business-partner-agent/src/main/resources/3rdPartyLicenses"]
COPY --from=VUE /frontend/dist ./business-partner-agent/src/main/resources/public

# Cache Maven Artefacts
RUN mvn dependency:go-offline || true

# Build .jar
RUN mvn clean install -DskipTests=true -Dspotbugs.skip=true -Dpmd.skip=true

# Runtime Container
FROM amazoncorretto:15-alpine
COPY --from=MAVEN /home/maven/backend/business-partner-agent/target/business-partner-agent*SNAPSHOT.jar business-partner-agent.jar
COPY --from=MAVEN /home/maven/backend/business-partner-agent/src/main/resources/3rdPartyLicenses ./3rdPartyLicenses

EXPOSE 8080
CMD java -XX:+UnlockExperimentalVMOptions -Dcom.sun.management.jmxremote -noverify ${JAVA_OPTS} -jar business-partner-agent.jar
