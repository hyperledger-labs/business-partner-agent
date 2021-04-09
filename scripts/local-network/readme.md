# Local-network


## First Run!
Windows/Mac
```
cp ./.env-example ./.env
./start_infra.sh
export LEDGER_URL=http://localhost:9000
./register-dids.sh
docker-compose up 
```

Linux machines need to define host.docker.internal for each service, so use `docker-compose -f docker-compose-linux.yml up ` which as those extra hosts defined. 

add `['bpa1','bpa2','bpa-agent1','bpa-agent2','bpa-wallet-db1','bpa-wallet-db2']` to `docker-compose up` to  select which logs you want to see, or add `-d` flag to run in the background. 


This folder enables easy setup of an entire isolated network for local development. Including

 - [von-network](https://github.com/bcgov/von-network)
 - [universal-resolver](https://github.com/decentralized-identity/universal-resolver)
    - Configured to resolve did's for the local von-network

## Restarting 
  Assuming we want to keep the ledger, wallets, and seeds, run `docker-compose down` and `docker-compose up` will allow you to continue. Use `docker-compose build` to rebuild the bpa images if you are testing code modifications. 

  Some code simple modifications can be hotloaded into the application while it's running, see [Debugging](../DEBUGGING.md). 