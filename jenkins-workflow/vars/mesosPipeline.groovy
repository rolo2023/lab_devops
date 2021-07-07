def call(Map config) {
    def ciFlow = new ci.Ci()
    def settings = new globals.Settings()

    pipeline {
        options {
            ansiColor('xterm')
            buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '3'))
            timestamps()
            timeout(time: config.timeoutInHours, unit: 'HOURS')
        }

        agent none

        stages {
            stage('Pipeline engine start-up') {
                steps {
                    node('generic') {
                        script {
                            ciFlow.prepareConfiguration(config)
                        }
                    }
                }
            }
            stage('Creating deliverable') {
                steps {
                    node(globals.Settings.build.get('node_label', 'generic')) {
                        script {
                            ciFlow.executeBuild()
                        }
                    }
                }
            }
            stage('Deploy') {
                steps {
                    node(globals.Settings.deploy.get('node_label', 'generic')) {
                        script {
                            ciFlow.executeDeploy()
                        }
                    }
                }
            }
        }

        post {
            always {
                node('generic') {
                    script {
                        ciFlow.executeEndSteps()
                    }
                }
            }
        }
    }
}

return this
