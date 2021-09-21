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

## Wiping Clean

  If you want to nuke your storage and start from scratch. You simply need to delete to the database volumes and generate and register new acapy-seeds, don't worry about the ledger, everything is uniquely identified, so old stuff will be there, but won't interact with your refreshed wallets and seeds. 

  ```
  docker-compose down 
  docker volume rm local-network_bpa-wallet-db1 local-network_bpa-wallet-db2
  export LEDGER_URL=http://localhost:9000
  ./register-dids.sh
  ...
  ``` 
  followed by whatever startup commmand you are using (e.g. `docker-compose up`), this assumes you didn't tear down the resolver or von-network, if you did, just run `./start_infra.sh` first.
  

## Building
  None of the commands above will check if the source code has changed and re-build the ap. To rebuild the app with new source code, run `docker-compose build`, 

  This may take up to 20 minutes, if you have a small change you want quickly to test, consider applying a hot fix to your existing image by connecting a [debugger](../../docs/DEBUGGING.md)
