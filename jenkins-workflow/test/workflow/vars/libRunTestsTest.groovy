package workflow.vars

import org.junit.Before
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat


class libRunTestsTest extends BaseVarsTest {

    void setPathToScript() {
        this.pathToScript = "vars/libRunTests.groovy"
    }

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()

        // By default, credentials are always ok
        helper.registerAllowedMethod("file", [Map.class], { Map m -> 'something'} )
        helper.registerAllowedMethod("string", [Map.class], { Map m -> 'something'} )
        helper.registerAllowedMethod("usernameColonPassword", [Map.class], { Map m -> 'something'} )
    }

    private Map defaultContext = [
            artifactoryNpmCredentials: 'npm_creds',
            dockerRegistryCredentials: 'docker_creds',
            testManagerCredentials   : 'test_man_creds',
            dataManagerCredentials   : 'data_man_creds',
            saucelabsCredentials     : 'saucelabs-creds',
            application              : 'defaultAppName',
            namespace                : 'defaultNamespace'
    ]

    @Test
    void "libRunTests needs at least one test defined"() {
        // Given the minimum configuration
        Map stepParams = [
                context: defaultContext,
                tests  : [
                    []
                ]
        ]

        // When I call the module
        systemUnderTest.fromStep(stepParams)

        // Then there should be an error (exit.call returns call two error methods)
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(2)
    }

    @Test
    void "libRunTests tests need to have a runner"() {
        // Given the minimum configuration
        Map stepParams = [
                context: defaultContext,
                tests  : [
                        [:],
                ]
        ]

        // When I call the module
        systemUnderTest.fromStep(stepParams)

        // Then there should be an error (exit.call returns call two error methods)
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(2)
    }

    @Test
    void "libRunTests needs a minimum set of values in the context object"() {
        //saucelabs credentials is not mandatory in 'backend testing'
        Map defaultContextMinNew = [
            artifactoryNpmCredentials: 'npm_creds',
            dockerRegistryCredentials: 'docker_creds',
            testManagerCredentials   : 'test_man_creds',
            dataManagerCredentials   : 'data_man_creds',
            application              : 'defaultAppName',
            namespace                : 'defaultNamespace'
        ]
        for (entry in defaultContextMinNew) {

            // Given a configuration missing a value
            def tempContext = new HashMap(defaultContextMinNew)
            tempContext.remove(entry.key)
            Map stepParams = [
                    context: tempContext,
                    tests  : [
                        [runner: [ type: 'spring' ] ],
                    ]
            ]

            // When I call the module
            helper.callStack = []
            systemUnderTest.fromStep(stepParams)

            // Then there should be an error (exit.call returns call two error methods)
            assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(2)
        }
    }

    @Test
    void "testManagerCredentials credentials need to be a string-type credential"() {
        // Given the minimum configuration
        Map stepParams = [
                context: defaultContext,
                tests  : [
                        [runner: [ type: 'spring' ] ],
                ]
        ]

        // And valid credential entries for some of the passed-in credentials
        helper.registerAllowedMethod("string", [Map.class], { Map m ->
            if (m.credentialsId.endsWith(defaultContext.testManagerCredentials)) {
                return null
            }
            return 'aaa'
        })

        // When I call the module
        systemUnderTest.fromStep(stepParams)

        // Then there should be an error (exit.call returns call two error methods)
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(2)
    }

    @Test
    void "dataManagerCredentials credentials need to be a string-type credential"() {
        // Given the minimum configuration
        Map stepParams = [
                context: defaultContext,
                tests  : [
                        [runner: [ type: 'spring' ] ],
                ]
        ]

        // And valid credential entries for some of the passed-in credentials
        helper.registerAllowedMethod("string", [Map.class], { Map m ->
            if (m.credentialsId.endsWith(defaultContext.dataManagerCredentials)) { return null }
            return 'aaa'
        })
        helper.registerAllowedMethod('runTests', [Map.class], { m -> })

        // When I call the module
        systemUnderTest.fromStep(stepParams)

        // Then there should be an error (exit.call returns call two error methods)
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(2)
    }

    @Test
    void "saucelabsCredentials credentials need to be a userpass-type credential"() {
        // Given the minimum configuration
        Map stepParams = [
                context: defaultContext,
                tests  : [
                        [runner: [ type: 'spring' ] ],
                ]
        ]

        // And valid credential entries for some of the passed-in credentials
        helper.registerAllowedMethod("usernameColonPassword", [Map.class], { Map m ->
            if (m.credentialsId.endsWith(defaultContext.saucelabsCredentials)) { return null }
            return 'aaa'
        })

        // When I call the module
        systemUnderTest.fromStep(stepParams)

        // Then there should be an error (exit.call returns call two error methods)
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(2)
    }

    @Test
    void "dockerRegistryCredentials Artifactory credentials need to be a userpass-type credential"() {
        // Given the minimum configuration
        Map stepParams = [
                context: defaultContext,
                tests  : [
                        [runner: [ type: 'spring' ] ],
                ]
        ]

        // And valid credential entries for some of the passed-in credentials
        helper.registerAllowedMethod("usernameColonPassword", [Map.class], { Map m ->
            if (m.credentialsId.endsWith(defaultContext.dockerRegistryCredentials)) { return null }
            return 'aaa'
        })

        // When I call the module
        systemUnderTest.fromStep(stepParams)

        // Then there should be an error (exit.call returns call two error methods)
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(2)
    }

    @Test
    void "polyfemoApi credentials need to be a userpass-type credential"() {
        Map defaultContextNew = [
            artifactoryNpmCredentials: 'npm_creds',
            dockerRegistryCredentials: 'docker_creds',
            testManagerCredentials   : 'test_man_creds',
            dataManagerCredentials   : 'data_man_creds',
            polyfemoApiCredentials   : 'polyfemo_api_creds',
            application              : 'defaultAppName',
            namespace                : 'defaultNamespace'
    ]

        // Given the minimum configuration
        Map stepParams = [
                context: defaultContextNew,
                tests  : [
                        [runner: [ type: 'spring' ] ],
                ]
        ]

        // And valid credential entries for some of the passed-in credentials
        helper.registerAllowedMethod("usernameColonPassword", [Map.class], { Map m ->
            if (m.credentialsId.endsWith(defaultContext.polyfemoApiCredentials)) { return null }
            return 'aaa'
        })

        // When I call the module
        systemUnderTest.fromStep(stepParams)

        // Then there should be an error (exit.call returns call two error methods)
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(2)
    }

    @Test
    void "libRunTests NPM credentials needs to be a file-type credential"() {
        // Given the minimum configuration
        Map stepParams = [
                context: defaultContext,
                tests  : [
                        [runner: [ type: 'spring' ] ],
                ]
        ]

        // And valid credential entries for some of the passed-in credentials
        helper.registerAllowedMethod("file", [Map.class], { Map m ->
            if (m.credentialsId.endsWith(defaultContext.artifactoryNpmCredentials)) { return null }
            return 'aaa'
        })

        // When I call the module
        systemUnderTest.fromStep(stepParams)

        // Then there should be an error (exit.call returns call two error methods)
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(2)
    }

    @Test
    void "Using invalid credentials in the Spring runner will return an error"() {
        // Given the minimum configuration for a Spring runner with credentials
        Map stepParams = [
                context: defaultContext,
                tests  : [
                        [runner: [
                            type: 'spring',
                            credentials: 'invalid_creds',
                        ] ],
                ]
        ]

        // And a failing credential check
        helper.registerAllowedMethod("file", [Map.class], { Map m ->
            if (m.credentialsId.endsWith('invalid_creds')) { return null }
            'ok'
        })

        // And a fake call to the library
        helper.registerAllowedMethod('runTests', [Map.class], { m -> })

        // When I call the module
        systemUnderTest.fromStep(stepParams)

        // Then there should be an error
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(2)
    }

    @Test
    void "libRunTests converts a spring runner into a callable closure"() {
        // Given the minimum configuration
        Map stepParams = [
                context: defaultContext,
                tests  : [
                        [runner: [ type: 'spring' ] ],
                ]
        ]

        // And a fake call to the library
        helper.registerAllowedMethod('runTests', [Map.class], { m -> })

        // When I call the module
        systemUnderTest.fromStep(stepParams)

        // Then there should not be an error
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(0)

        // And the library has been called
        assertThat(helper.callStack.findAll { it.methodName == 'runTests' }.size()).isEqualTo(1)

        // And the parameters are the curated list of arguments
        Map libArgs = helper.callStack.find { it.methodName == 'runTests' }.args[0] as Map
        List testArgs = libArgs.tests
        assertThat(testArgs.size()).isEqualTo(1)
        assertThat(testArgs[0].runner).isInstanceOf(Closure)
    }

    @Test
    void "libRunTests converts a spring runner into a callable closure and expand additional parameters (string one)"() {
        // Given the minimum configuration
        Map stepParams = [
                context: defaultContext,
                tests  : [
                        [runner: [ type: 'spring', parameters: '-Dbrowser.name=\'${ctx.browser}\' -Dcucumber.options=\'--plugin json:target/browsers/cucumber-${ctx.browser}.json --name .*\' -Dsaucelabs.username=${ctx.deviceHub.username} -Dsaucelabs.password=${ctx.deviceHub.password} -Dsaucelabs.tunnelid=${ctx.deviceHub.tunnelid} -Dsaucelabs.endpoint=${ctx.deviceHub.endpoint} -Dapplication.endpoint=${ctx.endpoints.orquidea} -Dvbank.enabled=true -Dvbank.host=\'vbank\' -Dvbank.recording=true -Ddatamanager.endpoint=http://vbank:1660' ]]
                ]
        ]

        systemUnderTest.binding.setVariable('dmApiKeyBotPolyfemo', 'dummy')

        // And a fake call to the library
        helper.registerAllowedMethod('runTests', [Map.class], { m -> })

        // When I call the module
        systemUnderTest.fromStep(stepParams)

        // And the parameters are the curated list of arguments
        Map libArgs = helper.callStack.find { it.methodName == 'runTests' }.args[0] as Map
        List testArgs = libArgs.tests
        assertThat(testArgs.size()).isEqualTo(1)
        assertThat(testArgs[0].runner).isInstanceOf(Closure)

        // Execute closure
        testArgs[0].runner([
            browser:'chrome',
            deviceHub: [
                username: 'username',
                password: 'password',
                tunnelid: 'tunelid',
                endpoint: 'https://endpoint'
            ],
            endpoints: [
                orquidea: 'https://appendpoint'
            ]])

        // Check mvn command executed
        List shCalls = helper.callStack.findAll { it.methodName == 'sh' }
        assertThat(shCalls.size()).isEqualTo(2)
        
        // Check mvn command for chrome
        assertThat(shCalls[1].args[0] as String).isEqualTo("mvn --batch-mode -Dstyle.color=always -V -U clean verify -f acceptance -Dbrowser.name='chrome' -Dcucumber.options='--plugin json:target/browsers/cucumber-chrome.json --name .*' -Dsaucelabs.username=username -Dsaucelabs.password=password -Dsaucelabs.tunnelid=tunelid -Dsaucelabs.endpoint=https://endpoint -Dapplication.endpoint=https://appendpoint -Dvbank.enabled=true -Dvbank.host='vbank' -Dvbank.recording=true -Ddatamanager.endpoint=http://vbank:1660")
    }

    @Test
    void "libRunTests converts a spring runner into a callable closure and expand additional parameters (as block)"() {
        // Given the minimum configuration
        Map stepParams = [
            context: defaultContext,
            tests  : [
                [
                    runner: [
                        type: 'spring',
                        parameters: """{ it ->
                                println "\${ctx.extraParam}"
                                if("\${ctx.extraParam}" == "qa") {    
                                    "-Dbrowser.name=\'\${ctx.browser}\' -Dcucumber.options=\'--plugin json:target/browsers/cucumber-\${ctx.browser}.json --name .*\' -Dsaucelabs.username=\${ctx.deviceHub.username} -Dsaucelabs.password=\${ctx.deviceHub.password} -Dsaucelabs.tunnelid=\${ctx.deviceHub.tunnelid} -Dsaucelabs.endpoint=\${ctx.deviceHub.endpoint} -Dapplication.endpoint=\${ctx.endpoints.orquidea} -Dvbank.enabled=true -Dvbank.host=\'vbank\' -Dvbank.recording=true -Ddatamanager.endpoint=http://vbank:1660"
                                } else { "this isn't possible" }
                            }()
                            """
                    ]
                ]
            ]
        ]

        systemUnderTest.binding.setVariable('dmApiKeyBotPolyfemo', 'dummy')

        // And a fake call to the library
        helper.registerAllowedMethod('runTests', [Map.class], { m -> })

        // When I call the module
        systemUnderTest.fromStep(stepParams)

        // And the parameters are the curated list of arguments
        Map libArgs = helper.callStack.find { it.methodName == 'runTests' }.args[0] as Map
        List testArgs = libArgs.tests
        assertThat(testArgs.size()).isEqualTo(1)
        assertThat(testArgs[0].runner).isInstanceOf(Closure)

        // Execute closure
        testArgs[0].runner([
            extraParam: 'qa',
            browser:'chrome',
            deviceHub: [
                username: 'username',
                password: 'password',
                tunnelid: 'tunelid',
                endpoint: 'https://endpoint'
            ],
            endpoints: [
                orquidea: 'https://appendpoint'
            ]])

        // Check mvn command executed
        List shCalls = helper.callStack.findAll { it.methodName == 'sh' }
        assertThat(shCalls.size()).isEqualTo(2)

        // Check mvn command for chrome
        assertThat(shCalls[1].args[0] as String).isEqualTo("mvn --batch-mode -Dstyle.color=always -V -U clean verify -f acceptance -Dbrowser.name='chrome' -Dcucumber.options='--plugin json:target/browsers/cucumber-chrome.json --name .*' -Dsaucelabs.username=username -Dsaucelabs.password=password -Dsaucelabs.tunnelid=tunelid -Dsaucelabs.endpoint=https://endpoint -Dapplication.endpoint=https://appendpoint -Dvbank.enabled=true -Dvbank.host='vbank' -Dvbank.recording=true -Ddatamanager.endpoint=http://vbank:1660")
    }

}
