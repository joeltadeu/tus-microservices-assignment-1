# Patient Management Service – CI/CD Pipeline

> Spring Boot microservice with a fully automated Jenkins pipeline triggered by GitHub webhooks on every push/merge to `master`.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [CI/CD Architecture](#2-cicd-architecture)
3. [Prerequisites](#3-prerequisites)
4. [Secrets Management](#4-secrets-management)
5. [Start the Stack](#5-start-the-stack)
6. [Expose Jenkins with ngrok](#6-expose-jenkins-with-ngrok)
7. [Configure the GitHub Webhook](#7-configure-the-github-webhook)
8. [Verify Automatic Triggering](#8-verify-automatic-triggering)
9. [Pipeline Stages](#9-pipeline-stages)
10. [SonarQube Setup](#10-sonarqube-setup)
11. [Troubleshooting](#11-troubleshooting)

---

## 1. Project Overview

| Component | Technology | Port |
|-----------|-----------|------|
| Application | Spring Boot 3 / Java 21 | 9081 |
| CI/CD server | Jenkins (Docker) | 8080 |
| Code quality | SonarQube 10 Community | 9000 |
| Database (Sonar) | PostgreSQL 15 | 5432 (internal) |
| Tunnel | ngrok | dynamic |

---

## 2. CI/CD Architecture

```
Developer pushes / merges to master
        |
        v
   GitHub repository
        |  POST /github-webhook/
        v
   ngrok public URL  ──────────────────────►  Jenkins :8080
                                                   |
                          ┌────────────────────────┤
                          v                        |
                     Jenkinsfile              8 Stages
                     (in repo)               ────────
                                             1. Checkout
                                             2. Build
                                             3. Unit Tests + JaCoCo
                                             4. SonarQube Analysis
                                             5. Quality Gate
                                             6. Integration / E2E (Karate)
                                             7. Docker Build
                                             8. Deploy (local)
```

---

## 3. Prerequisites

| Tool | Version | Install |
|------|---------|---------|
| Docker + Docker Compose | 24+ | https://docs.docker.com/get-docker/ |
| ngrok | any | https://ngrok.com/download |
| Git | any | https://git-scm.com |

---

## 4. Secrets Management

All sensitive values are stored in a local `.env` file that is **never committed to git**.

### 4.1 Create your `.env`

```bash
cp .env.example .env
```

### 4.2 Fill in the values

| Variable | Where to get it |
|----------|----------------|
| `GITHUB_USERNAME` | Your GitHub username |
| `GITHUB_TOKEN` | github.com/settings/tokens → Classic token → scopes: `repo`, `admin:repo_hook` |
| `GITHUB_WEBHOOK_SECRET` | Run locally: `openssl rand -hex 32` |
| `SONAR_TOKEN` | http://localhost:9000 → My Account → Security (after first login) |
| `JENKINS_ADMIN_PASSWORD` | Any strong password |

### 4.3 How secrets reach Jenkins

```
.env file
  └─► Docker Compose reads it at startup
        └─► Injects vars into Jenkins container environment
              └─► JCasC casc.yaml interpolates placeholders like ${GITHUB_TOKEN}
                    └─► Jenkins credentials store (encrypted at rest)
```

No secrets ever touch the files committed to git. `casc.yaml` only contains placeholder names, never real values.

### 4.4 What is committed vs what is not

| File | Committed | Contains secrets? |
|------|-----------|------------------|
| `cicd-pipeline/jenkins/casc.yaml` | Yes | No – only placeholder names |
| `.env.example` | Yes | No – fake placeholder values |
| `.env` | **Never** | Yes – real secrets live here |

The `.gitignore` already excludes `.env`.

---

## 5. Start the Stack

```bash
cd cicd-pipeline
docker compose up -d --build

# Follow logs (optional)
docker compose logs -f jenkins
```

Wait approximately 2 minutes for SonarQube to become healthy before Jenkins is ready.

| Service | URL | Credentials |
|---------|-----|-------------|
| Jenkins | http://localhost:8080 | admin / your JENKINS_ADMIN_PASSWORD |
| SonarQube | http://localhost:9000 | admin / admin |

---

## 6. Expose Jenkins with ngrok

GitHub needs a public URL to reach your local Jenkins. ngrok creates a secure tunnel from the internet to localhost.

### 6.1 Authenticate ngrok (one-time)

```bash
# Sign up free at https://ngrok.com, copy your authtoken from the dashboard
ngrok config add-authtoken <YOUR_NGROK_AUTHTOKEN>
```

### 6.2 Start the tunnel

```bash
chmod +x scripts/ngrok-webhook.sh
./scripts/ngrok-webhook.sh
```

The script starts ngrok on port 8080 and prints:

```
Tunnel active!
  Public URL  : https://a1b2-203-0-113-5.ngrok-free.app
  Webhook URL : https://a1b2-203-0-113-5.ngrok-free.app/github-webhook/
```

Copy the **Webhook URL** – you will use it in the next step.

> **Important:** The free ngrok URL changes every time you restart the tunnel. Update the GitHub webhook URL whenever you restart ngrok, or upgrade to a paid ngrok plan to get a static domain.

---

## 7. Configure the GitHub Webhook

1. Open your repository on GitHub.
2. Go to **Settings → Webhooks → Add webhook**.
3. Fill in the form:

| Field | Value |
|-------|-------|
| Payload URL | The Webhook URL printed by ngrok-webhook.sh |
| Content type | `application/json` |
| Secret | Value of `GITHUB_WEBHOOK_SECRET` from your `.env` |
| Which events | **Just the push event** |
| Active | checked |

4. Click **Add webhook**.

GitHub immediately sends a ping event. A green tick confirms Jenkins received it.

### Verify the ping in Jenkins logs

**Manage Jenkins → System Log → All Jenkins Logs** should contain:

```
Received POST for https://github.com/<your-repo>
```

---

## 8. Verify Automatic Triggering

```bash
# Make any small change and push to master
echo "# trigger" >> trigger.txt
git add trigger.txt
git commit -m "chore: test webhook trigger"
git push origin master
```

Within seconds a new build appears at:

`http://localhost:8080/job/pmanagement-service-pipeline/`

---

## 9. Pipeline Stages

| # | Stage | What it does |
|---|-------|-------------|
| 1 | Checkout | Clones repo; prints branch, commit, author |
| 2 | Build | `mvn clean package` (no tests); archives JAR |
| 3 | Unit Tests | `mvn test`; JUnit + JaCoCo coverage report |
| 4 | Code Analysis | SonarQube scanner with coverage XML |
| 5 | Quality Gate | Waits up to 5 min; aborts pipeline on failure |
| 6 | Integration & E2E | `mvn verify`; Karate HTML report |
| 7 | Docker Build | Image tagged `pmanagement-service:<BUILD>` and `:latest` |
| 8 | Deploy (Local) | `scripts/deploy.sh`; app on port 9081 |

JaCoCo coverage thresholds:

| Metric | Minimum |
|--------|---------|
| Line | 70% |
| Branch | 60% |
| Method | 70% |
| Class | 70% |

---

## 10. SonarQube Setup

Run once after the stack is up:

```bash
chmod +x scripts/sonar-setup.sh
./scripts/sonar-setup.sh
```

Then generate a token at **http://localhost:9000 → My Account → Security**, update `SONAR_TOKEN` in `.env`, and restart Jenkins:

```bash
cd cicd-pipeline && docker compose restart jenkins
```

Register the SonarQube → Jenkins callback webhook so the Quality Gate result reaches Jenkins:

```bash
chmod +x scripts/sonar-webhook.sh
./scripts/sonar-webhook.sh
```

---

## 11. Troubleshooting

**Webhook returns 302 / redirect**
Make sure the Payload URL ends with `/github-webhook/` (trailing slash is required by the Jenkins GitHub plugin).

**Build not triggered after push**
1. Verify ngrok is running: `curl http://localhost:4040/api/tunnels`
2. Check delivery log in GitHub: Settings → Webhooks → Recent Deliveries
3. Check Jenkins log: Manage Jenkins → System Log

**SonarQube Quality Gate always pending**
The SonarQube webhook back to Jenkins is not registered. Run `./scripts/sonar-webhook.sh`.

**ngrok URL changed after restart**
Update the Payload URL in the GitHub webhook settings, or use a paid ngrok static domain.

**Docker permission denied inside Jenkins**
The host Docker socket must be mounted. Check that `/var/run/docker.sock:/var/run/docker.sock` is in `cicd-pipeline/docker-compose.yml`.

---

## Repository Structure

```
.
├── Dockerfile                   App Docker image
├── Jenkinsfile                  Declarative pipeline (8 stages)
├── .env.example                 Secret template – copy to .env, never commit .env
├── cicd-pipeline/
│   ├── docker-compose.yml       Jenkins + SonarQube stack
│   └── jenkins/
│       ├── Dockerfile           Jenkins image with plugins
│       ├── casc.yaml            Jenkins Configuration as Code (no secrets)
│       └── plugins.txt          Plugin list
├── scripts/
│   ├── deploy.sh                Local Docker deployment
│   ├── ngrok-webhook.sh         Start ngrok and print webhook URL
│   ├── sonar-setup.sh           First-time SonarQube configuration
│   └── sonar-webhook.sh         Register SonarQube → Jenkins webhook
└── src/                         Application source code
```
