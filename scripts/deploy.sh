#!/usr/bin/env sh

docker login --username oauth --password y0_AgAAAABp1djNAATuwQAAAADwuempiuY7OGLZRn61xDkMBxhMHnRjfw8 cr.yandex

docker build --build-arg cluster_id="${CLUSTER_ID}" --build-arg db_name="${DB_NAME}" \
 --build-arg db_username="${DB_USERNAME}" --build-arg db_password="${DB_PASSWORD}" \
  . -t cr.yandex/${REGISTRY_ID}/backend:v${VERSION_NUMBER}

docker push cr.yandex/${REGISTRY_ID}/backend:v${VERSION_NUMBER}