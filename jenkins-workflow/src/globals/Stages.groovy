package globals

import ci.WorkflowStep

class Stages {
    static void executeStage(Map configuration, String stageLabel) {
        if (!configuration) { return }

        Helpers.log.debug("Executing '${stageLabel} with:\n${configuration}")
        WorkflowStep.newCps(configuration).executeStep()
    }
}




