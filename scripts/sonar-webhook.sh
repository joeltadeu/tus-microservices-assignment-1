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
DEFAULT_USER="admin"
PASSWORD="Admin@cicd2025"

AUTH="${DEFAULT_USER}:${PASSWORD}"

echo "[sonar-setup] Registering Jenkins webhook..."
curl -sf -u "${AUTH}" \
    -X POST "${SONAR_URL}/api/webhooks/create" \
    -d "name=Jenkins&url=http://jenkins:8080/sonarqube-webhook/" \
    || echo "[sonar-setup] Webhook may already exist – continuing"
