package workflow.vars

import org.junit.Before
import org.junit.Test
import org.junit.Ignore
import com.lesfurets.jenkins.unit.BasePipelineTest

import static org.assertj.core.api.Assertions.*


class fromLibraryTest extends BaseVarsTest {

    void setPathToScript() {
        this.pathToScript = "vars/fromLibrary.groovy"
    }

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
    }

    @Test
    void "'fromLibrary' can't be called from a step without parameters"() {
        Map stepParams = [:]
        systemUnderTest.fromStep(stepParams)
        printCallStack()
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(3)
    }

    @Test
    void "'fromLibrary' can't run a method without loading the library first"() {
        def loadLibrery = 1
        Map stepParams = [
          command: 'dynamic.createPackage(x.steps, x.env.WORKSPACE, "esia_project", "master")'
        ]
        try{
          systemUnderTest.fromStep(stepParams)
          printCallStack()
        } catch(Exception e) {
          if (e.getMessage() == "No such property: dynamic for class: fromLibrary") {
            loadLibrery = 0
          }
        }

        assertThat(loadLibrery).isEqualTo(0)
    }
}
