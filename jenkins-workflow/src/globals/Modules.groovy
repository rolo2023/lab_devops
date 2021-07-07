package globals

import modules.*


class Modules {

    static def git = null
    static def bitbucket = null
    static def ephemerals = null
    static def artifactory = null
    static def vtrack = null
    static def samuel = null

    static def load(moduleName, config = [:]) {
        switch (moduleName) {
            case 'samuel':
                samuel = SamuelModule.newCps()
                break
            case 'ephemerals':
                ephemerals = Ephemerals.newCps()
                break
            case 'artifactory':
                artifactory = new ArtifactoryModule().newCps()
                break
            case 'vtrack':
                vtrack = VtrackModule.newCps()
                break
            case 'git':
                git = GitModule.newCps()
                break
            case 'bitbucket':
                bitbucket = BitbucketModule.newCps()
                break
            default:
                Helpers.log.info("Modules :: load('${moduleName}') :: NON EXISTING MODULE")
        }
    }
}
