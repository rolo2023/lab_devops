package workflow.vars

import globals.Modules
import org.junit.Before
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat


class chimeraTest extends BaseVarsTest {

    void setPathToScript() {
        this.pathToScript = "vars/chimera.groovy"
    }

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()

        // We don't really want to EXECUTE the closure, just mock it
        systemUnderTest.binding.setVariable('chimeraCodeReview', [codeReview: { c -> c() }])
    }

    @Test
    void "'Chimera' can be called from a Step"() {
        // Given the mandatory information for Chimera
        Map stepParams = [
          chimeraEnvName: 'work',
          chimeraCredentialsId: 'chimera-work'
        ]

        // And some mocked-up, fake GIT Information
        globals.Settings.repo = [
            GIT_URL: 'ssh://git@globaldevtools.bbva.com:7999/bcon/kqco.git',
            GIT_BRANCH: 'develop'
        ]

        // And some mocked-up vtrack calls
        Modules.vtrack = [
                registerArtifact   : { -> [] },
                registerBuildAction: { action, result -> }
        ]

        // When I call the 'Chimera' module from a step
        systemUnderTest.fromStep(stepParams)

        // Then there should be no errors
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).is(0)

        // And the chimera Lib will have been called
        assertThat(helper.callStack.find {it.methodName == 'library'}.args[0]).isEqualTo('chimera@2.3.0')
    }

    @Test
    void "'Chimera' needs chimeraCredentialsId to work"() {
        // Given some configuration without chimeraCredentialsId
        Map stepParams = [
          chimeraEnvName: 'work'
        ]

        // When I call the 'Chimera' module from a step
        systemUnderTest.fromStep(stepParams)

        // Then there should be an error
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).is(1)
    }

    @Test
    void "'Chimera' needs an chimeraEnvName to work"() {
        // Given some configuration without chimeraEnvName
        Map stepParams = [
          chimeraCredentialsId: 'chimera-work'
        ]

        // When I call the 'Chimera' module from a step
        systemUnderTest.fromStep(stepParams)

        // Then there should be an error
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).is(1)
    }
}
