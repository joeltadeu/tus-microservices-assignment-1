#!/usr/bin/env bash
set -euo pipefail

# ── Resolve paths ─────────────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${REPO_ROOT}/.env"

# ── Step 0: Load .env ─────────────────────────────────────────────────────────
echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║              sonar-token.sh – Generate Token                 ║"
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

# ── Step 1: Generate analysis token ──────────────────────────────────────────
echo ""
echo "──────────────────────────────────────────────────────────────"
echo " Step 1 – Generating SonarQube analysis token"
echo "──────────────────────────────────────────────────────────────"

SONAR_AUTH="admin:${SONAR_NEW_PASSWORD}"

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
  _TOKEN_AVAILABLE=false
else
  echo "✅  Token generated"
  _TOKEN_AVAILABLE=true
fi

# ── Step 2: Push token into Jenkins credentials (no restart needed) ───────────
echo ""
echo "──────────────────────────────────────────────────────────────"
echo " Step 2 – Updating Jenkins credential '${JENKINS_CREDENTIAL_ID}'"
echo "──────────────────────────────────────────────────────────────"

if [[ "${_TOKEN_AVAILABLE}" == "false" ]]; then
  echo "⚠️   Skipping Jenkins update – no token available (see Step 1)."
else
  JENKINS_AUTH="${JENKINS_ADMIN_USERNAME}:${JENKINS_ADMIN_PASSWORD}"

  # ── Obtain a crumb (CSRF protection) ────────────────────────────────────────
  echo "ℹ️   Fetching Jenkins crumb..."

  COOKIE_JAR=$(mktemp)

  CRUMB_RESPONSE=$(curl -sf -u "${JENKINS_AUTH}" \
    -c "${COOKIE_JAR}" \
    "${JENKINS_URL}/crumbIssuer/api/json" 2>/dev/null || true)

  CRUMB_FIELD=$(echo "${CRUMB_RESPONSE}" | jq -r '.crumbRequestField // empty' 2>/dev/null || true)
  CRUMB_VALUE=$(echo "${CRUMB_RESPONSE}" | jq -r '.crumb // empty'             2>/dev/null || true)

  if [[ -z "${CRUMB_FIELD}" ]] || [[ -z "${CRUMB_VALUE}" ]]; then
    echo "⚠️   Could not retrieve Jenkins crumb."
    echo "    Jenkins may be starting up or the credentials in .env may be wrong."
    echo "    Skipping automatic Jenkins update."
    echo "    → Update SONAR_TOKEN in .env and restart Jenkins manually:"
    echo "        cd cicd-pipeline && docker compose restart jenkins"
    rm -f "${COOKIE_JAR}"
    _JENKINS_UPDATED=false
  else
    echo "✅  Crumb obtained"

    # ── Execute Groovy script to update the credential in-place ─────────────
    # Bash expands ${JENKINS_CREDENTIAL_ID} and ${SONAR_TOKEN_VALUE} here.
    # All other $ references are Groovy variables — escaped so bash ignores them.
    GROOVY_SCRIPT=$(cat <<GROOVY
import com.cloudbees.plugins.credentials.SystemCredentialsProvider
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl
import com.cloudbees.plugins.credentials.CredentialsScope
import hudson.util.Secret

def credentialId = '${JENKINS_CREDENTIAL_ID}'
def newSecret    = '${SONAR_TOKEN_VALUE}'

def store  = SystemCredentialsProvider.getInstance().getStore()
def domain = com.cloudbees.plugins.credentials.domains.Domain.global()

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
    println "SUCCESS: credential '${JENKINS_CREDENTIAL_ID}' updated in-place"
} else {
    println "WARNING: credential '${JENKINS_CREDENTIAL_ID}' not found – creating it"
    def created = new StringCredentialsImpl(
        CredentialsScope.GLOBAL,
        credentialId,
        'SonarQube authentication token',
        Secret.fromString(newSecret)
    )
    store.addCredentials(domain, created)
    println "SUCCESS: credential '${JENKINS_CREDENTIAL_ID}' created"
}
GROOVY
)

    SCRIPT_OUTPUT=$(curl -s \
      -u "${JENKINS_AUTH}" \
      -b "${COOKIE_JAR}" \
      -H "${CRUMB_FIELD}: ${CRUMB_VALUE}" \
      -X POST "${JENKINS_URL}/scriptText" \
      --data-urlencode "script=${GROOVY_SCRIPT}" 2>/dev/null || true)

    rm -f "${COOKIE_JAR}"

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

# ── Step 3: Update SONAR_TOKEN in .env ───────────────────────────────────────
echo ""
echo "──────────────────────────────────────────────────────────────"
echo " Step 3 – Updating SONAR_TOKEN in .env"
echo "──────────────────────────────────────────────────────────────"

if [[ "${_TOKEN_AVAILABLE}" == "true" ]]; then
  if grep -q '^SONAR_TOKEN=' "${ENV_FILE}"; then
    sed -i "s|^SONAR_TOKEN=.*|SONAR_TOKEN=${SONAR_TOKEN_VALUE}|" "${ENV_FILE}"
    echo "✅  SONAR_TOKEN updated in .env"
  else
    echo "SONAR_TOKEN=${SONAR_TOKEN_VALUE}" >> "${ENV_FILE}"
    echo "✅  SONAR_TOKEN appended to .env"
  fi
else
  echo "⚠️   Skipping .env update – no token was generated."
fi