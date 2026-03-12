#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────
# deploy.sh  –  Zero-downtime local deployment
# Usage: ./scripts/deploy.sh <image:tag> <host_port>
# ─────────────────────────────────────────────────────────────
set -euo pipefail

IMAGE="${1:-pmanagement-service:latest}"
HOST_PORT="${2:-9081}"
CONTAINER_NAME="pmanagement-service"
HEALTH_URL="http://localhost:${HOST_PORT}/actuator/health"
MAX_WAIT=90
POLL_INTERVAL=5

echo "──────────────────────────────────────────────"
echo " Deploying : ${IMAGE}"
echo " Port      : ${HOST_PORT} -> 9081"
echo " Health    : ${HEALTH_URL}"
echo "──────────────────────────────────────────────"

# ── Check current container state ────────────────────────────
CONTAINER_EXISTS=$(docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$" && echo "yes" || echo "no")
CONTAINER_RUNNING=$(docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$" && echo "yes" || echo "no")
CURRENT_IMAGE=""
if [ "${CONTAINER_EXISTS}" = "yes" ]; then
    CURRENT_IMAGE=$(docker inspect --format '{{.Config.Image}}' "${CONTAINER_NAME}" 2>/dev/null || true)
fi

echo "[deploy] Container exists : ${CONTAINER_EXISTS}"
echo "[deploy] Container running: ${CONTAINER_RUNNING}"
echo "[deploy] Current image    : ${CURRENT_IMAGE:-none}"
echo "[deploy] New image        : ${IMAGE}"

# ── Skip redeploy if same image is already running ───────────
if [ "${CONTAINER_RUNNING}" = "yes" ] && [ "${CURRENT_IMAGE}" = "${IMAGE}" ]; then
    echo "[deploy] ✅ Same image already running – verifying health..."
    STATUS=$(curl -sf "${HEALTH_URL}" | grep -o '"status":"[^"]*"' | head -1 || true)
    if echo "${STATUS}" | grep -q "UP"; then
        echo "[deploy] ✅ Service is healthy – skipping redeploy"
        echo "[deploy] Swagger UI → http://localhost:${HOST_PORT}/swagger-ui.html"
        exit 0
    fi
    echo "[deploy] ⚠️  Same image but unhealthy – redeploying..."
fi

# ── Stop container if running ─────────────────────────────────
if [ "${CONTAINER_RUNNING}" = "yes" ]; then
    echo "[deploy] Stopping container: ${CONTAINER_NAME}"
    docker stop "${CONTAINER_NAME}"
fi

# ── Remove container if exists ────────────────────────────────
# Always remove to ensure clean state (port bindings, env vars, etc.)
if [ "${CONTAINER_EXISTS}" = "yes" ]; then
    echo "[deploy] Removing container: ${CONTAINER_NAME}"
    docker rm "${CONTAINER_NAME}"
fi

# ── Remove old image if different from new one ────────────────
if [ -n "${CURRENT_IMAGE}" ] && [ "${CURRENT_IMAGE}" != "${IMAGE}" ]; then
    echo "[deploy] Removing old image: ${CURRENT_IMAGE}"
    docker rmi "${CURRENT_IMAGE}" 2>/dev/null || echo "[deploy] Could not remove old image (may be in use elsewhere) – continuing"
fi

# ── Start new container ───────────────────────────────────────
echo "[deploy] Starting container: ${CONTAINER_NAME}"
docker run -d \
    --name "${CONTAINER_NAME}" \
    --restart unless-stopped \
    -p "${HOST_PORT}:9081" \
    -e SPRING_PROFILES_ACTIVE=default \
    "${IMAGE}"

# ── Health check ──────────────────────────────────────────────
echo "[deploy] Waiting for application to start..."
ATTEMPTS=$(( MAX_WAIT / POLL_INTERVAL ))
for i in $(seq 1 "${ATTEMPTS}"); do
    sleep "${POLL_INTERVAL}"
    STATUS=$(curl -sf "${HEALTH_URL}" | grep -o '"status":"[^"]*"' | head -1 || true)
    if echo "${STATUS}" | grep -q "UP"; then
        echo "[deploy] ✅ Application is UP (attempt ${i}/${ATTEMPTS})"
        echo "──────────────────────────────────────────────"
        echo " App       → http://localhost:${HOST_PORT}"
        echo " Swagger   → http://localhost:${HOST_PORT}/swagger-ui.html"
        echo " Health    → ${HEALTH_URL}"
        echo " H2 Console→ http://localhost:${HOST_PORT}/h2-console"
        echo "──────────────────────────────────────────────"
        exit 0
    fi
    echo "[deploy] Attempt ${i}/${ATTEMPTS} – not ready yet, waiting ${POLL_INTERVAL}s..."
done

# ── Timeout – print logs for debugging ───────────────────────
echo "[deploy] ❌ Health check timed out after ${MAX_WAIT}s"
echo "[deploy] Last 50 lines of container logs:"
echo "──────────────────────────────────────────────"
docker logs "${CONTAINER_NAME}" --tail 50
echo "──────────────────────────────────────────────"
exit 1