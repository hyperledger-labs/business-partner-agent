**For Reference only, this is handled by [start_infra.sh](../start_infra.sh)**

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

1. open a new terminal
2. get the genesis transactions from our ledger
3. start the resolver

```shell script
curl http://localhost:9000/genesis -o localhost_9000.txn
docker-compose up
```

Now you have a "Universal" DID Resolver at [http://localhost:7776](http://localhost:7776).

Set your bbp-network environments as follows (assumes all default used):

```
BPA_RESOLVER_URL=http://host.docker.internal:7776
BPA_LEDGER_BROWSER=http://host.docker.internal:7776
ACAPY_GENESIS_URL=http://host.docker.internal:9000/genesis
```

*NOTE* The same procedure for initializing and starting a DID Resolver will work for any (DID:SOV:) ledger, simply get the genesis transactions and initialize the resolver.
