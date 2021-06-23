# Docker-compose setup

## Prerequistes

The following tools should be installed on your developer machine:
- docker
- docker-compose
- (optional) [ngrok](https://ngrok.com/) or [diode](https://support.diode.io/) to have a public endpoint to communicate with other Business Partner Agents

As well, make sure you are not sitting behind a restrictive company firewall.
If so, at least the setup has to be adopted (e.g. configure proxy configuration in the maven settings in the [Dockerfile](../Dockerfile)).
Furthermore the firewall might block traffic to other agents depending on its endpoint configuration (if e.g. in the firewall other ports than 443 are blocked).

## Quickstart

```s
git clone https://github.com/hyperledger-labs/business-partner-agent
cd ./business-partner-agent/scripts
./register-did.sh
```

- If you have a setup using ngrok for making the agent publically avaiable, running
```
./start-with-tunnels.sh
```
will setup the tunnel and start everything for you. Before making your agent publically avaiable, you most likely want to change the security options, at least set passwords, in the `.env` file. See the security section below for details.

- Alternatively, for a local test, just run
```
docker-compose up
```

The frontend will be served at `http://localhost:8080`. If you did not change the password in `.env` the default login is "admin"/"changeme".


## Register a new DID before starting an Business Partner Agent

You can use the `./register-dids.sh` script to register a new DID on our test network.
Just run:

```s
./register-dids.sh
```

You should see the following output:
```s
{
  "did": "W3KMxGKUmajhiJzGmqVcAM",
  "seed": "rxg9SAfvJsdQZjcsguSQCJofuPMmK4Ke",
  "verkey": "Gq1ZuUcU4mwkdinNjwovDMgYhNq2Z6gVdbf9WgFFbQEb"
}
Registration on https://indy-test.bosch-digital.de successful
Setting AGENT1_SEED in .env file
```

Alternatively, you can register a DID manually:

1. Go to https://indy-test.bosch-digital.de/
2. Provide a 32 characterer wallet seed on the right side under "Authenticate a new DID" and click on "Register DID"
3. Make a copy of the provided [.env-example file](.env-example) with the name `.env`. Set the `AGENT_SEED` to the wallet seed.

## Start a Business Partner Agent instance

You can start an instance of the Business Partner Agent with docker compose. It will start the following
- Frontend (Vue.js)
- Controller Backend (Java Micronaut)
- Aries Cloud Agent Python
- Postgres

with a default configuration.

### Build and run
```s
docker-compose up
```

### Rebuild
```s
docker-compose build
```

Access the frontend:

http://localhost:8080

Access the swagger-ui:

http://localhost:8080/swagger-ui

### Stopping the instance
```s
docker-compose down
```

If you want to wipe the database as well you can use

```s
docker-compose down -v
```

## Getting a public IP
If you did not deploy your agent on a server with a public ip it won't have public endpoints to communicate with other agents.
A simple way to get public endpoints for your agent is to setup [ngrok](https://ngrok.com/).

If you have set up ngrok you can use the `start-with-tunnels.sh` script to start your agent with public endpoints. Note that this scripts expects the `ngrok` command to be available in the global path, and additionally requires the `jq` command (which may need to be installed first on your machine).
```s
./start-with-tunnels.sh
```
To terminate all ngrok tunnels you can use
```s
./kill-tunnels.sh
```

***BE AWARE:*** If you don't have any security enabled the Business Partner API and the frontend will be publicly available. This is in particular important when running in Aries mode where the public IP is written to the ledger.

### Setup Security

In your `.env` under `Security config` file set
```s
BPA_SECURITY_ENABLED=true
```
and a user name and password.

Ideally also configure a secure connection between the backend services (core and aca-py).
This can be achieved by setting an API key in `.env` file via `ACAPY_ADMIN_CONFIG` (see example).
