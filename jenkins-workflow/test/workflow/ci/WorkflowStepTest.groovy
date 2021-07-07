package workflow.ci


import globals.Settings
import org.junit.Test
import workflow.jenkins.BasePipelineTest

import static org.assertj.core.api.Assertions.assertThat

class WorkflowStepTest extends BasePipelineTest {

    @Override
    void setPathToScript() {
        this.pathToScript = "src/ci/WorkflowStep.groovy"
    }

    @Test
    void "We can init a Step off configuration"() {
        // Given a deploy configuration for Dimensions
        def dimensionsConfiguration = [
                when_branch: ['{{ vars.branch }}'],
                use        : 'dimensions',
                with_params: [param1: 1]
        ]

        // And a current state where its conditions are met
        Settings.repo = [branch: 'develop']
        Settings.vars = [
                branch: 'develop'
        ]

        // When we create a new Step instance off the first step
        initAndMock(dimensionsConfiguration)

        // Then we get all pieces in-place
        assertThat(systemUnderTest.use as String).isEqualTo('dimensions')
        assertThat(systemUnderTest.executionConditions as List).isEqualTo([
                [branch: ['{{ vars.branch }}']]
        ])
        assertThat(systemUnderTest.withParamsMap as Map).isEqualTo([param1: 1])
        assertThat(systemUnderTest.agentNodeLabel as String).isNull()

        // And conditions evaluate correctly
        assertThat(systemUnderTest.canExecute() as Boolean).isTrue()

        // And are still stored 'raw'
        assertThat(systemUnderTest.executionConditions as List).isEqualTo([
                [branch: ['{{ vars.branch }}']]
        ])
    }

    @Test
    void "If execution conditions are not met, step is not executed"() {
        // Given a deploy configuration with a condition that is not met
        def unmetConditions = [
                when       : false,
                use        : 'dimensions',
                label      : 'Deploy To Dimensions',
                with_params: [param1: 1]
        ]

        // Amd we create a new Step instance off the first step
        initAndMock(unmetConditions)

        // Then we would block execution
        assertThat(systemUnderTest.canExecute() as Boolean).isFalse()
    }

    @Test
    void "If a label is set, it is used as Stage label"() {
        // Given a deploy configuration with a label
        def dimensionsConfiguration = [
                when_branch: ['develop'],
                use        : 'dimensions',
                label      : 'Deploy To Dimensions',
                with_params: [param1: 1]
        ]

        // When we create a new Step instance off the first step
        initAndMock(dimensionsConfiguration)

        // Then we get the expected label
        assertThat(systemUnderTest.label as String).isEqualTo 'Deploy To Dimensions'
    }

    @Test
    void "If a label is NOT set, a non-empty one is generated"() {
        // Given a deploy configuration WITHOUT a label
        def dimensionsConfiguration = [
                when_branch: ['develop'],
                use        : 'dimensions',
                with_params: [param1: 1]
        ]

        // When we create a new Step instance off the first step
        initAndMock(dimensionsConfiguration)

        // Then we get the expected label for the stage
        assertThat(systemUnderTest.label as String).isEqualTo 'Execute dimensions'

        // And when we add an agent parameter
        dimensionsConfiguration << [agent: 'deploy_agent']
        initAndMock(dimensionsConfiguration)

        // Then we get an expanded label for the stage
        assertThat(systemUnderTest.label as String).isEqualTo 'Execute dimensions in deploy_agent'
    }

    @Test
    void "we can init a series of steps off proper configuration"() {
        // Given a systemUnderTest configuration
        def publishConfiguration = [
                when : true,
                label: 'multi-step',
                steps: [
                        [
                                when_branch: ['master'],
                                use        : 'chimera',
                                with_params: [param1: 1],
                                label      : 'executing chimera',
                                node_label : 'chimera_agent'
                        ],
                        [
                                when_branch: ['develop'],
                                use        : 'dimensions',
                                with_params: [param1: 1],
                                label      : 'executing dimensions',
                                node_label : 'dimensions_agent'
                        ]
                ]
        ]

        // And a current state where some conditions are met
        Settings.repo = [branch: 'develop']

        // And a step with that configuration
        initAndMock(publishConfiguration)

        // And I have  mocked up modules
        Boolean executedDimensions = false
        Boolean executedChimera = false
        systemUnderTest.subSteps[0].metaClass.chimera = [fromStep: { m -> executedChimera = true }]
        systemUnderTest.subSteps[1].metaClass.dimensions = [fromStep: { m -> executedDimensions = true }]

        // Then we have two sub steps
        List subSteps = systemUnderTest.subSteps
        assertThat(subSteps.size()).isEqualTo(2)

        // And each is different
        assertThat(subSteps[0].use as String).isEqualTo('chimera')
        assertThat(subSteps[1].use as String).isEqualTo('dimensions')

        // But just one will be executed
        assertThat(subSteps[0].canExecute() as Boolean).isFalse()
        assertThat(subSteps[1].canExecute() as Boolean).isTrue()

        // And When I execute it
        systemUnderTest.executeStep()
        printCallStack()

        // Just one "module" has executed
        assertThat(executedChimera).isFalse()
        assertThat(executedDimensions).isTrue()
    }

    @Test
    void "we can choose when a step is executed with 'when_branch' clauses"() {
        // Given a deploy configuration for Dimensions
        def dimensionsConfiguration = [
                when_branch: ['develop', 'feature/DEVELOP_.*'],
                use        : 'dimensions',
                with_params: [param1: 1]
        ]

        // And the git branch does not match condition
        Settings.repo = [branch: 'feature/DEVELOP']

        // When we init step with that configuration
        initAndMock(dimensionsConfiguration)

        // Then it will not be executed
        assertThat(systemUnderTest.canExecute()).isFalse()

        // And when the git branch DOES match
        Settings.repo = [branch: 'feature/DEVELOP_SOMETHING']
        initAndMock(dimensionsConfiguration)

        // Then it will be executed
        assertThat(systemUnderTest.canExecute()).isTrue()
    }

    @Test
    void "we run nothing when no 'use' module is set"() {
        // Given a deploy configuration without 'use'
        def configWithoutUse = [
                with_params: [param1: 1]
        ]

        // When we create a new Step instance off it
        initAndMock(configWithoutUse)

        // And execute the step
        def result = systemUnderTest.executeStep()

        // Then nothing has happened
        assertThat(result).isFalse()

        // And there should be no errors
        assertThat(helper.getCallStack().findAll { it.methodName == "error" }.size()).isEqualTo(0)
    }

    @Test
    void "we get an error when a not-defined module is used"() {
        // Given a deploy configuration with a not-implemented module
        def unknownModuleConfiguration = [
                with_params: [param1: 1],
                use        : 'something'
        ]

        // When we create a new Step instance off it
        initAndMock(unknownModuleConfiguration)

        // And execute the step
        def result = systemUnderTest.executeStep()

        // Then Nothing has happened
        assertThat(result).isFalse()

        // And we get an error
        assertThat(helper.getCallStack().findAll { it.methodName == "error" }.size()).isEqualTo(1)
    }

    @Test
    void "Execution is NEVER done in a separate node, if it is not configured"() {
        // Given a deploy configuration for Dimensions WITHOUT a node
        def dimensionsConfiguration = [
                when_branch: ['develop'],
                use        : 'dimensions',
                with_params: [param1: 1]
        ]

        // And a step with that configuration
        initAndMock(dimensionsConfiguration)

        // And I have an empty dimensions module
        systemUnderTest.binding.setVariable('dimensions', [fromStep: { m -> }])

        // And when the git branch DOES match
        Settings.repo = [branch: 'develop']

        // And execute the step
        systemUnderTest.executeStep()

        // Then  no error has happened
        assertThat(helper.getCallStack().findAll { it.methodName == "error" }.size()).isEqualTo(0)

        // And it has NOT been run inside a node
        assertThat(helper.getCallStack().findAll { it.methodName == "node" }.size()).isEqualTo(0)
    }

    @Test
    void "Execution is done in a separate node, if it so configured"() {
        // Given a deploy configuration for Dimensions WITHOUT a node
        def dimensionsConfiguration = [
                when       : true,
                use        : 'dimensions',
                node_label : 'dimensions-node-label',
                with_params: [param1: 1]
        ]

        // And a step with that configuration
        initAndMock(dimensionsConfiguration)

        // And I have an empty dimensions module
        Boolean executedDimensions = false
        systemUnderTest.binding.setVariable('dimensions', [fromStep: { m -> executedDimensions = true }])

        // And when the git branch DOES match
        Settings.repo = [branch: 'develop']

        // And execute the step
        systemUnderTest.executeStep()

        // Then no error has happened
        assertThat(helper.getCallStack().findAll { it.methodName == "error" }.size()).isEqualTo(0)

        // And it NOW has been run inside a node
        assertThat(helper.getCallStack().findAll { it.methodName == "node" }.size()).isEqualTo(1)

        // And the "module" has executed
        assertThat(executedDimensions).isTrue()
    }

    @Test
    void "getEnvList correctly formats values in Environment values"() {
        // Given a map with values to set in environment
        Map testValues = ['entry': 'value']

        // When we call getenvlist
        def result = systemUnderTest.getEnvList(testValues)

        // Then we get a nice envList
        assertThat(result[0] as String).isEqualTo('entry=value')
    }

    @Test
    void "getEnvList correctly formats EMPTY values in Environment values"() {
        // When we call getenvlist with a null map
        List result = systemUnderTest.getEnvList(null)

        // Then we get a nice envList
        assertThat(result).isEmpty()

        // And when we call getenvlist with an empty map
        result = systemUnderTest.getEnvList([:])

        // Then we get a nice envList too
        assertThat(result).isEmpty()
    }

    @Test
    void "fromStep can correctly parse an empty definition"() {
        // Given an empty step config
        Map stepConfig = [:]

        // When we parse it
        def step = systemUnderTest.fromStep(stepConfig)

        // Then we get a valid step object
        assertThat(step).isNotNull()
    }

    @Test
    void "fromStep that defines just a container works"() {
        // Given an empty step config
        Map stepConfig = [
                container: 'a-container-label',
                steps    : []
        ]

        // When we parse it
        def step = systemUnderTest.fromStep(stepConfig)

        // Then we get a valid step object
        assertThat(step).isNotNull()
    }
}