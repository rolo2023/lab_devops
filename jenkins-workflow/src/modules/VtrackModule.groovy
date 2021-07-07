package modules

import globals.Helpers
import globals.Settings
import jenkins.Pipeline

static VtrackModuleCps newCps() {
    new VtrackModuleCps()
}

class VtrackModuleCps extends Pipeline {

    private def client = null
    private def component = null
    private def deployResult = null

    private String vtrackCountry = null
    private String vtrackUUAA = null
    private String vtrackArchitecture = null

    // Store current vtrack build information
    private Map buildResults = [:]

    // Store current vtrack deploy information
    private Map deployInfo = [:]

    private def build = null

    def setBuild(build) {
        this.build = build
    }

    /**
     * Init Vtrack Module and generate a BuildMap
     * TODO Extend this to support multiple builds, potentially moving it to each module that
     *       can build an artifact: maven, docker, ...
     */
    void initVtrackBuildMap() {
        // Double check
        this.init()

        // Register starting buildMap
        Map buildMap = this.generateBuildMap(
                start_date: Helpers.now(true),
                url: Settings.repo.GIT_URL,
                userbuild: Settings.repo.GIT_AUTHOR_NAME,
                commit: Settings.repo.GIT_COMMIT,
                metadata: [author_email: Settings.repo.GIT_AUTHOR_EMAIL]
        )

        this.buildResults = buildMap
    }

    //* Useful for injecting a component, when testing
    void setComponent(vtrackComponent) { this.component = vtrackComponent }

    /**
     * Read Vtrack UUAA from configuration.
     * We will use the Module uuaa over the Global value, which should be deprecated (it is ESP specific)
     */
    private String getUUAA(Map configuration) {
        configuration.uuaa ?: Settings.uuaa
    }

    /**
     * Read Vtrack architecture from configuration.
     * Name must meet Ether standard, they can be checked by getting all Vtrack architectures via API
     *
     * @param configuration for Vtrack module
     */
    private String getArchitecture(Map configuration) {
        String architecture = configuration.architecture ?: Settings.architecture
        switch (architecture.toLowerCase()) {
            case 'spring': return 'spring.r1'
            case 'nacar': return 'nacar.r1'
            default: return architecture
        }
    }

    /**
     * Read Vtrack country from configuration.
     * This country must meet the 3-char Ether standard, and is different from the one set in the default config.
     *
     * We try to translate the codes we had until now to the new standard, the way other APIs are forced to do
     *
     * @param configuration from Vtrack module
     * @return String
     */
    private String getCountry(Map configuration) {
        List validCountries = ["GLO", "ESP", "MEX", "PER", "COL", "USA", "ARG", "CHL", "PRY", "PRT", "TUR", "URY", "VEN"]
        def validMappings = { String two_char_country ->
            switch (two_char_country.toLowerCase()) {
                case 'es': return 'ESP'
                case 'gl': return 'GLO'
                case 'co': return 'COL'
                case 'pe': return 'PER'
                default: return 'GLO'
            }
        }

        // Default to the one set in main configuration, probably not using Ether standard
        String country = configuration.get('country', 'undefined')
        if (!validCountries.contains(country)) {
            String defaultCountryCode = validMappings(Settings.country)

            Helpers.log.warn """
            A country value of '${country}' is not valid.
            The list of valid country codes is ${validCountries}.

            Vtrack configuration should define the country value from now on
            *For backwards compatibility only* we are converting your pipeline's country to '${defaultCountryCode}'"""

            country = defaultCountryCode
        }

        return country
    }

    /**
     * If not set, we will not register anything in Vtrack
     * NOTE - This is not ideal, but it is DISABLED BY DEFAULT
     */
    private Boolean isEnabled = false

    /**
     * If set, the module will ignore errors that happen when calling the VTrack API
     */
    private Boolean shouldIgnoreError = false

    /**
     * Return Ether namespace for this application.
     *
     * If we define UUAA,  AND a namespace is NOT set in Vtrack config, it will be used,
     *  and the namespace ID is retrieved from the Government API
     *
     * If no UUAA is defined AND a namespace is configured in Vtrack config, it will be used.
     *
     * If none of the above is true, we fail, as we have no Namespace
     *
     * @param vtrackSettings
     * @param vtrackClient
     * @return a Vtrack.Namespace response
     */
    String getEtherNamespaceId(Map vtrackSettings, vtrackClient) {
        String namespaceId
        String vtrackUUAA = this.getUUAA(vtrackSettings)

        // If this.vtrackUUAA exist, will call to government to get namespace
        if (vtrackUUAA && vtrackUUAA != '' && vtrackSettings.namespace == '') {
            String uuaaEther = vtrackUUAA
            String geoCodeEther = this.getCountry(vtrackSettings)


            def etherGovernmentNamespace = vtrackClient.getNamespaceFromGovernment(uuaaEther, geoCodeEther)
            if (!etherGovernmentNamespace) {
                failUnlessIgnoredOrDisabled """
            VtrackModule :: getEtherNamespaceId :: Empty response from Governent API
            Make sure you have a namespace there, or contact the GVT API team.
            """

                return null
            }
            namespaceId = etherGovernmentNamespace.id
        } else if (vtrackSettings.namespace && vtrackSettings.namespace != '') {
            namespaceId = vtrackSettings.namespace
        } else {
            failUnlessIgnoredOrDisabled """
        VtrackModule :: getEtherNamespaceId :: Vtrack info missing from Settings.
        Make sure you have uuaa or etherNamespace
        """

            return null
        }

        // Get namespace from Vtrack
        def etherNamespace = vtrackClient.getNamespace(namespaceId)
        if (etherNamespace) {
            Helpers.log.debug("VtrackModule :: init :: Using namespace: ${etherNamespace}")
        }
        return etherNamespace
    }

    private Boolean getEnabledFlagFromSettings(Map vtrackConfig) {
        if ('enabled' in vtrackConfig.keySet()) {
            return vtrackConfig.enabled
        }
        return false // disabled by default
    }

    void initModuleFromSettings(Map settings) {
        // Check if config enables the module
        this.isEnabled = getEnabledFlagFromSettings(settings)
        if (!this.isEnabled) {
            // Do not even continue
            return
        }

        this.shouldIgnoreError = settings?.should_ignore_error ?: false
        if (this.shouldIgnoreError) {
            Helpers.log.warn("VtrackModule :: 'should_ignore_error' flag is True, all Vtrack exceptions and errors will be IGNORED")
        }

        // Do not start if mandatory parameters are missing
        if (!Settings.repo?.GIT_URL) {
            failUnlessIgnoredOrDisabled """
        VtrackModule :: init :: GIT info missing from Settings.
        Make sure you are in a node where 'Modules.git.setRepositoryConfig()' has been called
        """
            return
        }

        this.vtrackUUAA = getUUAA(settings)
        Helpers.log.debug("Selected Vtrack UUAA: ${this.vtrackUUAA}")

        this.vtrackCountry = getCountry(settings)
        Helpers.log.debug("Selected Vtrack Country: ${this.vtrackCountry}")

        this.vtrackArchitecture = getArchitecture(settings)
        Helpers.log.debug("Selected Vtrack architecture ${this.vtrackArchitecture}")
    }

    void init() {
        // Do not reload client if it's already loaded
        if (this.client != null) {
            return
        }

        // Get settings
        /**
         * Config may bring in variables (beware, we might depend on things not set yet)
         * Make sure this module is init AFTER Git and Artifactory
         */
        Map vtrackConfig = Settings.modules.vtrack as Map ?: [:]
        vtrackConfig = Helpers.substituteTree(vtrackConfig)

        initModuleFromSettings(vtrackConfig)

        // return if not enabled, but print a BIG warning
        if (!this.isEnabled) {
            Helpers.log.warn '''
        VtrackModule :: Module has been DISABLED by configuration.

        This means that you will not register anything, consider enabling this module and reading the docs:
        https://globaldevtools.bbva.com/bitbucket/projects/GPIPE/repos/jenkins-workflow/browse/doc/modules/vtrack.md
        '''

            return
        } else {
            Helpers.log.debug "Vtrack Module init with config: ${vtrackConfig}"
        }

        initClientFromSettings(vtrackConfig)

        initComponentFromSettingsAndClient(vtrackConfig)
    }

    private def getOrCreateComponentForNamespace(vtrackNamespace) {
        Map componentData = this.getComponentDataFromSettings()
        String componentName = componentData['component']
        def vtrackComponent = vtrackNamespace.getComponent(componentName, componentData)
        if (!vtrackComponent) {
            failUnlessIgnoredOrDisabled("VtrackModule :: init :: Unable to create component with name '${componentName}'")
            return
        }

        Helpers.log.debug("VtrackModule :: init :: Using component: ${Helpers.dump(vtrackComponent.json() as Map)}")
        return vtrackComponent
    }

    private Map getComponentDataFromSettings() {
        String arch = this.vtrackArchitecture
        String repo = Settings.repo.GIT_URL
        String slug = Settings.repo.slug
        String componentName = "${arch}.${slug}"
        Map componentData = [
                component   : componentName,
                architecture: arch,
                vcs_repo    : repo,
        ]

        if (this.vtrackUUAA != null) {
            componentData << [
                    uuaa: this.vtrackUUAA
            ]
        }
        return componentData
    }

    /**
     * Generates build information array
     */
    Map generateBuildMap(Map overrides = [:]) {
        def buildMap = [
                artifacts: [],
                metadata : [:]
        ]

        if (this.component) {
            try {
                buildMap << this.component.generateBuildMap(overrides)

                //* Create a unique ID for this build
                buildMap.version = getBuildVersion()
            } catch (Exception ex) {
                failUnlessIgnoredOrDisabled(ex)
            }
        } else {
            failUnlessIgnoredOrDisabled("VtrackModule :: generateBuildMap :: component needs to be created")
        }

        return buildMap
    }

    /**
     * Returns an unique Build ID for Vtrack
     */
    static String getBuildVersion() {
        return [
                Settings.artifact.version,
                Helpers.jenkins.currentBuild.startTimeInMillis
        ].join('.')
    }

    /**
     * Register a build in Vtrack, returning current information about it
     */
    Map registerBuild() {
        Map buildMap = this.buildResults
        if (this.component) {
            try {
                Helpers.log.debug "VtrackModule :: registering build ${Helpers.dump(buildMap)}"
                this.build = this.component.createBuild(buildMap)
                if (this.build) {
                    Helpers.log.debug "VtrackModule :: registerBuild :: ${Helpers.dump(this.build.values)}"
                } else {
                    failUnlessIgnoredOrDisabled("VtrackModule :: registerBuild :: unable to register build")
                }
            } catch (Exception ex) {
                failUnlessIgnoredOrDisabled(ex)
            }
        } else {
            failUnlessIgnoredOrDisabled("VtrackModule :: registerBuild :: component needs to be created")
        }

        return buildMap
    }

    /**
     * Returns registered artifacts, if any.
     */
    List getArtifactsList() {
        this.buildResults.artifacts as List ?: []
    }

    /**
     * End a build, by registering time and result
     */
    def updateVtrackBuildResults(Boolean result) {
        this.buildResults.end_date = Helpers.now(true)
        this.buildResults.status = result ? 'successful' : 'failed'
    }

    /**
     * Register a meta-parameter for current build
     */
    void registerBuildAction(String actionName, Boolean result) {
        def vtrackActionName = "${actionName}_executed_successfully"

        if (!this.buildResults.metadata) {
            this.buildResults.metadata = [:]
        }
        this.buildResults.metadata[vtrackActionName] = result ? 'true' : 'false'

        Helpers.log.debug "VtrackModule :: registerBuildAction(${actionName}, ${result})"
    }

    /**
     * This registers an artifact in Vtrack. This info is consumed by the dimensions module.
     * TODO: This should be vTrack compliant, not dimensions compatible
     */
    void registerArtifact() {
        def file = Helpers.subst(Settings.artifact.file.name as String)
        def prefix = Settings.artifact.suffix == 'releases' ? 'release' : 'snapshot'
        def newArtifactEntry = [
                id  : "${prefix}:${file}",
                type: 'file'
        ]

        Map buildMap = this.buildResults
        if (!buildMap.artifacts) {
            buildMap.artifacts = []
        }
        buildMap.artifacts << newArtifactEntry
        Helpers.log.debug("VtrackModule :: registerArtifact :: Updated artifact list: ${newArtifactEntry}")

        this.buildResults = buildMap
    }

    def initVtrackDeployMap(Map overrides = [:]) {
        if (this.build) {
            try {
                this.deployInfo = this.build.generateDeployMap(overrides)
                this.deployInfo << [
                        country: this.vtrackCountry
                ]
                Helpers.log.debug "VtrackModule :: initVtrackDeployMap :: Success! ${this.deployInfo}"
            } catch (Exception ex) {
                failUnlessIgnoredOrDisabled(ex)
            }
        } else {
            failUnlessIgnoredOrDisabled("VtrackModule :: initVtrackDeployMap :: a build needs to be registered")
        }
    }

    void updateVtrackDeployResults(Boolean deploySuccessful) {
        this.updateVtrackDeployResult([
                status: deploySuccessful ? 'deployed' : 'failed',
        ])
    }

    void updateVtrackDeployResult(Map results) {
        if (!this.deployInfo) {
            this.initVtrackDeployMap()
        }
        this.deployInfo << results
    }

    void registerDeploy() {
        if (this.build) {
            try {
                this.deployResult = this.build.createDeploy(this.deployInfo)
                Helpers.log.debug "VtrackModule :: registerDeploy :: Success! ${this.deployResult}"
            } catch (Exception ex) {
                failUnlessIgnoredOrDisabled(ex)
            }
        } else {
            failUnlessIgnoredOrDisabled("VtrackModule :: initVtrackDeployMap :: a build needs to be registered to register deploys")
        }
    }

    /**
     * If the module is disabled, just ignore this
     * Using should_ignore_error boolean we can ignore the vTrack Errors, preventing the build from breaking.
     */
    private void failUnlessIgnoredOrDisabled(String errorMessage) {
        failUnlessIgnoredOrDisabled(new Exception(errorMessage))
    }

    private void failUnlessIgnoredOrDisabled(Exception ex) {
        String message = "VTRACK :: EXCEPTION :: ${ex.message}"
        if (!this.isEnabled) {
            Helpers.log.info "${message}\nVtrack Error ignored because module is DISABLED"
        } else if (this.shouldIgnoreError) {
            Helpers.log.info "${message}\nVtrack Error ignored because module is ENABLED but errors are set to IGNORED"
        } else {
            Helpers.error message
        }
    }

    def initClientFromSettings(Map settings) {

        // Verify credentials
        def credentials = [
                certificate: settings.ether_certificate ?: '',
                key        : settings.ether_key ?: ''
        ]
        if (credentials.any { _, v -> !v || !Helpers.credentialExists(v as String) }) {
            failUnlessIgnoredOrDisabled "VtrackModule :: init :: Ether credentials not found"
            return
        }

        // If debugging AND no base URL, point to lab region
        Boolean isDebugMode = settings.debug ?: false
        String vtrackBaseUrl = settings.base_url ?: ''
        if (vtrackBaseUrl == '' && isDebugMode) {
            vtrackBaseUrl = 'https://vtrack.lab-01.platform.bbva.com'
            Helpers.log.warn "VtrackModule :: init :: Base URL set to ${vtrackBaseUrl} because DEBUG is TRUE and no base_url was set"
        }

        // Init Vtrack client, from shared library 'gpipe/sharedlib_vtrack'
        this.client = vtrackClient.withSecretFileCredentialIds(credentials.certificate, credentials.key) {
            baseUrl = vtrackBaseUrl
            apiVersion = 'v0'
            debug = isDebugMode
            architecture = this.vtrackArchitecture
        }
    }

    void initComponentFromSettingsAndClient(Map settings) {
        try {
            def vtrackNamespace = this.getEtherNamespaceId(settings, this.client)
            Helpers.log.debug "VtrackModule :: got Namespace ${vtrackNamespace}"
            if (!vtrackNamespace) {
                failUnlessIgnoredOrDisabled "VtrackModule :: init :: Unable to find Vtrack namespace"
                return
            }

            def vtrackComponent = this.getOrCreateComponentForNamespace(vtrackNamespace)
            Helpers.log.debug "VtrackModule :: got component ${vtrackComponent}"

            this.setComponent(vtrackComponent)
        } catch (Exception ex) {
            failUnlessIgnoredOrDisabled(ex)
        }
    }
}

return this
