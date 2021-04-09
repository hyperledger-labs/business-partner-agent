# Debugging


## Backend
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

### Frontend
If the backend/bpa is started with `BPA_SECURITY_ENABLED=false`, we can stand up another frontend and use that for debugging.


1. navigate to services/bb-pacman/frontend
2. install required packages
3. create a local development environment configuration
4. serve the app
5. run debugger in VSCode

```
cd ../../services/bb-pacman/frontend
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
        "webRoot": "${workspaceFolder}/services/bb-pacman/frontend/src",
        "breakOnLoad": true,
        "sourceMapPathOverrides": {
          "webpack:///src/*": "${webRoot}/*"
        }
      }
```

You can now set breakpoints in your Vue code.
