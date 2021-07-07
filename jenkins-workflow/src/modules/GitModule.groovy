package modules

import globals.*
import jenkins.Pipeline

static GitModuleCps newCps() { return new GitModuleCps() }

class GitModuleCps extends Pipeline {
    def repoObject
    def credentials
    def branchingModel

    // Should any configuration be changed, do it here
    def init() {
        def gitSettings = Helpers.substituteTree(Settings.modules.git as Map)
        credentials = gitSettings.credentialsId
        if (!credentials) {
            credentials = Settings.ether.git.credentialsId
        }
        branchingModel = Settings.branchingModel

        Helpers.log.debug """
        GitModule :: init
            * credentials : ${this.credentials}
            * branchingModel : ${this.branchingModel}
        """
    }

    Map collectFromUrl(gitUrl){
        def parts = gitUrl.split('/')
        def slug = parts[-1].replace('.git', '')
        def project = parts[-2]
        return [ project: project, slug: slug ]
    }

    private void run(String command) {
        String sshKey = Settings.ether.git.credentialsId
        def credCfg = sshUserPrivateKey(credentialsId: sshKey, keyFileVariable: 'SSHKEY')
        withCredentials([credCfg]){
            String e = "export GIT_SSH_COMMAND='ssh -i ${SSHKEY} -o StrictHostKeyChecking=no'"
            sh "${e}; ${command}"
        }
    }

    def getGitflowParent(branch) {
        def parent = [
                feature: 'develop',
                bugfix : 'release',
                hotfix : 'master'
        ]
        return (parent."${branch}" ?: branch)
    }

    String getGenericBranchName(branchName) {
        def match = ['feature', 'bugfix', 'hotfix', 'release', 'develop', 'master'].findAll { name ->
            branchName.startsWith(name)
        }
        if (match.size() > 0) {
            return match[0]
        }
        return branchName.split('/')[0].split('-')[0]
    }

    /**
    * This will clone the Repo in the node it is called from, and returns
    *   a shared library with information about it and utilities to interact
    *   with Git
    * @link https://globaldevtools.bbva.com/bitbucket/projects/BGDTS/repos/workflowlibs/browse/docs/vars/repo.md?at=refs%2Ftags%2F1.13.0
    */
    def cloneRepo() {
        Helpers.cleanNode()
        repo {
            credentialsId = this.credentials
            shallow = false
            branchingModel = this.branchingModel
        }
    }

    def setRepositoryConfig() {
        this.repoObject = repo
        if (!repoObject.codeInfo) {
            Helpers.error "GitModule :: setRepositoryConfig :: Trying to set repo info without calling 'cloneRepo"
            return
        }

        String branch = this.repoObject.codeInfo.GIT_BRANCH
        Map urlInfo = collectFromUrl(this.repoObject.codeInfo.GIT_URL)
        Boolean hasSemVer = this.repoObject.hasSemVer()

        /**
         * For this to work, the repository HAS TO follow a known Branching model
         *
         * @see ./doc/semver.md
         * TODO Decide what default name is better here (this ends up in artifact names)
         */
        def version = hasSemVer ? this.repoObject.nextVersion() : 'SNAPSHOT'

        // TODO - Send an unified list from here
        String git_subject = this.repoObject.codeInfo.GIT_SUBJECT
        Settings.setRepositoryConfig(this.repoObject.codeInfo, [
            branch: branch,
            project: urlInfo.project,
            slug: urlInfo.slug,
            subject: git_subject,
            author_name: this.repoObject.codeInfo.GIT_AUTHOR_NAME,
            author_email: this.repoObject.codeInfo.GIT_AUTHOR_EMAIL,
            mergedFrom: getMergeFromSubject(git_subject),
            pullrequest: [
                is: branch.startsWith('PR'),
                comesFrom: null,
                goesTo: null
            ],
            hasSemVer: hasSemVer,
            version: version,
        ])

        if(branch.startsWith('PR')){
            def pullrequest = Modules.bitbucket.getPrInfo(branch.split('-').last())
            Settings.branch = getGenericBranchName(pullrequest.fromRef)
            Settings.parent = pullrequest.toRef
            Settings.repo.pullrequest << [ comesFrom: pullrequest.fromRef, goesTo: pullrequest.toRef ]
        } else {
            Settings.branch = getGenericBranchName(branch)
            Settings.parent = getGitflowParent(Settings.branch)
        }

        Helpers.log.debug("Git info: ${showRepoConfig()}")
    }

    Boolean isBranchMergeable(){
        return ( Settings.modules.git.mergeDevelop && (Settings.branch != Settings.parent) && !Settings.repo.pullrequest.is)
    }

    Boolean isTagged() {
        this.repoObject.hasSemVer()
    }

    String showRepoConfig(){
        def prInfo = Settings.repo.pullrequest.is ? """
            Branch Origin: ${Settings.repo.pullrequest.comesFrom}
            Branch Target: ${Settings.repo.pullrequest.goesTo}
        """ : ""

        return """
            subject: ${Settings.repo.GIT_SUBJECT}
            author name: ${Settings.repo.GIT_AUTHOR_NAME}
            author email: ${Settings.repo.GIT_AUTHOR_EMAIL}
            branch (complete): ${Settings.repo.branch}
            branch (short): ${Settings.branch}
            commit (complete): ${Settings.repo.GIT_COMMIT}
            commit (short): ${Settings.commit}

            project: ${Settings.repo.project}
            repo slug: ${Settings.repo.slug}
            Branch parent: ${Settings.parent}
            Pull Request: ${Settings.repo.pullrequest.is.toString()} ${prInfo}
            Comes from a merge: ${(Settings.repo.mergedFrom != null).toString()}
            Will create a PR: ${shouldPropagatePr().toString()}

            Will apply Semantic Versioning? ${Settings.repo.hasSemVer}
            If so, which is the next version? ${Settings.repo.version}

            credentials ID: ${this.credentials}
        """
    }

    String getMergeFromSubject(String subject){
        def commitInfoMerge = ( subject =~ /.*Merge pull request .* in .* from (.*) to (.*)/ )
        if(commitInfoMerge.matches()) return commitInfoMerge[0][1]
    }

    Boolean shouldPropagatePr(){
        return (   (Settings.repo.pullrequest.is)
                && (Settings.branch in ['bugfix', 'hotfix'])
                )
    }
}

return this
