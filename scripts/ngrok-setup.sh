#!/usr/bin/env bash
# =============================================================================
# ngrok-setup.sh
# Installs ngrok (if missing), configures the authtoken from .env, and starts
# a tunnel that exposes local Jenkins to GitHub webhooks.
#
# Usage:
#   ./scripts/ngrok-setup.sh [JENKINS_PORT]
#
#   JENKINS_PORT defaults to 8080 if not provided.
#
# What this script does (in order):
#   1. Locate the .env file at the repository root and load variables
#   2. Install ngrok via apt if it is not already installed
#   3. Configure the ngrok authtoken (read from NGROK_TOKEN in .env)
#   4. Kill any leftover ngrok process
#   5. Start a new HTTP tunnel to JENKINS_PORT
#   6. Wait for the tunnel to come up and retrieve the public URL
#   7. Print the Webhook URL and the step-by-step GitHub configuration guide
#
# Required variable in .env:
#   NGROK_TOKEN=<your ngrok authtoken>   (https://dashboard.ngrok.com/authtokens)
#
# Optional variables read from .env:
#   GITHUB_WEBHOOK_SECRET  – shown in the GitHub webhook setup reminder
# =============================================================================

set -euo pipefail

# ── Resolve paths ─────────────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${REPO_ROOT}/.env"

JENKINS_PORT="${1:-8080}"
NGROK_API="http://localhost:4040/api/tunnels"
WEBHOOK_PATH="/github-webhook/"

# ── Banner ─────────────────────────────────────────────────────────────────────
echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║              ngrok-setup.sh – Jenkins webhook tunnel         ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# ── Step 1: Load .env ─────────────────────────────────────────────────────────
echo "──────────────────────────────────────────────────────────────"
echo " Step 1 – Loading secrets from .env"
echo "──────────────────────────────────────────────────────────────"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo ""
  echo "❌  .env file not found at: ${ENV_FILE}"
  echo ""
  echo "    Create it first:"
  echo "      cp .env.example .env"
  echo "    Then fill in at least NGROK_TOKEN."
  echo ""
  exit 1
fi

# Source only KEY=VALUE lines; ignore comments and blank lines
set -a
# shellcheck disable=SC1090
source <(grep -E '^[A-Za-z_][A-Za-z0-9_]*=' "${ENV_FILE}" | grep -v '^#')
set +a

echo "✅  .env loaded from ${ENV_FILE}"

# Validate NGROK_TOKEN is present and not a placeholder
if [[ -z "${NGROK_TOKEN:-}" ]] || [[ "${NGROK_TOKEN}" == *"CHANGEME"* ]]; then
  echo ""
  echo "❌  NGROK_TOKEN is missing or still set to the placeholder value."
  echo ""
  echo "    1. Sign up (free) at https://ngrok.com"
  echo "    2. Copy your authtoken from https://dashboard.ngrok.com/authtokens"
  echo "    3. Add it to your .env file:"
  echo "         NGROK_TOKEN=<your_actual_token>"
  echo ""
  exit 1
fi

echo "✅  NGROK_TOKEN found"

# ── Step 2: Install ngrok if missing ──────────────────────────────────────────
echo ""
echo "──────────────────────────────────────────────────────────────"
echo " Step 2 – Checking ngrok installation"
echo "──────────────────────────────────────────────────────────────"

if command -v ngrok &>/dev/null; then
  NGROK_VERSION="$(ngrok version 2>&1 | head -1)"
  echo "✅  ngrok already installed: ${NGROK_VERSION}"
else
  echo "⚙️   ngrok not found – installing via apt..."
  echo ""

  # Verify apt is available (Debian/Ubuntu)
  if ! command -v apt &>/dev/null; then
    echo "❌  apt not found. Please install ngrok manually:"
    echo "    https://ngrok.com/download"
    exit 1
  fi

  curl -sSL https://ngrok-agent.s3.amazonaws.com/ngrok.asc \
    | sudo tee /etc/apt/trusted.gpg.d/ngrok.asc >/dev/null \
    && echo "deb https://ngrok-agent.s3.amazonaws.com bookworm main" \
    | sudo tee /etc/apt/sources.list.d/ngrok.list \
    && sudo apt update -qq \
    && sudo apt install -y ngrok

  echo ""
  echo "✅  ngrok installed: $(ngrok version 2>&1 | head -1)"
fi

# ── Step 3: Configure authtoken ───────────────────────────────────────────────
echo ""
echo "──────────────────────────────────────────────────────────────"
echo " Step 3 – Configuring ngrok authtoken"
echo "──────────────────────────────────────────────────────────────"

ngrok config add-authtoken "${NGROK_TOKEN}"
echo "✅  Authtoken configured"

# ── Step 4: Kill any existing ngrok process ───────────────────────────────────
echo ""
echo "──────────────────────────────────────────────────────────────"
echo " Step 4 – Starting ngrok tunnel → localhost:${JENKINS_PORT}"
echo "──────────────────────────────────────────────────────────────"

if pkill -f "ngrok http" 2>/dev/null; then
  echo "ℹ️   Stopped a previous ngrok session"
  sleep 1
fi

# ── Step 5: Start tunnel ──────────────────────────────────────────────────────
ngrok http "${JENKINS_PORT}" --log=stdout > /tmp/ngrok.log 2>&1 &
NGROK_PID=$!
echo "ℹ️   ngrok started (PID ${NGROK_PID})"

# ── Step 6: Wait for public URL ───────────────────────────────────────────────
echo "⏳  Waiting for tunnel to initialise..."

PUBLIC_URL=""
for i in $(seq 1 20); do
  sleep 1
  PUBLIC_URL=$(curl -s "${NGROK_API}" 2>/dev/null \
    | grep -o '"public_url":"[^"]*"' \
    | grep "https" \
    | head -1 \
    | sed 's/"public_url":"//;s/"//') || true

  if [[ -n "${PUBLIC_URL}" ]]; then
    break
  fi

  # Surface early failures
  if ! kill -0 "${NGROK_PID}" 2>/dev/null; then
    echo ""
    echo "❌  ngrok process exited unexpectedly. Last log output:"
    echo ""
    cat /tmp/ngrok.log
    exit 1
  fi
done

if [[ -z "${PUBLIC_URL}" ]]; then
  echo ""
  echo "❌  Could not retrieve the public URL after 20 seconds."
  echo "    ngrok log output:"
  echo ""
  cat /tmp/ngrok.log
  kill "${NGROK_PID}" 2>/dev/null || true
  exit 1
fi

WEBHOOK_URL="${PUBLIC_URL}${WEBHOOK_PATH}"
WEBHOOK_SECRET_HINT="${GITHUB_WEBHOOK_SECRET:-<GITHUB_WEBHOOK_SECRET from .env>}"

# ── Step 7: Print summary and instructions ────────────────────────────────────
echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║  ✅  Tunnel active!                                          ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "  Jenkins port : ${JENKINS_PORT}"
echo "  Public URL   : ${PUBLIC_URL}"
echo "  Webhook URL  : ${WEBHOOK_URL}"
echo ""
echo "──────────────────────────────────────────────────────────────"
echo " Configure GitHub webhook – follow these steps:"
echo "──────────────────────────────────────────────────────────────"
echo ""
echo "  1. Open your repository on GitHub"
echo "  2. Go to: Settings → Webhooks → Add webhook"
echo "  3. Fill in the form:"
echo ""
echo "       Payload URL   :  ${WEBHOOK_URL}"
echo "       Content type  :  application/json"
echo "       Secret        :  ${WEBHOOK_SECRET_HINT}"
echo "       Which events  :  ● Just the push event"
echo "       Active        :  ✅ checked"
echo ""
echo "  4. Click 'Add webhook'"
echo "  5. GitHub sends a ping – look for a green tick ✅ next to the hook"
echo ""
echo "──────────────────────────────────────────────────────────────"
echo " Test the pipeline:"
echo "──────────────────────────────────────────────────────────────"
echo ""
echo "  git commit --allow-empty -m \"chore: trigger CI\""
echo "  git push origin master"
echo ""
echo "  Then watch: http://localhost:8080/job/pmanagement-service-pipeline/"
echo ""
echo "──────────────────────────────────────────────────────────────"
echo " ⚠️  This ngrok URL expires when the process stops."
echo "    You must update the GitHub webhook URL after each restart"
echo "    (or upgrade to a paid ngrok plan for a static domain)."
echo ""
echo "    Press Ctrl+C to stop the tunnel."
echo "──────────────────────────────────────────────────────────────"

# Keep the script alive so the tunnel stays open
wait "${NGROK_PID}"
