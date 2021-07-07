import globals.*

/**
 * wSonar
 *
 * Wrapper over the Sonar library, so it can be run anywhere
 *
 * Parameters:
 * - useCredentialsArtifactory
 * - versionLibSonar
 * - command
 * - qualityGate
 * - qualityProfile
 * - enableIssuesReport
 * - enableQgReport
 * - waitForQualityGate
 * - parameters
 */
def call(Map config=[:]) {
    try {
        def useCredentialsArtifactory   = config.useCredentialsArtifactory ?: false
        def versionLibSonar             = config.versionLibSonar ?: "${Settings.libSonar}"

        Helpers.loadLibrary("sonar@${versionLibSonar}")
        Helpers.log.debug "Sonar :: version libray sonar loaded? :: ${versionLibSonar}"

        if (useCredentialsArtifactory) {
            List envList = []

            def credId = Settings.modules.artifactory.credentialsId
            withCredentials([usernamePassword(
                        credentialsId: credId,
                        usernameVariable: "ARTIFACTORY_CREDENTIALS_USR",
                        passwordVariable: "ARTIFACTORY_CREDENTIALS_PSW"
                        )]){
                envList << "ARTIFACTORY_CREDENTIALS_USR=${this."ARTIFACTORY_CREDENTIALS_USR"}"
                envList << "ARTIFACTORY_CREDENTIALS_PSW=${this."ARTIFACTORY_CREDENTIALS_PSW"}"
                withEnv(envList) { callSonar(config, envList) }
            }
        } else {
            callSonar(config)
        }

        Modules.vtrack.registerBuildAction('sonar', true)
    } catch (Exception sonarException) {
        Modules.vtrack.registerBuildAction('sonar', false)
        error "SONAR :: call(${config}) :: Error: ${sonarException}"
    }
}

private void callSonar(Map config, List envList=[]) {
    def sonarCommand                = "${config.command ?: 'sonar-scanner'} ${config.parameters ?: ''}"
    def qualityGate                 = config.qualityGate ?: null
    def qualityProfile              = config.qualityProfile ?: null
    def enableIssuesReport          = config.containsKey('enableIssuesReport') ? config.enableIssuesReport: true
    def enableQgReport              = config.containsKey('enableQgReport') ? config.enableQgReport: true
    def waitForQualityGate          = config.waitForQualityGate ?: false

    withEnv(envList){
        sonar([
            'qualityProfile': qualityProfile,
            'qualityGate': qualityGate,
            'enableIssuesReport': enableIssuesReport,
            'enableQgReport': enableQgReport,
            'waitForQualityGate': waitForQualityGate], {
                sh sonarCommand
        })
    }
}

/**
 * When called automatically from within a step, this will be invoked.
 * Use it to sanitize parameters before sending them to the 'call' method
 * @param stepArgs branch dependent parameters
 */
def fromStep(Map stepArgs) {
    this.call(stepArgs)
}

return this
