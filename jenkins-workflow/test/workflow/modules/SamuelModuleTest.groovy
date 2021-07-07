package workflow.modules

import globals.Settings
import org.junit.Before
import org.junit.Test
import workflow.Utils
import workflow.jenkins.BasePipelineTest

import static org.assertj.core.api.Assertions.assertThat

class SamuelModuleTest extends BasePipelineTest {

    void setPathToScript() {
        this.pathToScript = "src/modules/SamuelModule.groovy"
    }

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
    }

    @Test
    void "Calling init will continue if it is disabled by configuration"() {
        // Given a base configuration where Samuel is disabled
        Settings.modules.samuel = [
                enabled: false
        ]

        helper.registerAllowedMethod('samuelEntityExists', [], { -> true })

        // And the plugin installed
        Utils.mockPluginManager(['samuel-jenkins-plugin'])

        Settings.uuaa = 'kyfd'
        Settings.country = 'global'
        Settings.architecture = 'spring.r1'

        // When we call init on the module
        systemUnderTest.init()

        // Then no error has been raised
        assertThat(helper.getCallStack().findAll { it.methodName == "error" }.size()).isEqualTo(0)
    }

    @Test
    void "Calling init will fail if the plugin is not installed"() {

        // And the plugin NOT installed
        Utils.mockPluginManager(['git'])

        // When we call init on the module
        systemUnderTest.init()

        // Then an error has been raised
        assertThat(helper.getCallStack().findAll { it.methodName == "error" }.size()).isEqualTo(1)
    }

    @Test
    void "Calling init with Samuel enabled will retrieve an existing entity"() {
        // Given minimum Samuel configuration
        Settings.modules.samuel = [
                enabled: true
        ]

        // And the plugin installed
        Utils.mockPluginManager(['samuel-jenkins-plugin'])

        // And a fake call to samuelEntityExists that returns true
        helper.registerAllowedMethod('samuelEntityExists', [], { -> true })

        // When we call init on the module
        systemUnderTest.init()

        // Then no error has been raised
        assertThat(helper.getCallStack().findAll { it.methodName == "error" }.size()).isEqualTo(0)
    }

    @Test
    void "Calling init with minimum valid config will create an entity if it does not exist"() {
        Settings.modules.samuel = [ enabled: false ]
   
        // And a UUAA in Settings
        Settings.uuaa = 'kyfd'
        Settings.country = 'global'
        Settings.architecture = 'spring.r1'

        // And proper GIT configuration
        Settings.repo << [
                slug   : 'the_repo_slug',
                project: 'the_repo_project'
        ]

        // And the plugin installed
        Utils.mockPluginManager(['samuel-jenkins-plugin'])

        // And a fake call to samuelEntityExists that returns false
        helper.registerAllowedMethod('samuelEntityExists', [], { -> false })

        // And a fake call to createSamuelEntity that returns nothing
        helper.registerAllowedMethod('createSamuelEntity', [Map.class], { _ -> })

        // When we call init on the module
        systemUnderTest.init()

        // Then no error has been raised
        assertThat(helper.getCallStack().findAll { it.methodName == "error" }.size()).isEqualTo(0)
    }

    @Test
    void "createDefaultEntityMap() will set owners and default values"() {
        // Given the minimum configuration for Samuel to work
        def samuelSettings = [
                owners : [
                        'juanito@bbva.com', 'julito@bbva.com'
                ]
        ]
        Settings.uuaa = 'kyfd'
        Settings.country = 'global'
        Settings.architecture = 'spring.r1'

        // And proper GIT configuration
        Settings.repo << [
                slug   : 'the_repo_slug',
                project: 'the_repo_project'
        ]

        // When we call validateSamuelMap
        Map validSettings = systemUnderTest.createDefaultEntityMap(samuelSettings.owners)

        // Then the validation succeeds
        assertThat(validSettings).isNotNull()

        // And the correct entity Map has been stored
        assertThat(validSettings.coordinates as List).isEqualTo(['kyfd', 'the_repo_slug'])

        assertThat(validSettings.type).isEqualTo('spring')

        assertThat(validSettings.kind).isEqualTo('r1')

        assertThat(validSettings.name).isEqualTo(Settings.repo.slug)
    }

    @Test
    void "validateSamuelMap() will set default values, if found empty"() {
        // Given the minimum configuration for Samuel to work
        Settings.uuaa = 'kyfd'
        Settings.country = 'global'
        Settings.architecture = 'spring.r1'


        // And proper GIT configuration
        Settings.repo << [
                slug   : 'the_repo_slug',
                project: 'the_repo_project'
        ]

        // When we call validateSamuelMap
        Map validSettings = systemUnderTest.createDefaultEntityMap()

        // Then the validation succeeds
        assertThat(validSettings).isNotNull()

        // And the correct entity Map has been stored
        assertThat(validSettings.coordinates as List).isEqualTo(['kyfd', 'the_repo_slug'])

        assertThat(validSettings.type).isEqualTo('spring')

        assertThat(validSettings.name).isEqualTo(Settings.repo.slug)
    }

   
    @Test
    void "createDefaultEntityMap() will fail if there isn't a country in Settings"() {
        // Given the minimum configuration for Samuel to work
        Settings.uuaa = 'kyfd'
        Settings.country = null
        Settings.architecture = 'spring.r1'

        // And proper GIT configuration
        Settings.repo << [
                slug   : 'the_repo_slug',
                project: 'the_repo_project'
        ]

        // When we call validateSamuelMap
        systemUnderTest.createDefaultEntityMap()

        // Then an error has been raised
        assertThat(helper.getCallStack().findAll { it.methodName == "error" }.size()).isEqualTo(2)
    }

    @Test
    void "createDefaultEntityMap() will fail if there isn't a valid architecture in Settings"() {
        // Given the minimum configuration for Samuel to work
        Settings.uuaa = 'kyfd'
        Settings.country = 'es'
        Settings.architecture = 'sproong.r1'

        // And proper GIT configuration
        Settings.repo << [
                slug   : 'the_repo_slug',
                project: 'the_repo_project'
        ]

        // When we call validateSamuelMap
        systemUnderTest.createDefaultEntityMap()

        // Then an error has been raised
        assertThat(helper.getCallStack().findAll { it.methodName == "error" }.size()).isEqualTo(1)
    }

    @Test
    void "createDefaultEntityMap() will fail if a country in config is not in the list"() {
        // Given the minimum configuration for Samuel to work
        def samuelSettings = [
                owners : ['juan@bbva.com']
        ]

        // And a UUAA in Settings
        Settings.uuaa = 'kyfd'
        Settings.country = 'POLAND'

        // And proper GIT configuration
        Settings.repo << [
                slug   : 'the_repo_slug',
                project: 'the_repo_project'
        ]

        // When we call createDefaultEntityMap
        systemUnderTest.createDefaultEntityMap(samuelSettings.owners)

        // Then an error has been raised
        assertThat(helper.getCallStack().findAll { it.methodName == "error" }.size()).isEqualTo(1)
    }

    @Test
    void "createDefaultEntityMap() will not fail if there aren't any owners in config"() {
        // Given the minimum configuration for Samuel to work
        def samuelSettings = []

        // And a UUAA in Settings
        Settings.uuaa = 'kyfd'
        Settings.country = 'global'

        // And proper GIT configuration
        Settings.repo << [
                slug   : 'the_repo_slug',
                project: 'the_repo_project'
        ]

        // When we call validateSamuelMap
        systemUnderTest.createDefaultEntityMap(samuelSettings.owners)

        // Then an error has been raised
        assertThat(helper.getCallStack().findAll { it.methodName == "error" }.size()).isEqualTo(0)
    }

    @Test
    void "createDefaultEntityMap() with country specified in Jenkinsfile"() {
        // Given the minimum configuration for Samuel to work
        def samuelSettings = []

        Settings.uuaa = 'kyfd'
        Settings.country = 'es'
        Settings.architecture = 'spring.r1'


        // When we call validateSamuelMap
        Map validSettings = systemUnderTest.createDefaultEntityMap()

        // Then an error has been raised
        assertThat(validSettings.country).isEqualTo('ES')
    }

    @Test
    void "getDefaultCountry() will fail if there aren't any valid country in config and Jenkinsfile"() {
        // Given the minimum configuration for Samuel to work
        Settings.modules.samuel = [country : 'cub']

        // And a UUAA in Settings
        Settings.uuaa = 'kyfd'
        Settings.country = 'grm'

        // And proper GIT configuration
        Settings.repo << [
                slug   : 'the_repo_slug',
                project: 'the_repo_project'
        ]

        helper.registerAllowedMethod('samuelEntityExists', [], { -> false })

        // When we call init on the module
        systemUnderTest.getDefaultCountry()


        // Then an error has been raised
        assertThat(helper.getCallStack().findAll { it.methodName == "error" }.size()).isEqualTo(1)
    }
}
