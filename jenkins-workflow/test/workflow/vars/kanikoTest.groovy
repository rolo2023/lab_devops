package workflow.vars

import globals.Settings
import org.junit.Before
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat


class kanikoTest extends BaseVarsTest {

    void setPathToScript() {
        this.pathToScript = "vars/kaniko.groovy"
    }

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()

        helper.registerAllowedMethod("container", [Map.class, Closure.class], { m, c -> c() })
        helper.registerAllowedMethod("wrap", [Map.class, Closure.class], { m, c -> c() })
    }

    @Test
    void "'kaniko' cannot be called without name"() {
        // Given an invalid configuration
        Map stepParams = [
                version: '2.0.0'
        ]

        // When I call the 'kaniko' module from a step
        systemUnderTest.fromStep(stepParams)
        printCallStack()

        // Then there should be an error
        assertThat(helper.callStack.findAll {
            it.methodName == 'error' && it.argsToString().contains('Missing mandatory: name')
        }.size()).isEqualTo(1)
    }

    @Test
    void "'kaniko' can be called with name only"() {
        // Given some configuration
        Map stepParams = [
                name: 'project/app',
        ]

        // And the version set in Settings
        Settings.artifact = [version: 'SNAPSHOT-1014']

        // When I call the 'kaniko' module from a step
        systemUnderTest.fromStep(stepParams)
        printCallStack()

        // Then there should be no errors
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(0)

        // And the expect command should be there
        assertThat(helper.callStack.findAll { it.methodName == 'sh' }.any { c ->
            c.argsToString().contains('/kaniko/executor --context `pwd` --destination globaldevtools.bbva.com:5000/project/app:SNAPSHOT-1014')
        }).isTrue()
    }


    @Test
    void "'kaniko' can be called with name and version"() {
        // Given some configuration
        Map stepParams = [
                name   : 'project/app',
                version: '2.0.0'
        ]

        // When I call the 'kaniko' module from a step
        systemUnderTest.fromStep(stepParams)
        printCallStack()

        // Then there should be no errors
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(0)

        // And the expect command should be there
        assertThat(helper.callStack.findAll { it.methodName == 'sh' }.any { c ->
            c.argsToString().contains('/kaniko/executor --context `pwd` --destination globaldevtools.bbva.com:5000/project/app:2.0.0')
        }).isTrue()
    }

    @Test
    void "'kaniko' can be called with name and version and absolute dockerfile"() {
        // Given some configuration
        Map stepParams = [
                name      : 'app',
                version   : '2.0.0',
                dockerfile: '/absolute/path/to/dockerfile/Dockerfile'
        ]

        // When I call the 'kaniko' module from a step
        systemUnderTest.fromStep(stepParams)
        printCallStack()

        // Then there should be no errors
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(0)

        // And the expect command should be there
        assertThat(helper.callStack.findAll { it.methodName == 'sh' }.any { c ->
            c.argsToString().contains("cd '/absolute/path/to/dockerfile'")
        }).isTrue()
    }

    @Test
    void "'kaniko' can be called with name and version and relative dockerfile"() {
        // Given some configuration
        Map stepParams = [
                name      : 'app',
                version   : '2.0.0',
                dockerfile: 'path/to/dockerfile/Dockerfile'
        ]

        // When I call the 'kaniko' module from a step
        systemUnderTest.fromStep(stepParams)
        printCallStack()

        // Then there should be no errors
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(0)

        // And the expect command should be there
        assertThat(helper.callStack.findAll { it.methodName == 'sh' }.any { c ->
            c.argsToString().contains("cd 'path/to/dockerfile'")
        }).isTrue()
    }

    @Test
    void "'kaniko' can be called with a custom container name"() {
        // Given some configuration
        Map stepParams = [
                name     : 'app',
                version  : '2.0.0',
                container: 'the_kaniko_node'
        ]

        // When I call the 'kaniko' module from a step
        systemUnderTest.fromStep(stepParams)
        printCallStack()

        // Then there should be no errors
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(0)

        // And the expect command should be there
        assertThat(helper.callStack.findAll { it.methodName == 'container' }.any { c ->
            c.argsToString().contains("the_kaniko_node")
        }).isTrue()
    }

    @Test
    void "kaniko with 'push' action does not get executed"() {
        // Given some configuration
        Map stepParams = [
                name  : 'app',
                action: 'push'
        ]

        // When I call the 'kaniko' module from a step
        systemUnderTest.fromStep(stepParams)
        printCallStack()

        // Then there should be no errors
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(0)

        // And the expect command should NOT be there
        assertThat(helper.callStack.findAll { it.methodName == 'sh' }.any { c ->
            c.argsToString().contains('/kaniko/executor')
        }).isFalse()
    }

    @Test
    void "'kaniko' can be called with a context path and a dockerfile"() {
        // Given some configuration
        Map stepParams = [
                name       : 'app',
                version    : '2.0.0',
                dockerfile : 'path/to/dockerfile/Dockerfile',
                contextPath: 'path/to/context/'
        ]

        // When I call the 'kaniko' module from a step
        systemUnderTest.fromStep(stepParams)
        printCallStack()

        // Then there should be no errors
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(0)

        // And the expect command should be there
        List shellCalls = helper.callStack.findAll { it.methodName == 'sh' }
        assertThat(shellCalls.any { it.argsToString().contains("cd 'path/to/context/'") }).isTrue()
        assertThat(shellCalls.any { it.argsToString().contains("cp 'path/to/dockerfile/Dockerfile' .") }).isTrue()
        assertThat(shellCalls.any { it.argsToString().contains('/kaniko/executor --context `pwd` --destination globaldevtools.bbva.com:5000/app:2.0.0') }).isTrue()
    }

    @Test
    void "'kaniko' can be called with a context path but NO dockerfile"() {
        // Given some configuration
        Map stepParams = [
                name       : 'app',
                version    : '2.0.0',
                contextPath: 'path/to/context/'
        ]

        // When I call the 'kaniko' module from a step
        systemUnderTest.fromStep(stepParams)
        printCallStack()

        // Then there should be no errors
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(0)

        // And the expect command should be there
        List shellCalls = helper.callStack.findAll { it.methodName == 'sh' }
        assertThat(shellCalls.any { it.argsToString().contains("cd 'path/to/context/'") }).isTrue()
        assertThat(shellCalls.any { it.argsToString().contains("cp 'path/to/dockerfile/Dockerfile' .") }).isFalse()
        assertThat(shellCalls.any { it.argsToString().contains('/kaniko/executor --context `pwd` --destination globaldevtools.bbva.com:5000/app:2.0.0') }).isTrue()
    }
}
