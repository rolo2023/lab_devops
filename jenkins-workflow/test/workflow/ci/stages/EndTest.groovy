package workflow.ci.stages

import ci.WorkflowStepImpl
import globals.Helpers
import globals.Settings
import org.junit.Test
import workflow.jenkins.BasePipelineTest

import static org.assertj.core.api.Assertions.assertThat

class EndTest extends BasePipelineTest {

    void setPathToScript() {
        this.pathToScript = "src/ci/stages/End.groovy"
    }

    @Test
    void "End won't execute if it has not, at least one condition"() {
        // Given an empty 'end' stage configuration
        Settings.end = [:]

        // Then it should not ever execute
        assertThat(systemUnderTest.shouldExecute()).isFalse()
    }

    @Test
    void "End will execute if it has at least one condition"() {
        // Given a simple 'end' stage configuration
        Settings.end.success = [
                use        : 'log',
                with_params: [:]
        ]

        // Then it will execute
        assertThat(systemUnderTest.shouldExecute()).isTrue()
    }

    @Test
    void "End will execute an 'always' condition, no matter what"() {
        // Given an 'end' stage configuration with an 'always' case
        Settings.end.always = [
                [
                        use        : 'log',
                        with_params: [:]
                ]
        ]

        // [MOCK] I need certain 'vars' files to be present
        WorkflowStepImpl.metaClass.log = helper.loadScript("vars/log.groovy")

        // And a random Jenkins state
        Helpers.jenkins.currentBuild = [
                status: 'super'
        ]

        // When we execute the end stage
        systemUnderTest.execute()

        // Then it will execute the log module without errors
        print(helper.callStack)
        assertThat(helper.callStack.findAll { it.methodName == 'error' }).isEmpty()
        assertThat(helper.callStack.findAll { it.methodName == 'fromStep' }).hasSize 1 // the log call
    }
}
