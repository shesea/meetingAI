#!/usr/bin/env sh

docker login --username oauth --password ${OAUTH} cr.yandex

docker build . -t cr.yandex/${REGISTRY_ID}/backend:v${VERSION_NUMBER}

docker push cr.yandex/${REGISTRY_ID}/backend:v${VERSION_NUMBER}