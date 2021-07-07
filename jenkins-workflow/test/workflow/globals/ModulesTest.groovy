package workflow.globals

import globals.*
import org.junit.*
import workflow.BaseTest

import static org.assertj.core.api.Assertions.assertThat

class ModulesTest extends BaseTest {

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()

        // This should mock all Jenkins variables and methods in
        Helpers.jenkins = helper.loadScript("src/jenkins/Pipeline.groovy", binding)
    }

    @Test
    void "I cannot load a non-existing module"() {
        // Given an invalid module name
        def moduleName = 'google'

        // When I load the module
        Modules.load(moduleName)

        // Then there are no errors
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(0)

        // And an INFO message has been shown
        assertThat(helper.callStack.findAll { it.methodName == 'echo' }.size()).isEqualTo(1)
    }

    /**
    * TODO This module should eventually dissapear
    */
    @Test
    void "I can load the Ephemerals module"() {
        // Given the minimum running configuration for Ephemerals
        Settings.modules.ephemerals = [:]

        // When I load the module
        Modules.load('ephemerals')

        // Then there are no  errors
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(0)

        // And the module returned is not empty
        assertThat(Modules.ephemerals).isNotNull()
    }

    @Test
    void "I can load the Vtrack module"() {
        // When I load the module
        Modules.load('vtrack')

        // Then there are no  errors
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(0)

        // And the module returned is not empty
        assertThat(Modules.vtrack).isNotNull()
    }

    @Test
    void "I can load the Git  module"() {
        // When I load the module
        Modules.load('git')

        // Then there are no  errors
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(0)

        // And the module returned is not empty
        assertThat(Modules.git).isNotNull()
    }

    @Test
    void "I can load the Artifactory module"() {
        // Given a templated artifactory configuration
        Settings.modules.artifactory = [
                credentials_id: '{{ vars.credentials_id }}'
        ]

        // And the matching value for that template
        Settings.vars = [
                credentials_id: 'the_credential'
        ]

        // When I load the module
        Modules.load('artifactory')

        // Then there are no  errors
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(0)

        // And the module returned is not empty
        assertThat(Modules.artifactory).isNotNull()
    }

    @Test
    void "I can load the Bitbucket module"() {
        // Given the minimum running configuration for Bitbucket
        Settings.modules.bitbucket = [:]

        // When I load the module
        Modules.load('bitbucket')

        // Then there are no  errors
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(0)

        // And the module returned is not empty
        assertThat(Modules.bitbucket).isNotNull()
    }
}
