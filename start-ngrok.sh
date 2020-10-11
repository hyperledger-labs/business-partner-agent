#!/usr/bin/env bash

# THIS IS FOR TESTING ONLY!

# Script starts ngrok https tunnels defined in ngrok.yml
# and sets the public endpoint as ACA-Py endpoint.
# Needs ngrok locally installed and an account with ngrok

# Check if web mode is enabled in .env
eval $(grep -i  'BPA_WEB_MODE' .env)

if [[ "$BPA_WEB_MODE" = "" ]]; then
    BPA_WEB_MODE=false
fi

echo "Web Mode: $BPA_WEB_MODE"


# Run ngrok
if "$BPA_WEB_MODE"; then
    NGROK_TUNNELS="businesspartner"
    # Remove agent seed such that no public DID gets generated
    export AGENT_SEED=""
else
    NGROK_TUNNELS="acapyendpoint businesspartner"
fi

# Get public ip
function getTunnels () {

    TUNNELS=$(curl --silent http://127.0.0.1:4040/api/tunnels  | jq -c .tunnels[])
    if [[ "$TUNNELS" == "" ]]; then
        sleep 2
        getTunnels
    fi

}

echo "Starting ngrok..."

if ngrok start --config ngrok.yml $NGROK_TUNNELS >/dev/null 2>&1 & then
    echo "ngrok started successfully"
    getTunnels
    for TUNNEL in $TUNNELS
        do
            TUNNEL_NAME=$( echo $TUNNEL | jq -r .name)
            if [[ "$TUNNEL_NAME" == "acapyendpoint" ]]; then
                ACA_PY_ENDPOINT=$( echo $TUNNEL | jq -r .public_url )
            fi
            if [[ "$TUNNEL_NAME" == "businesspartner" ]]; then
                BPA_HOST=$( echo $TUNNEL | jq -r .public_url | sed 's/.*https:\/\///')
            fi    
        done
else
    echo "Could not start ngrok."
fi


# Generate random API Key
ACAPY_API_KEY=$(cat /dev/urandom | env LC_CTYPE=C tr -dc 'a-zA-Z0-9' | fold -w 32 | head -n 1)
export ACAPY_ADMIN_URL_API_KEY=$ACAPY_API_KEY
echo "ACA PY Admin Key: $ACAPY_API_KEY"


# write public ip to env
export BPA_HOST=$BPA_HOST
export AGENT_ENDPOINT=$ACA_PY_ENDPOINT

echo "Business Partner Agent Public URL: $BPA_HOST"
echo "Public ACA-PY Endpoint: $AGENT_ENDPOINT"

# Start agent
docker-compose up