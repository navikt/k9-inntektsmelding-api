FROM ghcr.io/navikt/sif-baseimages/java-25:2026.10.05.1242Z
LABEL org.opencontainers.image.source=https://github.com/navikt/k9-inntektsmelding-api

COPY target/classes/logback*.xml conf/
COPY target/lib/*.jar lib/
COPY target/app.jar .
