#!/bin/bash
# Swap Log4j jars inside the server jar, then start normally.
# On first run the jar doesn't exist yet — the itzg image downloads it.
# We let /start run once to download, then patch and restart.

SERVER_JAR="/data/minecraft_server.*.jar"

if ls $SERVER_JAR >/dev/null 2>&1; then
    # Server jar already exists (not first run) — swap and start
    /swap-log4j.sh
    exec /start
else
    # First run — start to download, wait for jar, then stop, swap, restart
    /start &
    PID=$!

    while ! ls $SERVER_JAR >/dev/null 2>&1; do
        sleep 1
    done

    # Wait for the server to finish starting so extraction is complete
    until grep -q "Done" /data/logs/latest.log 2>/dev/null; do
        sleep 1
    done

    kill $PID 2>/dev/null
    wait $PID 2>/dev/null

    /swap-log4j.sh
    exec /start
fi
