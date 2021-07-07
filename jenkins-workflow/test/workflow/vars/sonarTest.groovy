package workflow.vars

import globals.Modules
import org.junit.Before
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class sonarTest extends BaseVarsTest {

    void setPathToScript() {
        this.pathToScript = "vars/wSonar.groovy"
    }

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()

        // We don't really want to EXECUTE the sonar closure, just grab info
        helper.registerAllowedMethod("sonar", [Map.class, Closure.class], { options, c -> c() })

        // And some mocked-up vtrack calls
        Modules.vtrack = [
                registerArtifact   : { -> [] },
                registerBuildAction: { action, result -> }
        ]
    }

    @Test
    void "If I specify that we should wait for the WG result, a flag is true, and the rest of the parameters are the default"() {
        // Given a possible Sonar configuration
        def sonarParam = [
            waitForQualityGate: true
        ]

        // When we execute Sonar
        systemUnderTest.call(sonarParam)

        // Then no exception has been thrown
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(0)

        // And the sonar command has been called with relevant parameters
        def sonarArgs = helper.callStack.find { it.methodName == 'sonar' }.args
        assertThat(sonarArgs[0]['qualityProfile']).isEqualTo(null)
        assertThat(sonarArgs[0]['qualityGate']).isEqualTo(null)
        assertThat(sonarArgs[0]['waitForQualityGate']).isEqualTo(true)
        assertThat(sonarArgs[0]['enableIssuesReport']).isEqualTo(true)
        assertThat(sonarArgs[0]['enableQgReport']).isEqualTo(true)
    }

    @Test
    void "If I specify a Quality Profile it is used, and the rest of the parameters are the default"() {
        // Given a possible Sonar configuration
        def qualityProfileName = 'The Quality Profile'
        def sonarParams = [
            qualityProfile: qualityProfileName,
            qualityGate : ''
        ]

        // When we execute Sonar
        systemUnderTest.call(sonarParams)

        // Then no exception has been thrown
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(0)

        // And the sonar command has been called with relevant parameters
        def sonarArgs = helper.callStack.find { it.methodName == 'sonar' }.args
        assertThat(sonarArgs[0]['qualityProfile']).isEqualTo(qualityProfileName)
        assertThat(sonarArgs[0]['qualityGate']).isEqualTo(null)
        assertThat(sonarArgs[0]['waitForQualityGate']).isEqualTo(false)
        assertThat(sonarArgs[0]['enableIssuesReport']).isEqualTo(true)
        assertThat(sonarArgs[0]['enableQgReport']).isEqualTo(true)
    }

    @Test
    void "If I specify a Quality Gate it is used, and the rest of the parameters are the default"() {
        // Given a possible Sonar configuration
        def qualityGateName = 'The Quality Gate'
        Map sonarParams = [
            qualityProfile: '',
            qualityGate: qualityGateName
        ]

        // When we execute Sonar
        systemUnderTest.call(sonarParams)

        // Then no exception has been thrown
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(0)

        // And the sonar command has been called with relevant parameters
        def sonarArgs = helper.callStack.find { it.methodName == 'sonar' }.args
        assertThat(sonarArgs[0]['qualityProfile']).isEqualTo(null)
        assertThat(sonarArgs[0]['qualityGate']).isEqualTo(qualityGateName)
        assertThat(sonarArgs[0]['waitForQualityGate']).isEqualTo(false)
        assertThat(sonarArgs[0]['enableIssuesReport']).isEqualTo(true)
        assertThat(sonarArgs[0]['enableQgReport']).isEqualTo(true)
    }

        @Test
    void "If I specify a Quality Gate, a Quality Gate, and disble reporting, and the rest of the parameters are the default"() {
        // Given a possible Sonar configuration
        def qualityGateName = 'The Quality Gate'
        def qualityProfileName = 'The Quality Profile'
        Map sonarParams = [
            qualityProfile: qualityProfileName,
            qualityGate: qualityGateName,
            enableQgReport: false,
            enableIssuesReport: false
        ]

        // When we execute Sonar
        systemUnderTest.call(sonarParams)

        // Then no exception has been thrown
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(0)

        // And the sonar command has been called with relevant parameters
        def sonarArgs = helper.callStack.find { it.methodName == 'sonar' }.args
        assertThat(sonarArgs[0]['qualityProfile']).isEqualTo(qualityProfileName)
        assertThat(sonarArgs[0]['qualityGate']).isEqualTo(qualityGateName)
        assertThat(sonarArgs[0]['waitForQualityGate']).isEqualTo(false)
        assertThat(sonarArgs[0]['enableIssuesReport']).isEqualTo(false)
        assertThat(sonarArgs[0]['enableQgReport']).isEqualTo(false)
    }

    @Test
    void "If I send in some Sonar parameters, they are added to the sonar-scanner call"() {
        // Given some (random) sonar parameters and enabled module
        Map sonarParams = [
            parameters: '-Dsonar.sources=src/ -Drandom.sonar.param=yes'
        ]

        // When we execute Sonar
        systemUnderTest.call(sonarParams)

        // Then no exception has been thrown
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(0)

        // And the shell command has been called with those parameters
        def shellArgs = helper.callStack.find { it.methodName == 'sh' }.args[0] as String
        assertThat("sonar-scanner -Dsonar.sources=src/ -Drandom.sonar.param=yes").isEqualTo(shellArgs)
    }

    @Test
    void "With the minimum configuration, Sonar is executed without parameters, no Quality Gate, and no Quality Profile"() {
        // When we execute Sonar with minimum configuration
        systemUnderTest.call()

        // Then no exception has been thrown
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(0)

        // And the sonar command has been called with the default parameters
        def sonarArgs = helper.callStack.find { it.methodName == 'sonar' }.args
        assertThat(sonarArgs[0]['qualityProfile']).isEqualTo(null)
        assertThat(sonarArgs[0]['qualityGate']).isEqualTo(null)
        assertThat(sonarArgs[0]['waitForQualityGate']).isEqualTo(false)
        assertThat(sonarArgs[0]['enableIssuesReport']).isEqualTo(true)
        assertThat(sonarArgs[0]['enableQgReport']).isEqualTo(true)

        // And the shell command has been called with no parameters
        List shellArgs = ( helper.callStack.find { it.methodName == 'sh' }.args[0] as String).split()
        assertThat(shellArgs.size()).isEqualTo(1)
        assertThat("sonar-scanner").isEqualTo(shellArgs[0])
    }

    @Test
    void "If sonar would be to raise any exception, it is captured and returned after cleaning the node"() {
        // Given a sonar call that breaks, somehow
        helper.registerAllowedMethod("sonar", [Map.class, Closure.class], { options, c ->
            throw new RuntimeException("argh!")
        })

        // When we execute the Sonar stage
        try { systemUnderTest.call() }

        // Then an exception has been thrown
        finally { assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(1) }
    }

    @Test
    void "If I send the sonar parameter called command, it is used how default command"() {
        // Given some (random) sonar parameters and enabled module
        Map sonarParams = [
            command: 'mvn sonar:sonar'
        ]

        // When we execute Sonar
        systemUnderTest.call(sonarParams)

        // Then no exception has been thrown
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(0)

        // And the shell command has been called with those parameters
        def shellArgs = helper.callStack.find { it.methodName == 'sh' }.args[0] as String
        assertThat("mvn sonar:sonar ").isEqualTo(shellArgs)
    }

    @Test
    void "If I send true in useCredentialsArtifactory param, then withCredentials is invoked"() {
        // Given some (random) sonar parameters and enabled module
        Map sonarParams = [
            command: 'mvn sonar:sonar',
            useCredentialsArtifactory: true
        ]

        // Given a series of mocks that are available when running in jenkins
        mockJenkinsEnvironment()

        // And a set of normally configured Artifactory credentials
        globals.Settings.modules.artifactory = [credentialsId: 'artifactory_credentials']

        // When we execute Sonar
        systemUnderTest.call(sonarParams)

        // Then no exception has been thrown
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(0)

        String envArgs = helper.callStack.find { it.methodName.contains('withEnv') }.args[0]
        assertThat(envArgs).contains('ARTIFACTORY_CREDENTIALS_USR')
        assertThat(envArgs).contains('ARTIFACTORY_CREDENTIALS_PSW')

        // And the shell command has been called with those parameters
        def shellArgs = helper.callStack.find { it.methodName == 'sh' }.args[0] as String
        assertThat("mvn sonar:sonar ").isEqualTo(shellArgs)
    }

    private void mockJenkinsEnvironment() {
        systemUnderTest.binding.setVariable('WORKSPACE', '/your/home')
        systemUnderTest.binding.setVariable('env', [:])
    }
}
