package workflow

import globals.Settings
import org.junit.Before
import org.yaml.snakeyaml.Yaml
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import com.lesfurets.jenkins.unit.BasePipelineTest

abstract class BaseTest extends BasePipelineTest {
    def systemUnderTest

    def withCredentialsInterceptor = { list, closure ->
        list.forEach { binding.setVariable(it, "$it") }
        def res = closure.call()
        list.forEach { binding.setVariable(it, null) }
        return res
    }

    Map defaultJenkinsVariables = [
        env      : [
            GIT_URL: 'ssh://git@globaldevtools.bbva.com:7999/bbvaeseccpdevops/workflow-spring.git',
            JENKINS_HOME: '/home/jenkins'
        ],
        workspace: '/home/jenkins'
    ]

    protected void intercept(metaClass) {
        def interceptor = helper.getMethodInterceptor()
        metaClass.invokeMethod = interceptor
        metaClass.static.invokeMethod = interceptor
        metaClass.methodMissing = helper.getMethodMissingInterceptor()
    }

    /**
     * Inserts a variable called 'variableName' in currently loaded script, through its binding.
     * It will have a predefined value, or a specific one can be set.

     * This is used during closures that load values off Jenkins' credentials, for example
     */
    protected void updateSUTwithTemporaryValue(String variableName, String variableValue = null) {
        String value = variableValue ?: "test_value_for_${variableName}"
        systemUnderTest.binding.setVariable(variableName, value)
    }

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()

        helper.registerAllowedMethod("ansiColor", [String.class, Closure.class], { _, c -> c() })
        helper.registerAllowedMethod("timestamps", [Closure.class], { c -> c() })
        helper.registerAllowedMethod("cleanWs", [], null)
        helper.registerAllowedMethod("withEnv", [Object.class, Closure.class], null)
        helper.registerAllowedMethod("sshUserPrivateKey", [Map.class], { args -> updateSUTwithTemporaryValue(args.keyFileVariable) })
        helper.registerAllowedMethod("usernameColonPassword", [Map.class], { args -> updateSUTwithTemporaryValue(args.variable) })
        helper.registerAllowedMethod("usernamePassword", [Map.class], { args ->
            updateSUTwithTemporaryValue args.usernameVariable
            updateSUTwithTemporaryValue args.passwordVariable
        })
        helper.registerAllowedMethod("echo", [Object.class], { "${it}" })
        helper.registerAllowedMethod("readYaml", [Map.class], { m -> new Yaml().load(m.text) })
        helper.registerAllowedMethod("file", [Map.class], { args -> updateSUTwithTemporaryValue(args.variable) })
        helper.registerAllowedMethod("library", [String.class], null)
        helper.registerAllowedMethod("writeFile", [Map.class], { m -> })
        helper.registerAllowedMethod("readJSON", [Map.class], { m ->
            if (m.text) { return new JsonSlurper().parseText(m.text) }
            if (m.file) { return new JsonSlurper().parse( new File(m.file) ) }
        })

        // Params
        helper.registerAllowedMethod("booleanParam", [Map.class], { m -> m})
        helper.registerAllowedMethod("string", [Map.class], { m -> m})
        helper.registerAllowedMethod("choice", [Map.class], { m -> m})
        helper.registerAllowedMethod("parameters", [List.class], { l -> l})

        // Settings as empty as possible!
        Settings.modules = [:]
        Settings.build = [:]
        Settings.test = [:]
        Settings.end = [:]
        Settings.deploy = [:]
        Settings.params = []
        Settings.artifact = null

        // Make sure config is reset between tries
        Settings.config.defaults = Settings.config.application = [:]

        // Mock jenkins variables
        defaultJenkinsVariables.each { k, v -> binding.setVariable(k, v) }

        // Configuration is read from resources in tests
        Settings.metaClass.static.getApplicationConfig = { ->
            def country = Settings.country
            def group = Settings.group
            def filePath = "test/resources/${country}/${group}.yml"
            if (group == 'generic') {
                //* GENERIC config is actually versioned here!!
                filePath = "resources/${group}.yml"
            }
            new Yaml().load(new FileInputStream(filePath))
        }
    }
}
