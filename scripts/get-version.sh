#!/usr/bin/env sh
REPOSITORY_NAME="${REGISTRY_ID}/${IMAGE_NAME}"

TOKEN=$(curl -d "{\"yandexPassportOauthToken\":\"${OAUTH}\"}" "https://iam.api.cloud.yandex.net/iam/v1/tokens" | grep 'iamToken')

IAM_TOKEN=$(echo "${TOKEN}"  | awk '{print $2}' | sed 's/^"\(.*\)",*/\1/')

PREV_VERSION=$(curl -H "Authorization: Bearer ${IAM_TOKEN}" \
  https://container-registry.api.cloud.yandex.net/container-registry/v1/images?repositoryName=${REPOSITORY_NAME} \
   | jq '.images' | jq '.[0]' | jq -r '.tags[0]' \
    | cut -c2-)

echo "NEW_VERSION=$((PREV_VERSION + 1))" >> "$GITHUB_ENV"