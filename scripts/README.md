# Docker-compose setup

## Prerequistes

The following tools should be installed on your developer machine:
- docker
- docker-compose
- (optional) [ngrok](https://ngrok.com/) or [diode](https://support.diode.io/) to have a public endpoint to communicate with other Business Partner Agents

As well, make sure you are not sitting behind a restrictive company firewall.
If so, at least the setup has to be adopted (e.g. configure proxy configuration in the maven settings in the [Dockerfile](../Dockerfile)).
Furthermore, the firewall might block traffic to other agents depending on its endpoint configuration (if e.g. in the firewall other ports than 443 are blocked).

## TL;DR

Spinning up a single instance that is not exposed to the internet.

```s
git clone https://github.com/hyperledger-labs/business-partner-agent
cd scripts
./register-dids.sh
docker compose up
```

## Spinning up a single BPA

- If you have a setup using ngrok for making the agent publicly available, running
```s
./start-with-tunnels.sh
```
will set up the tunnel and start everything for you. Before making your agent publicly available, 
you most likely want to change the security options, at least set passwords, in the `.env` file. 
See the security section below for details.

- Alternatively, for a local test, just run
```s
# If not done already, run
# ./register-dids.sh
docker-compose up
```

### Accessing the frontend

- The frontend will be served at `http://localhost:8080`. If you did not change the password in `.env` the default login is "admin"/"changeme".
- The backends swagger will be served at: `http://localhost:8080/swagger-ui`
- aca-py's swagger api will be served at: `http://localhost:8031/api/doc`

## Spinning up two local instances of the BPA

If you have run the `register-dids.sh` script you should have a .env file. In the file make sure the `BPA_SCHEME` is set to `BPA_SCHEME=http`.
Otherwise, the agents won't be able to connect. Https is needed if you are running with ngrok or diode
or running behind a proxy that terminates TLS.

Afterwards it is the same as above, but now we use a profile to enable a second instance

```s
# If not done already, run
# ./register-dids.sh
docker-compose --profile second_bpa up
```

### Accessing the second frontend
- The second frontend will be served at `http://localhost:8090`. If you did not change the password in `.env` the default login is "admin"/"changeme".
- The second backends swagger will be served at: `http://localhost:8090/swagger-ui`
- The second aca-py's swagger api will be served at: `http://localhost:8041/api/doc`

### Stopping the instance

```s
docker-compose down
```

If you want to wipe the databases as well you can use

```s
docker-compose down -v
```

## Register a new DID before starting a Business Partner Agent

You can use the `./register-dids.sh` script to register a two new DIDs on our test network
Just run:

```s
./register-dids.sh
```

You should see some output like this:
```s
Registering DID for ACAPY_SEED
{
  "did": "Tc8VTYTryxJGW3sz9RucDd",
  "seed": "12345678912345678912345678912300",
  "verkey": "FW4MZZhmcSsFnDZLCT5689EoUvzuEXqNRNYem1X6PZFYt"
}
Registration on http://test.bcovrin.vonx.io successful
Setting ACAPY_SEED in .env file
.env does not exist
Creating .env from .env-example
Registering DID for ACAPY_SEED2
...
```

Alternatively, you can register a DID manually:

1. Go to http://test.bcovrin.vonx.io/
2. Provide a 32 character wallet seed on the right side under "Authenticate a new DID" and click on "Register DID"
3. Make a copy of the provided [.env-example file](.env-example) with the name `.env`. Set the `AGENT1_SEED` to the wallet seed. Repeat this process for the second DID if needed, and set the `ACAPY_SEED2` to the second wallet seed.

## Get a public IP
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

## Setup Security

In your `.env` under `Security config` file set
```s
BPA_SECURITY_ENABLED=true
```
and a username and password.

Ideally also configure a secure connection between the backend services (core and aca-py).
This can be achieved by setting an API key in `.env` file via `ACAPY_ADMIN_CONFIG` (see example).

## Customizing the frontend

There are some limited options to customize the UI without recompiling the code
e.g. exchanging the image or setting some styles. For options see the ux properties in:
[application.yml](../backend/business-partner-agent/src/main/resources/application.yml)

E.g. to exchange the logo you can set:

```s
-Dbpa.ux.navigation.avatar.agent.enabled="true"
-Dbpa.ux.navigation.avatar.agent.default="false"
-Dbpa.ux.navigation.avatar.agent.src=data:image/png;base64,<...>
```
In the JAVA_OPTS section of the bpa-agent1 or bpa-agent2 in the docker compose file.