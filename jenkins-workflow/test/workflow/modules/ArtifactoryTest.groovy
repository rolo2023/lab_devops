package workflow.modules

import globals.Settings
import org.junit.Before
import org.junit.Test
import workflow.jenkins.BasePipelineTest

import static org.assertj.core.api.Assertions.assertThat

class ArtifactoryTest extends BasePipelineTest {

    void setPathToScript() {
        this.pathToScript = "src/modules/ArtifactoryModule.groovy"
    }

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()

        // Base settings: empty
        Settings.modules.artifactory = [:]

        // Some repo settings from the Git Module
        Settings.repo = [
                pullrequest: [is: false]
        ]
    }

    @Test
    void "Calling init will initialise the variables in the configuration"() {
        // Given a templated artifactory configuration
        Settings.modules.artifactory = [
                credentials_id: '{{ vars.credentials_id }}'
        ]

        // And the matching value for that template
        Settings.vars = [
                credentials_id: 'the_credential'
        ]

        // When we call init on the module
        systemUnderTest.init()

        // Then the configuration has been updated
        assertThat(Settings.modules.artifactory.credentials_id as String).isEqualTo 'the_credential'
    }

    @Test
    void "I get the Artifactory url off the Artifactory plugin if so configured and the plugin exists"() {
        // Given some mocked settings
        this.mockSettings()

        // And a server_id in artifactory module
        Settings.modules.artifactory << [
            server_id: 'test_id'
        ]

        // And a mock call to the Artifactory plugin
        systemUnderTest.binding.setVariable('Artifactory', [
            server: { _ -> [ url: 'your_custom_url'] }
        ])

        // IF the plugin exists
        workflow.Utils.mockPluginManager(['artifactory', 'git'])


        // When I set artifact Info
        systemUnderTest.setArtifactConfig()

        // Then I get the configured Artifactory URL
        assertThat(Settings.artifact.server).isEqualTo 'your_custom_url'
    }

    @Test
    void "I get the DEFAULT Artifactory if the Artifactory plugin is not installed"() {
        // Given some mocked settings
        this.mockSettings()

        // And a valid Artifactory configuration
        Settings.modules.artifactory << [
            server_id: 'test_id'
        ]

        // And a mock call to the Artifactory plugin
        systemUnderTest.binding.setVariable('Artifactory', [
            server: { _ -> [ url: 'your_custom_url'] }
        ])

        // If the plugin does not exist
        workflow.Utils.mockPluginManager(['git'])

        // When I set artifact Info
        systemUnderTest.setArtifactConfig()

        // Then I get Ether default Docker Registry  URL
        assertThat(Settings.artifact.server).isEqualTo 'https://globaldevtools.bbva.com/artifactory-api'
    }

    @Test
    void "I get Ether default Artifactory URL if no others exist"() {
        // Given some mocked settings
        this.mockSettings()

        // When I set artifact Info
        systemUnderTest.setArtifactConfig()

        // Then I get Ether default Artifactory URL
        assertThat(Settings.artifact.server).isEqualTo 'https://globaldevtools.bbva.com/artifactory-api'
    }

    @Test
    void "I get a custom Docker Registry URL if so configured"() {
        // Given some mocked settings
        this.mockSettings()

        // And a custom Docker registry in module settings
        Settings.modules.artifactory << [
            registry: 'my_custom_registry'
        ]

        // When I set artifact Info
        systemUnderTest.setArtifactConfig()

        // Then I get Ether default Artifactory URL
        assertThat(Settings.artifact.registry).isEqualTo 'my_custom_registry'
    }


    @Test
    void "I get Ether default Registry URL if no others exist"() {
        // Given some mocked settings
        this.mockSettings()

        // When I set artifact Info
        systemUnderTest.setArtifactConfig()

        // Then I get Ether default Artifactory URL
        assertThat(Settings.artifact.registry).isEqualTo 'globaldevtools.bbva.com:5000'
    }

    private void mockSettings() {
        // Given a store configuration
        Settings.store = [
                context: 'co-orquidea-mvn',
                virtual: '',
                release: [
                        any: [
                                name  : 'SNAPSHOT-12345',
                                suffix: 'snapshots',
                                upload: 'no'
                        ]

                ],
                file   : [
                        path: '.',
                        name: "{{ artifact.version }}.zip"
                ]
        ]

        Settings.repo = [
                pullrequest: [is: false]
        ]

        // And a Git branch set by the Git Module
        Settings.branch = 'feature/default_branch'

        // And the historically-mandatory UUAA
        Settings.uuaa = 'qwjc'

        // And an empty artifact Settings
        Settings.artifact = [:]

    }

    @Test
    void "I can set artifact info if proper settings exist"() {
        // Given some mocked settings
        this.mockSettings()

        // When I set artifact Info
        systemUnderTest.setArtifactConfig()

        // Then I get artifact Settings
        assertThat(Settings.artifact.version).isEqualTo 'SNAPSHOT-12345'
        assertThat(Settings.artifact.file.name as String).isEqualTo 'SNAPSHOT-12345.zip'
        assertThat(Settings.artifact.registry).isEqualTo 'globaldevtools.bbva.com:5000'
        assertThat(Settings.artifact.urlUpload).
                isEqualTo 'https://globaldevtools.bbva.com/artifactory-api/co-orquidea-mvn-snapshots/qwjc'
        assertThat(Settings.artifact.urlDownload).
                isEqualTo 'https://globaldevtools.bbva.com/artifactory-api/repository-co-orquidea-mvn'
        assertThat(Settings.artifact.context).isEqualTo 'co-orquidea-mvn'
    }

    @Test
    void "I can set artifact info even if store settings are incomplete"() {
        // Given some mocked settings
        this.mockSettings()

        // That do not define 'store.file'
        Settings.store.remove('file')

        // When I set artifact Info
        systemUnderTest.setArtifactConfig()

        // Then I get artifact Settings
        assertThat(Settings.artifact.version).isEqualTo 'SNAPSHOT-12345'
        assertThat(Settings.artifact.file.name as String).isEmpty()
        assertThat(Settings.artifact.file.path as String).isEmpty()

        assertThat(Settings.artifact.registry).isEqualTo 'globaldevtools.bbva.com:5000'
        assertThat(Settings.artifact.urlUpload).
                isEqualTo 'https://globaldevtools.bbva.com/artifactory-api/co-orquidea-mvn-snapshots/qwjc'
        assertThat(Settings.artifact.urlDownload).
                isEqualTo 'https://globaldevtools.bbva.com/artifactory-api/repository-co-orquidea-mvn'
        assertThat(Settings.artifact.context).isEqualTo 'co-orquidea-mvn'
    }

    @Test
    void "the uploadUrl is crafted based on store properties"() {
        // Given some mocked settings
        this.mockSettings()

        // When I set artifact Info
        systemUnderTest.setArtifactConfig()

        // Then I get the uploadUrl with the default UUAA
        assertThat(systemUnderTest.urlUpload).isEqualTo 'https://globaldevtools.bbva.com/artifactory-api/co-orquidea-mvn-snapshots/qwjc'
    }

    @Test
    void "the download URL is crafted based on store properties"() {
        // Given some mocked settings
        this.mockSettings()

        // When I set artifact Info
        systemUnderTest.setArtifactConfig()

        // Then I get the downloadUrl as expected for a virtual repo
        assertThat(systemUnderTest.urlDownload).isEqualTo 'https://globaldevtools.bbva.com/artifactory-api/repository-co-orquidea-mvn'
    }

    @Test
    void "the download URL for virtual repositories is crafted based on store properties"() {
        // Given some mocked settings
        this.mockSettings()

        // And the virtual repostiry configured in the store
        Settings.store.virtual = 'your-virtual-repo-path'

        // When I set artifact Info
        systemUnderTest.setArtifactConfig()

        // Then I get the downloadUrl as expected for a virtual repo
        assertThat(systemUnderTest.urlDownload).isEqualTo 'https://globaldevtools.bbva.com/artifactory-api/your-virtual-repo-path'
    }
}
