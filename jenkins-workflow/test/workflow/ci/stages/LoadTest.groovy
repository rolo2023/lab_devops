package workflow.ci.stages


import globals.Settings
import org.junit.Test
import workflow.jenkins.BasePipelineTest

import static org.assertj.core.api.Assertions.assertThat

class LoadTest extends BasePipelineTest {

    void setPathToScript() {
        this.pathToScript = "src/ci/stages/Load.groovy"
    }

    @Override
    void setUp() throws Exception {
        super.setUp()

        helper.registerAllowedMethod("string", [Map.class], { m -> m })
        helper.registerAllowedMethod("choice", [Map.class], { m -> m })
        helper.registerAllowedMethod("booleanParam", [Map.class], { m -> m })
    }

    @Test
    void "checkCredentials does not crash without any credentials"() {
        // Given a credential set
        Settings.build.environment = [:]

        // When I check credentials'
        systemUnderTest.checkCredentials()

        // I should not get an error
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).is(0)
        assertJobStatusSuccess()
    }

    @Test
    void "checkCredentials does not crash with empty credentials"() {
        // Given an empty credential set
        Settings.build.environment = [
                maven_settings: ''
        ]

        // When I check credentials'
        systemUnderTest.checkCredentials()

        // I should not get an error
        assertThat(helper.callStack.findAll { it.methodName == 'error' }).isEmpty()
        assertJobStatusSuccess()
    }

    @Test
    void "checkCredentials will fail with templated credentials if they are not found"() {
        // Given a templated credential set
        Settings.build.environment = [
                maven_settings: 'file: {{ vars.maven_settings_file }}'
        ]

        // And no matching values
        Settings.vars = [:]

        // When I check credentials
        systemUnderTest.checkCredentials()

        // I should get an error
        List errorMsgs = helper.callStack.findAll { it.methodName == 'error' }
        assertThat(errorMsgs.size()).isNotZero()
    }

    @Test
    void "checkCredentials will work with templated credentials if they are found"() {
        // Given a templated credential set
        Settings.build.environment = [
                maven_settings: 'file: {{ vars.maven_settings_file }}'
        ]

        // And its matching values
        Settings.vars = [
                maven_settings_file: 'JENKINS_CREDENTIAL_SET'
        ]

        // And valid credential entries for it
        helper.registerAllowedMethod("file", [Map.class], { Map m ->
            if (m.credentialsId.endsWith('JENKINS_CREDENTIAL_SET')) {
                return 'ok'
            }
            return null
        })

        // When I check credentials
        systemUnderTest.checkCredentials()

        // I should not get a credential not found error
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(0)
    }

    @Test
    void "checkCredentials verifies 'file' type credentials"() {
        // Given a credential set
        Settings.build.environment = [
                maven_settings: 'file: JENKINS_CREDENTIAL_SET'
        ]

        // When I check credentials'
        systemUnderTest.checkCredentials()

        // I should get a credential not found error
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isNotZero()
    }

    @Test
    void "checkCredentials does not crash with 'env' type credentials"() {
        // Given a credential set
        Settings.build.environment = [
                maven_settings: 'env: ARTIFACTORY_CREDENTIALS'
        ]

        // When I check credentials'
        systemUnderTest.checkCredentials()

        // I should not get an error
        assertThat(helper.callStack.findAll { it.methodName == 'error' }).isEmpty()
    }

    @Test
    void "I can load checkCredentials for a generic app with safe defaults"() {
        // Given current credential set
        systemUnderTest.loadCiConfig([
                country: 'es',
                group  : 'generic',
                uuaa   : 'mandatory'
        ])

        // And valid credential checks
        helper.registerAllowedMethod("string", [Map.class], { Map m -> 'ok' })

        // When I check credentials'
        systemUnderTest.checkCredentials()

        // I should not get an error
        assertThat(helper.callStack.findAll { it.methodName == 'error' }).isEmpty()
    }

    @Test
    void "I can load a configuration that contains form parameters"() {
        // Given a valid config
        def config = [
                uuaa   : 'uuaa',
                country: 'global',
                group  : 'simple_with_params'
        ]

        // When I load configuration
        systemUnderTest.loadCiConfig(config)

        // And process parameters
        Settings.processFormParameters()

        // I should not get an error
        assertThat(helper.callStack.findAll { it.methodName == 'error' }).isEmpty()

        // And those parameters now belong to the Settings class
        assertThat(Settings.params.size()).isEqualTo(3)
    }

    @Test
    void "A form parameter without alternative condition should be an error"() {
        // Given a valid config
        def config = [
                uuaa   : 'uuaa',
                country: 'global',
                group  : 'simple_with_wrong_conditional_params'
        ]

        // And that I'm in a branch not in the conditions
        Settings.repo << [branch: 'feature/something']

        // When I load configuration
        systemUnderTest.loadCiConfig(config)

        // And process parameters
        Settings.processFormParameters()

        // I should get an error
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(1)
    }

    @Test
    void "Conditional form parameters do not get rendered when condition is not met"() {
        // Given a valid config
        def config = [
                uuaa   : 'uuaa',
                country: 'global',
                group  : 'simple_with_conditional_params'
        ]

        // And that I'm in a branch not in the conditions
        Settings.repo << [branch: 'feature/something']

        // When I load configuration
        systemUnderTest.loadCiConfig(config)
        if(!Settings.artifact) Settings.artifact = [version: 'SNAPSHOT']

        // And process parameters
        Settings.processFormParameters()

        // I should not get an error
        assertThat(helper.callStack.findAll { it.methodName == 'error' }).isEmpty()

        // And only some of those parameters will be rendered
        assertThat(Settings.params.size()).isEqualTo(1)

        // And of the rest will be set as hidden
        assertThat(Settings.formValues.hiddenParams['maven_build_command']).isEqualTo('clean install versions:set -DnewVersion=SNAPSHOT')
    }

    @Test
    void "Conditional form parameters get defaultValue only when conditions are met"() {
        // Given a valid config
        def config = [
                uuaa   : 'uuaa',
                country: 'global',
                group  : 'simple_with_conditional_params'
        ]

        // and Repository information for a substitution
        Settings.artifact = [
                version: '1.3.0RC15'
        ]

        // And that I'm in a branch that matches the conditions
        Settings.repo << [branch: 'develop']

        // When I load configuration
        systemUnderTest.loadCiConfig(config)

        // And process parameters
        Settings.processFormParameters()

        // I should not get an error
        assertThat(helper.callStack.findAll { it.methodName == 'error' }).isEmpty()

        // And only some of those parameters now belong to the Settings class
        assertThat(Settings.params.size()).isEqualTo(2)

        // And of the two params with the same ID, we got the correct condition
        assertThat(Settings.params.findAll { p ->
            p.defaultValue.equals('clean install versions:set -DnewVersion=1.3.0RC15')
        }.size()).isEqualTo(1)
    }
}
