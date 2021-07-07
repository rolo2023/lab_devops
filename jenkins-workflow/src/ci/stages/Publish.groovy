package ci.stages

import globals.*
import jenkins.Pipeline


static PublishCps newCps() { return new PublishCps() }

class PublishCps extends Pipeline {

    private static Boolean shouldExecute() {
        Settings.publish && Settings.publish.steps
    }

    def execute() {
        if (!shouldExecute()) {
            return
        }

        try {
            Settings.currentStage = 'publish'
            Stages.executeStage(Settings.publish, 'Publish Artifact')

            //* TODO This must be done inside the publish code
            Modules.vtrack.registerArtifact()
            Modules.vtrack.registerBuildAction('publish', true)
        } catch (any) {
            Modules.vtrack.registerBuildAction('publish', false)
            error(any)
        }
    }
}

return this
