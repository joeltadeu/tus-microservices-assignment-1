#!/usr/bin/env bash
set -euo pipefail

SONAR_URL="http://localhost:9000"
DEFAULT_USER="admin"
PASSWORD="Admin@cicd2025"

AUTH="${DEFAULT_USER}:${PASSWORD}"

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
    echo "  Password  : ${PASSWORD}"
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