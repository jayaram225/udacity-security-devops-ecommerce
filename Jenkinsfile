pipeline {
    agent any

    tools {
        maven 'Maven'
    }

    stages {

        stage('Build') {
            steps {
                dir('starter_code') {
                    bat 'mvn clean compile'
                }
            }
        }

        stage('Test') {
            steps {
                dir('starter_code') {
                    bat 'mvn test'
                }
            }
        }

        stage('Package') {
            steps {
                dir('starter_code') {
                    bat 'mvn package -DskipTests'
                }
            }
        }
    }

    post {

        always {
            junit allowEmptyResults: true,
                    testResults: 'starter_code/target/surefire-reports/*.xml'
        }

        success {
            archiveArtifacts artifacts: 'starter_code/target/*.war',
                    fingerprint: true
        }
    }
}