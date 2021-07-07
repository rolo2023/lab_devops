package globals

class Settings {
    final static Map libraryVersions = [
            /**
             * Read from
             * - ssh://git@globaldevtools.bbva.com:7999/bgdts/workflowlibs.git
             */
            workflowlibs: '1.25'
    ]
    final static String libSonar = 'lts'
    final static String libChimera = '2.3.0'
    final static String libCachelo = '1.3.0'
    final static String libRunTests = 'develop'

    // Debug Levels
    static Integer LOGLEVEL = Helpers.log.INFO

    // Temporary folders
    static Map temp = [
            artifacts: '/tmp/artifacts',
            scripts  : '/tmp/spring'
    ]

    final static Map ether = [
            git        : [credentialsId: 'bot-globaldevops-pro-ssh'],
            artifactory: [
                    url     : 'https://globaldevtools.bbva.com/artifactory-api',
                    registry: 'globaldevtools.bbva.com:5000'
            ]
    ]

    // Parameters if any
    static List params = []

    // General
    static String architecture = 'generic'
    static String country = 'global'
    static String group = 'generic'
    static String branchingModel = 'auto'   // default

    //* Default version for country/group configuration
    static String revision = 'HEAD'

    //* Repository template for country/group configuration location
    static applicationConfigurationRepo =
            { country -> "ssh://git@globaldevtools.bbva.com:7999/gpipe/configs_${country}.git" }
    static Boolean isFirstRun = false

    // UUAA (by Jenkinsfile config)
    static String uuaa = 'none'

    static Map vars = [:]

    // Used by legacy maven module
    static String java
    static String maven

    // Used by Dimensions module (legacy)
    static String circuit

    // Git
    static String branch = ''
    static String commit = ''
    static String parent = ''
    static String tag = ''

    // Modules configuration
    static Map modules = [:]

    // Non-modules configuration
    static Map jenkinsfile = [:]
    static Map environment = [:]
    static Map repo = [:]
    static Map store = [:]

    // Stages
    static String currentStage = ''
    static Map build = [:]
    static Map test = [:]
    static Map publish = [:]
    static Map deploy = [:]
    static Map end = [:]

    // Artifact
    static def artifact = [:]

    static Map config = [
            defaults   : [:],
            application: [:],
            custom     : [:]
    ]

    /**
     * Will store user parameters to be rendered, and hidden parameters (those that do not match conditions)
     */
    static def formValues

    /**
     * This value identifies a set of parameters, so that two executions with the same hash share:
     * - remote configuration file (no variable replacement)
     * - local Jenkinsfile values: 'country', 'revision', 'group' and 'vars'
     *
     * It is used to create unique agent labels, so we could potentially share pods between exact same executions
     */
    static String configurationSetIdentifier

    static void loadDefaultConfiguration() {
        Helpers.log.debug('Settings :: loadDefaultConfiguration')
        config.defaults = Helpers.loadResource('template.yml')
        modules = getDefaultModuleConfig()
    }

    static void loadApplicationConfiguration() {
        Helpers.log.debug('Settings :: loadApplicationConfiguration')
        config.application = getApplicationConfig()
        modules = getOverridenModuleConfig()
    }

    static void load(Map cfg = [:]) {
        config.custom = cfg

        //* Mandatory params
        country = getMandatory(cfg, 'country') as String

        group = getMandatory(cfg, 'group') as String

        //* Optional Params
        architecture = cfg.get('architecture', 'generic')
        revision = cfg.get('revision', 'HEAD')
        vars = cfg.get('vars', [:])

        //* LEGACY PARAMS
        java = cfg.get('java', '') // This will later map to 'java_tool'
        maven = cfg.get('maven', '')   // This will later map to 'maven_tool'
        circuit = cfg.get('circuit', '')
        uuaa = cfg.get('uuaa', 'NO_UUAA')

        loadDefaultConfiguration()
        loadApplicationConfiguration()

        LOGLEVEL = getValue('verbosity', Helpers.log.INFO) as Integer

        branchingModel = getBranchingModel()

        // Artifact configuration
        this.store = getStoreConfig()

        // Stages
        this.build = getBuildConfig()
        this.test = getTestConfig()
        this.publish = getPublishConfig()
        this.deploy = getDeployConfig()
        this.end = getMap('stages.end')

        //* XXX Sonar stage is deprecated, in favour of vars/wSonar.groovy
        getSonarConfig()

        // Once configuration is stable (remote file + locally modified values) create the agent suffix
        def agentTag = getValue('agentTag')
        if (!agentTag || agentTag == '') {
            configurationSetIdentifier = this.hashCode()
        } else {
            configurationSetIdentifier = agentTag
        }
    }

    static String getBranchingModel() {
        String model = getValue('branching_model', 'auto').toLowerCase()
        if (!['auto', 'release', 'norelease', 'none'].contains(model)) {
            Helpers.log.warning "Invalid branching model selected: ${model}: defaulting to 'auto'"
            model = 'auto'
        }

        return model
    }

    static void substituteSettings() {
        ['modules', 'store', 'build', 'test', 'publish', 'deploy', 'end'].each { tree ->
            Map newTree = Helpers.substituteTree(Settings."${tree}")
            Settings."${tree}" = newTree
            Helpers.log.debug "Settings :: substituteSettings :: ${tree}\n" + Helpers.dump(newTree)
        }
    }

    private static String getMandatory(Map config, String mandatoryKey) {
        def value = config.get(mandatoryKey, null)
        if (!value) {
            Helpers.error "'${mandatoryKey}' is mandatory but not found in configuration"
        }
        value
    }

    static void processFormParameters() {
        //* These come from configuration, making builds parametric
        List formParameters = (config.application.form ?: config.defaults.form) as List

        def form = FormHelper.newCps()
        form.fromYaml(formParameters)
        addToParameters(form.currentForm)

        Settings.formValues = form
    }

    static void addToParameters(newParameter) { this.params << newParameter }

    static void addToParameters(List newParameters) { newParameters.each { this.params << it } }

    static List getParameters() { return this.params }

    static Map getMap(keyObject) {
        Map base = config.defaults
        Map override = config.application
        for (part in keyObject.split('\\.')) {
            if (base) {
                base = base.get(part, null)
            }
            if (override) {
                override = override.get(part, null)
            }
        }

        /**
         * If override is null, then assume we use 'base'
         * Users choose not to override anything and rely on base config
         */
        if (override == null) {
            return base
        }

        /**
         * If override is set, then use it.
         * Override can be empty, and that is user's choice
         */
        return override
    }

    static Map getDefaultModuleConfig() {
        config.defaults.modules as Map ?: [:]
    }

    static Map getOverridenModuleConfig() {
        def defaultModulesMap = config.defaults.modules ?: [:]
        def overrideModulesMap = config.application.modules ?: [:]
        def modules = [:]

        // Override defaults
        defaultModulesMap.each { String moduleName, Map defaultModuleConfig ->
            if (overrideModulesMap[moduleName]) {
                defaultModuleConfig << overrideModulesMap[moduleName]
            }
            modules[moduleName] = defaultModuleConfig
        }

        return modules
    }

    static Map getApplicationConfig() {
        def cfg = config.custom
        if (cfg.yaml) {
            Helpers.log.info('Settings :: getApplicationConfig :: Reading APP configuration directly from config')
            return cfg.yaml
        }

        if (!cfg.group || cfg.group == 'generic' || cfg.template == 'generic') {
            Helpers.log.debug('Settings :: getApplicationConfig :: Using generic application config')
            return Helpers.loadResource('generic.yml')
        }

        Helpers.log.debug('Settings :: getApplicationConfig :: Getting remote configuration')
        def remoteConfigFile = getRemoteConfig()
        return Helpers.jenkins.readYaml(file: remoteConfigFile)
    }

    static String getRemoteConfig(fileName = null) {
        if (!fileName) {
            fileName = "${this.group}.yml"
        }
        def repository = applicationConfigurationRepo(country)
        def fileVersion = this.revision
        def overrideGitCredentials = false

        Helpers.log.debug("Settings :: getRemoteConfig :: get ${repository}/${fileName} (${fileVersion})")
        if (!Settings.modules.git.credentialsId) {
            overrideGitCredentials = true
            Settings.modules.git.credentialsId = ether.git.credentialsId
        }

        Helpers.jenkins.getFileFromRepo([file: fileName, remote: repository, revision: fileVersion])
        if (overrideGitCredentials) Settings.modules.git.credentialsId = null
        return fileName
    }

    //* TODO remove hardcodeds, it's either explicit in conf, or not there
    private static Map getBuildConfig() {
        def buildMap = getMap('stages.build')
        if (buildMap.java) buildMap.environment << [java_tool: buildMap.java]
        if (buildMap.maven) buildMap.environment << [maven_tool: buildMap.maven]
        if (buildMap.maven_settings) buildMap.environment << [maven_settings: buildMap.maven_settings]
        if (buildMap.maven_args) buildMap.environment << [maven_args: buildMap.maven_args]
        if (buildMap.artifact_path) Settings.store.file.path = buildMap.artifact_path
        if (buildMap.artifact_file) Settings.store.file.name = buildMap.artifact_file

        //* XXX Remove in 3.2?
        if (buildMap.tagged) {
            Helpers.log.warn """
            The 'tagged: ${buildMap.tagged}' parameter in Build stage is DEPRECATED.
            It is now assumed we follow a known Branching Model, and we'll only tag
            when we are in 'develop', 'release' or 'master'.
            """
        }
        return buildMap
    }

    private static Map getTestConfig() {
        Map testMap = getMap('stages.test')
        return testMap
    }

    private static Map getDeployConfig() {
        def cfg = config.custom
        Map deployMap = getMap('stages.deploy')
        if (deployMap.module) deployMap << [circuit: cfg.circuit, uuaa: cfg.uuaa]
        return deployMap
    }

    //* XXX Remove in 3.2?
    private static Map getSonarConfig() {
        Map sonarMap = getMap('stages.sonar')
        if (sonarMap && sonarMap.keySet().size() > 0) {
            Helpers.log.warn """
            Sonar stage has been deprecated in favour of an embeddable module
            One example of use can be:
            ---
            stages:
                build:
                    - use: 'wSonar'
                      with_params:
                        ${sonarMap}
            """
        }
    }

    private static Map getPublishConfig() {
        Map publishMap = getMap('stages.publish')
        return publishMap
    }

    private static def getValue(key) {
        return this.getValue(key, null)
    }

    private static def getValue(key, defaultValue) {
        if (config.custom.containsKey(key)) {
            return config.custom.get(key)
        } else if (config.application.containsKey(key)) {
            return config.application.get(key)
        } else if (config.defaults.containsKey(key)) {
            return config.defaults.get(key)
        }
        return defaultValue
    }

    static Map getYaml() {
        return [
                modules: this.modules,
                stages : [build: this.build, test: this.test, deploy: this.deploy]
        ] as Map
    }

    static void setRepositoryConfig(codeInfo, overrides) {
        this.repo = codeInfo + overrides
        this.commit = codeInfo.GIT_COMMIT[0..6]
    }

    private static Map getStoreConfig() {
        getMap('store')
    }
}
