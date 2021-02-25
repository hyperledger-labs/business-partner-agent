# Running a local example with Two Business Partners

## Build and run the universal DID resolver

First clone and build the sovrin resolver (for bcovrin test):

```bash
git clone https://github.com/ianco/uni-resolver-driver-did-sov.git
cd uni-resolver-driver-did-sov
docker build -f ./docker/Dockerfile . -t universalresolver/driver-did-sov
```

Now run the universal resolver (the only profile is for our bcovrin resolver)

```bash
git clone https://github.com/ianco/universal-resolver.git
cd universal-resolver
docker pull universalresolver/uni-resolver-web:latest
docker-compose -f docker-compose.yml up
```

Once the uni resolver is running you should be able to curl a DID on the ledger, for example:

```bash
curl -X GET http://localhost:8080/1.0/identifiers/did:sov:MTMagrM95WXayAHfwTNY17
```

## Run the first BP Agent

```bash
git clone https://github.com/ianco/business-partner-agent.git
cd business-partner-agent
docker build -t ghcr.io/hyperledger-labs/business-partner-agent:local .
cd scripts
cp .env-example1 .env
./register-did.sh
docker-compose -f docker-compose-1.yml up
```

Once the app is running open a browser to http://localhost:8000

Login as admin/changeme

## Run the second BP Agent

In a different shell/directory:

```bash
git clone https://github.com/ianco/business-partner-agent.git
cd business-partner-agent
cd scripts
cp .env-example2 .env
docker-compose -f docker-compose-2.yml up
```

Once the app is running open a browser to http://localhost:8010 (suggest to use a different browser, e.g. Safari or FireFox vs Chrome since it maintains a logged-in session)

Login as admin/changeme

## Connecting the BP agents

In one of the BP agent windows, copy their DID (e.g. `did:sov:6fM1wPUPd9E6jpxuVfSGGD`).

In the other BP agent window, select "Business Partners" and click on the purple "+" to add a new BP.  Paste their DID and click on "Lookup Partner".

