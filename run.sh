#!/bin/bash
#if [ -n "$HOME" ]; then
#  rm -rf ${HOME}/.axelor/attachments/
#fi
#./gradlew --stop

set -e
clear
#docker stop educaflow-db
#docker run --name educaflow-db --hostname educaflow-db  -e POSTGRES_USER=educaflow -e POSTGRES_PASSWORD=educaflow -e POSTGRES_DB=educaflow -p 5432:5432 -d --rm postgres:12.22
#./gradlew clean build --info --refresh-dependencies
./gradlew clean build --info
# ./gradlew clean test --info
#./gradlew --no-daemon run --debug-jvm --port 8080 --context-path /
./gradlew --no-daemon run --port 8080 --context-path /  --config ../secretaria-virtual-secrets/secretaria-virtual.properties

