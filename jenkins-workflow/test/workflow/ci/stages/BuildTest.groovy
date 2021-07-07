package workflow.ci.stages

import ci.WorkflowStepImpl
import globals.Helpers
import globals.Modules
import globals.Settings
import org.junit.Test
import workflow.jenkins.BasePipelineTest

import static org.assertj.core.api.Assertions.assertThat

class BuildTest extends BasePipelineTest {

    void setPathToScript() {
        this.pathToScript = "src/ci/stages/Build.groovy"
    }

    @Override
    void setUp() throws Exception {
        super.setUp()

        // Mock a lot of Jenkins stuff
        [
                container: { label, c -> c() },
        ].each { method, impl -> Helpers.jenkins.metaClass.setProperty(method, impl) }

        // Let the framework intercept the calls to these classes too
        intercept(WorkflowStepImpl.metaClass)
        intercept(Helpers.jenkins.metaClass)
    }

    @Test
    void "We can build a multi-step build flow"() {
        // Given a build flow
        Settings.build = [
                pod_def  : 'pod_def_orquidea.yml',
                container: 'maven-builder',
                steps    : [
                        [
                                label      : '"build" with maven',
                                use        : 'dump',
                                with_params: [
                                        message: 'It works!'
                                ]
                        ],
                        [
                                label: 'pack application',
                                bin  : 'sh',
                                run  : 'ls'
                        ]
                ]
        ]

        // and a mock for vtrack
        Modules.vtrack = [updateVtrackBuildResults: { _ -> }]

        // Then the result is successful
        Boolean result = systemUnderTest.stageBuildApplication()
        assertThat(result).isTrue()

        // And three steps have been loaded and executed
        assertThat(helper.getCallStack().findAll { it.methodName == "fromStep" }.size()).isEqualTo(3)
        assertThat(helper.getCallStack().findAll { it.methodName == "executeStep" }.size()).isEqualTo(3)
    }

    @Test
    void "We can nest substeps in a build flow"() {
        // Given a build flow
        Settings.build = [
                pod_def  : 'pod_def_orquidea.yml',
                container: 'maven-builder',
                steps    : [
                        [
                                label: 'prepare artifact',
                                steps: [
                                        [
                                                label: 'pack application',
                                                bin  : 'sh',
                                                run  : 'ls'
                                        ],
                                        [
                                                label: 'do something else',
                                                bin  : 'sh',
                                                run  : 'ls'
                                        ]
                                ]
                        ]
                ]
        ]

        // and a mock for vtrack
        Modules.vtrack = [updateVtrackBuildResults: { _ -> }]

        // Then the result is successful
        Boolean result = systemUnderTest.stageBuildApplication()
        assertThat(result).isTrue()

        // And four steps have been loaded and executed (two 'main' and two 'inner')
        assertThat(helper.getCallStack().findAll { it.methodName == "fromStep" }.size()).isEqualTo(4)
        assertThat(helper.getCallStack().findAll { it.methodName == "executeStep" }.size()).isEqualTo(4)
        printCallStack()
    }
}
