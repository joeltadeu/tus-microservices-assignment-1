#!/usr/bin/env bash
# =============================================================================
# sonar-setup.sh
#
# Run ONCE after `docker compose up` when SonarQube is fully started.
#
# What it does:
#   1. Loads credentials from the .env file at the repository root
#   2. Waits for SonarQube to become ready
#   3. Changes the default admin password to SONAR_NEW_PASSWORD
#   4. Creates the SonarQube project
#   5. Creates a custom Quality Gate with sensible thresholds
#   6. Generates an analysis token
#   7. Pushes the token directly into Jenkins credentials via the Jenkins API
#      (no container restart required)
#   8. Registers the SonarQube → Jenkins webhook
#
# Prerequisites: curl, jq  (jq is installed automatically if missing)
# Usage: bash scripts/sonar-setup.sh
#
# Required variables in .env:
#   SONAR_DEFAULT_PASSWORD    – factory default ("admin"); do not change
#   SONAR_NEW_PASSWORD        – strong password to set on first run
#   JENKINS_URL               – e.g. http://localhost:8080
#   JENKINS_ADMIN_USERNAME    – Jenkins admin username (default: admin)
#   JENKINS_ADMIN_PASSWORD    – Jenkins admin password
# =============================================================================
set -euo pipefail

# ── Resolve paths ─────────────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${REPO_ROOT}/.env"

# ── Step 0: Load .env ─────────────────────────────────────────────────────────
echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║              sonar-setup.sh – first-time setup               ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "──────────────────────────────────────────────────────────────"
echo " Step 0 – Loading secrets from .env"
echo "──────────────────────────────────────────────────────────────"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo ""
  echo "❌  .env not found at: ${ENV_FILE}"
  echo "    Create it first:"
  echo "      cp .env.example .env"
  echo "    Then fill in all required variables."
  echo ""
  exit 1
fi

# Source only KEY=VALUE lines; skip comments and blank lines
set -a
# shellcheck disable=SC1090
source <(grep -E '^[A-Za-z_][A-Za-z0-9_]*=' "${ENV_FILE}" | grep -v '^#')
set +a

echo "✅  .env loaded"

# Validate required variables
_missing=()
for _var in SONAR_DEFAULT_PASSWORD SONAR_NEW_PASSWORD \
            JENKINS_URL JENKINS_ADMIN_USERNAME JENKINS_ADMIN_PASSWORD; do
  _val="${!_var:-}"
  if [[ -z "${_val}" ]] || [[ "${_val}" == *"CHANGEME"* ]]; then
    _missing+=("${_var}")
  fi
done

if [[ ${#_missing[@]} -gt 0 ]]; then
  echo ""
  echo "❌  The following variables are missing or still set to placeholders:"
  for _var in "${_missing[@]}"; do
    echo "      ${_var}"
  done
  echo ""
  echo "    Edit your .env file and set real values, then re-run this script."
  echo ""
  exit 1
fi

echo "✅  All required variables are set"

# ── Static configuration (no secrets) ────────────────────────────────────────
SONAR_URL="http://localhost:9000"
PROJECT_KEY="pmanagement-service"
PROJECT_NAME="Patient Management Service"
GATE_NAME="PMS Quality Gate"
JENKINS_CREDENTIAL_ID="SONAR_TOKEN"   # must match the id in casc.yaml

# ── Helper: ensure jq is available ───────────────────────────────────────────
if ! command -v jq &>/dev/null; then
  echo ""
  echo "⚙️   jq not found – installing..."
  sudo apt-get update -qq && sudo apt-get install -y jq
  echo "✅  jq installed"
fi

# ── Step 1: Wait for SonarQube ────────────────────────────────────────────────
echo ""
echo "──────────────────────────────────────────────────────────────"
echo " Step 1 – Waiting for SonarQube to become ready"
echo "──────────────────────────────────────────────────────────────"

for i in $(seq 1 30); do
  STATUS=$(curl -sf "${SONAR_URL}/api/system/status" \
    | grep -o '"status":"[^"]*"' || true)
  if echo "${STATUS}" | grep -q "UP"; then
    echo "✅  SonarQube is UP"
    break
  fi
  echo "    Attempt ${i}/30 – not ready yet, waiting 10 s..."
  sleep 10
  if [[ "${i}" -eq 30 ]]; then
    echo "❌  SonarQube did not start within 5 minutes."
    exit 1
  fi
done

# ── Step 2: Change admin password ─────────────────────────────────────────────
echo ""
echo "──────────────────────────────────────────────────────────────"
echo " Step 2 – Setting admin password"
echo "──────────────────────────────────────────────────────────────"

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
  -u "admin:${SONAR_DEFAULT_PASSWORD}" \
  -X POST "${SONAR_URL}/api/users/change_password" \
  -d "login=admin&previousPassword=${SONAR_DEFAULT_PASSWORD}&password=${SONAR_NEW_PASSWORD}")

if [[ "${HTTP_CODE}" == "204" ]]; then
  echo "✅  Admin password updated"
  SONAR_AUTH="admin:${SONAR_NEW_PASSWORD}"
else
  # Password may have been changed in a previous run; try new password
  if curl -sf -u "admin:${SONAR_NEW_PASSWORD}" \
      "${SONAR_URL}/api/system/status" > /dev/null 2>&1; then
    echo "ℹ️   Password was already changed in a previous run"
    SONAR_AUTH="admin:${SONAR_NEW_PASSWORD}"
  else
    echo "❌  Could not authenticate with either the default or new password."
    echo "    Check SONAR_DEFAULT_PASSWORD and SONAR_NEW_PASSWORD in your .env."
    exit 1
  fi
fi

# ── Step 3: Create project ────────────────────────────────────────────────────
echo ""
echo "──────────────────────────────────────────────────────────────"
echo " Step 3 – Creating SonarQube project"
echo "──────────────────────────────────────────────────────────────"

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
  -u "${SONAR_AUTH}" \
  -X POST "${SONAR_URL}/api/projects/create" \
  -d "project=${PROJECT_KEY}&name=${PROJECT_NAME}&visibility=public")

if [[ "${HTTP_CODE}" == "200" ]]; then
  echo "✅  Project '${PROJECT_KEY}' created"
else
  echo "ℹ️   Project may already exist (HTTP ${HTTP_CODE}) – continuing"
fi

# ── Step 4: Create / retrieve Quality Gate ────────────────────────────────────
echo ""
echo "──────────────────────────────────────────────────────────────"
echo " Step 4 – Configuring Quality Gate: ${GATE_NAME}"
echo "──────────────────────────────────────────────────────────────"

GATE_RESPONSE=$(curl -sf -u "${SONAR_AUTH}" \
  -X POST "${SONAR_URL}/api/qualitygates/create" \
  -d "name=${GATE_NAME}" 2>/dev/null || true)

GATE_ID=$(echo "${GATE_RESPONSE}" \
  | grep -o '"id":[0-9]*' | grep -o '[0-9]*' || true)

if [[ -z "${GATE_ID}" ]]; then
  # Gate already exists – look up its ID
  GATE_ID=$(curl -sf -u "${SONAR_AUTH}" \
    "${SONAR_URL}/api/qualitygates/list" \
    | jq -r --arg name "${GATE_NAME}" \
      '.qualitygates[] | select(.name==$name) | .id' 2>/dev/null || true)
fi

if [[ -z "${GATE_ID}" ]]; then
  echo "⚠️   Could not create or locate Quality Gate – skipping gate conditions"
else
  echo "ℹ️   Gate ID: ${GATE_ID}"

  _add_condition() {
    local metric="$1" op="$2" error="$3" label="$4"
    local code
    code=$(curl -s -o /dev/null -w "%{http_code}" \
      -u "${SONAR_AUTH}" \
      -X POST "${SONAR_URL}/api/qualitygates/create_condition" \
      -d "gateId=${GATE_ID}&metric=${metric}&op=${op}&error=${error}")
    if [[ "${code}" == "200" ]]; then
      echo "    ✅  ${label}"
    else
      echo "    ℹ️   ${label} (HTTP ${code} – may already exist)"
    fi
  }

  _add_condition "coverage"                 "LT" "70" "Line coverage >= 70 %"
  _add_condition "duplicated_lines_density" "GT" "5"  "Duplicated lines < 5 %"
  _add_condition "blocker_violations"       "GT" "0"  "No blocker issues"
  _add_condition "critical_violations"      "GT" "0"  "No critical issues"
  _add_condition "reliability_rating"       "GT" "1"  "Reliability rating <= A"
  _add_condition "security_rating"          "GT" "1"  "Security rating <= A"

  curl -sf -u "${SONAR_AUTH}" \
    -X POST "${SONAR_URL}/api/qualitygates/select" \
    -d "gateId=${GATE_ID}&projectKey=${PROJECT_KEY}" > /dev/null \
    && echo "✅  Gate assigned to project '${PROJECT_KEY}'"
fi

# ── Step 5: Generate analysis token ──────────────────────────────────────────
echo ""
echo "──────────────────────────────────────────────────────────────"
echo " Step 5 – Generating SonarQube analysis token"
echo "──────────────────────────────────────────────────────────────"

TOKEN_RESPONSE=$(curl -sf -u "${SONAR_AUTH}" \
  -X POST "${SONAR_URL}/api/user_tokens/generate" \
  -d "name=jenkins-pipeline" 2>/dev/null || true)

SONAR_TOKEN_VALUE=$(echo "${TOKEN_RESPONSE}" \
  | grep -o '"token":"[^"]*"' | cut -d'"' -f4 || true)

if [[ -z "${SONAR_TOKEN_VALUE}" ]]; then
  echo "⚠️   Token could not be generated automatically."
  echo "    → ${SONAR_URL} → My Account → Security → Generate Token"
  echo "    → Paste the value as SONAR_TOKEN in your .env and re-run."
  echo ""
  # Cannot proceed to push token into Jenkins – bail gracefully
  _TOKEN_AVAILABLE=false
else
  echo "✅  Token generated"
  _TOKEN_AVAILABLE=true
fi

# ── Step 6: Push token into Jenkins credentials (no restart needed) ───────────
echo ""
echo "──────────────────────────────────────────────────────────────"
echo " Step 6 – Updating Jenkins credential '${JENKINS_CREDENTIAL_ID}'"
echo "──────────────────────────────────────────────────────────────"

if [[ "${_TOKEN_AVAILABLE}" == "false" ]]; then
  echo "⚠️   Skipping Jenkins update – no token available (see Step 5)."
else
  JENKINS_AUTH="${JENKINS_ADMIN_USERNAME}:${JENKINS_ADMIN_PASSWORD}"

  # ── 6a: Obtain a crumb (CSRF protection) ────────────────────────────────────
  echo "ℹ️   Fetching Jenkins crumb..."
  CRUMB_RESPONSE=$(curl -sf -u "${JENKINS_AUTH}" \
    "${JENKINS_URL}/crumbIssuer/api/json" 2>/dev/null || true)

  CRUMB_FIELD=$(echo "${CRUMB_RESPONSE}" | jq -r '.crumbRequestField // empty' 2>/dev/null || true)
  CRUMB_VALUE=$(echo "${CRUMB_RESPONSE}" | jq -r '.crumb // empty'             2>/dev/null || true)

  if [[ -z "${CRUMB_FIELD}" ]] || [[ -z "${CRUMB_VALUE}" ]]; then
    echo "⚠️   Could not retrieve Jenkins crumb."
    echo "    Jenkins may be starting up or the credentials in .env may be wrong."
    echo "    Skipping automatic Jenkins update."
    echo "    → Update SONAR_TOKEN in .env and restart Jenkins manually:"
    echo "        cd cicd-pipeline && docker compose restart jenkins"
    _JENKINS_UPDATED=false
  else
    echo "✅  Crumb obtained"

    # ── 6b: Execute Groovy script to update the credential in-place ─────────────
    # The script locates the existing StringCredentialsImpl with id=SONAR_TOKEN
    # and replaces its secret — no container restart required.
    GROOVY_SCRIPT=$(cat <<GROOVY
import com.cloudbees.plugins.credentials.SystemCredentialsProvider
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl
import com.cloudbees.plugins.credentials.CredentialsScope
import hudson.util.Secret

def credentialId = '${JENKINS_CREDENTIAL_ID}'
def newSecret    = '${SONAR_TOKEN_VALUE}'

def store  = SystemCredentialsProvider.getInstance().getStore()
def domain = com.cloudbees.plugins.credentials.domains.Domain.global()

// Find the existing credential
def existing = com.cloudbees.plugins.credentials.CredentialsProvider
    .lookupCredentials(StringCredentialsImpl, Jenkins.instance, null, null)
    .find { it.id == credentialId }

if (existing) {
    def updated = new StringCredentialsImpl(
        CredentialsScope.GLOBAL,
        credentialId,
        existing.description,
        Secret.fromString(newSecret)
    )
    store.updateCredentials(domain, existing, updated)
    println "SUCCESS: credential '${credentialId}' updated in-place"
} else {
    println "WARNING: credential '${credentialId}' not found – creating it"
    def created = new StringCredentialsImpl(
        CredentialsScope.GLOBAL,
        credentialId,
        'SonarQube authentication token',
        Secret.fromString(newSecret)
    )
    store.addCredentials(domain, created)
    println "SUCCESS: credential '${credentialId}' created"
}
GROOVY
)

    SCRIPT_OUTPUT=$(curl -s \
      -u "${JENKINS_AUTH}" \
      -H "${CRUMB_FIELD}: ${CRUMB_VALUE}" \
      -X POST "${JENKINS_URL}/scriptText" \
      --data-urlencode "script=${GROOVY_SCRIPT}" 2>/dev/null || true)

    if echo "${SCRIPT_OUTPUT}" | grep -q "^SUCCESS"; then
      echo "✅  ${SCRIPT_OUTPUT}"
      _JENKINS_UPDATED=true
    else
      echo "⚠️   Jenkins script console returned an unexpected response:"
      echo "    ${SCRIPT_OUTPUT}"
      echo ""
      echo "    The token was generated but could not be pushed to Jenkins."
      echo "    → Add it manually to .env as SONAR_TOKEN=${SONAR_TOKEN_VALUE}"
      echo "    → Then restart Jenkins:"
      echo "        cd cicd-pipeline && docker compose restart jenkins"
      _JENKINS_UPDATED=false
    fi
  fi
fi

# ── Step 7: Register SonarQube → Jenkins webhook ─────────────────────────────
echo ""
echo "──────────────────────────────────────────────────────────────"
echo " Step 7 – Registering Jenkins webhook in SonarQube"
echo "──────────────────────────────────────────────────────────────"

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
  -u "${SONAR_AUTH}" \
  -X POST "${SONAR_URL}/api/webhooks/create" \
  -d "name=Jenkins&url=http://jenkins:8080/sonarqube-webhook/")

if [[ "${HTTP_CODE}" == "200" ]]; then
  echo "✅  Jenkins webhook registered"
else
  echo "ℹ️   Webhook may already exist (HTTP ${HTTP_CODE}) – continuing"
fi

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║  ✅  SonarQube setup complete!                               ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "  SonarQube URL : ${SONAR_URL}"
echo "  Username      : admin"
echo "  Password      : (value of SONAR_NEW_PASSWORD in your .env)"
echo ""

if [[ "${_TOKEN_AVAILABLE}" == "true" ]]; then
  if [[ "${_JENKINS_UPDATED:-false}" == "true" ]]; then
    echo "  ✅  SONAR_TOKEN was pushed directly into Jenkins credentials."
    echo "      No restart required – the pipeline is ready to run."
    echo ""
    echo "  ℹ️   Optionally keep your .env in sync by updating:"
    echo "      SONAR_TOKEN=${SONAR_TOKEN_VALUE}"
  else
    echo "  ⚠️   SONAR_TOKEN was generated but could NOT be pushed to Jenkins."
    echo "      Update your .env and restart Jenkins manually:"
    echo ""
    echo "      SONAR_TOKEN=${SONAR_TOKEN_VALUE}"
    echo ""
    echo "      cd cicd-pipeline && docker compose restart jenkins"
  fi
else
  echo "  ⚠️   No token was generated. Follow the manual steps in Step 5 above."
fi

echo ""