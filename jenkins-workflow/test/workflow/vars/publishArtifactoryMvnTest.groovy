package workflow.vars

import org.junit.Before
import org.junit.Test
import org.junit.Ignore
import com.lesfurets.jenkins.unit.BasePipelineTest

import static org.assertj.core.api.Assertions.*


class publishArtifactoryMvnTest extends BaseVarsTest {

    void setPathToScript() {
        this.pathToScript = "vars/publishArtifactoryMvn.groovy"
    }

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
    }

    @Test
    void "when send all parameters, then the module work"() {

        Map publishArtifactoryParams = [
            artifactory_repo: 'artifactory_repo',
            artifactory_id: 'artifactory_id',
            command: 'command'
        ]

        workflow.Utils.mockPluginManager(['artifactory', 'git'])

        // Given a series of mocks that are available when running in jenkins
        mockJenkinsEnvironment()

        // And I call the 'publishArtifactory' module from a step
        systemUnderTest.fromStep(publishArtifactoryParams)

        // Then no exception has been thrown
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).is(0)
    }

    @Test
    void "when send all parameters but the plug-in not exists then the module not work"() {

        Map publishArtifactoryParams = [
            artifactory_repo: 'artifactory_repo',
            artifactory_id: 'artifactory_id',
            command: 'command'
        ]

        workflow.Utils.mockPluginManager(['git'])

        // Given a series of mocks that are available when running in jenkins
        mockJenkinsEnvironment()

        // And I call the 'publishArtifactory' module from a step
        try { systemUnderTest.fromStep(publishArtifactoryParams) }

        // Then a exception has been thrown
        finally { 
            // And validate that the msg error
            def shellArgs = helper.callStack.find { it.methodName == 'error' }.args[0] as String

            assertThat(shellArgs).isEqualTo('publishArtifactory :: Plugin Artifactory not found in Jenkins')
        }
    }

    @Test
    void "when send the incomplete parameters, then the module return error"() {

        Map publishArtifactoryParams = [
            artifactory_repo: 'artifactory_repo'
        ]

        workflow.Utils.mockPluginManager(['artifactory', 'git'])

        // Given a series of mocks that are available when running in jenkins
        mockJenkinsEnvironment()

        // And I call the 'publishArtifactory' module from a step
        systemUnderTest.fromStep(publishArtifactoryParams)

        // Then a exception has been thrown
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).is(1)

        // And validate that the msg error
        def shellArgs = helper.callStack.find { it.methodName == 'error' }.args[0] as String

        assertThat(shellArgs).isEqualTo('Missing mandatory: artifactory_id, command')
    }

    private void mockJenkinsEnvironment() {
        systemUnderTest.binding.setVariable('Artifactory', [
            newMavenBuild: { -> [
                resolver: { Map m -> },
                deployer: { Map m -> },
                run:      { _ -> [ pom: './pom.xml', goals: 'mvn deploy', env: [:]]}
            ] },
            newServer: { _ -> [ 
                url: 'your_custom_url', 
                credentialsId: 'artifactory_id',
                publishBuildInfo: { Map m -> }
            ] }
        ])

        systemUnderTest.binding.setVariable('env', [
            capture:true
            ])
    }
}
