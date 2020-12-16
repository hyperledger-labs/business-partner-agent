# Company Agent

## Build

### Build backend only

```s
# Build jar
mvn clean package
```

Like this only the swagger-ui is available.

### Build with frontend

```s
# Build jar
mvn clean package -Pbuild-frontend
```

## Run

## Setup Profiles

In you IDE e.g. Eclipse righ click Application.java Run As/Run Configurations and add the following VM argument:

```
-Dmicronaut.environments=dev
-Dmicronaut.server.port=8082
```

If you want to run in web only mode you also have to set:

```
-Dbpa.web.only=true
```

### Aries Mode

From the project root follow the general setup steps and then run:

```s
docker-compose -f docker-compose.yml up
```

To be able to also test aries connections and webhooks you have to run with ngrok (or diode):

```s
./start-with-tunnels.sh
```

### Web Mode

```s
docker-compose -f docker-compose-backend-webonly.yml up
```

### Run from IDE

Right click Application.java and Run

## UI

Swagger UI: http://localhost:8082/swagger-ui
Frontend: http://localhost:8080
