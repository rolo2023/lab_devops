package workflow.vars

import org.junit.Before
import org.junit.Test

import static org.assertj.core.api.Assertions.*

class mavenTest extends BaseVarsTest {

    void setPathToScript() {
        this.pathToScript = "vars/maven.groovy"
    }

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()

        helper.registerAllowedMethod("error", [String.class], { e -> throw new PipelineRuntimeException(e) } )
    }

    private void mockJenkinsEnvironment() {
        systemUnderTest.binding.setVariable('WORKSPACE', '/your/home')
        systemUnderTest.binding.setVariable('env', [:])
        helper.registerAllowedMethod('tool', [String.class], { s -> s })
    }

    @Test
    void "Using an 'env' credential type tells maven to read values from environment"() {
        // Given a series of mocks that are available when running in jenkins
        mockJenkinsEnvironment()

        // And a set of normally configured Artifactory credentials
        globals.Settings.modules.artifactory = [credentialsId: 'artifactory_credentials']

        // When I invoke the maven module
        systemUnderTest.call {
            goal = 'run'
            mavenTool = 'mvn3.5'
            javaTool = 'java8'
            mavenSettings = 'env: ARTIFACTORY_CREDENTIALS'
        }

        // Then a maven command is executed
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(0)
        String mavenArgs = helper.callStack.find { it.methodName == 'sh' }.args[0]
        assertThat(mavenArgs).contains('run')

        // Just to be sure!!
        assertThat(mavenArgs).doesNotContain(' -s ')

        // And the proper values are set in the environment
        String envArgs = helper.callStack.find { it.methodName.contains('withEnv') }.args[0]
        assertThat(envArgs).contains('mvn3.5')
        assertThat(envArgs).contains('java8')
        assertThat(envArgs).contains('ARTIFACTORY_CREDENTIALS_USR')
        assertThat(envArgs).contains('ARTIFACTORY_CREDENTIALS_PSW')

        // And a withCredentials block is invoked
        assertThat(helper.callStack.findAll { it.methodName.contains('withCredentials') }.size()).isEqualTo(1)
    }
    @Test
    void "Using a 'file' credential type will tell maven to use a settings file"() {
        // Given a series of mocks that are available when running in jenkins
        mockJenkinsEnvironment()

        // When I invoke the maven module
        systemUnderTest.call {
            goal = 'run'
            mavenTool = 'mvn3.5'
            javaTool = 'java8'
            mavenSettings = 'file: settings_artifactory'
        }

        // Then a maven command is executed
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(0)
        String mavenArgs = helper.callStack.find { it.methodName == 'sh' }.args[0]
        assertThat(mavenArgs).contains('run')
        assertThat(mavenArgs).contains('-s test_value_for_settingsXml')

        // And the propr tools are configured
        String envArgs = helper.callStack.find { it.methodName.contains('withEnv') }.args[0]
        assertThat(envArgs).contains('mvn3.5')
        assertThat(envArgs).contains('java8')

        // And a withCredentials block is invoked
        assertThat(helper.callStack.findAll { it.methodName.contains('withCredentials') }.size()).isEqualTo(1)
    }

    @Test(expected = PipelineRuntimeException.class)
    void "I cannot use cachelo without all its parameters"() {
        systemUnderTest.call {
            mavenSettings = 'file: credentialID'
            goal ='some_goal'
            with_cachelo = [:]
        }
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(1)
    }

    @Test(expected = PipelineRuntimeException.class)
    void "I cannot call maven without a goal"() {
        systemUnderTest.call {
            mavenSettings = 'file: credentialID'
        }
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(1)
    }

    @Test(expected = PipelineRuntimeException.class)
    void "Credential type other than 'file' or 'env' is not valid"() {
        systemUnderTest.call {
            goal = 'clean install'
            mavenSettings = 'id: credentialID'
        }
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(1)
    }

    @Test
    void "If cachelo parameters are OK, library is called and downloadAttempted"() {
        // Given parameters that will trigger a cachelo download
        def cacheloClosure = {
            goal = 'clean install'
            mavenSettings = 'file: credentialFile'
            with_cachelo = [
                key: 'cachelo_key',
                paths: ['/tmp']
            ]
        }

        // And a series of Mocks available in Jenkins
        mockJenkinsEnvironment()

        // And mocked 'cachelo' methods
        helper.registerAllowedMethod('downloadCache', [Map.class], { m -> })
        helper.registerAllowedMethod('uploadCache', [Map.class], { m -> })

        // When we call maven
        systemUnderTest.call(cacheloClosure)

        // Then a download has happened
        assertThat(helper.callStack.findAll { it.methodName == 'downloadCache' }).hasSize(1)

        // And the library has been loaded
        assertThat(helper.callStack.findAll { m ->
            m.methodName.contains('library') && m.argsToString().contains('cachelo@') }).hasSize(1)

        // And an uploadIs done at the end
        assertThat(helper.callStack.findAll { it.methodName == 'uploadCache' }).hasSize(1)
    }

    @Test
    void "If cachelo download fails, upload is not attempted"() {
        // Given parameters that will trigger a cachelo download
        def cacheloClosure = {
            goal = 'clean install'
            mavenSettings = 'file: credentialFile'
            with_cachelo = [
                key: 'cachelo_key',
                paths: ['/tmp']
            ]
        }

        // And a series of Mocks available in Jenkins
        mockJenkinsEnvironment()

        // And a downloadCache method that fails
        helper.registerAllowedMethod('downloadCache', [Map.class], { m ->
            throw new PipelineRuntimeException("Error")
        })

        // When we call maven
        systemUnderTest.call(cacheloClosure)

        // Then a download has happened
        assertThat(helper.callStack.findAll { it.methodName == 'downloadCache' }.size()).isEqualTo(1)

        // But no upload
        assertThat(helper.callStack.findAll { it.methodName == 'uploadCache' }.size()).isEqualTo(0)

        // And no errors
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(0)
    }

    @Test
    void "If cachelo upload fails, no error should be raised"() {
        // Given parameters that will trigger a cachelo download/upload
        def cacheloClosure = {
            goal = 'clean install'
            mavenSettings = 'file: credentialFile'
            with_cachelo = [
                key: 'cachelo_key',
                paths: ['/tmp']
            ]
        }

        // And a series of Mocks available in Jenkins
        mockJenkinsEnvironment()
        helper.registerAllowedMethod('downloadCache', [Map.class], { m -> })

        // And an uploadCache method that fails
        helper.registerAllowedMethod('uploadCache', [Map.class], { m ->
            throw new PipelineRuntimeException("Error")
        })

        // When we call maven
        systemUnderTest.call(cacheloClosure)

        // Then a download has happened
        assertThat(helper.callStack.findAll { it.methodName == 'downloadCache' }.size()).isEqualTo(1)

        // And an upload
        assertThat(helper.callStack.findAll { it.methodName == 'uploadCache' }.size()).isEqualTo(1)

        // And no errors
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(0)
    }

    @Test
    void "When called from a Step, all parameters get to the entrypoint function"() {
        // Given the parameters as they would be in a step 'with_params' map
        Map mavenArgs = [
            goal: 'clean install',
            maven_settings: 'file: SettingsName',
            with_cachelo: [
                key: 'cachelo_key',
                paths: ["/home/jenkins/.m2", '/tmp']
            ]
        ]

        // And a series of Mocks available in Jenkins
        mockJenkinsEnvironment()
        helper.registerAllowedMethod('downloadCache', [Map.class], { m -> })
        helper.registerAllowedMethod('uploadCache', [Map.class], { m -> })

        // When we call maven through fromStep
        systemUnderTest.fromStep(mavenArgs)

        // Then no error has happened
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(0)

        // And a download has happened
        assertThat(helper.callStack.findAll { it.methodName == 'downloadCache' }.size()).isEqualTo(1)

        // And an upload
        assertThat(helper.callStack.findAll { it.methodName == 'uploadCache' }.size()).isEqualTo(1)
    }
}
