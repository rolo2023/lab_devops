package workflow.globals

import globals.Helpers
import globals.Settings
import org.junit.Before
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class SettingsTest {

    @Before
    void setUp() throws Exception {
        // Make sure config is reset between tries
        Settings.config.defaults = Settings.config.application = [:]
        Settings.artifact = [:]

        // Mock Jenkins methods
        Helpers.jenkins = [
                echo : { s -> println(s as String) },
                error: { x -> throw new TemporaryTestException(x) }
        ]
        Settings.artifact << [version: '1.2.0']
    }

    class TemporaryTestException extends Exception {
        TemporaryTestException(String error) {
            super(error)
        }
    }

    @Test
    void "getModuleConfig overrides work on empty module overrides"() {
        // Given a faulty configuration
        Settings.config.defaults = Settings.config.application = [:]

        // When we issue a config merge
        Map mergedModulesConfig = Settings.getOverridenModuleConfig()

        // I get a valid, but empty, module config
        assertThat(mergedModulesConfig).isEqualTo([:])
    }

    @Test
    void "getModuleConfig can override default config parameters"() {
        // Given a default configuration
        Settings.config.defaults = [
                modules: [
                        artifactory: [
                                param1: 'param1',
                                param2: 'FAIL'
                        ]
                ]
        ]

        // And an app-specific override
        Settings.config.application = [
                modules: [
                        artifactory: [
                                param2: 'param2',
                                param3: 'NEW'
                        ]
                ]
        ]

        // When I call getOverridenModuleConfig
        Map mergedModulesConfig = Settings.getOverridenModuleConfig()

        // Then I get proper parameter overrides
        assertThat(mergedModulesConfig.artifactory).isEqualTo([
                param1: 'param1',
                param2: 'param2',
                param3: 'NEW'
        ])
    }

    @Test
    void "getModuleConfig can enable modules that come disabled by default"() {
        // Given a default configuration
        Settings.config.defaults = [
                modules: [
                        testModule: [
                                enabled: false,
                                param1 : 'param1'
                        ]
                ]
        ]

        // And an app-specific override
        Settings.config.application = [
                modules: [
                        testModule: [
                                enabled: true,
                                param2 : 'param2'
                        ]
                ]
        ]

        // When I call getOverridenModuleConfig
        Map mergedModulesConfig = Settings.getOverridenModuleConfig()

        // Then I get properly enabled overridden modules
        assertThat(mergedModulesConfig.testModule.enabled as Boolean).isTrue()
    }

    @Test
    void "getModuleConfig will not add non-default-modules not found in default configuration"() {
        // Given a default configuration
        Settings.config.defaults = [
                modules: [:]
        ]

        // And an app-specific override
        Settings.config.application = [
                modules: [
                        newModule: [
                                enabled: true,
                                param2 : 'param2'
                        ]
                ]
        ]

        // When I call getOverridenModuleConfig
        Map mergedModulesConfig = Settings.getOverridenModuleConfig()

        // Then I will not have the override
        assertThat(mergedModulesConfig.newModule).isNull()
    }

    @Test
    void "using getMap we substitute base config with incoming one"() {
        //Given a base configuration
        Settings.config.defaults = [
                stages: [
                        test: [
                                environment: [var1: 1],
                                module     : 'test',
                                parameters : [
                                        param1: 1,
                                        param2: 2
                                ]
                        ]
                ]
        ]

        // And an override
        Settings.config.application = [
                stages: [
                        test: [
                                module    : 'test_better',
                                parameters: [
                                        param1: 'new_1',
                                ]
                        ]
                ]
        ]

        // When we use getMap on that stage
        def newMap = Settings.getMap('stages.test')

        // Then we get an expanded configuration
        assertThat(newMap.module).isEqualTo('test_better')
        assertThat(newMap.keySet()).doesNotContain 'environment'
        assertThat(newMap.parameters).isEqualTo([param1: 'new_1'])
    }

    @Test
    void "using getMap, we will not delete default steps if we do not specify a stage"() {
        //Given a base configuration with two steps
        Settings.config.defaults = [
                stages: [
                        build: [
                                label: 'build_stage',
                                steps: [
                                        [
                                                label: 'Build with maven',
                                                bin  : 'mvn',
                                                run  : 'install versions:set -DnewVersion={{ artifact.version }}'
                                        ],
                                        [
                                                label: 'Sonar stage',
                                                use  : 'wSonar'
                                        ]
                                ]
                        ]
                ]
        ]

        // And an override without that stage
        Settings.config.application = [
                stages: [
                        deploy: [:],
                        test  : [:]
                ]
        ]

        // When we use getMap on that stage
        def newMap = Settings.getMap('stages.build')

        // Then we get an expanded configuration
        assertThat(newMap.label).isEqualTo('build_stage')
        assertThat(newMap.steps as List).hasSize 2
    }

    @Test
    void "using getMap, we can delete any default steps with an empty array inside a stage"() {
        //Given a base configuration with two steps
        Settings.config.defaults = [
                stages: [
                        build: [
                                label: 'build_stage',
                                steps: [
                                        [
                                                label: 'Build with maven',
                                                bin  : 'mvn',
                                                run  : 'install versions:set -DnewVersion={{ artifact.version }}'
                                        ],
                                        [
                                                label: 'Sonar stage',
                                                use  : 'wSonar'
                                        ]
                                ]
                        ]
                ]
        ]

        // And an override to an empty stage
        Settings.config.application = [
                stages: [
                        build: [:]
                ]
        ]

        // When we use getMap on that stage
        def newMap = Settings.getMap('stages.build')

        // Then we get an empty stage
        assertThat(newMap.keySet()).hasSize 0
    }

    @Test
    void "we can substitute all relevant trees at once"() {
        // Given some information on the Settings
        Settings.modules = [
                artifactory: [
                        credentials_id: "spring_{{ country }}_credentials"
                ]
        ]
        Settings.build = [
                node_label: 'ldbad103',
                steps     : [
                        [
                                environment: 'file_settings_{{ uuaa }}_id',
                                url        : '{{ repo.GIT_URL }}'
                        ]
                ]
        ]

        // And some values in Settings for the substitution
        Settings.country = 'co'
        Settings.uuaa = 'huha'
        Settings.repo = [
                GIT_URL: 'a_git_url',
                version: '1.2.0'
        ]

        // When we call substituteSettings()
        Settings.substituteSettings()

        // Then those values will get replaced
        assertThat(Settings.modules.artifactory.credentials_id).isEqualTo 'spring_co_credentials'
        assertThat(Settings.build.steps[0].environment).isEqualTo 'file_settings_huha_id'
        assertThat(Settings.build.steps[0].url).isEqualTo 'a_git_url'
    }

    @Test
    void "we can only select one of the valid branching models"() {
        // Given a random branching model name
        Settings.config.defaults << [branching_model: 'random']

        // When we load it
        def model = Settings.getBranchingModel()

        // Then we get the default value - 'auto
        assertThat(model).isEqualTo 'auto'

        // And given an empty one
        Settings.config.defaults.branching_model = ''

        // When we load it
        model = Settings.getBranchingModel()

        // Then we get the default value - 'auto
        assertThat(model).isEqualTo 'auto'
    }

    @Test
    void "getValue can read a default value "() {
        // Given no values in the configuration arrays
        Settings.config = [
                defaults: [:], application: [:], custom: [:]
        ]

        // Then if no default value is sent, null is returned
        assertThat(Settings.getValue('key')).isNull()

        // And If I read a value with default value, it is returned
        assertThat(Settings.getValue('key', 'empty')).isEqualTo('empty')
    }

    @Test
    void "getValue can read a value from any of the embedded configs"() {
        // Given some values in the configuration arrays
        Settings.config.custom = [key1: 1]
        Settings.config.application = [key2: 2]
        Settings.config.defaults = [key3: 3]

        // Then I can read a value from custom
        assertThat(Settings.getValue('key1')).isEqualTo(1)

        // And I can read a value from application

        // And I can read a value from defaults
        assertThat(Settings.getValue('key3')).isEqualTo(3)

    }
}
