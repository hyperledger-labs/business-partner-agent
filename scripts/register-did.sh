# Copyright (c) 2020 - for information on the respective copyright owner
# see the NOTICE file and/or the repository at
# https://github.com/hyperledger-labs/organizational-agent
# 
# SPDX-License-Identifier: Apache-2.0

#!/bin/bash

# Set URL
URL=https://indy-test.bosch-digital.de

# Set random alias
ALIAS=BPA-$(cat /dev/urandom | env LC_CTYPE=C tr -dc 'a-zA-Z0-9' | fold -w 4 | head -n 1)
# Generate random seed
SEED=$(cat /dev/urandom | env LC_CTYPE=C tr -dc 'a-zA-Z0-9' | fold -w 32 | head -n 1)

PAYLOAD='{"alias":"'"$ALIAS"'","seed":"'"$SEED"'","role":"ENDORSER"}'

# Register DID
if curl --fail -s -d $PAYLOAD  -H "Content-Type: application/json" -X POST ${URL}/register; then
    # Registration (probably) successfull
    echo ""
    echo ""Registration on $URL successful""
    echo ""Setting AGENT_SEED in .env file""
    if [ ! -f .env ]; then
        echo "".env does not exist""
        echo ""Creating .env from .env-example""
        cp .env-example .env
    fi
    #sed -i "" 's/AGENT_SEED .*$/AGENT_SEED="$SEED"/' .env
    sed -i '' '/AGENT_SEED=/c\
    AGENT_SEED='"${SEED}"'
    ' .env
else
    # Something went wrong
    echo ""
    echo Something went wrong
fi;