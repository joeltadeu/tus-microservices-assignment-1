#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# github-webhook-setup.sh
#
# Registers a GitHub webhook on the repository so that every push
# automatically triggers the Jenkins pipeline.
#
# Prerequisites:
#   - curl
#   - A GitHub PAT with scopes: repo, admin:repo_hook
#   - Jenkins reachable from the internet (use ngrok if running locally)
#
# Usage:
#   GITHUB_PAT=ghp_xxx JENKINS_URL=https://your-ngrok-url.ngrok.io bash scripts/github-webhook-setup.sh
#
# Or edit the variables below and run directly.
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

GITHUB_PAT="${GITHUB_PAT:-ghp_REPLACE_WITH_YOUR_TOKEN}"
GITHUB_OWNER="joeltadeu"
GITHUB_REPO="tus-microservices-assignment-1"

# Jenkins public URL – must be reachable by GitHub's servers.
# If running locally, expose Jenkins with: ngrok http 8080
# Then set JENKINS_URL to the ngrok https URL.
JENKINS_URL="${JENKINS_URL:-http://localhost:8080}"
WEBHOOK_URL="${JENKINS_URL}/github-webhook/"

echo "──────────────────────────────────────────────────────────"
echo " Registering GitHub webhook"
echo " Repo    : ${GITHUB_OWNER}/${GITHUB_REPO}"
echo " Endpoint: ${WEBHOOK_URL}"
echo "──────────────────────────────────────────────────────────"

# Check if webhook already exists
EXISTING=$(curl -sf \
    -H "Authorization: token ${GITHUB_PAT}" \
    -H "Accept: application/vnd.github+json" \
    "https://api.github.com/repos/${GITHUB_OWNER}/${GITHUB_REPO}/hooks" \
    | grep -o "${WEBHOOK_URL}" || true)

if [ -n "${EXISTING}" ]; then
    echo "[webhook] Webhook already registered for ${WEBHOOK_URL}"
    exit 0
fi

# Register webhook
RESPONSE=$(curl -sf \
    -X POST \
    -H "Authorization: token ${GITHUB_PAT}" \
    -H "Accept: application/vnd.github+json" \
    -H "Content-Type: application/json" \
    "https://api.github.com/repos/${GITHUB_OWNER}/${GITHUB_REPO}/hooks" \
    -d "{
        \"name\": \"web\",
        \"active\": true,
        \"events\": [\"push\", \"pull_request\"],
        \"config\": {
            \"url\": \"${WEBHOOK_URL}\",
            \"content_type\": \"json\",
            \"insecure_ssl\": \"0\"
        }
    }")

HOOK_ID=$(echo "${RESPONSE}" | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*' || true)

if [ -n "${HOOK_ID}" ]; then
    echo ""
    echo "════════════════════════════════════════════════════════"
    echo "  ✅  Webhook registered successfully!"
    echo "  Hook ID  : ${HOOK_ID}"
    echo "  Endpoint : ${WEBHOOK_URL}"
    echo ""
    echo "  Every git push to '${GITHUB_OWNER}/${GITHUB_REPO}'"
    echo "  will now automatically trigger the Jenkins pipeline."
    echo "════════════════════════════════════════════════════════"
else
    echo ""
    echo "  ⚠️  Could not register webhook automatically."
    echo "  Add it manually at:"
    echo "  https://github.com/${GITHUB_OWNER}/${GITHUB_REPO}/settings/hooks"
    echo ""
    echo "  Settings:"
    echo "    Payload URL : ${WEBHOOK_URL}"
    echo "    Content type: application/json"
    echo "    Events      : Just the push event"
    echo "    Active      : ✅"
fi
