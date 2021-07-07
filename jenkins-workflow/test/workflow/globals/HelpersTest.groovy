package workflow.globals


import globals.FormHelper
import globals.Helpers
import globals.Settings
import org.junit.Before
import org.junit.Test
import workflow.BaseTest
import workflow.Utils

import static org.assertj.core.api.Assertions.assertThat

class HelpersTest extends BaseTest {

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()

        // This should mock all Jenkins variables and methods in
        Helpers.jenkins = helper.loadScript("src/jenkins/Pipeline.groovy", binding)

        Settings.currentStage = null
    }

    @Test
    void "getEnvMap will not fail if current stage has not a valid configuration"() {
        // Given a random stage
        Settings.currentStage = 'verify'

        // And empty global environment
        Settings.environment = null

        // When I get the envMap
        def envMap = Helpers.getEnvMap([:])

        // It is correctly created and it is empty
        assertThat(envMap).isEqualTo([:])
    }

    @Test
    void "getEnvMap will not fail if current stage has NULL configuration"() {
        // Given a random stage
        Settings.currentStage = 'verify'

        // And empty global environment
        Settings.environment = null

        // When I get the envMap
        def envMap = Helpers.getEnvMap(null)

        // It is correctly created and it is empty
        assertThat(envMap).isEqualTo([:])
    }

    @Test
    void "getEnvMap works even if global environment is empty"() {
        // Given a random stage
        def randomStage = 'build'

        // And empty global environment
        Settings.environment = null

        // And its environment in settings
        Settings.currentStage = randomStage
        Settings."${randomStage}".environment = [param1: 1]

        // When I get the envMap
        def envMap = Helpers.getEnvMap([:])

        // It is correctly merged
        assertThat(envMap).isEqualTo([param1: 1])
    }

    @Test
    void "I can call the maven module from Helpers_run"() {
        // Given a random stage
        def randomStage = 'build'

        // And empty global environment
        Settings.environment = [:]

        // And a patched up 'maven' command
        systemUnderTest = helper.loadScript('vars/maven.groovy', binding)
        systemUnderTest.metaClass.fromMap = { m -> helper.loadScript("vars/fromMap.groovy").call(m) }
        systemUnderTest.metaClass.tool = { s -> s }
        Helpers.jenkins.metaClass.maven = systemUnderTest
        systemUnderTest.binding.setVariable('WORKSPACE', '/your/home')

        // When we call maven through run
        def runStep = [
                bin        : 'mvn',
                run        : 'install',
                environment: [
                        java_tool     : 'jdk1.8',
                        maven_tool    : 'mvn34',
                        maven_settings: 'file: settings_artifactory'
                ]
        ]
        Helpers.run(runStep)

        // Then there are no  errors
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(0)

        // And command is in there
        String mavenArgs = helper.callStack.find { it.methodName == 'sh' }.args[0]
        assertThat(mavenArgs).contains(runStep.run as String)

        // And the settings file get loaded from the system
        assertThat(mavenArgs).contains('-s test_value_for_settingsXml')

        // And the tools are in the command
        String envArgs = helper.callStack.find { it.methodName.contains('withEnv') }.args[0]
        assertThat(envArgs).contains(runStep.environment.java_tool as String)
        assertThat(envArgs).contains(runStep.environment.maven_tool as String)

        // And there is a withCredentials blocks
        String credArgs = helper.callStack.find { it.methodName.contains('withCredentials') }.args[1]
    }

    @Test
    void "run can evaluate Settings in random groovy code found in a step"() {
        // Given a 'groovy' step with some random code using our Settings
        Map testStep = [
                bin: 'groovy',
                run: '''def message = """
            String artifactorySuffix = ${ y.artifact.suffix }
          """.trim()
          z.capture(message)
''']
        // and a custom stage with no environment set
        Settings.currentStage = 'end'
        Settings.end = [environment: [:]]

        // and a 'snapshot' suffix in Artifact configuration Setting
        Settings.artifact = [suffix: 'snapshot']

        // And some cool magic to capture its output
        Object result = null
        Helpers.metaClass.static.capture = { Object x -> result = x }

        // When we run the step
        Helpers.run(testStep)

        // Then magic happens
        assertThat(result).isEqualTo('String artifactorySuffix = snapshot')
    }

    @Test
    void "A true-ish 'when' clause always evaluates to TRUE"() {
        // Given a series of TRUE 'when' clauses
        def trueConditions = [
                [when: 'true'],
                [when: 'always'],
                [when: true],
                [when: 'yes'],
                [when: [
                        true: "true"
                ]],
                [when: [
                        false: "false"
                ]]
        ]

        // Then canDoWhen always evaluates to TRUE
        trueConditions.each {
            assertThat(Helpers.canDoWhen(it)).isTrue()
        }
    }

    @Test
    void "we can evaluate complex 'when' expressions as boolean"() {
        // Given a series of complex 'when' expressions
        def expressions = [
                [when: '"{{ vars.delivery_type }}" == "package" && "{{ vars.environment }}" == "dev"'],
                [when: [
                        '"{{ vars.delivery_type }}" == "package"',
                        '"{{ vars.environment }}" == "dev"'
                ]],    // same as above
                [when: '"{{ vars.delivery_type }}" == "package" || "{{ vars.delivery_type }}" == "both"'],
                [when: '!("{{ vars.delivery_type }}" == "none")'],
                [when: '"{{ vars.delivery_type }}" != "none"'],
        ]

        // And the variables that they need during substitution to be true
        Settings.vars << [
                delivery_type: 'package',
                environment: 'dev'
        ]

        // Then canDoWhen always evaluates to TRUE
        expressions.each {
            assertThat(Helpers.canDoWhen(it)).isTrue()
        }
    }

    @Test
    void "A false-ish 'when' clause always evaluates to FALSE "() {
        // Given a series of false 'when' clauses
        def falseConditions = [
                [when: [
                        false: true
                ]],
                [when: [
                        true: false
                ]],
                [when: 'false'],
                [when: false],
                [when: 'never'],
                [when: 'no'],
                [when: [
                        false: "true"
                ]],
        ]

        // Then canDoWhen always evaluates to false
        falseConditions.each {
            assertThat(Helpers.canDoWhen(it)).isFalse()
        }
    }

    @Test
    void "'when_branch' identifies regex expressions for branch names"() {
        def conditions = [
                when_branch: ['PR-.*', 'develop', 'master', 'feature/DEVELOPING_.*']
        ]

        [
                'PR-666'                      : true,
                'PR'                          : false,
                'develop'                     : true,
                'release'                     : false,
                'feature/DEVELOPING_SOMETHING': true,
                'feature/DEVELOPING'          : false,
        ].each { branch, expected ->
            Settings.repo << [branch: branch]
            def result = Helpers.canDoWhen(conditions)
            assertThat(expected).isEqualTo(result)
        }
    }

    @Test
    void "credentialExists does not crash with empty credentials"() {
        // Given an empty credential
        def emptyCredential = ''

        // When I check credentials'
        def result = Helpers.credentialExists(emptyCredential)

        // I should not get an error
        assertJobStatusSuccess()

        // And result should be true
        assertThat(result).isTrue()
    }

    @Test
    void "credentialExists verifies file type credentials"() {
        // Given a credential set
        def fileCredential = 'file: JENKINS_CREDENTIAL_SET'

        // And a Jenkins lookup that returns false
        Helpers.jenkins = [
                echo           : { _ -> }, error: { _ -> },
                file           : { x -> false },
                withCredentials: { m, c -> c() }
        ]

        // When I check if it exists
        def result = Helpers.credentialExists(fileCredential)

        // I should get a false
        assertThat(result).isFalse()

        // And when the Jenkins lookup returns true
        Helpers.jenkins.file = { x -> true }

        // When I check if it exists
        result = Helpers.credentialExists(fileCredential)

        // I should get a true
        assertThat(result).isTrue()
    }

    @Test
    void "credentialExists does not crash with env type credentials"() {
        // Given an 'env' credential
        def envCredential = 'env: ARTIFACTORY_CREDENTIALS'

        // When I check if it exists
        def result = Helpers.credentialExists(envCredential)

        // I should not get an error
        assertJobStatusSuccess()

        // And result should be true
        assertThat(result).isTrue()
    }

    @Test
    void "If a still-templated credential is checked, it will raise an error"() {
        // Given a still templated credential
        def invalidCredential = 'file : {{ maven_settings }}'

        // When I check if it exists
        def result = Helpers.credentialExists(invalidCredential)

        // I should get an error
        assertJobStatusFailure()
        assertThat(helper.getCallStack().findAll { it.methodName == "error" }.size()).isEqualTo(1)

        // And result should be false, just in case
        assertThat(result).isFalse()
    }

    @Test
    void "If a null value substitution is requested it will raise an error"() {
        // Given a tree to substitute
        def testMap = [
                entry: '{{ repo.version }}'
        ]

        // And no value to substitute
        Settings.repo = [version: null]

        // When we call substitutetree
        Helpers.substituteTree(testMap)

        // Then there is an error
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(1)
    }

    @Test
    void "If a deprecated value substitution is requested it will raise an error"() {
        // Given a tree to substitute
        def testMap = [
                entry: '{{ version }}'
        ]

        // When we call substitutetree
        Helpers.substituteTree(testMap)

        // Then there is an error
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(1)
    }

    @Test
    void "If an unknown value substitution is requested it will raise an error"() {
        // Given a tree to substitute
        def testMap = [
                entry: '{{ something }}'
        ]

        // When we call substitutetree
        Helpers.substituteTree(testMap)

        // Then there is an error
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(1)
    }

    @Test
    void "Until deprecated, 'release' admits special file options"() {
        // Given a tree to substitute
        def testMap = [
                file_name: '{{ release.fileName}}',
                file_path: '{{ release.filePath}}',
        ]

        // And values to substitute them
        Settings.artifact = [file:
                                     [
                                             name: 'file_name',
                                             path: 'file_path'
                                     ]
        ]

        // When we call substitutetree
        testMap = Helpers.substituteTree(testMap)

        // Then the values got replaced
        assertThat(testMap.file_name).isEqualTo 'file_name'
        assertThat(testMap.file_path).isEqualTo 'file_path'
    }

    @Test
    void "Until deprecated, 'artifact' replaces the file name"() {
        // Given a tree to substitute
        def testMap = [
                file_name: '{{ artifact }}'
        ]

        // And values to substitute them
        Settings.artifact = [
                file: [name: 'file_name']
        ]

        // When we call substitutetree
        testMap = Helpers.substituteTree(testMap)

        // Then the values got replaced
        assertThat(testMap.file_name).isEqualTo 'file_name'
    }

    @Test
    void "Until deprecated, 'release' options  are replaced by 'artifact' arguments"() {
        // Given a tree to substitute
        def testMap = [
                release_version : '{{ release.version }}',
                artifact_version: '{{ artifact.version }}'
        ]

        // When we call substitutetree
        testMap = Helpers.substituteTree(testMap)

        // Then the values got replaced
        assertThat(testMap.release_version).isEqualTo(testMap.artifact_version)
    }

    @Test
    void "A tree gets substitutions even if deeply nested"() {
        // Given a map with values to subst
        Map testMap = [
                entry: '{{ artifact.version }}',   // String
                extra: [    // Map
                            paths: ['{{ env.JENKINS_HOME }}/.m2', '/tmp/{{ uuaa }}'], // List of strings
                            key  : '{{ uuaa }}-key',  // nested String
                            tests: [    // Nested List of Maps
                                        [
                                                test1: [
                                                        runner        : "spring",
                                                        credentials_id: "spring_{{country}}_{{ group }}_settings"
                                                ],
                                                test2: [
                                                        runner        : "other",
                                                        credentials_id: "{{ uuaa }}_setting"
                                                ]
                                        ],
                                        [
                                                test1: [
                                                        credentials_id: "{{ form.hidden_form_value }}_setting"
                                                ]
                                        ],
                            ]
                ]
        ]

        // And values to substitute them
        Settings.artifact = [version: 'TEST_VERSION_abcdef']
        Settings.uuaa = 'TESTUA'

        // Simulates form part
        Settings.formValues = FormHelper.newCps()
        Helpers.jenkins.params = [:]
        Settings.formValues.hiddenParams = [hidden_form_value: 'test_uuaa_value']

        // When we call substitutetree
        def newMap = Helpers.substituteTree(testMap)

        // Then we get the proper version
        assertThat(newMap.entry).isEqualTo('TEST_VERSION_abcdef')

        // And the nested key also got init
        assertThat(newMap.extra.key).isEqualTo('TESTUA-key')

        // And the nested values also got init
        assertThat(newMap.extra.paths).isEqualTo(['/home/jenkins/.m2', '/tmp/TESTUA'])

        // And the nested list of maps also got init
        assertThat(newMap.extra.tests.size()).isEqualTo(2)
        assertThat(newMap.extra.tests[0].test2.credentials_id).isEqualTo("TESTUA_setting")
        assertThat(newMap.extra.tests[1].test1.credentials_id).isEqualTo("test_uuaa_value_setting")

    }

    @Test
    void "We can use the pluginExists check to get if a plugin is installed"() {
        // Given a list of 'installed' plugins
        Utils.mockPluginManager(['git', 'samuel'])

        // Then Samuel plugin should exits
        assertThat(Helpers.pluginExists('samuel')).isTrue()

        // And Bitbucket plugin should not
        assertThat(Helpers.pluginExists('bitbucket')).isFalse()
    }

    @Test
    void "We can use the isCurrentUserInGraasGroup check to get if the user that is execute a current job, belongs is a group of GrasS"() {
        // Given a list of groups to which the user belong
        Utils.mockUsersManager(['BBVA_CO_NET_USERS', 'jira-software-users'])

        // The user belongs to the group
        assertThat(Helpers.isCurrentUserInGraasGroup('BBVA_CO_NET_USERS')).isTrue()

        // And the user don't belongs to the group
        assertThat(Helpers.isCurrentUserInGraasGroup('bitbucket-users')).isFalse()
    }

    @Test
    void "we can use getArtifacts to unstash the artifact to deploy"() {
        // Given a mock for the unstash function
        Helpers.jenkins.metaClass.unstash = { c -> "bcc19744fc4876848f3a21aefc92960ea4c716cf" }

        // When we call getArtifacts with just a local name
        Helpers.getArtifacts("localName")

        // Then unstash gets called to get the artifact
        assertThat(helper.callStack.findAll { it.methodName == 'unstash' }.size()).isEqualTo(1)
    }

    @Test
    void "we can use getArtifacts to download from Artifactory, via curl, the artifact to deploy"() {
        // Given a mock of the curl method
        Helpers.jenkins.metaClass.curl = { _ -> }

        // And some fake settings for the curl call
        Settings.modules << [artifactory: [credentialsId: 'my_credentials_id']]

        // When we call getArtifacts with a local name and a remote URL
        Helpers.getArtifacts("localName", "remoteUrl")

        // Then unstash gets called to get the artifact
        assertThat(helper.callStack.findAll { it.methodName == 'curl' }.size()).isEqualTo(1)
    }
}
