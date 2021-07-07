package workflow.ci.stages


import globals.Modules
import globals.Settings
import org.junit.Before
import org.junit.Test
import workflow.jenkins.BasePipelineTest

import static org.assertj.core.api.Assertions.assertThat

class DeployTest extends BasePipelineTest {

    void setPathToScript() {
        this.pathToScript = "src/ci/stages/Deploy.groovy"
    }

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
    }

    @Test
    void "No configuration and nothing happens"() {
        // Given an empty configuration
        Settings.deploy = [:]

        // When we execute this stage
        systemUnderTest.execute()

        // Then not even a stage has been created
        assertThat(helper.getCallStack().findAll { it.methodName == "stage" }.size()).isEqualTo(0)

        // And no errors are thrown
        assertThat(helper.getCallStack().findAll { it.methodName == "error" }.size()).isEqualTo(0)
    }

    @Test
    void "With some configuration, it gets executed"() {
        // Given some deploy configuration
        Settings.deploy = [
                environment: [],
                steps      : [
                        [:]
                ]
        ]

        // And some fake Vtrack data it needs
        Modules.load('vtrack')
        Modules.vtrack.setBuild([
                        generateDeployMap: { _ -> [:] }
                ])

        // And Samuel is configured and disabled
        Settings.modules.samuel = [enabled: false]
        Modules.load('samuel')

        // When we execute this stage
        systemUnderTest.execute()

        // And No errors are thrown
        assertThat(helper.getCallStack().findAll { it.methodName == "error" }.size()).isEqualTo(0)
    }
}
