package workflow.modules


import globals.Helpers
import globals.Settings
import modules.VtrackModuleCps
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import workflow.jenkins.BasePipelineTest

import static org.assertj.core.api.Assertions.assertThat

class VtrackModuleTest extends BasePipelineTest {

    void setPathToScript() {
        this.pathToScript = "src/modules/VtrackModule.groovy"
    }

    @Test
    @Ignore("We have to mock up quite some things for this to work")
    void "we cannot init VTrack without valid GIT information"() {
        Settings.repo = null
        systemUnderTest.init()
        assertThat(helper.getCallStack().findAll { it.methodName == "error" }.size()).isEqualTo(1)
    }

    @Test
    void "generateBuildMap crashes when no component is registered and module is enabled"() {
        Settings.repo = [
                GIT_URL: '/path/to/fake/repo'
        ]
        systemUnderTest.initModuleFromSettings([ enabled: true])

        systemUnderTest.generateBuildMap([:])
        assertThat(helper.getCallStack().findAll { it.methodName == "error" }.size()).isEqualTo(1)
    }

    @Test
    void "registerBuild crashes when no component is registered and module is enabled"() {
        Settings.repo = [
                GIT_URL: '/path/to/fake/repo'
        ]
        systemUnderTest.initModuleFromSettings([ enabled: true])

        systemUnderTest.registerBuild()
        assertThat(helper.getCallStack().findAll { it.methodName == "error" }.size()).isEqualTo(1)
    }

    @Test
    void "initVtrackDeployMap crashes when no build is registered and module is enabled"() {
        Settings.repo = [
                GIT_URL: '/path/to/fake/repo'
        ]
        systemUnderTest.initModuleFromSettings([enabled: true])

        systemUnderTest.initVtrackDeployMap([:])
        printCallStack()
        assertThat(helper.getCallStack().findAll { it.methodName == "error" }.size()).isEqualTo(1)
    }

    @Test
    void "initVtrackDeployMap crashes when no component is registered and module is enabled"() {
        Settings.repo = [
                GIT_URL: '/path/to/fake/repo'
        ]
        systemUnderTest.initModuleFromSettings([ enabled: true])

        systemUnderTest.initVtrackDeployMap()
        assertThat(helper.getCallStack().findAll { it.methodName == "error" }.size()).isEqualTo(1)
    }

    @Test
    void "updateBuildStatus registers correct codes for success and failure"() {
        Settings.repo = [
                GIT_URL: '/path/to/fake/repo'
        ]
        systemUnderTest.initModuleFromSettings([ enabled: true])

        systemUnderTest.updateVtrackBuildResults(false)
        assertThat(systemUnderTest.buildResults.status).isEqualTo 'failed'

        systemUnderTest.updateVtrackBuildResults(true)
        assertThat(systemUnderTest.buildResults.status).isEqualTo 'successful'
    }

    @Test
    void "updateVtrackDeployResults registers correct codes for success and failure"() {
        Settings.repo = [
                GIT_URL: '/path/to/fake/repo'
        ]
        systemUnderTest.initModuleFromSettings([ enabled: true])
        systemUnderTest.setBuild([
                generateDeployMap: { _ -> [:] }
        ])

        systemUnderTest.updateVtrackDeployResults(false)
        assertThat(systemUnderTest.deployInfo.status).isEqualTo 'failed'

        systemUnderTest.updateVtrackDeployResults(true)
        assertThat(systemUnderTest.deployInfo.status).isEqualTo 'deployed'
    }

    @Test
    void "Module is DISABLED BY DEFAULT"() {
        Settings.repo = [
                GIT_URL: '/path/to/fake/repo'
        ]
        systemUnderTest.initModuleFromSettings([:])

        assertThat(systemUnderTest.isEnabled as Boolean).isFalse()
    }

    @Test
    void "generateBuildMap returns a map ready to be populated"() {
        // Given a fake call to the Vtrack API
        systemUnderTest.setComponent([
                generateBuildMap: { x -> [called: true] }
        ])

        // And a fake Version
        Settings.artifact = [version: '1.2.0']

        // And a mocked property on Jenkins built-in
        Helpers.jenkins.currentBuild = [
                startTimeInMillis: '123456789'
        ]

        // When we call 'generateBuildMap'
        Map result = systemUnderTest.generateBuildMap([:])

        // Then we get a valid map to fill
        assertThat(result).isEqualTo([
                artifacts: [],
                called   : true,
                metadata : [:],
                version  : '1.2.0.123456789'
        ])
    }

    @Test
    void "registerArtifact always registers an artifact"() {
        // Given a perfectly valid Artifactory configuration coming from the 'store' module
        Settings.artifact = [
                file  : [
                        name: 'ARTIFACT_NAME',
                        path: '/path/to/artifact'
                ],
                suffix: 'releases'  // This is a spring-related thingie

        ]

        // And a mocked property on Jenkins built-in
        Helpers.jenkins.currentBuild = [
                startTimeInMillis: '123456789'
        ]

        // When we call 'registerArtifact'
        systemUnderTest.registerArtifact()

        // Then we get a valid map as a result
        assertThat(systemUnderTest.buildResults.artifacts[0].id as String).isEqualTo('release:ARTIFACT_NAME')
    }

    @Test
    void "we get the Namespace from the UUAA if available"() {
        // Given a Vtrack configuration with UUAA and Country, and empty namespace
        Map vtrackConfiguration = [
                uuaa     : 'test',
                country  : 'GLO',
                namespace: ''
        ]

        // And global Settings
        Settings.uuaa = 'GLOBAL_TEST'
        Settings.country = 'es'

        // And a fake Vtrack client that returns input parameters in a Map
        def vtrackClient = [
                getNamespaceFromGovernment: { uuaa, country -> [id: "${uuaa}.${country}"] },
                getNamespace              : { ns -> ns }
        ]

        // When we get the Ether namespace id
        String namespace = systemUnderTest.getEtherNamespaceId(vtrackConfiguration, vtrackClient)

        // Then we get the one from "government api"
        assertThat(namespace).isEqualTo('test.GLO')
    }

    @Test
    void "we get the Namespace from its value if available in config"() {
        // Given a Vtrack configuration with UUAA and Country, and a valid namespace
        Map vtrackConfiguration = [
                uuaa     : 'test',
                country  : 'GLO',
                namespace: 'my.namespace'
        ]

        // And global Settings
        Settings.uuaa = 'GLOBAL_TEST'
        Settings.country = 'es'

        // And a fake Vtrack client that returns input parameters in a Map
        def vtrackClient = [
                getNamespaceFromGovernment: { uuaa, country -> [id: "${uuaa}.${country}"] },
                getNamespace              : { ns -> ns }
        ]

        // When we get the Ether namespace id
        String namespace = systemUnderTest.getEtherNamespaceId(vtrackConfiguration, vtrackClient)

        // Then we get the vtrack configured values
        assertThat(namespace).isEqualTo('my.namespace')
    }

    @Test
    void "we return null if no valid configuration for namespace"() {
        // Given a Vtrack configuration with UUAA and Country, and a valid namespace
        Map vtrackConfiguration = [:]

        // And global Settings
        Settings.uuaa = 'GLOBAL_TEST'
        Settings.country = 'es'

        // And a fake Vtrack client that returns input parameters in a Map
        def vtrackClient = [
                getNamespaceFromGovernment: { uuaa, country -> [id: "${uuaa}.${country}"] },
                getNamespace              : { ns -> ns }
        ]

        // When we get the Ether namespace id
        String namespace = systemUnderTest.getEtherNamespaceId(vtrackConfiguration, vtrackClient)

        // Then we get a null value
        assertThat(namespace).isNull()
    }

    @Test
    void "we get the country from global settings if not in configuration"() {
        // Given a Vtrack configuration with Country
        Map vtrackConfiguration = [
                country: 'GLO',
        ]

        // And global Settings
        Settings.country = 'es'

        // When we get the vtrack country
        String country = systemUnderTest.getCountry(vtrackConfiguration)

        // Then we get the configured one
        assertThat(country).isEqualTo('GLO')

        // And if we sent empty config
        country = systemUnderTest.getCountry([:])

        // Then we get the Global one, adapted to Ether syntax
        assertThat(country).isEqualTo('ESP')

        // And if we sent empty values
        country = systemUnderTest.getCountry([country: ''])

        // Then we STILL get the Global one, adapted to Ether syntax
        assertThat(country).isEqualTo('ESP')
    }

    @Test
    void "we get the architecture from global settings if not in configuration"() {
        // Given a Vtrack configuration with architecture
        Map vtrackConfiguration = [
                architecture: 'spring',
        ]

        // And global Settings
        Settings.architecture = 'generic'

        // When we get the vtrack architecture
        String architecture = systemUnderTest.getArchitecture(vtrackConfiguration)

        // Then we get the configured one
        assertThat(architecture).isEqualTo('spring.r1')

        // And if we sent empty config
        architecture = systemUnderTest.getArchitecture([:])

        // Then we get the Global one, adapted to Ether syntax
        assertThat(architecture).isEqualTo('generic')

        // And if we sent empty values
        architecture = systemUnderTest.getArchitecture([country: ''])

        // Then we STILL get the Global one, adapted to Ether syntax
        assertThat(architecture).isEqualTo('generic')
    }

    @Test
    void "we get the UUAA from global settings if not in configuration"() {
        // Given a Vtrack configuration with uuaa
        Map vtrackConfiguration = [
                uuaa: 'test',
        ]

        // And global Settings
        Settings.uuaa = 'GLOBAL_TEST'

        // When we get the vtrack uuaa
        String uuaa = systemUnderTest.getUUAA(vtrackConfiguration)

        // Then we get the configured one
        assertThat(uuaa).isEqualTo('test')

        // And if we sent empty config
        uuaa = systemUnderTest.getUUAA([:])

        // Then we get the Global one, adapted to Ether syntax
        assertThat(uuaa).isEqualTo('GLOBAL_TEST')

        // And if we sent empty values
        uuaa = systemUnderTest.getUUAA([uuaa: ''])

        // Then we STILL get the Global one, adapted to Ether syntax
        assertThat(uuaa).isEqualTo('GLOBAL_TEST')
    }
}
