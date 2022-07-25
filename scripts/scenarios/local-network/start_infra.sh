#!/usr/bin/env bash

mkdir resolver

# DL, BUILD, RUN, LOCAL HYPERLEDGER
git clone https://github.com/bcgov/von-network.git von-network

# Set node IP to local host internal IP - required when executed from Windows Subsystem Linux (WSL)
if grep -q "microsoft" /proc/version; then
    echo 'Starting in WSL mode...'
    IP=$(hostname -I | xargs)
    echo 'Node IP:' $IP
fi
./von-network/manage build
./von-network/manage start $IP

# GET LOCAL LEDGER GENESIS FILES AND START RESOLVER
curl --retry-connrefused --retry 5 --retry-delay 2 http://$IP:9000/genesis -o resolver/localhost_9000.txn

while [ $? -ne 0 ];
do
    echo '...waiting for von-network to finish startup'
    sleep 3
    curl --retry-connrefused --retry 5 --retry-delay 2 http://$IP:9000/genesis -o resolver/localhost_9000.txn
done
# Currently not needed
# docker-compose -f ./resolver/docker-compose.yml up -d driver-did-sov