#!/usr/bin/env sh

docker login --username oauth --password y0_AgAAAABp1djNAATuwQAAAADwuempiuY7OGLZRn61xDkMBxhMHnRjfw8 cr.yandex

docker build . -t cr.yandex/${REGISTRY_ID}/backend:v${VERSION_NUMBER}

docker push cr.yandex/${REGISTRY_ID}/backend:v${VERSION_NUMBER}