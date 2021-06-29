#!/usr/bin/env bash

# THIS IS FOR TESTING ONLY!

# Script starts ngrok https tunnels defined in ngrok.yml 
# Or alternatively diode (when started with --diode)
# and sets the public endpoint as ACA-Py endpoint.
# If ngrok is used, ngrok should be locally installed and an account with ngrok should be setup.

# Options 
# - set custom docker compose file with option -f
# - diode mode with -d
DOCKERFILE="docker-compose.yml"
MODE="ngrok"
while getopts "df:" opt; do
	echo ${opt}
	case ${opt} in
                d ) MODE="diode"
		;;
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

if [ "$MODE" = "diode" ]; then
    echo "Use diode"
    
    if ! command -v diode &> /dev/null
	then
	    echo "diode could not be found, start "
	    curl -Ssf https://diode.io/install.sh | sh
	fi

	diode publish -public 8030:8030 -public 8080:80 >/dev/null 2>&1 &
    	ACAPY_ENDPOINT=https://$(diode config 2>&1 | awk '/Client address/ { print $5 }').diode.link:8030
	BPA_HOST=$(diode config 2>&1 | awk '/Client address/ { print $5 }').diode.link
else
    echo "Use ngrok"
    NGROK_TUNNELS="acapyendpoint businesspartner"

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
	                ACAPY_ENDPOINT=$( echo $TUNNEL | jq -r .public_url )
	            fi
	            if [[ "$TUNNEL_NAME" == "businesspartner" ]]; then
	                BPA_HOST=$( echo $TUNNEL | jq -r .public_url | sed 's/.*https:\/\///')
	            fi    
	        done
	else
	    echo "Could not start ngrok."
	fi
fi

# write public ip to env
export BPA_HOST=$BPA_HOST
export ACAPY_ENDPOINT=$ACAPY_ENDPOINT

echo "Business Partner Agent Public URL: https://$BPA_HOST"
echo "Public ACA-PY Endpoint: $ACAPY_ENDPOINT"

# Start agent
docker-compose -f $DOCKERFILE up
