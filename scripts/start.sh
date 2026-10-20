#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
docker compose up -d --wait
printf 'Очікуємо готовності Keycloak'
ready=false
for attempt in {1..90}; do
    if curl --silent --fail --max-time 3 http://localhost:8081/realms/autoservice/.well-known/openid-configuration >/dev/null; then
        ready=true
        break
    fi
    printf '.'
    sleep 2
done
printf '\n'
if [ "$ready" != true ]; then
    docker compose logs --tail=40 keycloak
    exit 1
fi
exec ./mvnw spring-boot:run
