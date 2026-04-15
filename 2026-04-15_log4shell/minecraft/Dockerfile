FROM itzg/minecraft-server:java25

# Download vulnerable Log4j jars to swap in at runtime
RUN mkdir -p /vulnerable-log4j && \
    curl -sL -o /vulnerable-log4j/log4j-core-2.14.1.jar \
      https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-core/2.14.1/log4j-core-2.14.1.jar && \
    curl -sL -o /vulnerable-log4j/log4j-api-2.14.1.jar \
      https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-api/2.14.1/log4j-api-2.14.1.jar && \
    curl -sL -o /vulnerable-log4j/log4j-slf4j18-impl-2.14.1.jar \
      https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-slf4j18-impl/2.14.1/log4j-slf4j18-impl-2.14.1.jar

# Script to replace patched Log4j with vulnerable ones after server setup
COPY swap-log4j.sh /swap-log4j.sh
RUN chmod +x /swap-log4j.sh
