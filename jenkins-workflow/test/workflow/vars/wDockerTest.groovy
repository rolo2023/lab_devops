package workflow.vars

import globals.Settings
import org.junit.Before
import org.junit.Test

import static com.lesfurets.jenkins.unit.MethodCall.callArgsToString
import static org.assertj.core.api.Assertions.assertThat

class wDockerTest extends BaseVarsTest {

    void setPathToScript() {
        this.pathToScript = "vars/wDocker.groovy"
    }

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
        systemUnderTest.metaClass.runningOnKubernetesMaster = { -> false }
    }

    //* Assert that login and logout have been called
    private void checkLoginLogout() {
        assertThat(helper.callStack.findAll { it.methodName == 'sh' }.any { m ->
            callArgsToString(m)
                .contains('docker login -u test_value_for_user -p test_value_for_password globaldevtools.bbva.com:5000')
        }).isTrue()

        assertThat(helper.callStack.findAll { it.methodName == 'sh' }.any { m ->
            callArgsToString(m)
                .contains('docker logout globaldevtools.bbva.com:5000')
        }).isTrue()
    }

    @Test
    void "docker build can be called with the minimum configuration"() {
        // Given the minium configuration for Docker Push
        Map stepParams = [
          action: 'build',
          name: 'test_image_name'
        ]

        // And some fake Artifactory credentials:
        globals.Settings.modules << [artifactory: [credentialsId: 'superCreds']]

        // When I call the 'dockerModule' module from a step
        systemUnderTest.fromStep(stepParams)

        // Then there should be no errors
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).is(0)

        // And is wrapped by docker login/logout calls
        checkLoginLogout()

        // And the docker command must have a docker build
        assertThat(helper.callStack.findAll { it.methodName == 'sh' }.any { m ->
            callArgsToString(m)
                .contains("docker build --pull -t ${stepParams.name} -f Dockerfile .")
        }).isTrue()
    }

    @Test
    void "docker run can be called with the minimum configuration"() {
        // Given the minium configuration for Docker Push
        Map stepParams = [
          action: 'run',
          name: 'test_image_name'
        ]

        // And some fake Artifactory credentials:
        globals.Settings.modules << [artifactory: [credentialsId: 'superCreds']]

        // When I call the 'dockerModule' module from a step
        systemUnderTest.fromStep(stepParams)

        // Then there should be no errors
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).is(0)

        // And is wrapped by docker login/logout calls
        checkLoginLogout()

        // And the docker command must have the docker run format
        assertThat(helper.callStack.findAll { it.methodName == 'sh' }.any { m ->
            callArgsToString(m)
                .contains("docker run ${stepParams.name}")
        }).isTrue()
    }

    @Test
    void "docker push can be called with the minimum configuration"() {
        // Given the minium configuration for Docker Push
        Map stepParams = [
          action: 'push',
          name: 'test_image_name'
        ]

        // And some fake Artifactory credentials:
        globals.Settings.modules << [artifactory: [credentialsId: 'superCreds']]

        // And a valid artifact version
        Settings.artifact = [version: '234567']

        // When I call the 'dockerModule' module from a step
        systemUnderTest.fromStep(stepParams)

        // Then there should be no errors
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).is(0)

        // And is wrapped by docker login/logout calls
        checkLoginLogout()

        // And the docker command must have a docker tag
        assertThat(helper.callStack.findAll { it.methodName == 'sh' }.any { m ->
            callArgsToString(m)
                .contains("docker tag ${stepParams.name}")
        }).isTrue()

        // And the docker command must have a docker push
        assertThat(helper.callStack.findAll { it.methodName == 'sh' }.any { m ->
            callArgsToString(m)
                .contains("docker push ${stepParams.name}")
        }).isTrue()
    }

    @Test
    void "docker push can be called with version"() {
        // Given the minium configuration for Docker Push
        Map stepParams = [
          action: 'push',
          name: 'test_image_name',
          version: 'test-version'
        ]

        // And some fake Artifactory credentials:
        globals.Settings.modules << [artifactory: [credentialsId: 'superCreds']]

        // And a valid artifact version
        //Settings.artifact = [version: '234567']

        // When I call the 'dockerModule' module from a step
        systemUnderTest.fromStep(stepParams)

        // Then there should be no errors
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).is(0)

        // And is wrapped by docker login/logout calls
        checkLoginLogout()

        // And the docker command must have a docker tag
        assertThat(helper.callStack.findAll { it.methodName == 'sh' }.any { m ->
            callArgsToString(m)
                .contains("docker tag ${stepParams.name}")
        }).isTrue()

        // And the docker command must have a docker push
        assertThat(helper.callStack.findAll { it.methodName == 'sh' }.any { m ->
            callArgsToString(m)
                .contains("docker push ${stepParams.name}:${stepParams.version}")
        }).isTrue()
    }
}
