package workflow.vars

import org.junit.Before
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class fromMapTest extends BaseVarsTest {

    void setPathToScript() {
        this.pathToScript = "vars/fromMap.groovy"
    }

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
    }

    @Test
    void "toEnv converts the map to an env-like list for Jenkins withEnv"() {
        // Given a map of values
        def testMap = [
            'var1': 1,
            'var2': 2
        ]

        // When we call toEnv() on it
        List result = systemUnderTest.call(testMap).toEnv()

        // Then we get a valid argument for a withEnv call
        assertThat(result[0] as String).isEqualTo("var1=1")
        assertThat(result[1] as String).isEqualTo("var2=2")
    }
}
