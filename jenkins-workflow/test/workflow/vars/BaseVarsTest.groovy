package workflow.vars


import workflow.BaseTest

abstract class BaseVarsTest extends BaseTest {

    protected String usernameColonPasswordValue = 'USER:PASSW'

    protected String pathToScript

    /**
     * This is used to populate 'pathToScript' with
     *  the path to the script to test
     */
    abstract void setPathToScript()

    /**
     * Make sure this is called before every test
     * <code>
     * @Override
     * @Before
     * void setUp() throws Exception {
     *     super.setUp()
     * }
     * </code>
     */
    @Override void setUp() throws Exception {
        super.setUp()

        setPathToScript()

        // When setting 'binding' variables, call them BEFORE loading a script,
        //  so they become available for its context
        binding.setVariable("log", helper.loadScript("vars/log.groovy", binding))

        systemUnderTest = helper.loadScript(pathToScript, binding)

        // Mock the rest of the variables in Jenkins
        globals.Helpers.jenkins = systemUnderTest

        // Vars
        //helper.registerAllowedMethod("fromMap", [Map.class],  { m -> helper.loadScript("vars/fromMap.groovy").call(m) } )

        // When adding 'vars/*.groovy' metaClasses, make sure to use the binding variation of 'loadScript',
        //  this way you'll get access to all Jenkins methods (plus anuthing added before, like 'log' above)
        systemUnderTest.metaClass.fromMap = { helper.loadScript("vars/fromMap.groovy", binding).call(it) }
        systemUnderTest.metaClass.dump =    { helper.loadScript("vars/dump.groovy", binding).call(it) }
        systemUnderTest.metaClass.exit =    { helper.loadScript("vars/exit.groovy", binding).call(it) }
        systemUnderTest.metaClass.fromMap = { m  ->
            def fromMapVar = helper.loadScript("vars/fromMap.groovy", binding)
            fromMapVar.metaClass.exit = { s -> helper.loadScript("vars/exit.groovy", binding).call(s) }
            return fromMapVar.call(m)
        }

        // Jenkins Mocks
        helper.registerAllowedMethod("node", [String.class, Closure.class], {_, c -> c()})
        helper.registerAllowedMethod("withEnv", [Object.class, Closure.class], null)
        helper.registerAllowedMethod("usernameColonPassword", [Map.class], { args -> addVar(args.variable, usernameColonPasswordValue) })
        helper.registerAllowedMethod("withCredentials", [List.class, Closure.class], withCredentialsInterceptor)
        helper.registerAllowedMethod("sh", [Map.class], { args -> prettyprintln(args.script) ; return "0" } )
    }

    protected void addVar(key, value=null) {
        if(!value) value = "test_value_for_${key}"
        systemUnderTest.binding.setVariable(key, value)
    }

    protected void prettyprint(message){ print ("\u001B[35m${message}\u001B[0m") }
    protected void prettyprintln(message){ prettyprint(message) ; print ('\n') }

}
