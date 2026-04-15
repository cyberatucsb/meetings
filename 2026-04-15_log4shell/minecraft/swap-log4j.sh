#!/bin/bash
# Replace patched Log4j jars with vulnerable 2.14.1 versions
# Called as an init script before the server starts

LOG4J_DIR="/data/libraries/org/apache/logging/log4j"

if [ ! -d "$LOG4J_DIR" ]; then
    echo "[swap-log4j] libraries not extracted yet, skipping (will run on next start)"
    exit 0
fi

echo "[swap-log4j] replacing patched Log4j with vulnerable 2.14.1..."

find "$LOG4J_DIR" -name "log4j-core-*.jar" -exec cp /vulnerable-log4j/log4j-core-2.14.1.jar {} \;
find "$LOG4J_DIR" -name "log4j-api-*.jar" -exec cp /vulnerable-log4j/log4j-api-2.14.1.jar {} \;
find "$LOG4J_DIR" -name "log4j-slf4j18-impl-*.jar" -exec cp /vulnerable-log4j/log4j-slf4j18-impl-2.14.1.jar {} \;

echo "[swap-log4j] done — server is now vulnerable to CVE-2021-44228"
