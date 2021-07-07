package workflow.vars

import org.junit.Before
import org.junit.Test
import org.junit.Ignore
import com.lesfurets.jenkins.unit.BasePipelineTest

import static org.assertj.core.api.Assertions.*

import globals.*
import org.yaml.snakeyaml.Yaml


class deployOnServerTest extends BaseVarsTest {

    void setPathToScript() {
        this.pathToScript = "vars/deployOnServer.groovy"
    }

    protected void updateSUTwithTemporaryValue(String variableName, String variableValue = null) {
        String value = variableValue ?: "test_value_for_${variableName}"
        systemUnderTest.binding.setVariable(variableName, value)
    }

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()

        helper.registerAllowedMethod("sshUserPrivateKey", [Map.class], { args ->
            if(args.credentialsId == 'pe-jboss-test'){
                updateSUTwithTemporaryValue(args.keyFileVariable)
                return 'ok'
            }
        })

        helper.registerAllowedMethod("usernamePassword", [Map.class], { args ->
            if(args.credentialsId == 'server_test_dummy'){
                updateSUTwithTemporaryValue(args.usernameVariable)
                updateSUTwithTemporaryValue(args.passwordVariable)
                return 'ok'
            }
        })

        
        helper.registerAllowedMethod("emailext", [Map.class], { map -> "email enviado" })
        helper.registerAllowedMethod("input", [Map.class], { map -> [
            Aprobar: true,
            approval: 'mail@bbva.com'
        ] })
        helper.registerAllowedMethod("sh", [Map.class], { cmd ->
            if(cmd.script.contains("ls artifacts")){
                return "appear.ear"
            }
        })
        helper.registerAllowedMethod("stash", [Map.class], { c -> "bcc19744fc4876848f3a21aefc92960ea4c716cf" })
        helper.registerAllowedMethod("node", [String.class, Closure.class], { nd, c -> c() })
        helper.registerAllowedMethod("sshagent", [Map.class, Closure.class], { map, c -> c() })
        helper.registerAllowedMethod("unstash", [String.class], { c -> "bcc19744fc4876848f3a21aefc92960ea4c716cf" })
        helper.registerAllowedMethod("deleteDir", [], null)
    }

    @Test
    void "when send the minimum params and exists the configuration files, then the module work"() {

        Map deployOnServerParams = [
            node: 'node',
            deployablefile: 'artifactsToDeployment.zip',
            messageEmail: 'hello, there is a deployment...'
        ]

        // Given a not-null repository in global SCM configuration
        Settings.repo.GIT_URL = 'aaa'
        Settings.country = 'pe'
        Settings.repo.slug = 'app_dummy'

        Modules.git = [
            getFileFromRepo: { file, remote, version -> }
        ]

        helper.registerAllowedMethod("readYaml", [Map.class], { m ->
            if (m.text) { return new Yaml().load(m.text) }
            if (m.file) {

                if(m.file == 'confAppsTEST.yml'){
                    def filePathTest = "test/resources/${Settings.country}/confAppsTEST.yml"
                    return new Yaml().load( new FileInputStream(filePathTest) )
                }

                if(m.file == 'confServersTEST.yml'){
                    def filePathServers = "test/resources/${Settings.country}/confServersTEST.yml"
                    return new Yaml().load( new FileInputStream(filePathServers) )
                }
            }
        })

        // And I call the 'deployOnServer' module from a step
        systemUnderTest.fromStep(deployOnServerParams)

        // Then no exception has been thrown
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).is(0)

        // And the node command has been called with relevant parameters
        def deployOk = false
        helper.callStack.findAll { it.methodName == 'sh' }.each {
            //println "it.args: ${it.args}"
            if(it.args[0] && "${it.args}".contains("-name=appear.ear --runtime-name=appear.ear")){
                deployOk = true
            }
        }
        assertThat(deployOk).isTrue()
    }

    @Test
    void "when send the minimum params and not exists the configuration files, then the module not work"() {

        Map deployOnServerParams = [
            node: 'node',
            deployablefile: 'artifactsToDeployment.zip',
            messageEmail: 'hello, there is a deployment...'
        ]

        // Given a not-null repository in global SCM configuration
        Settings.repo.GIT_URL = 'aaa'
        Settings.country = 'pe'
        Settings.repo.slug = 'app_dummy'

        Modules.git = [
            getFileFromRepo: { file, remote, version -> Helpers.error "Modules :: GIT :: getFileFromRepo error"  }
        ]
        
        helper.registerAllowedMethod("readYaml", [Map.class], { m ->
            if (m.text) { return new Yaml().load(m.text) }
            if (m.file) {

                if(m.file == 'confAppsTEST.yml'){
                    return ""
                }

                if(m.file == 'confServersTEST.yml'){
                    return ""
                }
            }
        })

        // And I call the 'deployOnServer' module from a step
        systemUnderTest.fromStep(deployOnServerParams)
        /*
        helper.callStack.each {
            println "it: ${it} | ${it.methodName} | args: ${it.args}"
        }
        */

        // Then exception has been thrown
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).is(1)
    }
}
