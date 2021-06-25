#!/usr/bin/env bash

eval $(gp env -e)
export VUE_APP_API_BASE_URL=`gp url 8080`/api
export VUE_APP_EVENTS_PATH=`gp url 8080`/events
export ACAPY_ENDPOINT=`gp url 8030`
export ACAPY2_ENDPOINT=`gp url 8040`
export BPA_HOST=`gp url 8080 | sed 's/.*https:\/\///'`
export PA_WEBHOOK_URL=`gp url 8080`/log
export BPA2_HOST=`gp url 8090 | sed 's/.*https:\/\///'`
export BPA_SECURITY_ENABLED=false