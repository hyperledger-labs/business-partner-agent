#!/usr/bin/env bash

# Kill all running ngrok processes to get new tunnels if they are expired
kill $(ps aux | grep ngrok | awk '{print $2}')