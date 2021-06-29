# Setup Dependencies

## Java

Install JDK >= 16 in any openjdk compliant version e.g.

https://adoptopenjdk.net/?variant=openjdk16  
https://docs.aws.amazon.com/corretto/latest/corretto-16-ug/downloads-list.html

## Maven

Maven >= 3.6

https://maven.apache.org/download.cgi

## Docker

To run the backends dependencies, and the unit tests you need to have docker and docker-compose setup on your development machine.

# Setup IDE

## Install lombok.jar

Eclipse: https://projectlombok.org/setup/eclipse  
IntelliJ: https://projectlombok.org/setup/intellij

## Enable micronaut annotation processor

https://docs.micronaut.io/latest/guide/index.html#ideSetup

# Run BPA from within your IDE

1. CLI: Build the UI

```s
mvn clean package -Pbuild-frontend
```

2. Set up the .env file for docker-compose

Depending on the docker version the .env file either needs to reside in the root directory (older versions) or in the script's directory (newer versions)

[See .env file set up](https://github.com/hyperledger-labs/business-partner-agent/blob/master/scripts/README.md) 

3. Start dependent services
```s
# e.g. run from the root directory
docker-compose -f scripts/docker-compose.yml up bpa-agent1 bpa-wallet-db1
```

4. Set VM Options

Eclipse: Right click Application.java Run As/Run Configurations.  
IntelliJ: Run/Edit Configurations

This assumes you kept the default usernames and passwords from the .env example.

```
-Dmicronaut.security.enabled=false
-Dbpa.host=<BPA_HOST>
-Dbpa.acapy.endpoint=<ACAPY_ENDPOINT>
-Dmicronaut.config.files=classpath:application.yml,classpath:schemas.yml
```

Depending on your set up, the values for `BPA_HOST` and `ACAPY_ENDPOINT` are either set in the .env file, or the output of the start-with-tunnels.sh script.

If you want to run in web only mode you also have to set:

```
-Dbpa.web.only=true
```

5. Access the UI

Swagger UI: http://localhost:8080/swagger-ui   
Frontend: http://localhost:8080

# Websocket Events

see: WebSocketMessageBody.java

1. Connection Request (no auto flags)
2. Partner Received (with auto flags)
3. Credential Received
4. Proof Received