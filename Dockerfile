FROM ghcr.io/navikt/baseimages/temurin:21

COPY /.scripts/export-dbconfig.sh /init-scripts/export-dbconfig.sh
COPY /app/build/libs/app-all.jar app.jar