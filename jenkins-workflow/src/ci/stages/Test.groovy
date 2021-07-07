package ci.stages

import globals.*
import jenkins.Pipeline

static TestCps newCps() { return new TestCps() }

class TestCps extends Pipeline {
    def execute() {
        if (!shouldExecute()) { return }

        try {
            Settings.currentStage = 'test'
            switch (Settings.test.module) {
                case 'ephemerals':
                    return testWithEphemerals()
                default:
                    throw new Exception("Unknown test module: ${Settings.test.module}")
            }

            Modules.vtrack.registerBuildAction('test', true)
        } catch(any) {
            Modules.vtrack.registerBuildAction('test', false)
            error(any)
        }
    }

    def testWithEphemerals() {
        return Modules.ephemerals.ephemerals()
    }

    Boolean shouldExecute() {
        Settings.test.module && Helpers.canDoWhen(Settings.test)
    }
}

return this
