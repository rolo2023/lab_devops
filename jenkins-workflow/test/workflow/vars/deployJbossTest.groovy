package workflow.vars

import org.junit.Before
import org.junit.Test
import org.junit.Ignore
import com.lesfurets.jenkins.unit.BasePipelineTest

import static org.assertj.core.api.Assertions.*


class deployJbossTest extends BaseVarsTest {

    void setPathToScript() {
        this.pathToScript = "vars/deployJboss.groovy"
    }

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()

        helper.registerAllowedMethod("node", [String.class, Closure.class], { nd, c -> c() })
        helper.registerAllowedMethod("stash", [Map.class], { c -> "bcc19744fc4876848f3a21aefc92960ea4c716cf" })
        helper.registerAllowedMethod("unstash", [String.class], { c -> "bcc19744fc4876848f3a21aefc92960ea4c716cf" })
        helper.registerAllowedMethod("sshagent", [Map.class, Closure.class], { map, c -> c() })
        helper.registerAllowedMethod("deleteDir", [], null)
    }

    @Test
    void "when send all parameters, then the module work"() {

        Map deployJbossParams = [
            node: 'node',
            server_ip: 'server_ip',
            ssh_pk_id: 'ssh_pk_id',
            ssh_user: 'ssh_user',
            server_id: 'server_id',
            warPath: './path/file.war',
            server_group: 'server_group',
            contex_root: 'contex_root'
        ]

        // And I call the 'deployJboss' module from a step
        systemUnderTest.fromStep(deployJbossParams)

        // Then no exception has been thrown
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).is(0)

        // And the node command has been called with relevant parameters
        def deployArgs = helper.callStack.find { it.methodName == 'node' }.args
        assertThat('node').isEqualTo(deployArgs[0])
    }

    @Test
    void "when send the incomplete parameters, then the module return error"() {

        Map deployJbossParams = [
            node: 'node',
            server_ip: 'server_ip',
            ssh_user: 'ssh_user',
            server_id: 'server_id',
            warPath: './path/file.war',
            server_group: 'server_group',
            contex_root: 'contex_root'
        ]

        // And I call the 'deployJboss' module from a step
        systemUnderTest.fromStep(deployJbossParams)

        // Then no exception has been thrown
        assertThat(helper.callStack.findAll { it.methodName == 'error' }).isNotEmpty()

        // And the error message is launched
        def errorArgs = helper.callStack.find { it.methodName == 'error' }.args
        assertThat("Missing mandatory: ssh_pk_id").isEqualTo(errorArgs[0])
    }
}
