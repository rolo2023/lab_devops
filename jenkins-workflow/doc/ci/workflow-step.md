# Workflow Step

Inside each 'stage', we define a series of steps, that identifiy an operation to do, with some parameters, under some circumstances.

## What can be executed?

The 'use' directive controls what to execute, by matching the value with a script inside the 'vars' directory.

Examples of scripts that are usable now are:
* **maven**: supports execution of an arbitrary Maven goal
* **curl**: support for curl GET and PUT operation
* **dimensions**: Deploy to a dimensions server
* And many more

## Where to execute?

The 'agent' label lets us select in which Agent this step will be executed.

>> For historical reasons, the 'node-label' parameter does the same as this one. If both are present, only 'agent' will be used.

### Bear in mind that upon entering a new node, the workspace will be **EMPTY**

For this reason, a good practice would be to select a node, and then define new _steps_ that will all share the same workspace.

## When to execute?

You may use the **when: [ condition_list ]** verb to **control if the step should be executed:**
  * List of "AND" conditions map. Will execute if ALL of them return TRUE
  * Accepts one element instead of an array. Example:
    * when: { true: '{{env.MYVAR}} == value' }
  * Acceptable conditions are:   
    * true: "eval"
      * if-then sentence that equals to "if (eval) return true"
      * Substitutes previuos "when: 'eval' " behaviour
      * As all variables are parsed before step execution, you can use {{ jinja }} notation
    * false: "eval"
      * if-then sentence that equals to "if (!eval) return true"
      * Substitues previous "when_not: 'eval' " behaviour
    * branch: [ branch_list ]
      * The module is executed if current branch exists in the given array. Regular expressions are allowed.
      * Substitutes "when_branch: [ branch_list ]" behaviour
    * found: 'fileName' or 'directoryName/'
      * The step is executed only if the given file name (or dir name, ended by '/') exists
    * not_found: 'fileName' or 'directoryName/'
      * The step is executed only if the given file name (or dir name, ended by '/') doesn't exist

**CONSIDERATIONS FROM 4.0**
  * The previous 'when' behaviour was accepting jenkins built-in methods (Ex: x.fileExists). While still valid, this is highly discouraged for security reasons and misbehaviours. Also using methods as "Eval.xy" inside the true|false behaviours may have unwanted side efects.
  * Even though the old forms of 'when_branch' and 'when_not' are still valid for compatibility, they cannot be mixed with this new array form

```yml
# This snippet will break the pipeline
  steps:
    - use: maven
      when:
        - found: "settings.xml"
      when_branch: [ "develop", "master" ]

# This snippet is still allowed, but deprecated/discouraged
  steps:
    - use: maven
      when: x.fileExists('settings.xml')
      when_branch: ["develop", 'master']

# This snippet is the proper one
  steps:
    - use: maven
      when:
        - found: "settings.xml"
        - branch: ['develop', 'master']
```

## Examples

1. a 'Publish' stage, defining two sub-steps that are run inside a node.

```yml
  publish:
    agent: 'maven-builder'
    environment: {}
    steps:
      # This step will execute only if BOTH 'when' statements return true
      - use: 'chimera'
        when:
          - true: "{{ env.USE_CHIMERA }} == 'yes' "
          - branch: [ 'develop', 'release', 'master' ]
        with_params:
          envName: 'work'
      # This step will execute only if current branch is one in the list
      - use: 'maven'
        when: { branch: [ 'develop', 'release', 'master' ] }
        with_params:
          credentials: id:artifactory_credentials
          action: put
          name: "{{releaseName}}"
          url: "https://globaldevtools.bbva.com/artifactory-api/"
```
