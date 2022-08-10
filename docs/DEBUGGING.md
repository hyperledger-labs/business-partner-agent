# Demoing/Debugging

Gitpod can be used to launch two agents just for experimenting or even to do development (backend and/or frontend)

Just click on the Gitpod button in our [main readme](../README.md) which opens gitpod via [this link](https://gitpod.io/#https://github.com/hyperledger-labs/business-partner-agent).

The IDE - based on [Visual Studio Code](https://code.visualstudio.com/) - will be launched with the source code already checked-out and the software pre-built.

## Frontend and Backend demo with gitpod

The stack takes a while to start up, once its up and running you can open the remote explorer on the left-hand side (second to last icon):

<img width="408" alt="Screenshot 2022-06-01 at 11 55 28" src="https://user-images.githubusercontent.com/13498217/171378424-8e45ef92-d8aa-47a7-b470-65cdccf62477.png">

Clicking on open browser, for ports 8081 and 8090 gives you two BPA instances that you can connect to each other.

## Frontend and Backend debugging with gitpod

Once everything is up and running, you will find 3 terminals in the bottom right corner:
- One is launching an acapy needed for the *agent1* (including the database) and a full *agent2*
- One is launching the java backend for *agent1*.
- One is launching the frontend for *agent1* (with hot-reload of source code enabled).

As soon as all services are ready, the frontends of *agent1* and *agent2* will appear in browser windows. You can start experimenting with the agents now or modify the frontend code (You might have to hit the reload button in your browser taps).

Remarks:
- Security is disabled, never use these agents for production.
- Open one of the agents in an incognito browser window for proper demos.

To start backend development kill the process in the backend terminal and run (or debug) a new backend via the provided launch configuration (`Run and Debug` view).

## Backend debugging
Starting the BPA with the follow JAVA_OPT will allow you to connect debugging while it's running. 

```
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:1044
```

cli command is as follows `jdb -sourcepath ./src/main/java -attach 1044` or use your IDE's remote java debugger.

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

This connection can also be used for hot changes allowing for most development and running take place in a containerized environment. See your IDE's extension on how to do this. 

### Frontend debugging
If the backend/bpa is started with `BPA_SECURITY_ENABLED=false`, we can stand up another frontend and use that for debugging.


1. navigate to /frontend
2. install required packages
3. create a local development environment configuration
4. serve the app
5. run debugger in VSCode

```
cd ../frontend
npm i
touch .env.development.local
NODE_ENV=development npm run serve
```

*.env.development.local*
Set the server and ports to connect with your runnin BPA 1.

```
VUE_APP_API_BASE_URL=http://localhost:8000/api
VUE_APP_EVENTS_PATH=localhost:8000/events
```

*launch.json*
Make sure the `url` matches wherever the `npm run serve` says the App is running.

```
      {
        "type": "chrome",
        "request": "launch",
        "name": "vuejs: chrome",
        "url": "http://localhost:8081",
        "webRoot": "${workspaceFolder}/frontend/src",
        "breakOnLoad": true,
        "sourceMapPathOverrides": {
          "webpack:///src/*": "${webRoot}/*"
        }
      }
```

You can now set breakpoints in your Vue code.
