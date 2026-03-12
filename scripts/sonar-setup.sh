#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# sonar-setup.sh
#
# Run this ONCE after docker-compose up, when SonarQube is fully started.
# It:
#   1. Changes the default admin password
#   2. Creates the project
#   3. Creates a custom Quality Gate with sensible thresholds
#   4. Generates an analysis token and prints it
#
# Prerequisites: curl, jq
# Usage: bash scripts/sonar-setup.sh
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

SONAR_URL="http://localhost:9000"
DEFAULT_PASS="admin"
NEW_PASS="Admin@cicd2025"
PROJECT_KEY="pmanagement-service"
PROJECT_NAME="Patient Management Service"
GATE_NAME="PMS Quality Gate"

wait_for_sonar() {
    echo "[sonar-setup] Waiting for SonarQube to become ready..."
    for i in $(seq 1 30); do
        STATUS=$(curl -sf "${SONAR_URL}/api/system/status" | grep -o '"status":"[^"]*"' || true)
        if echo "${STATUS}" | grep -q "UP"; then
            echo "[sonar-setup] SonarQube is UP ✅"
            return 0
        fi
        echo "[sonar-setup] Attempt ${i}/30 – not ready yet, waiting 10s..."
        sleep 10
    done
    echo "[sonar-setup] ❌ SonarQube did not start in time."
    exit 1
}

wait_for_sonar

# 1. Change default admin password
echo "[sonar-setup] Changing admin password..."
curl -sf -u "${DEFAULT_PASS}:${DEFAULT_PASS}" \
    -X POST "${SONAR_URL}/api/users/change_password" \
    -d "login=admin&previousPassword=${DEFAULT_PASS}&password=${NEW_PASS}" \
    || echo "[sonar-setup] Password may already be changed – continuing"

AUTH="${DEFAULT_PASS}:${NEW_PASS}"
# Try new password first, fall back to unchanged
if ! curl -sf -u "admin:${NEW_PASS}" "${SONAR_URL}/api/system/status" > /dev/null 2>&1; then
    AUTH="admin:${DEFAULT_PASS}"
fi

# 2. Create project
echo "[sonar-setup] Creating SonarQube project..."
curl -sf -u "${AUTH}" \
    -X POST "${SONAR_URL}/api/projects/create" \
    -d "project=${PROJECT_KEY}&name=${PROJECT_NAME}&visibility=public" \
    || echo "[sonar-setup] Project may already exist – continuing"

# 3. Create custom Quality Gate
echo "[sonar-setup] Creating Quality Gate: ${GATE_NAME}"
GATE_ID=$(curl -sf -u "${AUTH}" \
    -X POST "${SONAR_URL}/api/qualitygates/create" \
    -d "name=${GATE_NAME}" | grep -o '"id":[0-9]*' | grep -o '[0-9]*' || true)

if [ -z "${GATE_ID}" ]; then
    # Gate already exists – get its ID
    GATE_ID=$(curl -sf -u "${AUTH}" \
        "${SONAR_URL}/api/qualitygates/list" \
        | python3 -c "import sys,json; gates=json.load(sys.stdin)['qualitygates']; print([g['id'] for g in gates if g['name']=='${GATE_NAME}'][0])" 2>/dev/null || echo "")
fi

if [ -n "${GATE_ID}" ]; then
    echo "[sonar-setup] Gate ID: ${GATE_ID} – adding conditions..."

    # Line coverage >= 70%
    curl -sf -u "${AUTH}" -X POST "${SONAR_URL}/api/qualitygates/create_condition" \
        -d "gateId=${GATE_ID}&metric=coverage&op=LT&error=70" || true

    # Duplicated lines < 5%
    curl -sf -u "${AUTH}" -X POST "${SONAR_URL}/api/qualitygates/create_condition" \
        -d "gateId=${GATE_ID}&metric=duplicated_lines_density&op=GT&error=5" || true

    # No blocker issues
    curl -sf -u "${AUTH}" -X POST "${SONAR_URL}/api/qualitygates/create_condition" \
        -d "gateId=${GATE_ID}&metric=blocker_violations&op=GT&error=0" || true

    # No critical issues
    curl -sf -u "${AUTH}" -X POST "${SONAR_URL}/api/qualitygates/create_condition" \
        -d "gateId=${GATE_ID}&metric=critical_violations&op=GT&error=0" || true

    # Reliability rating <= A (1)
    curl -sf -u "${AUTH}" -X POST "${SONAR_URL}/api/qualitygates/create_condition" \
        -d "gateId=${GATE_ID}&metric=reliability_rating&op=GT&error=1" || true

    # Security rating <= A (1)
    curl -sf -u "${AUTH}" -X POST "${SONAR_URL}/api/qualitygates/create_condition" \
        -d "gateId=${GATE_ID}&metric=security_rating&op=GT&error=1" || true

    # Assign gate to project
    curl -sf -u "${AUTH}" -X POST "${SONAR_URL}/api/qualitygates/select" \
        -d "gateId=${GATE_ID}&projectKey=${PROJECT_KEY}" || true

    echo "[sonar-setup] Quality Gate conditions set ✅"
fi

# 4. Generate analysis token
echo "[sonar-setup] Generating analysis token..."
TOKEN=$(curl -sf -u "${AUTH}" \
    -X POST "${SONAR_URL}/api/user_tokens/generate" \
    -d "name=jenkins-pipeline" | grep -o '"token":"[^"]*"' | cut -d'"' -f4 || true)

if [ -n "${TOKEN}" ]; then
    echo ""
    echo "════════════════════════════════════════════════════════"
    echo "  ✅  SonarQube setup complete!"
    echo ""
    echo "  Admin URL : ${SONAR_URL}"
    echo "  Username  : admin"
    echo "  Password  : ${NEW_PASS}"
    echo ""
    echo "  ⚠️  Copy this token to Jenkins Credentials (SONAR_TOKEN):"
    echo "  Token     : ${TOKEN}"
    echo "════════════════════════════════════════════════════════"
else
    echo ""
    echo "[sonar-setup] ⚠️  Could not generate token automatically."
    echo "  → Go to ${SONAR_URL} → My Account → Security → Generate Token"
    echo "  → Add it to Jenkins: Manage Jenkins → Credentials → SONAR_TOKEN"
fi
