/**
 * Since we are using Cachelo, make sure the master has the relevant plugins installed:
 * https://globaldevtools.bbva.com/bitbucket/projects/BGP/repos/cachelo/browse?at=refs%2Ftags%2F1.1.0
 */
@Library (['workflowlibs@1.25', 'cachelo@1.3.0', 'sonar@lts']) _


pipeline {
    options {
        ansiColor('xterm')
        preserveStashes(buildCount: 50)
        timestamps()
        timeout(time: 1, unit: 'HOURS')
    }
    agent {
        kubernetes {
            yamlFile './resources/pod_def_pipe.yml'
        }
    }
    stages {
        stage('Clone') {
            steps {
                container('maven-builder') {
                    cleanWs()
                    script {
                        repo {
                            credentialsId = 'bot-globaldevops-pro-ssh'
                            shallow = false
                        }
                    }
                }
            }
        }
        stage('Test') {
            steps {
                container('maven-builder') {
                    downloadCache key: "jenkins-workflow-cache-key"
                    sh "./gradlew clean test -i"
                    uploadCache key: "jenkins-workflow-cache-key", paths: ["${HOME}/.gradle"]
                }
            }
        }
        stage('Sonar') {
            steps {
                container('maven-builder') {
                    sonar(['qualityProfile': 'jenkins-workflowlibs',
                            'qualityGate': null,
                            'waitForQualityGate': false], {
                        sh "./gradlew -i sonarqube"
                    })
                }
            }
        }
        stage('Set new version') {
            when {
                expression {
                    repo.hasSemVer()
                }
            }
            steps {
                container('maven-builder') {
                    script {
                        repo.pushSemVer()
                    }
                }
            }
        }
    }
}
