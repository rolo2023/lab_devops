package ci.stages
import globals.*
import jenkins.Pipeline
import ci.WorkflowStep

static BuildCps newCps() { return new BuildCps() }

class BuildCps extends Pipeline {

    def stageMergeParent(){
        try{
            Modules.git.run(
                """
                    git checkout -b ${Settings.parent}
                    git fetch origin ${Settings.parent}
                    git checkout ${Settings.repo.GIT_BRANCH}
                    git merge ${Settings.parent}
                """
            )
        } catch (Exception _) { Helpers.error _ }
    }

    def stageBuildApplication(){
        def buildSuccessful = false
        try {
            buildSuccessful = WorkflowStep.newCps(Settings.build).executeStep()
            Modules.vtrack.updateVtrackBuildResults(true)
        } catch (any) {
            Modules.vtrack.updateVtrackBuildResults(false)
            Helpers.error(any)
        }

        buildSuccessful
    }

    def stageArchive(){
        Helpers.log.debug("Archive settings: ${Settings.build.keep.toString()}")
        def files = findFiles glob: Settings.build.keep.files
        def noArtifactMsg = "No file matches ${Settings.build.keep.files}"
        if (files.length > 0) {
            archiveArtifacts artifacts: Settings.build.keep.files, fingerprint: false
        } else if (Settings.build.keep.greedy) {
            echo "${noArtifactMsg}, but greedy is enabled: getting **/*.*"
            archiveArtifacts artifacts: "**/*.*", fingerprint: false
        } else {
            error noArtifactMsg
        }
    }

    def stageStash(){
        Settings.build.stash.each {
            if( (!it.files) || it.files in ['**/*', '**']){
                Helpers.error 'Cowardly refusing to stash the whole workspace'
            } else {
                stash includes: it.files, name: it.id, allowEmpty: true
            }
        }
    }

    /**
     * This stage will push the tag for current version, and check if
     *  more tags should be created as well:
     * - a RC0 tag if this is this is a new patch (e.g., 3.1.0)
     * - a rolling release tag if this is a new minor (e.g., from 2.7.1 to 2.7.2, 2.7 will be retagged to point to 2.7.2)
     *
     * @see "https://platform.bbva.com/en-us/developers/dev-tools/devops-clan/documentation/build-deploy/build-deploy-devops-guides/jenkins-enterprise/jenkins-pipelines/platform-shared-libraries-changelog"
     */
    private def stageSemverTagging() {
        Modules.git.repo.pushSemVer()
    }

    void executePreBuildBehaviours() {
        Modules.samuel.executeSamuelStage 'pre-build'
    }

    void setupBuild() {
        Modules.git.cloneRepo()
        Modules.vtrack.initVtrackBuildMap()
    }

    void execute(){
        Helpers.log.info('Starting Build')
        Settings.currentStage = 'build'

        if (Modules.git.isBranchMergeable()) { stageMergeParent() }

        // Build if there are build steps defined
        if (shouldExecute()) { stageBuildApplication() }

        // Push tags
        if (Modules.git.isTagged()) { stageSemverTagging() }

        // Store in Jenkins
        if((Settings.build.keep) && (Settings.branch in Settings.build.keep.branches)){
           stageArchive()
        }

        // Share files between stages
        if(Settings.build.stash) { stageStash() }

        // Create a PR only if set to propagate (check module)
        if(Modules.git.shouldPropagatePr()) { Modules.bitbucket.createPr() }
    }

    //* If there are no build steps, don't do anything
    private static Boolean shouldExecute() {
        Helpers.canDoWhen(Settings.build) && Settings.build.steps?.size() > 0
    }
}

return this
