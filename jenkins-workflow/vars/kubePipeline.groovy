def call(Map config) {
    def ciFlow = new ci.Ci()
    def settings = new globals.Settings()

    pipeline {
        options {
            ansiColor('xterm')
            preserveStashes(buildCount: 50)
            timestamps()
            timeout(time: config.timeoutInHours, unit: 'HOURS')
        }

        agent {
            kubernetes {
                label 'devops_pod_template'
                yaml libraryResource('pod_def.yml')
            }
        }

        stages {
            stage('Engine start-up') {
                steps {
                    container('devops-global-generic') {
                        script {
                            ciFlow.prepareConfiguration(config)
                        }
                    }
                }
            }

            stage('Create deliverable') {
                agent {
                    kubernetes {
                        label ciFlow.getBuildAgentLabel()
                        yaml loadPodDefinitions(ciFlow.getBuildSettings())
                    }
                }
                steps {
                    script {
                        ciFlow.executeBuild()
                    }
                }
            }

            stage('Deploy') {
                agent {
                    kubernetes {
                        label ciFlow.getDeployAgentLabel()
                        yaml loadPodDefinitions(ciFlow.getDeploySettings())
                    }
                }
                steps {
                    script {
                        ciFlow.executeDeploy()
                    }
                }
            }
        }

        post {
            always {
                script {
                    ciFlow.executeEndSteps()
                }
            }
        }
    }
}

return this
