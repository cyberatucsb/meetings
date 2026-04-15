#!/bin/bash
# Replace Log4j jars INSIDE the bundled Minecraft server jar
# so the bundler's hash check sees our vulnerable jars as canonical.

SERVER_JAR="/data/minecraft_server.*.jar"

# Wait for server jar to exist
while ! ls $SERVER_JAR >/dev/null 2>&1; do
    sleep 1
done

JAR=$(ls $SERVER_JAR 2>/dev/null | head -1)
echo "[swap-log4j] patching $JAR with vulnerable Log4j 2.14.1..."

WORK=$(mktemp -d)
cd "$WORK"

# Replace the jars inside META-INF/libraries/ in the server jar
mkdir -p META-INF/libraries/org/apache/logging/log4j/log4j-core/2.25.2
mkdir -p META-INF/libraries/org/apache/logging/log4j/log4j-api/2.25.2
mkdir -p META-INF/libraries/org/apache/logging/log4j/log4j-slf4j2-impl/2.25.2

cp /vulnerable-log4j/log4j-core-2.14.1.jar META-INF/libraries/org/apache/logging/log4j/log4j-core/2.25.2/log4j-core-2.25.2.jar
cp /vulnerable-log4j/log4j-api-2.14.1.jar META-INF/libraries/org/apache/logging/log4j/log4j-api/2.25.2/log4j-api-2.25.2.jar
cp /vulnerable-log4j/log4j-slf4j18-impl-2.14.1.jar META-INF/libraries/org/apache/logging/log4j/log4j-slf4j2-impl/2.25.2/log4j-slf4j2-impl-2.25.2.jar

# Update the server jar in-place (a JAR is just a ZIP)
zip -q -u "$JAR" -r META-INF/

# Also replace the already-extracted copies so a restart doesn't need re-extraction
cp /vulnerable-log4j/log4j-core-2.14.1.jar /data/libraries/org/apache/logging/log4j/log4j-core/2.25.2/log4j-core-2.25.2.jar 2>/dev/null
cp /vulnerable-log4j/log4j-api-2.14.1.jar /data/libraries/org/apache/logging/log4j/log4j-api/2.25.2/log4j-api-2.25.2.jar 2>/dev/null
cp /vulnerable-log4j/log4j-slf4j18-impl-2.14.1.jar /data/libraries/org/apache/logging/log4j/log4j-slf4j2-impl/2.25.2/log4j-slf4j2-impl-2.25.2.jar 2>/dev/null

rm -rf "$WORK"
echo "[swap-log4j] done — server jar patched"
