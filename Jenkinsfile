#!groovy
@Library('github.com/wooga/atlas-jenkins-pipeline@0.0.1') _

pipeline {
    agent {
        label 'windows&&unity_5.5'
    }

    environment {
        artifactoryCredentials  = credentials('artifactory_publish')
        nugetkey                = credentials('artifactory_deploy')
        TRAVIS_JOB_NUMBER       = "${BUILD_NUMBER}.WIN"
    }

    stages {
        stage('Preparation') {
            steps {
                sendSlackNotification "STARTED", true
            }
        }

        stage('Test') {
            steps {
                bat "gradlew.bat check"
            }
        }
    }

    post {
        always {
            sendSlackNotification currentBuild.result, true
            junit allowEmptyResults: true, testResults: 'build/test-result/**/*.xml'
            deleteDir()
        }
    }
}