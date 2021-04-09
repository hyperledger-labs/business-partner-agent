# local development guide

1. run local von network/ledger
2. run local did resolver
3. run local bpa + agent/wallet
4. run backend in ide with debug (uses agent/wallet)
5. run frontend in ide with debut (uses agent/wallet + backend)

Optional: the local bpa is configured to put in breakpoints and debug with jdb. Java debugger is not great for continual development as the docker image requires rebuild when new methods added.

### Local Von Network

1. open new terminal
2. clone [von-network](https://github.com/bcgov/von-network.git)
3. build the image
4. start the network/ledger

```shell script
git clone https://github.com/bcgov/von-network.git
cd von-network
./manage build
./manage start --logs
```

Go to [http://localhost:9000](http://localhost:9000) and ensure the ledger is up and all 4 nodes are running correctly.

### Local DID Resolver
We need to stand up a Universal DID Resolver so all Business Partner Agents can find each other and create connections. In this case, it will not be "universal", but will find DIDs on our local ledger. 

NOTE: we stand up the resolver separately from the BPA and agent/wallet because it needs to be fully running first.

1. open a new terminal
2. navigate to [resolver](./resolver)
3. get the genesis transactions from our ledger
4. start the resolver

```shell script
cd ./resolver
curl http://localhost:9000/genesis -o localhost_9000.txn
docker-compose up
```

Now you have a "Universal" DID Resolver at [http://localhost:7776](http://localhost:7776). 

#### Deploy Local Dev Stack
1. create an environment file, ensure all values are correct
2. register DIDs on ledger
3. build the Partner BPA image
4. run the Partner BPA + DEV BPA (the agent and wallet db)
5. run/debug the backend for DEV BPA in your IDE
6. run/debug the frontend for DEV BPA in your IDE 

```shell script
cp .env-example .env
LEDGER_URL=http://localhost:9000 ./register-dids.sh
docker-compose build 
docker-compose up
```

The full Partner BPA is at [http://localhost:38080](http://localhost:38080).  

### Run Local Backend/Frontend
This could be very dependent on your IDE and development process. Basically, we want to launch the locally configured Micronaut server, connected to our agent/wallet and serving our local frontend dist.  

You can use the [docker-compose.yml](./docker-compose.yml) as reference, you will need to set JAVA_OPTS and environment variables like you would for Docker. The CLASSPATH is massive and will include all those maven jars.  

Use `host.docker.internal` since you will be using a mix of docker hosted containers (agent and wallet) and your host machine (backend/frontend). 

Example:
```shell script
# set your environment variables...
export BPA_WEB_MODE=false
export BPA_RESOLVER_URL=http://host.docker.internal:7776
export BPA_LEDGER_BROWSER=http://host.docker.internal:9000
export BPA_DID_PREFIX=did:sov:
export BPA_BOOTSTRAP_UN=admin
export BPA_BOOTSTRAP_PW=changeme
export ACAPY_ENDPOINT=http://host.docker.internal:48030
export AGENT_NAME=Dev BPA
# pass in your JAVA_OPTS
java -Dbpa.acapy.url=http://host.docker.internal:48031 -Dmicronaut.security.enabled=false -Dmicronaut.server.port=48080 -Dbpa.pg.url=jdbc:postgresql://host.docker.internal:45432/walletuser -Dbpa.pg.username=walletuser -Dbpa.pg.password=walletpassword -Dbpa.host=host.docker.internal:48080 -Dbpa.scheme=http -Dmicronaut.environments=dev -classpath <YOUR-PROJECT-CLASSPATH> org.hyperledger.bpa.Application
```

Check your IDE for launching and setting breakpoints.  

#### Frontend
Again, check your IDE on how to debug a Vue application. Basically, we will serve up the Vue application and use this frontend, not the one served up by the backend. It is critical to start the backed with `-Dmicronaut.security.enabled=false`.  

1. install required libraries
2. configure Vue application to hit your backend
3. serve the application in development mode
4. set breakpoints and configure your IDE to attach to the Vue application.

```shell script
cd ../frontend
npm install
touch .env.development.local
NODE_ENV=development npm run serve
```

Example: `.env.development.local`:

```text
VUE_APP_API_BASE_URL=http://localhost:48080/api
VUE_APP_EVENTS_PATH=localhost:48080/events
```

`npm run serve` will choose port 8080 if available, otherwise, next open port.  
Go to [http://localhost:8080](http://localhost:8080). Use Business Partners to lookup your Partner BPA. 

##### VS Code

The following is an example for configuring VS Code for debugging our Vue Application.
Make sure the `url` matches wherever the `npm run serve` says the App is running (and all your paths are correct).

*launch.json*

```
      {
        "type": "chrome",
        "request": "launch",
        "name": "vuejs: chrome",
        "url": "http://localhost:8080",
        "webRoot": "${workspaceFolder}/services/bb-pacman/frontend/src",
        "breakOnLoad": true,
        "sourceMapPathOverrides": {
          "webpack:///src/*": "${webRoot}/*"
        }
      }
```

You can now set breakpoints in your Vue code.

### Stopping
You can stop all the components and leave the data intact so you can bring it back up again without having to re-build and re-seed data.

Open a new terminal for each container (`von-network`, `resolver`, `local-development`), and call `docker-compose down`.

### Tearing down
If you no longer want your ledger and agents, or want to start from scratch... 

Open a new terminal for each container (`von-network`, `resolver`, `local-development`), and call `docker-compose down -v --remove-orphans`.  

This will not only stop the containers, but will remove the volumes (where the data is stored). Anytime your remove the agent's data, you will need to register a new DID. Similarly, if you tear down the ledger, you will need to register new DIDs for your agents and reset AND you will need to refresh your DID resolver transactions. 


## Debugging into Partner BPA

When we bring up the `local-development` containers, `partner-bpa` has special parameters to enable remote debugging (default `BPA1_DEBUG_PORT` environment variable is `1044`).
```text
        -Dmicronaut.environments=dev
        -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:${BPA1_DEBUG_PORT}

```

This means that the Partner BPA has a debug server for `jdb`, which can be attached to at `localhost:1044`. This allows us to set breakpoints and allow debugging the running 'production' application.  
Most java debugging tools are built on jdb, see your IDE's configuration for remote debugging to connect to it.

cli command is as follows `jdb -sourcepath ./src/main/java -attach 1044`

VSCode's launch.json config looks like this to work with the 'Debugger for Java' extension.

```
    "configurations": [
        ...
        {
            "type": "java",
            "name": "Attach to Remote Program",
            "request": "attach",
            "hostName": "localhost",
            "port": 1044
        }
    ]
    ...
```



