package workflow.jenkins

import globals.Helpers
import org.junit.Before
import workflow.BaseTest

/**
 * This abstract class is used so we do not need
 * to use 'Helpers.jenkins' inside 'Pipeline' subclasses.
**/
abstract class BasePipelineTest extends BaseTest {

    // This will hold the file that contains the Pipeline subclass
    protected Script wrappingScript

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

        initAndMock()
    }

    /**
    * Prepares systemUnderTest for testing, returning a new instance
    * of the Pipeline subclass with all Jenkins mocks set up
    *
    * If arguments are passed, they get sent to the newCps method
    */
    protected void initAndMock(Object scriptArguments = null) {
        setPathToScript()

        wrappingScript = helper.loadScript(pathToScript, binding)

        // Needed in case initialization uses this variable
        Helpers.jenkins = wrappingScript

        if (scriptArguments) {
            systemUnderTest = wrappingScript.newCps(scriptArguments)
        } else {
            systemUnderTest = wrappingScript.newCps()
        }
        intercept(systemUnderTest.metaClass)

        Helpers.jenkins = systemUnderTest
        defaultJenkinsVariables.each { k, v ->
            systemUnderTest.metaClass.setProperty(k, v)
        }

        // Jenkins Mocks
        helper.registerAllowedMethod("withCredentials", [List.class, Closure.class], withCredentialsInterceptor)
        helper.registerAllowedMethod("withEnv", [Object.class, Closure.class], null)
        helper.registerAllowedMethod("string", [Map.class], { args -> updateSUTwithTemporaryValue(args.variable) })
    }
}
