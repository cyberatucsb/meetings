FROM itzg/minecraft-server:java25

RUN apt-get update -qq && apt-get install -y -qq zip >/dev/null 2>&1 && rm -rf /var/lib/apt/lists/*

# Download vulnerable Log4j jars to swap in at runtime
RUN mkdir -p /vulnerable-log4j && \
    curl -sL -o /vulnerable-log4j/log4j-core-2.14.1.jar \
      https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-core/2.14.1/log4j-core-2.14.1.jar && \
    curl -sL -o /vulnerable-log4j/log4j-api-2.14.1.jar \
      https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-api/2.14.1/log4j-api-2.14.1.jar && \
    curl -sL -o /vulnerable-log4j/log4j-slf4j18-impl-2.14.1.jar \
      https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-slf4j18-impl/2.14.1/log4j-slf4j18-impl-2.14.1.jar

COPY swap-log4j.sh /swap-log4j.sh
RUN chmod +x /swap-log4j.sh

# Wrap the original entrypoint: swap jars then hand off
COPY entrypoint.sh /custom-entrypoint.sh
RUN chmod +x /custom-entrypoint.sh
ENTRYPOINT ["/custom-entrypoint.sh"]
