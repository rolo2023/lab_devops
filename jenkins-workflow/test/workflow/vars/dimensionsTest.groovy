package workflow.vars


import globals.Modules
import globals.Settings
import groovy.json.JsonOutput
import modules.ArtifactoryModule
import org.junit.After
import org.junit.Before
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class dimensionsTest extends BaseVarsTest {

    //* Create a file in /tmp/${path} with given contents
    private String createFileFromMap(String path, Map contents = [:]) {
        // Make sure tree to file exists
        def ftb = new FileTreeBuilder(new File('/tmp/'))
        def dirName = path.replaceAll(/[^\/]*$/, '')
        ftb.dir(dirName)
        def fileName = path.replace(dirName, '')
        ftb.file(fileName)

        // Write contents
        def tmpFileName = "/tmp/${path}"
        new java.io.File(tmpFileName).withWriter('utf-8') { w ->
            w.writeLine(JsonOutput.toJson(contents))
        }

        return tmpFileName
    }

    void setPathToScript() {
        this.pathToScript = "vars/dimensions.groovy"
    }

    private String temporaryCredentialsFilename
    private List sampleLevelParameter = ['DEBUG', 'INFO']
    //   private String temporaryUnstashFile = "KFYU-1.0.2.jar"

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()

        // Create a temporary file for tests
        this.temporaryCredentialsFilename = createFileFromMap('credentials.json', [:])
    }

    @After
    void tearDown() {
        new File(this.temporaryCredentialsFilename).delete()
    }

    @Test
    void "Implantation levels is a mandatory Dimensions parameter"() {
        // Given some random Dimensions configuration, no levels
        Map randomConfig = [
                uuaa            : '1234',
                circuit         : 'jarama',
                credentials_file: this.temporaryCredentialsFilename
        ]

        // When we call Dimensions
        systemUnderTest.call(randomConfig)

        // Then we should get an error
        assertThat(helper.callStack.findAll { it.methodName == 'error' }).isNotEmpty()
    }

    @Test
    void "There has to exist an artifact in the Vtrack results map in order to deploy"() {
        // Given  a credentials file with no valid contents
        def credentialsFile = createFileFromMap('creds_file', [
                username: 'juan',
                password: 'sup3rs3cr3t'
        ])

        // And some Dimensions configuration
        Map completeConfig = [
                credentials_file: credentialsFile,
                uuaa            : '1234',
                circuit         : 'indianapolis',
                levels          : this.sampleLevelParameter
        ]

        // And no Vtrack artifacts in build results
        Modules.vtrack = [
                getArtifactsList: { -> [] }
        ]

        // When we call Dimensions
        systemUnderTest.call(completeConfig)

        // Then we should get an error
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).isEqualTo(1)
    }

    private Map mockEverythingIncludingDimensionsCall(dimensionsCallMock) {
        // Given  a credentials file with no valid contents
        def credentialsFile = createFileFromMap('tmp_file.json', [
                username: 'juan',
                password: 'sup3rs3cr3t'
        ])

        // And some Dimensions configuration
        Map completeConfig = [
                credentials_file: credentialsFile,
                uuaa            : '1234',
                circuit         : 'hungaroring',
                levels          : this.sampleLevelParameter
        ]

        // And some fake Vtrack data it needs
        Modules.vtrack = [
                initVtrackDeployMap: { m ->
                    [
                            status     : 'OK', params: [:],
                            environment: 'play',
                            country    : 'es',
                            userdeploy : 'nouser@nomail',
                            date       : false
                    ]
                }, registerDeploy: { s, b -> [:] }
        ]

        // And a fake commit ID to be used in tmp path
        Settings.commit = 'aeiou'

        // And a fake generated artifact to be released
        Modules.vtrack << [
                getArtifactsList: { ->
                    [
                            [id: 'release:KFYU-1.0.2.jar'],
                    ]
                }
        ]

        // And a 'valid' artifact object
        Settings.artifact = new ArtifactoryModule().newCps()

        // And some totally valid artifactory credentials
        Settings.modules.artifactory = [
                credentialsId: 'artifactory_credentials'
        ]

        // And a mock curl call
        helper.registerAllowedMethod("curl", [Object.class], { x -> })

        helper.registerAllowedMethod("unstash", [Object.class], { x -> })

        // And a mocked-up dimensions call
        systemUnderTest.metaClass.dimensionsCall = dimensionsCallMock

        // Return the applied configuration
        return completeConfig
    }

    @Test
    void "unstash is a mandatory Dimensions parameter"() {
        // Given some random Dimensions configuration
        Map randomConfig = [
                unstash: 'KFYU-1.0.2.jar'
        ]

        // When we call Dimensions
        systemUnderTest.call(randomConfig)

        //  Then we should get an error
        assertThat(helper.callStack.findAll { it.methodName == 'error' }).isNotEmpty()
    }

    @Test
    void "we correctly register deploy information when dimensions returns a valid response"() {
        // Given a mocked-up dimensions call that returns REAL SUCCESS
        def dimensionsConfig = mockEverythingIncludingDimensionsCall({ Map m ->
            [
                    date  : "2019-06-11 15:55:47.702335",
                    values: [
                            [
                                    date   : "2019-06-11 16:00:40",
                                    ok     : true,
                                    pimp_id: "EYPH_PIMP_2895"
                            ]
                    ],
                    ok    : true,
                    error : null
            ]
        })

        // And a controlled mock of Vtrack generated deploy that returns input
        Map deployInfo = [:]
        Modules.vtrack = [
                updateVtrackDeployResult: { data ->
                    deployInfo = data
                    deployInfo.status = 'deployed'
                },
                getArtifactsList        : { ->
                    [
                            [id: 'release:KFYU-1.0.2.jar'],
                    ]
                }
        ]

        // And some mock metadata from a fake build
        Settings.repo << [GIT_AUTHOR_EMAIL: "rafa.nadal@gmail.com"]

        // When we call Dimensions
        systemUnderTest.call(dimensionsConfig)

        // Then we should TOTALLY NOT get an error
        assertThat(helper.callStack.findAll { it.methodName == 'error' }).isEmpty()

        // And should have registered that information in Vtrack
        assertThat(deployInfo.status as String).isEqualTo('deployed')
        assertThat(deployInfo.date).isNotNull()
        assertThat(deployInfo.userdeploy as String).isEqualTo 'rafa.nadal@gmail.com'
        assertThat(deployInfo.params.UUAA as String).isEqualTo('1234')
        assertThat(deployInfo.params.environment as String).isEqualTo('INFO')
        assertThat(deployInfo.params.pimp_implantation_date as String).isNotNull()
        assertThat(deployInfo.params.pimp_id as String).isEqualTo('EYPH_PIMP_2895')
    }


}