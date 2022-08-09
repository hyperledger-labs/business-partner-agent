#!/bin/bash
# Copyright (c) 2022 - for information on the respective copyright owner
# see the NOTICE file and/or the repository at
# https://github.com/hyperledger-labs/organizational-agent
#
# SPDX-License-Identifier: Apache-2.0

export VON_FOLDER=von-network
export SCRIPT_PATH=${0%\/*}/$VON_FOLDER/manage

# -----------------------------------------------------------------------------------------------------------------
# Functions:
# -----------------------------------------------------------------------------------------------------------------
function toLower() {
  echo $(echo ${@} | tr '[:upper:]' '[:lower:]')
}

# =================================================================================================================
# Usage:
# -----------------------------------------------------------------------------------------------------------------
usage () {
  cat <<-EOF

  Usage: $0 [command]

  Commands:
  start  - Starts the local infrastructure.
  
  stop   - Stops the local infrastructure but does not delete the volumes and containers.

  prune  - Stops the local infrastructure and deletes the volumes and containers.

  status - Prints the status of the local infrastructure.
EOF
exit 1
}

# =================================================================================================================
# Start:
# -----------------------------------------------------------------------------------------------------------------
start () {
  if [ ! -d resolver ]; 
    then
      mkdir resolver
  fi

  # DL, BUILD, RUN, LOCAL HYPERLEDGER
  
  if [ ! -d $VON_FOLDER ]; 
    then
      git clone https://github.com/bcgov/von-network.git $VON_FOLDER
  fi

  # Set node IP to local host internal IP - required when executed from Windows Subsystem Linux (WSL)
  if grep -q "microsoft" /proc/version; then
      echo 'Starting in WSL mode...'
      IP=$(hostname -I | xargs)
      echo 'Node IP:' $IP
  fi

  $SCRIPT_PATH build
  $SCRIPT_PATH start $IP

  # GET LOCAL LEDGER GENESIS FILES AND START RESOLVER
  curl --retry-connrefused --retry 5 --retry-delay 2 http://localhost:9000/genesis -o resolver/localhost_9000.txn

  while [ $? -ne 0 ];
  do
    echo '...waiting for infra to finish startup'
    sleep 3
    curl --retry-connrefused --retry 5 --retry-delay 2 http://localhost:9000/genesis -o resolver/localhost_9000.txn
  done
}

# =================================================================================================================
# Stop:
# -----------------------------------------------------------------------------------------------------------------
stop () {
  $SCRIPT_PATH stop
}

# =================================================================================================================
# Prune:
# -----------------------------------------------------------------------------------------------------------------
prune () {
  $SCRIPT_PATH down
}

# =================================================================================================================
# Status:
# -----------------------------------------------------------------------------------------------------------------
status () {
  $SCRIPT_PATH dockerhost
  docker ps -a --filter 'name=von-' --format "{{.Names}}	-> {{.State}}"
}

# =================================================================================================================
COMMAND=$(toLower ${1})
shift || COMMAND=usage

case "${COMMAND}" in
  start|up)
      start
    ;;
  stop)
      stop
    ;;
  prune)
      prune
    ;;
  status)
      status
    ;;
  *)
      usage
    ;;
esac

