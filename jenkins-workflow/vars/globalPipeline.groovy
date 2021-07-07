def runningOnKubernetesMaster() {
    // This plugin can only be found in the Jenkins Kubernetes Masters
    Jenkins.instance.pluginManager.plugins.any { it.getShortName().equals('kube-agent-management') }
}

/**
 * Architecture affects Vtrack auditing, and can be overriden by parameters in configuration
 *
 <pre>
 globalPipeline {
    architecture = 'spring.r1'
    uuaa = 'KIFD'
    ...
 }
 </pre>
 */
def call(Closure body) { return globalPipeline('generic', body) }

def call(String architecture, Closure body) {
    def config = [
            commit        : env.GIT_COMMIT,
            buildNumber   : env.BUILD_NUMBER as Integer,
            timeoutInHours: 2,
            architecture  : architecture
    ]

    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    runningOnKubernetesMaster() ? kubePipeline(config) : mesosPipeline(config)
}

return this
