package workflow.vars

import org.junit.Before
import org.junit.Test
import org.junit.Ignore
import com.lesfurets.jenkins.unit.BasePipelineTest

import static org.assertj.core.api.Assertions.*


class sftpTest extends BaseVarsTest {

    private Map minimumMandatoryParamsExample = [
      localDir: 'localDir',
      remoteDir: 'remoteDir',
      remoteUser: 'remoteUser',
      remoteIp: 'remoteIp',
      remoteKey: 'remoteKey',
      deployableFile: 'deployableFile',
      userCredential: 'userCredential',
    ]

    @Override
    @Before
    void setPathToScript() {
    this.pathToScript = "vars/sftp.groovy"
    }
    void setUp() throws Exception {
        super.setUp()
    }

    @Test
    void "'sftp' needs the 'localDir' parameter to work"() {
        // Given the minimum viable configuration for the SFTP module
        Map stepParams = new HashMap(this.minimumMandatoryParamsExample)

        // When I remove the 'localDir' parameter
        stepParams.remove 'localDir'

        // And I call the 'sftp' module from a step
        systemUnderTest.fromStep(stepParams)

        // I should get an error
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).is(1)
    }
    @Test
    void "'sftp' needs the 'remoteDir' parameter to work"() {
        // Given the minimum viable configuration for the SFTP module
        Map stepParams = new HashMap(this.minimumMandatoryParamsExample)

        // When I remove the 'remoteDir' parameter
        stepParams.remove 'remoteDir'

        // And I call the 'sftp' module from a step
        systemUnderTest.fromStep(stepParams)

        // I should get an error
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).is(1)
    }
    @Test
    void "'sftp' needs the 'remoteUser' parameter to work"() {
        // Given the minimum viable configuration for the SFTP module
        Map stepParams = new HashMap(this.minimumMandatoryParamsExample)

        // When I remove the 'remoteUser' parameter
        stepParams.remove 'remoteUser'

        // And I call the 'sftp' module from a step
        systemUnderTest.fromStep(stepParams)

        // I should get an error
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).is(1)
    }	
	@Test
    void "'sftp' needs the 'remoteIp' parameter to work"() {
        // Given the minimum viable configuration for the SFTP module
        Map stepParams = new HashMap(this.minimumMandatoryParamsExample)

        // When I remove the 'remoteIp' parameter
        stepParams.remove 'remoteIp'

        // And I call the 'sftp' module from a step
        systemUnderTest.fromStep(stepParams)

        // I should get an error
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).is(1)
    }
	@Test
    void "'sftp' needs the 'remoteKey' parameter to work"() {
        // Given the minimum viable configuration for the SFTP module
        Map stepParams = new HashMap(this.minimumMandatoryParamsExample)

        // When I remove the 'remoteKey' parameter
        stepParams.remove 'remoteKey'

        // And I call the 'sftp' module from a step
        systemUnderTest.fromStep(stepParams)

        // I should get an error
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).is(1)
    }
	@Test
    void "'sftp' needs the 'deployableFile' parameter to work"() {
        // Given the minimum viable configuration for the SFTP module
        Map stepParams = new HashMap(this.minimumMandatoryParamsExample)

        // When I remove the 'deployableFile' parameter
        stepParams.remove 'deployableFile'

        // And I call the 'sftp' module from a step
        systemUnderTest.fromStep(stepParams)

        // I should get an error
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).is(1)
        
    }
	@Test
    void "'sftp' needs the 'userCredential' parameter to work"() {
        // Given the minimum viable configuration for the SFTP module
        Map stepParams = new HashMap(this.minimumMandatoryParamsExample)

        // When I remove the 'userCredential' parameter
        stepParams.remove 'userCredential'

        // And I call the 'sftp' module from a step
        systemUnderTest.fromStep(stepParams)

        // I should get an error
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).is(1)
        
    }
    @Test
    void "'sftp' there is an error, which is captured and checked, regarding invalid credentials"() {
        // Given the minimum viable configuration for the SFTP module
        // Map stepParams = new HashMap(this.minimumMandatoryParamsExample)
        Map sftpParams = [
            localDir: './kqco/target/',
            remoteDir: '/home/deploy_user/pipeline_deploy',
            remoteUser: 'deploy_user',
            remoteIp: '82.250.88.14',
            remoteKey: 'DRILL_DOWN_02',
            deployableFile: 'kqco_mult_web_front-01.war',
            remoteCommand: '/opt/IBM/WebSphere/AppServer/profiles/Dmgr02/bin/wsadmin.sh -user -password -f /home/deploy_user/pipeline_deploy/updateKQCO_pr02.py -lang jython ',
            userCredential: 'Desarrollo_WAS_CON',
            groupGraaS: 'BBVA_CO_NET_DEPLOY_DEV',
        ]

        // And I call the 'sftp' module from a step
        systemUnderTest.fromStep(sftpParams)

        // I should get an error
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).is(1)

    }
}
