package modules

import globals.*
import jenkins.Pipeline

ArtifactoryModuleCps newCps() { new ArtifactoryModuleCps() }

class ArtifactoryModuleCps extends Pipeline {

    //* DEFAULTS
    Boolean isUpload = false
    String version = 'SNAPSHOT'
    String server = ''
    String suffix = 'snapshots'
    String context = ''
    String virtual = ''
    String uuaa = Settings.uuaa
    String urlUpload = ''
    String urlDownload = ''
    String registry = ''
    Map file = [
            name: '',
            path: '.'
    ]

    //* Downloads are done from the Virtual repository
    String getDownloadUrl() {
        String path = virtual ?: ['repository', context].join('-')
        return [server, path].join('/')
    }

    //* Uploads are done using a branch-specific suffix to the remote repos
    //* TODO UUAA Shouldn't be mandatory
    String getUploadUrl(application = null) {
        def path = [context, suffix].join('-')
        if (!application) {
            application = this.uuaa
        }
        return [server, path, application].join('/')
    }

    def init() {
        Settings.modules.artifactory = Helpers.substituteTree(Settings.modules.artifactory)
    }

    private def getBaseUrl() {
        def baseUrl = Settings.ether.artifactory.url
        if (Settings.modules.artifactory.server_id) {
            if (!Helpers.pluginExists("artifactory")) {
                Helpers.log.warning """
                Trying to use Artifactory plugin for server ID ${Settings.modules.artifactory.server_id}, but such plugin is not installed.
                Please Install the plug-in artifactory. Visit: https://www.jfrog.com/confluence/display/RTF/Jenkins+Artifactory+Plug-in

                ==> Falling back to Default url: ${baseUrl}
                """
            } else {
                try {
                    def server = Artifactory.server Settings.modules.artifactory.server_id
                    baseUrl = server.url
                } catch (any) {
                    Helpers.log.warning """
                    Artifactory plugin cannot find server ID ${Settings.modules.artifactory.server_id}. Please review your Master configuration to get the correct ID.

                    ==> Falling back to Default url: ${baseUrl}
                    """
                }
            }
        }
        Helpers.log.info "ArtifactoryModuleCps :: getBaseUrl :: ${baseUrl}"
        return baseUrl
    }

    private def getDockerRegistry() {
        if (Settings.modules.artifactory.registry) {
            return Settings.modules.artifactory.registry
        } else {
            return Settings.ether.artifactory.registry
        }
    }

    String showArtifactConfig() {
        return """
            Docker Registry: ${this.registry}
            server url: ${this.server}
            server repository: ${this.context}
            server repository suffix: ${this.suffix}
            server upload path: ${this.urlUpload}
            server download path: ${this.urlDownload}

            file local path: ${this.file.path}
            file local name: ${this.file.name}
            file version: ${this.version}

            artifact version: ${this.version}
            artifact will be uploaded: ${this.isUpload ? 'yes' : 'no'}
        """
    }

    private Map validateStoreConfig() {
        Map store = Settings.store
        if (!store.release) {
            store.release = [:]
        }
        if (!store.context) {
            store.context = ''
        }

        if (!store.file) {
            store.file = [
                    name: '', path: ''
            ]
        }
        store
    }

    void setArtifactConfig() {
        Settings.store = validateStoreConfig()

        def branch = Settings.repo.pullrequest.is ? 'PR' : Settings.branch
        def release = Settings.store.release.get(branch, Settings.store.release.any)
        this.version = Helpers.subst(release.name as String)
        this.server = getBaseUrl()
        this.registry = getDockerRegistry()
        this.context = Helpers.subst(Settings.store.context as String)
        this.virtual = Settings.store.virtual
        this.suffix = release.suffix
        this.isUpload = release.upload
        this.uuaa = Settings.uuaa
        this.urlUpload = getUploadUrl()
        this.urlDownload = getDownloadUrl()

        /* As a result of the https://globaldevtools.bbva.com/jira/secure/RapidBoard.jspa?rapidView=7754&view=detail&selectedIssue=GDAD-952,
           we need to initialize Settings.artifact before defining the file.name. */
        Settings.artifact = this

        this.file = Helpers.substituteTree(Settings.store.file as Map)

        Settings.artifact = this
        Helpers.log.debug("Artifact info: ${showArtifactConfig()}")
    }
}

return this
