import globals.*

/**
 * Chimera
 *
 * TODO: description
 *
 * Mandatory params:
 * - chimeraEnvName
 * - chimeraCredentialsId
 */

def call(Map params){

    def chimeraCredentialsId = params.chimeraCredentialsId
    if (!chimeraCredentialsId) {
        error "'chimeraCredentialsId' is mandatory for Chimera."
        return
    }

    def chimeraEnvName = params.chimeraEnvName
    if (!chimeraEnvName) {
        error """
        'chimeraEnvName' is mandatory for Chimera.
        Possible values: work, live, play, dev
        """
        return
    }

    int timeout = params.chimeraTimeout ?: 0
    try {
        withCredentials([usernamePassword(credentialsId: chimeraCredentialsId, passwordVariable: 'PASS', usernameVariable: 'USER')]) {
            Helpers.loadLibrary("chimera@${Settings.libChimera}")
            def userCredential = USER
            def passCredential = PASS
            def reviewResult = chimeraCodeReview.codeReview {
                user = userCredential
                password = passCredential
                repo = Settings.repo.GIT_URL
                branch = Settings.repo.GIT_BRANCH
                projectId = getProjectId(Settings.repo.GIT_URL)
                time = timeout
                environment = chimeraEnvName
            }
            Helpers.log.info("Chimera :: Result :: '${reviewResult}'")
        }
        Modules.vtrack.registerBuildAction('chimera', true)
    } catch(Exception ex){
        Modules.vtrack.registerBuildAction('chimera', false)
        error "Catched Chimera Module Exception: ${ex}"
    }
}

def getProjectId(String gitUrl) {
    gitUrl.split('.git')[1].split('/')[1].toLowerCase()
}

/**
 * When called automatically from within a step, this will be invoked.
 * Use it to massage parameters before sending them to the 'call' method
 * @param stepArgs branch dependent parameters
 */
def fromStep(Map stepArgs) {
  call(stepArgs)
}

//* Used during testing to return a callable script
return this
