package ci.stages

import ci.WorkflowStep
import globals.Helpers
import globals.Modules
import globals.Settings
import jenkins.Pipeline

static DeployCps newCps() { return new DeployCps() }

class DeployCps extends Pipeline {
    void executePreDeployBehaviours() {
        if (shouldExecute()) {
            Modules.samuel.executeSamuelStage 'pre-deploy'
        }
    }

    //* If there are no build steps, don't do anything
    private Boolean shouldExecute() {
        Helpers.canDoWhen(Settings.deploy) && Settings.deploy.steps?.size() > 0
    }

    void execute() {
        if (!shouldExecute()) {
            Helpers.log.info "Deploy :: no steps defined"
            return
        }

        //* TODO: Consider currentStage deprecated
        Settings.currentStage = 'deploy'

        Modules.vtrack.initVtrackDeployMap()
        try {
            WorkflowStep.newCps(Settings.deploy).executeStep()
            Modules.vtrack.updateVtrackDeployResults(true)
        } catch (any) {
            Helpers.log.error("Stages :: Deploy :: ${any}")
            Modules.vtrack.updateVtrackDeployResults(false)
        }
        Modules.vtrack.registerDeploy()
    }
}

return this
