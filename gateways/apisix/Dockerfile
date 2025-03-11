# Use the official Apache APISIX image
FROM apache/apisix:3.11.0-debian

# Copy the corrected YAML configuration
COPY apisix.yaml /usr/local/apisix/conf/apisix.yaml
COPY config.yaml /usr/local/apisix/conf/config.yaml

EXPOSE 9080 9092 9443

ENV APISIX_STAND_ALONE=true

# Start APISIX in DB-less mode
CMD ["apisix", "start"]