// ─────────────────────────────────────────────────────────────────────────────
// Patient Management Service – Declarative Pipeline
// GitHub: https://github.com/joeltadeu/tus-microservices-assignment-1
// Stages: Checkout → Build → Unit Tests → Code Analysis → Quality Gate →
//         Integration + E2E Tests → Docker Build → Deploy Local
// ─────────────────────────────────────────────────────────────────────────────

pipeline {

    agent any

    tools {
        maven 'Maven 3'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        skipStagesAfterUnstable()
    }

    environment {
        GITHUB_REPO      = 'https://github.com/joeltadeu/tus-microservices-assignment-1.git'
        GITHUB_BRANCH    = 'master'
        APP_NAME         = 'pmanagement-service'
        APP_IMAGE        = "pmanagement-service:${BUILD_NUMBER}"
        APP_IMAGE_LATEST = 'pmanagement-service:latest'
        APP_PORT         = '9081'
        SONAR_HOST_URL   = 'http://sonarqube:9000'
        SONAR_PROJECT    = 'pmanagement-service'
        SONAR_TOKEN      = credentials('SONAR_TOKEN')
        MAVEN_OPTS       = '-Xmx1g'
    }

    stages {

        // ── 1. CHECKOUT ──────────────────────────────────────────────────────
        stage('Checkout') {
            steps {
                echo "==> Cloning ${GITHUB_REPO} [${GITHUB_BRANCH}]"
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: "*/${GITHUB_BRANCH}"]],
                    userRemoteConfigs: [[
                        url: "${GITHUB_REPO}",
                        credentialsId: 'GITHUB_CREDENTIALS'
                    ]],
                    extensions: [
                        [$class: 'CleanBeforeCheckout'],
                        [$class: 'CloneOption', shallow: false, timeout: 10]
                    ]
                ])
                sh '''
                    echo "──────────────────────────────────────────────"
                    echo "Branch : $(git rev-parse --abbrev-ref HEAD)"
                    echo "Commit : $(git rev-parse HEAD)"
                    echo "Author : $(git log -1 --format='%an <%ae>')"
                    echo "Message: $(git log -1 --format='%s')"
                    echo "──────────────────────────────────────────────"
                    git log --oneline -5
                '''
            }
        }

        // ── 2. BUILD ─────────────────────────────────────────────────────────
        stage('Build') {
            steps {
                echo '==> Compiling and packaging application'
                sh '''
                    mvn clean package \
                        -DskipTests \
                        --batch-mode \
                        --no-transfer-progress
                '''
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                    echo '==> JAR archived: target/pmanagement-service.jar'
                }
            }
        }

        // ── 3. UNIT TESTS ────────────────────────────────────────────────────
        stage('Unit Tests') {
            steps {
                echo '==> Running JUnit unit tests with JaCoCo coverage'
                sh '''
                    mvn test \
                        --batch-mode \
                        --no-transfer-progress \
                        -Dsurefire.failIfNoSpecifiedTests=false
                '''
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml',
                          allowEmptyResults: false

                    jacoco(
                        execPattern:      'target/jacoco.exec',
                        classPattern:     'target/classes',
                        sourcePattern:    'src/main/java',
                        exclusionPattern: '**/*Config.class,**/*Constants.class,**/Application.class',
                        minimumLineCoverage:   '70',
                        minimumBranchCoverage: '60',
                        minimumMethodCoverage: '70',
                        minimumClassCoverage:  '70',
                        changeBuildStatus: true
                    )

                    // ← ADDED: archive JaCoCo HTML report as a build artifact
                    archiveArtifacts artifacts: 'target/site/jacoco/**/*',
                                     fingerprint: false,
                                     allowEmptyArchive: true
                }
            }
        }

        // ── 4. STATIC CODE ANALYSIS ──────────────────────────────────────────
        stage('Code Analysis (SonarQube)') {
            steps {
                echo '==> Running SonarQube static analysis'
                withSonarQubeEnv('SonarQube') {
                    sh """
                        mvn sonar:sonar \
                            --batch-mode \
                            --no-transfer-progress \
                            -Dsonar.projectKey=${SONAR_PROJECT} \
                            -Dsonar.projectName="Patient Management Service" \
                            -Dsonar.projectVersion=${BUILD_NUMBER} \
                            -Dsonar.host.url=${SONAR_HOST_URL} \
                            -Dsonar.token=${SONAR_TOKEN} \
                            -Dsonar.java.coveragePlugin=jacoco \
                            -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
                            -Dsonar.exclusions=**/config/**,**/constants/**,**/Application.java \
                            -Dsonar.qualitygate.wait=false
                    """
                }
            }
        }

        // ── 5. QUALITY GATE ──────────────────────────────────────────────────
        stage('Quality Gate') {
            steps {
                echo '==> Waiting for SonarQube Quality Gate result'
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        // ── 6. INTEGRATION & E2E TESTS (Karate) ─────────────────────────────
        stage('Integration & E2E Tests') {
            steps {
                echo '==> Running integration tests and Karate E2E tests'
                sh '''
                    mvn verify \
                        -DskipUnitTests=true \
                        --batch-mode \
                        --no-transfer-progress \
                        -Dkarate.options="--tags ~@ignore" \
                        -Dkarate.env=e2e
                '''
            }
            post {
                always {
                    junit testResults: 'target/failsafe-reports/*.xml',
                          allowEmptyResults: true

                    // ← ADDED: archive Karate HTML report as a build artifact
                    archiveArtifacts artifacts: 'target/karate-reports/**/*',
                                     fingerprint: false,
                                     allowEmptyArchive: true
                }
            }
        }

        // ── 7. DOCKER BUILD ──────────────────────────────────────────────────
        stage('Docker Build') {
            steps {
                echo "==> Building Docker image: ${APP_IMAGE}"
                sh """
                    docker build \
                        --no-cache \
                        -t ${APP_IMAGE} \
                        -t ${APP_IMAGE_LATEST} \
                        .
                """
                sh "docker images | grep pmanagement-service"
            }
        }

        // ── 8. LOCAL DEPLOYMENT ──────────────────────────────────────────────
        stage('Deploy (Local)') {
            steps {
                echo '==> Deploying container locally'
                sh """
                    chmod +x scripts/deploy.sh
                    bash scripts/deploy.sh "${APP_IMAGE}" "${APP_PORT}"
                """
            }
            post {
                success {
                    echo "==> App running at http://localhost:${APP_PORT}"
                    echo "==> Swagger UI : http://localhost:${APP_PORT}/swagger-ui.html"
                    echo "==> Health     : http://localhost:${APP_PORT}/actuator/health"
                }
            }
        }
    }

    post {
        success {
            echo """
╔══════════════════════════════════════════════╗
║  ✅  PIPELINE SUCCEEDED – Build #${BUILD_NUMBER}
║  Image : ${APP_IMAGE}
║  App   : http://localhost:${APP_PORT}
╚══════════════════════════════════════════════╝
"""
        }
        failure {
            echo """
╔══════════════════════════════════════════════╗
║  ❌  PIPELINE FAILED – Build #${BUILD_NUMBER}
║  Check the stage logs above for details.
╚══════════════════════════════════════════════╝
"""
        }
        always {
            cleanWs()
        }
    }
}