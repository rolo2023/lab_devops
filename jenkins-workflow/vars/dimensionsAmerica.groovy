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

private void stageUnstash(String artifact) {
    String localFile = artifact

    // If for some reason there are artifacts for the same commit, delete them before downloading
    cleanNode()
    unstash "${localFile}"

    def deployDir = getDeployDir()
    sh """
        echo "this is the zip coming from config ${localFile}"
        echo "::Extracting files to deploy::"
        tar -zxvf ${localFile} -C \$PWD
        ls
        echo "::Saving files in temporal dir::"
        mkdir -p ${deployDir}
        echo "::Extracting baseline to deploy ::"
        tar -zxvf ${localFile} -C ${deployDir}
    """
}

private Map stageDimensions(Map configuration) {
    def deployData = [
            version     : configuration.version,
            artifactsDir: getDeployDir(),
            verbosity   : configuration.verbosity,
            output      : "${workspace}dimensions.json",
            levels      : configuration.levels
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

private def findExistingCredentials() {
    def uuaa = Settings.uuaa
    def country = Settings.country
    def existingCredential;
    List credentialsList = ["${country}_dimensions", "${country}_${uuaa}_dimensions"]
    credentialsList.each { credential ->
        try {
            withCredentials([
                    usernamePassword(credentialsId: "${credential}", usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD'),
            ]) {
                existingCredential = credential
            }
        } catch (Exception _) {
        }
    }

    return existingCredential;
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
Map dimensionsCall(Map args) {
    def artifactsDir = args.artifactsDir ?: pwd()
    def version = args.version ?: 'master'
    def output = args.output ?: "${workspace}result.json"
    def verbosity = args.verbosity ?: 0
    def verbose = 'v' * verbosity
    verbose = verbose == '' ?: "-${verbose}"
    def uuaa = Settings.uuaa
    def country = Settings.country
    def levels = args.levels

    Map result = [:]
    dir('dimensions') {
        git url: 'ssh://git@globaldevtools.bbva.com:7999/gpipe/dmwrapper.git',
                credentialsId: 'bot-globaldevops-pro-ssh',
                branch: 'feature/deployment'

        def credential = findExistingCredentials();

        withCredentials([
                usernamePassword(credentialsId: "${credential}", usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD'),
                file(credentialsId: "${country}_${uuaa}_dimensions_config", variable: 'FILE')
        ])

                {
                    def jsonConfig = sh(
                            returnStdout: true,
                            script: "cat ${FILE}"
                    )
                    def jsonObj = readJSON text: jsonConfig
                    def deployDate = lapse(minutes: 5)
                    jsonObj << [date: deployDate]
                    jsonObj << [promotions: levels]
                    writeJSON file: "${country}_${uuaa}_dimensions_config_tmp.json", json: jsonObj
                    sh " ./dimensions.py  ${verbose} -a deploy -o '${output}' -f '${country}_${uuaa}_dimensions_config_tmp.json' -p username=$USERNAME -p password=$PASSWORD -p artifacts=${artifactsDir} "
                    result = readJSON file: output
                }
    }
    Helpers.log.info("dimensionsCall :: Result :: ${result}")
    if (!result) {
        result = [error: "Dimensions CLI returned no output", ok: false]
    }
    return result
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
 For backwards compatibility, they are expected to be set in the app's Jenkinsfile,
 rather than coming in the configuration parameter:

 nacar {group = '...'
 country = '..'
 uuaa = 'UUAA'
 circuit = 'TST1'}* </TODO>
 */
def call(Map configuration) {

/*
    if (!fromMap(configuration).checkMandatory(['levels'])) {
        return
    }*/

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
        stageUnstash(artifactVersion)

        // Deploy with dimensions
        deployResult = stageDimensions(configuration)
        if (!deployResult) {
            errorMessage = "dimensions :: error :: Got empty result from dimensions, check logs"
        } else if (!deployResult.ok) {
            errorMessage = "dimensions :: error :: ${deployResult}"
        } else {
            configuration.deployLevel++
            Modules.vtrack.updateVtrackDeployResult([
                    userdeploy: Settings.repo.GIT_AUTHOR_EMAIL,
                    date      : Helpers.dateToUnix(deployResult.date, true),
                    params    : [
                            UUAA                  : Settings.uuaa,
                            pimp_id               : deployResult.values[0]['pimp_id'],
                            pimp_request_date     : Helpers.now(true),
                            pimp_implantation_date: Helpers.dateToUnix(deployResult.values[0]['date'], true, false),
                            environment           : configuration.levels[configuration.deployLevel]
                    ]
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