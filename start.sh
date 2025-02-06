#!/bin/bash

trap "echo 'Shutdown signal received. Exiting...'; exit" SIGINT SIGTERM

RESTART_EXIT_CODE=1

while true; do
    echo "Starting Bobo..."
    java -cp "bot.jar:libs/*" bobo.Bobo

    EXIT_STATUS=$?
    if [ $EXIT_STATUS -eq $RESTART_EXIT_CODE ]; then
        echo "Bobo is restarting..."
        sleep 5
    else
        echo "Bobo stopped with exit status $EXIT_STATUS."
        break
    fi
done