#!/bin/bash

# Loop to create 9 instances on ports from 10020 to 10028
for PORT in {10020..10028}
do
  echo "Starting instance on port $PORT..."
  SERVER_PORT=$PORT pm2 start bin/boot.sh --name "hermes-$PORT"
done
