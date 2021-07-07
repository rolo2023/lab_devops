package ci.stages

import ci.WorkflowStep
import globals.Helpers
import globals.Settings
import jenkins.Pipeline

static EndCps newCps() { return new EndCps() }

class EndCps extends Pipeline {

    Boolean shouldExecute() { Settings.end && Settings.end.keySet().size() > 0 }

    private List allowedStages = ['always', 'success', 'error', 'unstable', 'changed', 'fixed']

    private Boolean shouldRun(stage) {
        if (!allowedStages.contains(stage)) {
            Helpers.log.warn "Unsupport 'post' stage name: ${stage}"
            return false
        }

        if (stage == 'always') return true

        def thisBuild = Helpers.jenkins.currentBuild
        String buildResult = thisBuild.result
        if (stage == 'success' && buildResult == 'SUCCESS') return true
        if (stage == 'error' && buildResult == 'FAILURE') return true
        if (stage == 'unstable' && buildResult == 'UNSTABLE') return true

        String lastBuildResult = thisBuild.rawBuild.getPreviousBuild()?.getResult() ?: null
        def statusChanged = lastBuildResult != null && lastBuildResult != buildResult
        if (stage == 'changed' && statusChanged) return true
        if (stage == 'fixed' && buildResult == 'SUCCESS' && statusChanged) return true

        Helpers.log.debug("Will NOT run stage ${stage} with build result == '${buildResult}'")
        return false
    }

    void runStage(Map config) {
        try {
            WorkflowStep.newCps(config).executeStep()
        } catch (Exception anything) {
            Helpers.log.error("When running steps I got an error: ${anything}")
        }
    }

    void execute() {
        if (!shouldExecute()) {
            return
        }

        try {
            Settings.end.keySet().each { String post_stage ->
                if (shouldRun(post_stage)) {
                    Settings.end.get(post_stage).each { runStage(it as Map) }
                }
            }
        } catch (Exception endStepException) {
            Helpers.error endStepException
        }
    }
}

return this
