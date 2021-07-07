package workflow.modules


import globals.Settings
import org.junit.Before
import org.junit.Test
import workflow.jenkins.BasePipelineTest

import static org.assertj.core.api.Assertions.assertThat

class BitbucketModuleTest extends BasePipelineTest {

    void setPathToScript() {
        this.pathToScript = 'src/modules/BitbucketModule.groovy'
    }

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()

        // Base settings: empty
        Settings.modules.bitbucket = [:]
    }

    @Test
    void "Calling init will initialise the bitbucket client"() {
        // Given a bitbucket configuration
        Settings.modules.bitbucket = [
                credentialsId  : 'BB_CREDENTIALS',
                credentialsType: '',
                url            : 'the_bitbucket_url'
        ]

        // When we call init on the module
        systemUnderTest.init()

        // Then the client has been created
        def testClient = systemUnderTest.client
        assertThat(testClient).isNotNull()

        // And the parameters it got sent are the expected
        assertThat(testClient.url).isEqualTo 'the_bitbucket_url/rest/api/1.0'
        assertThat(testClient.session.headers['Authorization']).isEqualTo 'Bearer test_value_for_AUTH'
    }
}
