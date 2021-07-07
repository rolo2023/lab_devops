package ci.stages

import globals.*
import jenkins.Pipeline

static LoadCps newCps() { return new LoadCps() }

class LoadCps extends Pipeline {

    private List credentialsToFind = ['credentialsId', 'maven_settings']

    def checkCredentials() {
        String errorMsg = "The following credentials have not been found in this Jenkins:\n - "
        def settingsYaml = Settings.getYaml()
        def toFind = []
        credentialsToFind.each { key ->
            Helpers.recursiveKey(settingsYaml, key).each {
                String credential = Helpers.substituteObject(it as String)
                if (credential && !credential.startsWith('env')) {
                    toFind << credential
                }
            }
        }.unique()

        List notFound = []
        toFind.each { String cred ->
            if (!Helpers.credentialExists(cred)) {
                notFound << cred
            }
        }
        if (notFound.size() > 0) {
            error "${errorMsg}${notFound.join('\n - ')}"
        }
    }

    void loadDependencies() {
        Helpers.loadLibrary('workflowlibs', Settings.libraryVersions.workflowlibs)
        Helpers.log.debug('Stages :: Load :: External libraries loaded')
    }

    def loadCiConfig(config) {
        // Delete temporary files so far
        Helpers.cleanNode()

        // Git module is needed now
        Modules.load('git')

        Settings.load(config)

        // We use this to decide if we should deploy or not
        Settings.isFirstRun = (config.buildNumber == 0)

        // Delete temporary files so far
        Helpers.cleanNode()
    }

    void initModules() {
        // Init 'core' modules --> the rest depend on these
        List coreModules = ['git', 'artifactory', 'bitbucket']
        Settings.modules.keySet().findAll{ String k -> k in coreModules }.each {
            Modules."${it}".init()
        }

        // We can only set the complete config when we get the commit hash :(
        Modules.git.cloneRepo()
        Modules.git.setRepositoryConfig()
        Modules.artifactory.setArtifactConfig()

        // And now init the rest, since they depend on Git and Artifactory info
        Settings.modules.keySet().findAll{ String k -> !(k in coreModules) }.each {
            Modules."${it}".init()
        }
    }

    void loadModules(config) {
        Settings.modules.keySet().each {
            Modules.load(it, config)
        }
    }

    void addPipelineProperties() {
        // this needs to happen once Git and Artifactory modules properties are populated
        Settings.processFormParameters()
        def pipelineParams = Settings.getParameters()
        if (pipelineParams) {
            properties([parameters(pipelineParams)])
            Helpers.log.debug "Stages :: Load :: added new Pipeline Properties: ${pipelineParams}"
        }
    }

    def execute(Map config = [:]) {
        Settings.currentStage = 'Load'
        try {
            // External Libraries
            loadDependencies()

            // Country/App specific configuration
            loadCiConfig(config)

            // Verify configured credentials do exist
            checkCredentials()

            // Dynamic Modules and properties
            loadModules(config)
            initModules()

            // Process and load properties
            addPipelineProperties()

            // At this point, configuration should be considered stable
            Settings.substituteSettings()
        } catch (Exception initException) {
            Helpers.error initException
        } finally {
            Helpers.cleanNode()
        }
    }
}
return this
