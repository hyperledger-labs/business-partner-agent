#!/usr/bin/env bash

# THIS IS FOR TESTING ONLY!

# Script starts ngrok https tunnels defined in ngrok.yml 
# Or alternatively diode (when started with --diode)
# and sets the public endpoint as ACA-Py endpoint.
# If ngrok is used, ngrok should be locally installed and an account with ngrok should be setup.

# Allow to set custom docker compose file with option -f
DOCKERFILE="docker-compose.yml"
while getopts "f:" opt; do
	echo ${opt}
	case ${opt} in
		f ) DOCKERFILE=$OPTARG 
		;; 
		\? )
		#print option error
		echo "Invalid option: $OPTARG" 1>&2
		exit 1
		;;
		: )
		
	esac
done

# Check if web mode is enabled in .env
eval $(grep -i  'BPA_WEB_MODE' .env)

if [[ "$BPA_WEB_MODE" = "" ]]; then
    BPA_WEB_MODE=false
fi

echo "Web Mode: $BPA_WEB_MODE"

# Generate random API Key
ACAPY_API_KEY=$(cat /dev/urandom | env LC_CTYPE=C tr -dc 'a-zA-Z0-9' | fold -w 32 | head -n 1)
export ACAPY_ADMIN_URL_API_KEY=$ACAPY_API_KEY
echo "ACA PY Admin Key: $ACAPY_API_KEY"

if [ "$1" == "--diode" ]; then
    echo "Use diode"
    
    if ! command -v diode &> /dev/null
	then
	    echo "diode could not be found, start "
	    curl -Ssf https://diode.io/install.sh | sh
	fi

	# currently only one port is possible (acapy is more important). will be fixed soon.
	# see issue https://github.com/diodechain/diode_go_client/issues/60
	diode publish -public 8030:80 >/dev/null 2>&1 &
    ACA_PY_ENDPOINT=https://$(diode config 2>&1 | awk '/Client address/ { print $5 }').diode.link
    
    # write public ip to env
	export AGENT_ENDPOINT=$ACA_PY_ENDPOINT
	
	echo "Public ACA-PY Endpoint: $AGENT_ENDPOINT"

else
    echo "Use ngrok"
    # Run ngrok
	if "$BPA_WEB_MODE"; then
	    NGROK_TUNNELS="businesspartner"
	    # Remove agent seed such that no public DID gets generated
	    export AGENT_SEED=""
	else
	    NGROK_TUNNELS="acapyendpoint businesspartner"
	fi
	
	echo "Starting ngrok..."	
	if ! command -v ngrok &> /dev/null
	then
	    echo "ngrok could not be found"
	    exit
	fi
	
	# Get public ip
	function getTunnels () {
	    TUNNELS=$(curl --silent http://127.0.0.1:4040/api/tunnels  | jq -c .tunnels[])
	    if [[ "$TUNNELS" == "" ]]; then
	        sleep 2
	        getTunnels
	    fi	    
	}
	
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
	
	# write public ip to env
	export BPA_HOST=$BPA_HOST
	export AGENT_ENDPOINT=$ACA_PY_ENDPOINT
	
	echo "Business Partner Agent Public URL: $BPA_HOST"
	echo "Public ACA-PY Endpoint: $AGENT_ENDPOINT"
fi

# Start agent
docker-compose -f $DOCKERFILE up