import globals.Settings

/**
 * kaniko
 *
 * It only supports using current PWD as context
 *
 *
 * Mandatory parameters:
 * - name: Name of the image to be created
 * optional parameters
 * - dockerfile: path to dockerfile, either absolute or relative
 * - version: version of the image, if not set will default to the computed one for the artifact
 * - registry: registry path for the image (final path will be registry/name:version)
 * - container: name of the container with the Kaliko image, defined alongside your application pod
 * - contextPath: if set, the Docker context will be set to it
 */
def call(Map config) {

    if (!validate(config)) {
        return
    }

    if (config.action) {
        log.info """
            With kaniko, the only option is to build and push in one go.
            If 'action' is set, it will be ignored.
            For retrocompatibility, if it is set to 'build' we will execute Kaniko.
        """
        if (['run', 'push'].contains(config.action.toLowerCase())) {
            return
        }
    }

    // Create remote image name
    def registry = config.registry ?: "globaldevtools.bbva.com:5000"
    def version = config.version ?: Settings.artifact.version
    def remoteImageName = "${registry}/${config.name}:${version}"

    // path to dockerfile, without ending slash
    String dockerfilePath = config.dockerfile ? config.dockerfile : ""
    dockerfilePath = dockerfilePath.replaceFirst('/Dockerfile', '').replaceFirst(/^.*\/\+$/, '')

    /**
     * If a contextPath is set, we need to move there.
     * If not, and a Dockerfile exists, we use its directory as context.
     * The default case is to use current working dir and assume a Dockerfile resides there.
     */
    List shellCommand = ["#!/busybox/sh +x"]
    if (config.containsKey('contextPath')) {
        shellCommand << "cd '${config.contextPath}'"
        if (config.containsKey('dockerfile')) {
            shellCommand <<  "cp '${config.dockerfile}' ."
        }
    } else if (config.containsKey('dockerfile')) {
        dockerfilePath = config.dockerfile.replaceFirst('/Dockerfile', '').replaceFirst(/^.*\/\+$/, '')
        shellCommand << "cd '${dockerfilePath}'"
    }
    shellCommand << "/kaniko/executor --context `pwd` --destination ${remoteImageName}"

    // Execute inside defined container (it must exist in current pod definition for workspace to be shared)
    def containerName = config.container ?: 'kaniko'
    container(name: containerName, shell: '/busybox/sh') {
        withEnv(['PATH+EXTRA=/busybox']) {
            wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {
                sh """
                    ${shellCommand.join("\n")}
                """
            }
        }
    }
}

/**
 * Checks if all the conditions needed in order to execute kaniko
 *  are valid.
 * This should include input parameter validation, user/group permissions,
 *  mandatory plugins are present, etc...
 *
 * @param config Map with all the entries sent to the module in the
 *              'with_params' section of the configuration
 */
Boolean validate(Map config) {
    if (!fromMap(config).checkMandatory('name')) {
        return false
    }
    return true
}

/**
 * When called from within a step, this will be invoked AUTOMATICALLY.
 * Use it to massage parameters before sending them to the 'call' method
 *
 * @param stepArgs branch dependent parameters
 */
def fromStep(Map stepArgs) {
    call(stepArgs)
}

//* Used during testing to return a callable script
return this
