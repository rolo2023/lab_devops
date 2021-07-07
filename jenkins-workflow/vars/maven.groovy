import globals.*

/**
 * Maven wrapper to be used in steps
 *
 * - goal
 * Optionals:
 * - mavenSettings
    If present, either a 'file: name' or a 'env: PREFIX' entry
 * - with_cachelo: Use cachelo to leverage mvn cache
 *  - key: S3 Key used to upload/download cache
 *  - paths: List of paths to upload to S3
 */
def call(Closure body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config

    body()
    fromMap(config).checkMandatory(['goal'])

    config.environment = config.environment ?: []

    //* Cachelo library support
    def hasCachelo = 'with_cachelo' in config.keySet()
    if (hasCachelo) {
        hasCachelo = cacheloDownload(config.with_cachelo)
    }

    def mavenSettings = config.mavenSettings
    if (!mavenSettings) {
        Helpers.log.info "Maven :: Running without explicit credentials or settings"
        runMaven(config)
        return
    }

    def (settingType, settingArg) = mavenSettings.split(':').collect { s -> s.trim() }
    if ('file'.equals(settingType)) {
        withCredentials([file(credentialsId: settingArg, variable: 'settingsXml')]){
            runMaven(config, "-s ${settingsXml}")
        }
    } else if ('env'.equals(settingType)) {
        def credId = Settings.modules.artifactory.credentialsId
        withCredentials([usernamePassword(
                    credentialsId: credId,
                    usernameVariable: "${settingArg}_USR",
                    passwordVariable: "${settingArg}_PSW"
                    )])
        {
            config.environment << "${settingArg}_USR=${this."${settingArg}_USR"}"
            config.environment << "${settingArg}_PSW=${this."${settingArg}_PSW"}"
            runMaven(config)
        }
    } else {
        error "MAVEN :: Unsupported mavenSettings type '${settingType}. Only types of credentials supported are 'file' and 'env'"
    }

    if (hasCachelo) { uploadCachelo(config.with_cachelo) }
}

private Boolean uploadCachelo(Map config) {
    try {
        uploadCache key: config.key, paths: config.paths
        return true
    } catch (Exception ex) {
        log.warn "Maven :: Cachelo :: Error uploading Cache key ${config.key}: ${ex.message}"
        return false
    }
}

private Boolean cacheloDownload(Map config) {
    fromMap(config).checkMandatory(['key', 'paths'])
    try {
        Helpers.loadLibrary("cachelo@${Settings.libCachelo}")
        downloadCache(key: config.key)
        return true
    } catch (Exception ex) {
        log.warn "Maven :: Cachelo :: Error downloading Cache key ${config.key}: ${ex.message}"
        return false
    }
}

private def runMaven(Map config, extra_options='') {

    // mvn environment
    List envList = config.environment ?: []
    if (config.mavenTool) envList << "PATH+MAVEN=${tool config.mavenTool}/bin"
    if (config.javaTool) envList << "JAVA_HOME=${tool config.javaTool}"

    // custom options, such as settings file
    List optionals = [ '--batch-mode' ]
    optionals << "-Dmaven.repo.local=${config.m2 ?: "$WORKSPACE/.m2"}"
    if (extra_options) { optionals << extra_options }

    String mvnCmd = "mvn ${optionals.join(' ')} ${config.goal}"
    Helpers.log.debug("Run Maven Command\n- env: ${envList.toString()}\n- cmd: ${mvnCmd}")
    withEnv(envList){
        sh """#!/bin/sh
            mvn --version ; ${mvnCmd}
        """
    }
}

def fromStep(Map stepArgs){
    this.call {
        goal = stepArgs.goal
        mavenTool = Helpers.subst (stepArgs?.maven_tool)
        javaTool = Helpers.subst (stepArgs?.java_tool)
        m2 = stepArgs?.m2
        mavenSettings = stepArgs?.maven_settings
        if ('with_cachelo' in stepArgs) {
            with_cachelo = stepArgs?.with_cachelo
        }
    }
}

//* TODO To deprecate once we no longer support old 'bin: mvn' syntax in 'run blocks
def fromRunCommand(Map runCommandArgs) {
    Helpers.log.warn("vars :: maven :: DEPRECATED USE OF RUN:bin:mvn(${runCommandArgs})")
    fromStep([
        goal: runCommandArgs.run,
        maven_tool: runCommandArgs.environment?.maven_tool,
        java_tool: runCommandArgs.environment?.java_tool,
        m2:  '',
        maven_settings: runCommandArgs.environment?.maven_settings,
    ])
}

return this
