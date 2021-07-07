package workflow.modules

import globals.*
import org.junit.Before
import org.junit.Test
import workflow.jenkins.BasePipelineTest

import static org.assertj.core.api.Assertions.assertThat

class EphemeralsTest extends BasePipelineTest {

    void setPathToScript() {
        this.pathToScript = "src/modules/Ephemerals.groovy"
    }

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
    }

    @Test
    void "I can init the module correctly"() {
        // Given the minimum running configuration for Ephemerals
        Settings.modules.ephemerals = [:]

        // When I init the module
        systemUnderTest.init()

        // Then there are no  errors
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(0)

    }

    @Test
    void "I can add a new set of parameters when we load the Ephemerals module"() {
        // Given the minimum running configuration for Ephemerals
        Settings.modules.ephemerals = [:]

        // And an empty set of params
        Settings.params = []

        // When we call the module's setParams() method
        systemUnderTest.setParams()

        // Then we have a new set of params
        assertThat(Settings.params.size()).isEqualTo(6)
    }
}
