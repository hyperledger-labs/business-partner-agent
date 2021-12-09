# Dockerfile that builds frontend and backend, mainly used by the docker-compose files

# Vue Build Container
FROM node:14-alpine as VUE
WORKDIR /frontend
COPY frontend .
RUN npm install && npm run license && npm run build

# Micronaut build
FROM maven:3-amazoncorretto-17 as MAVEN

WORKDIR /home/maven

# Copy Files
COPY backend/pom.xml backend/formatter.xml ./
COPY backend/business-partner-agent ./business-partner-agent
COPY backend/business-partner-agent-core ./business-partner-agent-core
# Copy Vue App
COPY --from=VUE /frontend/dist ./business-partner-agent/src/main/resources/public

# Cache Maven Artefacts
RUN mvn dependency:go-offline || true

# Build .jar
RUN mvn clean package -DskipTests=true -Dspotbugs.skip=true -Dpmd.skip=true

# Runtime Container
FROM amazoncorretto:17-alpine
COPY --from=MAVEN /home/maven/business-partner-agent/target/business-partner-agent*SNAPSHOT.jar business-partner-agent.jar

# Overwrite frontend runtime variables
RUN mkdir public
COPY --from=VUE /frontend/dist/env.js /public/env.js
COPY --from=VUE /frontend/setup-runtime.sh setup-runtime.sh
RUN ["chmod", "a+x", "./setup-runtime.sh"]

EXPOSE 8080
ENTRYPOINT ./setup-runtime.sh public/env.js business-partner-agent.jar && java -XX:+UnlockExperimentalVMOptions -Dcom.sun.management.jmxremote ${JAVA_OPTS} -jar business-partner-agent.jar
