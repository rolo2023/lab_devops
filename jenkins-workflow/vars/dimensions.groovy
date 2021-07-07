import globals.Helpers
import globals.Modules
import globals.Settings

/**
 * Return current date (unix seconds) plus X time in future
 *
 */
private def lapse(args) {
    // Time equivalences in seconds
    def t = [
            seconds: 1,
            minutes: 60,
            hours  : 3600,
            days   : 86400
    ]

    def delta = 0
    args.keySet().each() { key ->
        if (t[key]) {
            delta += (args[key] * t[key])
        }
    }
    def result = Helpers.now() + delta
    return result
}

private void stageGet(String artifact, Map configuration) {
    String localFile = artifact
    // TODO: We should be able to retrieve the complete URL from a single method
    String remoteUrl = "${Settings.artifact.getUploadUrl()}/${localFile}"
    def curlParams = [action: 'get', url: remoteUrl, localFile: localFile]
    if (configuration.http_proxy) {
        curlParams << [proxy: configuration.http_proxy]
    }

    // These are the credentials defined in configuration: they are valid by definition
    def artifactoryCredentials = Settings.modules.artifactory.credentialsId
    curlParams << [credentials: artifactoryCredentials]
    if (configuration.environment?.http_proxy) {
        curlParams << [proxy: configuration.environment.http_proxy]
    }

    // If for some reason there are artifacts for the same commit, delete them before downloading
    cleanNode()
    curl(curlParams)
    def deployDir = getDeployDir()
    sh """
        unzip -q ${localFile} -d \$PWD
        mkdir -p ${deployDir}
        unzip -q data.zip -d ${deployDir}
    """
}

private Map stageDimensions(Map configuration) {
    def credentials = readJSON(file: configuration.credentials_file)
    def deployDate = lapse(minutes: 5)

    def deployData = [
            version     : configuration.version,
            artifactsDir: getDeployDir(),
            verbosity   : configuration.verbosity,
            output      : "${workspace}/dimensions.json",
            config      : [
                    username : credentials.username,
                    password : credentials.password,
                    country  : configuration.country,
                    pass_type: configuration?.pass_type,
                    petition : configuration.petition,
                    risk     : configuration.risk,
                    database : configuration.database,
                    host     : configuration.host,
                    date     : deployDate,
                    risk     : configuration?.risk,
                    pimps    : [
                            [
                                    title  : configuration.title,
                                    uuaa   : configuration.uuaa,
                                    circuit: configuration.circuit
                            ]
                    ]
            ]
    ]

    return dimensionsCall(deployData)
}

private def getArtifactRelease() {
    def release = null

    def artifactsList = Modules.vtrack.getArtifactsList()
    if (!artifactsList) {
        Helpers.log.warn 'No artifacts were generated yet the deploy pipeline was invoked.'
        return release
    }

    artifactsList.each { artifact ->
        if ((artifact.id.startsWith('release')) || (artifact.id.startsWith('snapshot'))) {
            release = artifact.id.split(':').last()
            return  // Exit 'each'
        } else {
            Helpers.log.info("Artifact ID ${artifact.id} is not a release or snapshot, continue search")
        }
    }

    if (!release) {
        Helpers.log.warn 'No artifact from this build has been uploaded as release or snapshot'
    }

    return release
}

private cleanNode() {
    sh "rm -fR ${getDeployDir()}"
    Helpers.cleanNode()
}

private def getDeployDir() {
    "${Settings.temp.artifacts}/${Settings.commit}"
}

/**
 * Deploy an entire directory with dimensions client
 *
 * @param Map args
 *    config: map which contains dimensions configuration (see client for more info)
 *    artifactsDir: where the artifacts are stored
 *    verbosity: level of verbosity - 0 = none, 1 = info/warning, 2+ = debug
 *    version: version of dimensions cli. Defaults to master branch
 *
 * @return a Map with the deployment ldresults.
 *
 * */
def dimensionsCall(Map args) {
    def artifactsDir = args.artifactsDir ?: pwd()
    def version = args.version ?: 'master'
    def output = args.output ?: 'result.json'
    def configValues = args.config
    def jsonCfg = "config.json"
    def verbosity = args.verbosity ?: 0
    def verbose = 'v' * verbosity
    verbose = verbose == '' ?: "-${verbose}"

    Map result = [:]
    dir('dimensions') {
        git url: 'ssh://git@globaldevtools.bbva.com:7999/ccore/dmwrapper.git',
                credentialsId: 'bot-globaldevops-pro-ssh',
                branch: version


        writeFile file: jsonCfg, encoding: 'UTF-8', text: groovy.json.JsonOutput.toJson(configValues)
        sh "./dimensions.py ${verbose} -a deploy -o ${output} -f ${jsonCfg} -p artifacts=${artifactsDir}"
        result = readJSON file: output
    }
    Helpers.log.info("dimensionsCall :: Result :: ${result}")
    if (!result) {
        result = [error: "Dimensions CLI returned no output", ok: false]
    }
    return result
}

private String validateCredentialsFile(String fileName) {
    try {
        def contents = readJSON(file: fileName)
        if (!contents.username || !contents.password) {
            return "dimensions :: validateCredentialsFile :: No username/password found in ${fileName}"
        }
        return null
    } catch (java.io.FileNotFoundException _) {
        return "dimensions :: validateCredentialsFile :: Credentials file '${fileName}' does not exist"
    } catch (Exception e) {
        return "dimensions :: validateCredentialsFile :: Unhandled exception reading '${fileName}' ${e.class}"
    }
}

private String getDimensionsCircuit(Map stepConfig) {
    try {
        stepConfig.circuit
    } catch (ignored) {
        Helpers.log.info("Dimensions circuit not found, using value in (${Settings.config.custom})")
        Settings.config.custom.circuit
    }
}


private String getDimensionsUUAA(Map stepConfig) {
    try {
        stepConfig.uuaa
    } catch (ignored) {
        Helpers.log.info("Dimensions UUAA not found, using value in Jenkinsfile (${Settings.uuaa})")
        Settings.uuaa
    }
}


/**
 * Deploys a zipped bundle using Dimensions.
 *
 * Mandatory params:
 * - circuit
 * - uuaa
 * - credentials_file
 * Optionals
 * - http_proxy NOTE this historically comes as environment, not anymore
 * <TODO>
 For backwards compatibility, they will be OPTIONALLY read from the app's Jenkinsfile,

 nacar {group = '...'
 country = '..'
 uuaa = 'UUAA'
 circuit = 'TST1'}* </TODO>
 */
def call(Map configuration) {
    // Expand configuration with custom values set in Jenkinsfile
    def dimensionsCircuit = this.getDimensionsCircuit(configuration)
    def dimensionsUUAA = this.getDimensionsUUAA(configuration)

    configuration << [circuit: dimensionsCircuit, uuaa: dimensionsUUAA]
    if (!fromMap(configuration).checkMandatory(['credentials_file', 'uuaa', 'circuit', 'levels'])) {
        return
    }

    String credentialError = validateCredentialsFile(configuration.credentials_file)
    if (credentialError) {
        Helpers.error("NO ENTIENDO NADA") //credentialError)
        return
    }

    // Extract some info from build
    def artifactVersion = getArtifactRelease()
    if (!artifactVersion) {
        Helpers.error "Dimensions :: Error :: Cannot deploy artifact without information"
        return
    }

    // Set this before the deploy to ZERO
    configuration.deployLevel = 0
    Map deployResult = null
    String errorMessage = null

    //* TODO This might be overkill, since we're inside a Step
    withEnv(Helpers.mapToArray(configuration.environment)) {
        // Get artifact
        stageGet(artifactVersion, configuration)

        // Deploy with dimensions
        deployResult = stageDimensions(configuration)
        if (!deployResult) {
            errorMessage = "dimensions :: error :: Got empty result from dimensions, check logs"
        } else if (!deployResult.ok) {
            errorMessage = "dimensions :: error :: ${deployResult}"
        } else {
            configuration.deployLevel++
            // TODO: There are two environmnets here....
            Modules.vtrack.updateVtrackDeployResult([
                    userdeploy : Settings.repo.GIT_AUTHOR_EMAIL,
                    date       : Helpers.dateToUnix(deployResult.date, true),
                    params     : [
                            UUAA                  : configuration.uuaa,
                            pimp_id               : deployResult.values[0]['pimp_id'],
                            pimp_request_date     : Helpers.now(true),
                            pimp_implantation_date: Helpers.dateToUnix(deployResult.values[0]['date'], true, false),
                            environment           : configuration.levels[configuration.deployLevel as int]
                    ],
                    environment: 'int'
            ])
        }
    }

    // Do not leave artifacts dangling before leaving
    cleanNode()

    // Crash in case of error
    if (errorMessage != null) {
        Helpers.error(errorMessage)
    }
}

def fromStep(Map step) { return call(step) }

def getDefaults(Map override = [:]) {
    return [
            title: "Automatic PIMP generated by Jenkins"
    ] + override
}

return this