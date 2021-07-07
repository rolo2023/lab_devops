package workflow.ci.stages

import globals.Modules
import globals.Settings
import org.junit.Before
import org.junit.Test
import workflow.BaseTest
import workflow.jenkins.BasePipelineTest

import static org.assertj.core.api.Assertions.assertThat

class PublishTestBasePipelineTest extends BasePipelineTest {

    void setPathToScript() {
        this.pathToScript = "src/ci/stages/Publish.groovy"
    }

    @Test
    void "No configuration and nothing happens"() {
        // Given an empty configuration
        Settings.publish = [:]

        // When we execute this stage
        systemUnderTest.execute()

        // Then not even a stage has been created
        assertThat(helper.getCallStack().findAll { it.methodName == "stage" }.size()).isEqualTo(0)

        // And no errors are thrown
        assertThat(helper.getCallStack().findAll { it.methodName == "error" }.size()).isEqualTo(0)
    }

    @Test
    void "With some configuration, it gets executed"() {
        // Given some publish configuration
        Settings.publish = [
                environment: [],
                steps      : [
                        [:]
                ]
        ]

        // And some mocked-up vtrack calls
        Modules.vtrack = [
                registerArtifact   : { -> [] },
                registerBuildAction: { action, result -> }
        ]

        // When we execute this stage
        systemUnderTest.execute()

        // Then no errors are thrown
        assertThat(helper.getCallStack().findAll { it.methodName == "error" }.size()).isEqualTo(0)
    }
}
