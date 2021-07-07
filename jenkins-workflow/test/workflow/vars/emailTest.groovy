package workflow.vars

import org.junit.Before
import org.junit.Test
import org.junit.Ignore
import com.lesfurets.jenkins.unit.BasePipelineTest

import static org.assertj.core.api.Assertions.*


class emailTest extends BaseVarsTest {

    void setPathToScript() {
        this.pathToScript = "vars/email.groovy"
    }

    @Test
    void "'email' can be called from a Step"() {
        // Given some configuration
        Map stepParams = [
          body: "Body",
          title: "Hello!"
        ]

        // And a mocked-up emailext call
        helper.registerAllowedMethod("mail", [Map.class], null)

        // When I call the 'email' module from a step
        systemUnderTest.fromStep(stepParams)

        // Then there should be no errors
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(0)
    }
}
