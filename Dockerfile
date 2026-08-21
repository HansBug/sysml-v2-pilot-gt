FROM eclipse-temurin:21-jre-alpine

ARG VERSION=dev
LABEL org.opencontainers.image.title="SysML v2 Pilot Ground Truth" \
      org.opencontainers.image.description="CLI backed by the official SysML v2 Pilot parser" \
      org.opencontainers.image.source="https://github.com/HansBug/sysml-v2-pilot-gt" \
      org.opencontainers.image.licenses="EPL-2.0" \
      org.opencontainers.image.version="$VERSION"

WORKDIR /app
COPY target/*-all.jar /app/sysml-v2-pilot-gt.jar

USER 10001
ENTRYPOINT ["java", "-jar", "/app/sysml-v2-pilot-gt.jar"]
CMD ["--help"]
