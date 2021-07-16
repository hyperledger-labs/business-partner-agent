

## Start one BPA with one aca-py with the default ledger.

At the folder backend execute the following.

`
scripts/.env-example -> scripts/.env &&
docker-compose -f scripts/docker-compose.yml up -d bpa-agent1 bpa-wallet-db1
`

##FAQ
I get a "Micronaut - Error starting Micronaut server: Switching from web only mode to aries is not supported" error?
The schema for web and aries mode differ and the database has to be reset.
`
docker-compose -f scripts/docker-compose.yml down &&
docker volume rm scripts_bpa-wallet-db1 &&
docker-compose -f scripts/docker-compose.yml up -d bpa-agent1 bpa-wallet-db1
`
