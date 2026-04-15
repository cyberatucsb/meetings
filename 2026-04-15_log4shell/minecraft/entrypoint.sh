#!/bin/bash
# Wait for the server to download and extract libraries on first run,
# then swap Log4j jars before it actually launches.
# We run the original entrypoint in the background, wait for libraries to appear,
# swap them, then let it continue.

# Start a background watcher that swaps jars as soon as they appear
(
    LOG4J_DIR="/data/libraries/org/apache/logging/log4j"
    echo "[entrypoint] waiting for Log4j libraries to appear..."
    while [ ! -d "$LOG4J_DIR" ]; do
        sleep 1
    done
    /swap-log4j.sh
) &

# Hand off to the original entrypoint
exec /start
