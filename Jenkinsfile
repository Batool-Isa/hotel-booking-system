pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    parameters {
        booleanParam(name: 'RUN_TESTS', defaultValue: false, description: 'Run the Maven tests before building')
    }

    environment {
        AWS_REGION   = 'us-east-1'
        ECR_HOST     = '682579208969.dkr.ecr.us-east-1.amazonaws.com'
        REGISTRY     = "${ECR_HOST}/vibestay"
        TAG          = "${BUILD_NUMBER}"
        APP_HOST     = '172.31.19.178'
        MODULES      = 'services/booking-service,services/billing-service'
        SERVICES     = 'api-gateway user-service hotel-service booking-service billing-service notification-service'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'git log --oneline -1'
            }
        }

        stage('Unit tests (JUnit)') {
            when { expression { params.RUN_TESTS } }
            steps {
                sh '''
                    docker run --rm -e MODULES="$MODULES" -v "$PWD":/workspace -v maven-repo:/root/.m2 -w /workspace \
                        maven:3.9-eclipse-temurin-17 sh -c '
                            mvn -B test -pl $MODULES -Dtest=!RunCucumberTest -Dsurefire.failIfNoSpecifiedTests=false
                            status=$?
                            chown -R '"$(id -u):$(id -g)"' /workspace
                            exit $status
                        '
                '''
            }
        }

        stage('BDD tests (Cucumber)') {
            when { expression { params.RUN_TESTS } }
            steps {
                sh '''
                    docker run --rm -e MODULES="$MODULES" -v "$PWD":/workspace -v maven-repo:/root/.m2 -w /workspace \
                        maven:3.9-eclipse-temurin-17 sh -c '
                            mvn -B test -pl $MODULES -Dtest=RunCucumberTest -Dsurefire.failIfNoSpecifiedTests=false
                            status=$?
                            chown -R '"$(id -u):$(id -g)"' /workspace
                            exit $status
                        '
                '''
            }
        }

        stage('Build images') {
            steps {
                sh '''
                    set -e
                    for svc in $SERVICES; do
                        docker build -f services/$svc/Dockerfile -t $REGISTRY/$svc:$TAG .
                    done
                    docker build -t $REGISTRY/frontend:$TAG frontend
                '''
            }
        }

        stage('Push to ECR') {
            steps {
                sh '''
                    set -e
                    aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR_HOST
                    for svc in $SERVICES frontend; do
                        docker push $REGISTRY/$svc:$TAG
                    done
                '''
            }
        }

        stage('Deploy') {
            steps {
                sshagent(credentials: ['app-server-ssh']) {
                    sh '''
                        ssh -o StrictHostKeyChecking=accept-new ubuntu@$APP_HOST "
                            set -e
                            cd ~/hotel-booking-system
                            git pull --ff-only
                            sed -i '/^REGISTRY=/d;/^TAG=/d' .env
                            echo >> .env
                            echo REGISTRY=$REGISTRY >> .env
                            echo TAG=$TAG >> .env
                            aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR_HOST
                            docker compose pull
                            docker compose up -d --no-build
                        "
                    '''
                }
            }
        }

        stage('Smoke test') {
            steps {
                sh '''
                    for i in $(seq 1 30); do
                        if curl -fsS -o /dev/null http://$APP_HOST/; then
                            echo "site is up"
                            exit 0
                        fi
                        sleep 5
                    done
                    echo "site did not come up"
                    exit 1
                '''
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: 'services/*/target/surefire-reports/*.xml'
            sh 'docker image prune -f || true'
        }
        success {
            echo "Deployed build ${BUILD_NUMBER} (tag ${TAG})"
        }
        failure {
            echo "Build ${BUILD_NUMBER} failed"
        }
    }
}
