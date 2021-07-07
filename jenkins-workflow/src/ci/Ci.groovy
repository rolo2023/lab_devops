package ci
/**
 * Main functions that are called from the vars/**Pipeline.groovy entrypoints.
 * We assume they are called from a valid node or container!
 */

import ci.stages.*
import globals.*
import jenkins.model.Jenkins

def prepareConfiguration(Map config = [:]) {

    Helpers.jenkins = this
    Helpers.plugins = Jenkins.instance.pluginManager.plugins

    // Make sure we check out in a clean configuration
    Helpers.cleanNode()

    Load.newCps().execute(config)
}

String getBuildSettings() {
    return Settings.build
}

String getDeploySettings() {
    return Settings.deploy
}

String getBuildAgentLabel() {
    "build_agent-${Settings.configurationSetIdentifier}"
}

String getDeployAgentLabel() {
    "deploy_agent-${Settings.configurationSetIdentifier}"
}

/**
 * Build stage also includes Testing and Publish, by default
 * These are the steps defined in your configuration, under the
 * 'test' and 'publish' sections, respectively.
 *
 * It is way easier to share the workspace this way, rather than
 * stashing and unstashing all the time -stashing has a size limit.
 *
 * Samuel behaviours might use the workspace, so we should pack them all together here.
 *
 */
void executeBuild() {
    def buildStage = Build.newCps()
    def buildError = null

    try {
        buildStage.setupBuild()
        buildStage.executePreBuildBehaviours()
        buildStage.execute()

        // Tests will be executed if any defined
        Test.newCps().execute()

        // Same for Publish
        Publish.newCps().execute()

        // These will only run if a deploy is happening
        Deploy.newCps().executePreDeployBehaviours()
    } catch(any) {
        buildError = any
    }

    // Register build info in Vtrack now, even if it has failed
    Modules.vtrack.registerBuild()

    // Clean up
    Helpers.cleanNode()

    // If there was an error, exit now
    if (buildError) { throw buildError }
}

void executeDeploy() {
    Deploy.newCps().execute()
    Helpers.log.debug('Ci :: Deploy successful')
}

// If there are 'ending' steps defined, execute them now
void executeEndSteps() {
    End.newCps().execute()
    Helpers.log.debug('Ci :: End steps successful')
}

return this
