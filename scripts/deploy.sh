#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────
# deploy.sh  –  Stop any existing container, run the new image
# Usage: ./scripts/deploy.sh <image:tag> <host_port>
# ─────────────────────────────────────────────────────────────
set -euo pipefail

IMAGE="${1:-pmanagement-service:latest}"
HOST_PORT="${2:-9081}"
CONTAINER_NAME="pmanagement-service"

echo "──────────────────────────────────────────────"
echo " Deploying : ${IMAGE}"
echo " Port      : ${HOST_PORT} -> 9081"
echo "──────────────────────────────────────────────"

# Stop & remove old container if running
if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "[deploy] Stopping existing container: ${CONTAINER_NAME}"
    docker stop "${CONTAINER_NAME}" || true
    docker rm   "${CONTAINER_NAME}" || true
fi

# Run new container
docker run -d \
    --name "${CONTAINER_NAME}" \
    --restart unless-stopped \
    -p "${HOST_PORT}:9081" \
    -e SPRING_PROFILES_ACTIVE=default \
    "${IMAGE}"

# Health check – wait up to 60 seconds
echo "[deploy] Waiting for health check..."
for i in $(seq 1 12); do
    STATUS=$(curl -sf "http://localhost:${HOST_PORT}/actuator/health" \
             | grep -o '"status":"[^"]*"' | head -1 || true)
    if echo "${STATUS}" | grep -q "UP"; then
        echo "[deploy] ✅ Application is UP (attempt ${i})"
        echo "[deploy] Swagger UI → http://localhost:${HOST_PORT}/swagger-ui.html"
        exit 0
    fi
    echo "[deploy] Attempt ${i}/12 – waiting 5s..."
    sleep 5
done

echo "[deploy] ❌ Health check timed out – check docker logs ${CONTAINER_NAME}"
docker logs "${CONTAINER_NAME}" --tail 30
exit 1
