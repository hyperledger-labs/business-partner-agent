#!/bin/bash
# Copyright (c) 2020 - for information on the respective copyright owner
# see the NOTICE file and/or the repository at
# https://github.com/hyperledger-labs/organizational-agent
# 
# SPDX-License-Identifier: Apache-2.0

if [ "$(which gp)" ]; then
    eval $(gp env -e)
    if [ $ACAPY_SEED ] ; then
        echo "There is already an ACAPY_SEED set and hopefully also a DID registered."
        exit 1
    fi
fi

# Check the system the script is running on
ARCHITECTURE="$(uname -s)"
if [[ ${ARCHITECTURE} == "Linux"* ]]; then
    ARCHITECTURE="Linux"
elif [[ ${ARCHITECTURE} == "Darwin"* ]]; then
    ARCHITECTURE="Mac"
fi

if [ "$ARCHITECTURE" != "Linux" ] && [ "$ARCHITECTURE" != "Mac" ]; then
    echo "No Linux or Mac OSX detected. You might need to do some steps manually."
fi

if [ ! -x "$(which curl)" ] ; then
    echo "Couldn't find curl. Please make sure that curl is installed."
    exit 1
fi


# Set URL
URL=${LEDGER_URL:-https://indy-test.bosch-digital.de}

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

    if [ "$(which gp)" ]; then
        echo ""Setting ACAPY_SEED permanently in gitpod environment""
        gp env ACAPY_SEED=$SEED
    else
        echo ""Setting ACAPY_SEED in .env file""
        if [ ! -f .env ]; then
            echo "".env does not exist""
            echo ""Creating .env from .env-example""
            cp .env-example .env
        fi
        # sed on Mac and Linux work differently
        if [ "$ARCHITECTURE" = "Mac" ]; then
            sed -i '' '/ACAPY_SEED=/c\
            ACAPY_SEED='"${SEED}"'
            ' .env
        else
            sed -i '/ACAPY_SEED=/c\
            ACAPY_SEED='"${SEED}"'
            ' .env
        fi 
    fi 

else
    # Something went wrong
    echo ""
    echo Something went wrong
fi;
