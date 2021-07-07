package ci

import globals.Helpers
import globals.Settings

static WorkflowStepImpl newCps(Map stepConfig) { new WorkflowStepImpl().fromStep(stepConfig) }

class WorkflowStepImpl extends jenkins.Pipeline {

    //* This represents a Jenkins Agent's node label
    private String agentNodeLabel = null

    /**
    This label corresponds to a DEFINED AND AVAILABLE container.
    It can belong to a defined container, or refer to a pod inside a definition
    */
    private String containerLabel = null

    //* This represents the name of the module to execute
    private String use = null

    /**
     * This represents the collection of all 'when' conditions that determine whether this step will be executed.
     * Defaults to empty (which will evaluate to true).
     *
     * IMPORTANT: These conditions are stored WITHOUT substitutions, allowing for a lazy evaluation of sorts.
     */
    private List executionConditions = []

    //* These represent a set of environment variables to be set during execution
    private Map environment

    //* String that will be logged (With INFO level) to the Jenkins output upon step execution
    private String label

    //* Ansible-style parameter blob to be sent to the module that gets executed
    private Map withParamsMap = [:]

    //* IDs of  Jenkins Credentials to be loaded and set during execution
    private Map withCredentialsMap = [:]

    //* If defined, an extra set of steps to execute
    private List subSteps = []

    //* For backwards compatibilty with 'run' blocks
    Map runParameters = null

    WorkflowStepImpl fromStep(Map rawStep) {
        if(!rawStep) rawStep = [:]
        this.environment = rawStep.environment as Map ?: [:]
        this.withCredentialsMap = rawStep.with_credentials as Map ?: [:]

        // Backwards compatibility
        if (rawStep.containsKey('run')) {
            this.runParameters = rawStep
        } else if (rawStep.containsKey('use')) {
            this.use = rawStep.use
        }

        this.withParamsMap = rawStep.with_params as Map ?: [:]

        // Determine execution conditions
        this.executionConditions = Helpers.getConditions(rawStep)

        // For each defined step, create a new Workflowstepimpl
        this.subSteps = rawStep.steps.collect { WorkflowStep.newCps(it as Map) }
        Helpers.log.debug "Step has ${this.subSteps.size() > 0 ? "no" : this.subSteps.size().toString()} sub-steps"

        // backwards compatibility (remove node_label on further versions)
        this.agentNodeLabel = rawStep.agent ?: (rawStep.node_label ?: null)

        // It is illegal for a step to define BOTH a container and an agent
        this.containerLabel = rawStep.container ?: null
        if (this.containerLabel != null && this.agentNodeLabel != null) {
            Helpers.error "Step definition error: you cannot define both a container and an agent"
            return null
        }

        // It is illegal for a Stage to have a null label
        this.label = this.generateStageLabel(rawStep)

        // Return instance, for chaining
        return this
    }

    private Boolean executeStepClosure() {
        // sub-steps in a step take more priority rather than any 'use' declaration
        if (this.subSteps.size() > 0) {
            Helpers.log.debug "Executing substeps: ${this.subSteps}"
            for (WorkflowStepImpl step in this.subSteps) {
                step.executeStep()
            }
            return true
        }

        List envList = getEnvList(this.environment)
        Closure stepClosure = getStepClosure(envList) // Breaks immediately if something is wrong
        if (!stepClosure) { return false }

        List credentialList = getCredentialList()
        if (credentialList.size() == 0) {
            Helpers.log.debug "Executing ${this.label} without credentials"
            stepClosure()
        } else {
            Helpers.jenkins.withCredentials(credentialList) {
                Helpers.log.debug "${this.label}"
                stepClosure()
            }
        }
        return true
    }

    Boolean canExecute() {
        Helpers.canDoWhen(this.executionConditions)
    }

    /**
     * Tries to execute current step, return false it conditions do not match
     */
    Boolean executeStep() {
        // Run step only if conditions are satisfied
        if (!this.canExecute()) {
            Helpers.log.debug "Step conditions not met, skipping..."
            return false
        }

        if (this.agentNodeLabel) {
            Helpers.log.debug "Running step in agent ${this.agentNodeLabel}"
            Helpers.jenkins.node(this.agentNodeLabel) { return executeStepClosure() }
        } else if (this.containerLabel) {
            Helpers.log.debug "Running step in container ${this.containerLabel}"
            Helpers.jenkins.container(this.containerLabel) { return executeStepClosure() }
        } else {
            return executeStepClosure()
        }
    }

    //* A Jenkins Stage without a label will raise a java.lang.IllegalArgumentException
    private String generateStageLabel(Map stepConfiguration) {
        if (stepConfiguration.containsKey('label')) {
            return stepConfiguration.label
        }

        // If there are substeps, no label needed
        String label = null
        if (this.subSteps.size() > 0) {
            return label
        }

        //* Try to print what it will be executed
        def what = null
        if (this.use != null) {
            what = this.use
        } else if (this.runParameters != null) {
            what = this.runParameters.bin
        } else {
            what = ""
        }

        //* Only dislay agent if one is set
        if (this.agentNodeLabel != null) {
            return "Execute ${what} in ${this.agentNodeLabel}"
        } else if (this.containerLabel != null) {
            return "Execute ${what} in ${this.containerLabel}"
        } else {
            return "Execute ${what}"
        }
    }

    private List getEnvList(Map values){
        List envList = []
        values?.each { k, v -> envList << "${k}=${v}" }
        return envList
    }

    private List getCredentialList(){
        if(!this.withCredentialsMap) { return [] }

        List credentialList = []
        int varId = 1
        this.withCredentialsMap.each { c ->
            Helpers.log.debug "Helpers :: getCredentialList :: each :: ${c}"
            if(! c.var){ c.var = "VAR_${varId}" ; varId++ }
            if(! c.type){ c.type = '' }
            Helpers.log.debug "Helpers :: getCredentialList :: each :: ${c} modified"
            credentialList << Helpers.getCredential(c.type, c.id, c.var)
        }
        Helpers.log.debug "Helpers :: getCredentialList :: return :: ${credentialList}"
        return credentialList
    }

    /**
     * This method would either call 'Helpers.run' to execute a binary command (and complain, since it is something
     * we want to deprecate) or try to call the module identified by 'use'
     *
     * @param envList   Environment variables to load in the call
     * @return
     */
    private Closure getStepClosure(List envList=[]) {
        if (!this.use && this.runParameters == null) {
            Helpers.log.warn "step has no 'steps', no 'run' commands, or no 'use' declaration: skipping"
            return null
        }

        if (this.runParameters != null) {
            return {
                Helpers.jenkins.withEnv(envList) {
                    Helpers.run(this.runParameters, Settings.build.environment) // TODO: it should use 'this.environment'
                }
            }
        } else {
            try {
                // This allows to catch a MissingPropertyException before the closure is executed
                this."${this.use}"
                return {
                    Helpers.jenkins.withEnv(envList) {
                        this."${this.use}".fromStep(this.withParamsMap)
                    }
                }
            } catch (MissingPropertyException missingPropertyEx) {
                Helpers.error "There is no callable unit called '${this.use}' in 'vars/' "
                return null
            }
        }
    }
}

return this
