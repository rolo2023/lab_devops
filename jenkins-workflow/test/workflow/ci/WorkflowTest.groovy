package workflow.ci

import globals.*
import org.junit.Before
import org.junit.Test
import workflow.BaseTest

import static org.assertj.core.api.Assertions.assertThat

class WorkflowTest extends BaseTest {

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()

        systemUnderTest = helper.loadScript("src/ci/Ci.groovy", binding)

        // There are certain utilites that are loaded from 'vars'
        systemUnderTest.metaClass.log = helper.loadScript("vars/log.groovy", binding)

        // Involved classes should resolve methods using my resolver
        intercept(ci.stages.Load.metaClass)
    }

    @org.junit.Ignore("We have to mock up quite some things for this to work")
    @Test
    void "We can load an empty workflow and convert it to Settings"() {
        // Given a config that points to an empty workflow
        Map emptyConfig = [
            group: 'simple',
            country: 'global',
            uuaa: 'random_uuaa'
        ]

        // And some mocked commands for Load stage that would read off Jenkins
        ci.stages.Load.metaClass.checkCredentials = { -> }
        ci.stages.Load.metaClass.loadModules =  { m -> }

        // When we init the workflow
        systemUnderTest.init(emptyConfig)

        // Then no errors will have happened
        assertThat(helper.getCallStack().findAll { it.methodName == "error" }.size()).isEqualTo(0)

        // And we get proper stage configs in Settings
        assertThat(Settings.build.steps).isEmpty()
        assertThat(Settings.publish.steps).isEmpty()
    }
}
