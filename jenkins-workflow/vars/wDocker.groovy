import globals.*

def runningOnKubernetesMaster() {
    // This plugin can only be found in the Jenkins Kubernetes Masters
    Jenkins.instance.pluginManager.plugins.any { it.getShortName().equals('kube-agent-management') }
}

/**
 * withDocker
 *
 * Abstraction of the underlying Docker engine, so that we can build,
 *  run or publish to a Registry from within a Pipeline step
 *
 * IMPORTANT: This is only availabkle for mesos Masters. In the Kubernetes masters you need to use Kaniko.
 *
 * Parameters:
 * - action: Either 'push' or 'build'
 * - name
 */
def call(String action, Map config=[:]){
    if (runningOnKubernetesMaster()) {
        log.warn """
            Docker module is not available in Kubernetes masters.
            Using Kaniko....
        """
        return kaniko(config)
    }

    fromMap(config).checkMandatory('action')
    config.name = config.name ?: env.BUILD_NUMBER as String

    if (!validateAction(config.action)) {
        error "Unknown or unavailable action '${action}'"
        return
    }

    def registryUrl = "globaldevtools.bbva.com:5000"
    def credentialsId = Settings.modules.artifactory.credentialsId
    credentialsId = credentialsId
    withCredentials([
            usernamePassword(credentialsId: credentialsId, passwordVariable: 'password', usernameVariable: 'user')
    ]) {
        //* Useful in demos...
        sh "docker network prune -f"
        sh "docker login -u ${user} -p ${password} ${registryUrl}"
        this."docker${action.capitalize()}"(config)
        sh "docker logout ${registryUrl}"
    }
}

private def validateAction(String action) {
    action in ['build', 'push', 'run']
}

private nameFromConfig(config) {
    fromMap(config).checkMandatory('name')
    def name = config.name
    if (config.registry) {
        return "${config.registry}/${name}"
    }
    return name
}

/**
 * Allows running the named image inside Jenkins.
 * Optional parameters are sent as docker run parameters:
 * - entrypoint: choose a different entrypoint
 */
private void dockerRun(Map config) {
    def name = nameFromConfig(config)
    List args = [name]
    if (config.entrypoint) { args << ["--entrypoint ${config.entrypoint}"] }
    sh script: "docker run ${args.join(' ')}"
}

private void dockerBuild(Map config) {
    String dockerfile = config.dockerfile ?: 'Dockerfile'

    //sh "#!/bin/sh\ndocker build -t ${this.name} -f ${dockerfile} ."
    def name = nameFromConfig(config)
    sh script: "docker build --pull -t ${name} -f ${dockerfile} ."
}

private void dockerPush(Map config) {
    def name = nameFromConfig(config)
    def version = config.version ?: Settings.artifact.version
    sh script: """
    docker tag ${name} ${name}:${version}
    docker push ${name}:${version}
    """
}

/**
 * When called automatically from within a step, this will be invoked.
 * Use it to sanitize parameters before sending them to the 'call' method
 * @param stepArgs branch dependent parameters
 */
def fromStep(Map stepArgs) {
    this.call(stepArgs.action, stepArgs)
}

//* Used during testing to return a callable script
return this
