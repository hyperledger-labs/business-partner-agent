#!/usr/bin/env bash

# Kill all running ngrok / diode processes to get new tunnels if they are expired
kill $(ps aux | grep ngrok | awk '{print $2}')
kill $(ps aux | grep diode | awk '{print $2}') 