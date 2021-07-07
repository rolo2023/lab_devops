# Setting up the group's configuration file
It's common to share a configuration between different components (different uuaas). Here is where the concept **group** rises.

> A **group** is a set of uuaas sharing the same configuration. That's the difference between **uuaa** and **group**.
> **group's** name is identified in the **Jenkinsfile**.

Configuration that affects the pipeline, serves as base values overwritable by Jenkinsfile.
Following sections relates to each of the parameters from the **{{group}}.yml** file.


## Logging traces

**verbosity**: console output level
* 0 - ERROR
* 1 - WARNING
* 2 - INFO **(default)**
* 3 - DEBUG

```yml
verbosity: 1
```

## Architecture type

**architecture**: architecture whose pipeline will be executed.
* spring **(default)**
* nacar
* generic

```yml
architecture: spring.r1
```

## Form values

In order to support custom values in Jenkins builds, we have the option of creating parameters directly from code, and using them via [variable substitution](./replace-variables.md).

The `form` entry is a list of Form objects, with the following syntax:

```yml
form:
  - id: a_boolean_param
    type: boolean
    default: true|false
    description: Lorem Ipsum loret....

  - id: a_string_value
    default: "PARAM_DEFAULT_VALUE"
    type: string
    description: A string value, can be null or empty...

  - id: a_dropdown_list
    type: choice
    choices:
      - ar
      - es
      - uy
```

We can use these parameters to modifify our build command, to enable/disable certain stages or steps... be creative!

### Conditional parameters

A form value can have its default value modified by a condition.
These conditions cannot depend on form values themselves, but they can depend on the current branch.
If a condition is set, it is *MANDATORY* to set an alternative default value:

```
  - id: maven_build_command
    when_branch:
      - develop
      - master
    type: string
    default: 'clean install versions:set -DnewVersion={{ artifact.version }}'
    default_when_false: 'clean install versions:set -DnewVersion=SNAPSHOT'
    description: |
      Maven Build command
```

## Modules
Modules are a core set of tools/technologies, that get used in different parts of the code

List of core modules:

Module | Mandatory | Practice | Short description
---------|----------|---------|-----
[bitbucket](./modules/bitbucket.md)|true| Code | -
[artifactory](./modules/artifactory.md)|true| Build & Deploy | -
[vtrack](./modules/vtrack.md)|false| cross | -
[samuel](./modules/samuel.md)|true| Enforcement | -


How to setup a module in a group's  file:
```yml
modules:
  {{module_A_id}}:
    {{param_1}}: {{param_1_value}}
    {{param_2}}: {{param_2_value}}
    {{param_3}}: {{param_3_value}}

  {{module_B_id}}:
    {{param_1}}: {{param_1_value}}
    {{param_2}}: {{param_2_value}}
    {{param_3}}: {{param_3_value}}
```


## Store
This parameter sets up the location and nomenclature of the artifact to be stored within the specified module (by default, **artifactory**). It can NOT be specified and so it will take the default values of the YML-template.

## Parameters
* **module**: storage module (Artifactory)
* **context**: Artifactory's local repository name
* **full_context**: true | false.
  * true: discard prefix and sufix to the context
    * _context + full_context=true_: the context is the full path to locale artifacts
  * false: add a prefix and sufix to the context
* **virtual**: Artifactory's virtual repository name
  * If virtual is empty the module resolves its name using **context** value (the local repository).
* **file**: Artifact itself
    * **name**: name of the artifact. By default:
      * for **PR** it will be `SNAPSHOT-{{commit}}`
      * for **develop** it will be `SNAPSHOT`
      * for **master** it will be `{{version}}`.
    * **path**: Location of the artefact built.
* **release**: Map of nomenclatures of the artifact according to the branch being executed. Requires the following mandatory parameters:
  * **Branch type**: Possible values are **feature, hotfix, develop, master or PR**. Filter the type of the branch on which it takes effect. To make an effect on all of them, use _any_.
    * **name**
    * **suffix**
    * **upload**: yes or no

## Example

```yml
store:
  module: artifactory
  context: 'arquitectura-spring-{{architecture}}-apps-global-generic'
  virtual: 'repository-arquitectura-spring-global-generic'
  full_context: yes
  file:
    path: "."
    name: "{{uuaa}}-{{version}}.zip"
  release:
    feature:
      name: 'SNAPSHOT-{{branch}}-{{build_in}}'
      suffix: 'snapshots'
      upload: yes
    hotfix:
      name: 'SNAPSHOT-{{branch}}-{{commit}}'
      suffix: 'snapshots'
      upload: yes
    develop:
      name: 'SNAPSHOT'
      suffix: 'releases'
      upload: yes
    master:
      name: "{{version}}"
      suffix: 'releases'
      upload: yes
    PR:
      name: "SNAPSHOT-{{commit}}"
      suffix: 'snapshots'
      upload: yes
```

## Stages

> **Stages** are mandatory and 1-time execution per build

Stages are the abstraction of a series of [steps](./ci/workflow-step.md) to be run inside a node (or series of nodes).
While these stages look very similar in terms of configuration, for historical reasons there are some key differences between them.

This Pipeline consists of these **FIXED** stages (some can be empty though):
* [Build](stages/build.md)
* [Publish](stages/publish.md)
* [Test](stages/test.md)
* [Deploy](stages/deploy.md)
* [End](stages/end.md)


### Conditional steps in stages

Each of the steps that are described in your configuration file can be _opted out_ if certain conditions are met.

1. Never executed

    ```yml
    - use: maven
      when: [never|false|no]
      with_params: {}
    ```
2. Always executed (Equivalent of not using 'when')

    ```yml
    - use: maven
      when: [always|true|yes]
      with_params: {}
    ```
3. Conditionally executed, depending on values

    ```yml
    - use: maven
      when: "{{ vars.packageType }} == 'jar' || {{ vars.release }} == false"
      with_params: {}

    - use: docker
      when: "{{ vars.packageType }} == 'jar' && {{ vars.release }} == true"
      with_params: {}

    # ALL MUST MATCH!! This is equivalent to &&
    - use: kaniko
      when: 
        - "{{ vars.packageType }} == 'jar'"
        - "{{ vars.release }} == true"
      with_params: {}

    # This one uses a Jenkins form value that is either true or false   
    - use: kaniko
      when: "{{ forms.some_check }}"
      with_params: {}
      ```

4. Conditionally executed, depending on current branch (NOTE: _when_branch_ allows for regular expressions of branch names)

    ```yml
    - use: maven
      when_branch: ['PR-*', 'develop']
      with_params: {}
 
    # Same as above, but WITHOUT regular expression support
    - use: kaniko
      when: "{{ repo.branch }} == 'develop'"
      with_params: {}
    ```

## Custom modules

The purpose of separating into modules is not only to reuse them in different stages, but also to facilitate the transition between modules.

> **Scenario**
> Suppose you want to shift _dimensions_ in favor of a new deployment system. If the new module adopts the names of the methods of the current one (since parameters are global in scope, they are not called between modules), one would only have to change the call in the corresponding stage without having to touch code.


# Real example

```yml
---
verbosity: 1
branching_model: ''  # either 'release', 'norelease', 'none' or 'auto'. Defaults to 'auto' if empty

# Empty parameters list, by default
form: []

modules:
  git:
    credentialsId: bot-globaldevops-pro-ssh
    # Por defecto en la construcción de las ramas features se hace un merge desde la rama develop
    # con este parámetro podemos deshabilitar este comportamiento
    mergeDevelop: true

  bitbucket:
    url: https://globaldevtools.bbva.com/bitbucket
    credentialsId: "spring_{{country}}_{{group}}_bitbucket_token"
    credential_type: userpass

  vtrack:
    # If unset, Vtrack will not be used. This is NOT ideal, but is the default case
    enabled: no

    # If set, Vtrack exceptions will not break your build
    should_ignore_error: no

    # You should not change this, unless you know what you are doing
    base_url: 'https://vtrack.central.platform.bbva.com'

    # If set, we'll point to Vtrack in the lab-01 region
    debug: no

    # These will be retrieved via platform or https://pkiglobal-ra.live.es.nextgen.igrupobbva/
    # Make sure they work in the lab-01 region when debugging!
    ether_certificate: 'vtrack_ether_certificate'
    ether_key: 'vtrack_ether_key'

    # If set here, it will be used, along with your country, to retrieve namespace
    # NOTE: Overrides global UUAA setting, to be deprecated
    uuaa: ''

    # If you do not have an UUAA we will write in this namespace -assuming your credentials have write access
    # DO NOT SET THIS VALUE if you have an UUAA
    namespace:  ''

    # One of 'GLO', 'ESP', 'MEX', 'PER', 'COL', 'USA', 'ARG', 'CHL', 'PRY', 'PRT', 'TUR', 'URY', 'VEN'
    country: 'ESP'

    # Usually 'spring.r1', 'nacar.r1' or 'generic', check Vtrack API for the full list
    architecture: 'generic'

  artifactory:
    server_id: globaldevtools
    credentialsId: "spring_{{country}}_{{group}}_artifactory_token"
    credential_type: userpass

## Configuración de almacén de artefactos.
store:
  module: artifactory
  context: '{{group}}-{{country}}-mvn'
  virtual: '' # Por si el repositorio virtual difiere de la nomenclatura para releases-snapshots

  # Artefacto que se publica/despliega
  file:
    # Ubicación. Si no se especifica abspath (/) la búsqueda comienza en el workspace
    path: '.'
    # Nombre del artefacto
    name: "{{ uuaa }}-{{ artifact.version }}.zip" 

  # Qué ramas publicarán un artefacto, con qué sufijo y en qué sub-repositorio (basado en store.upload)
  release: 
    any:
      name: 'SNAPSHOT-{{commit}}'
      suffix: 'snapshots'
      upload: no
    develop:
      name: 'SNAPSHOT'
      suffix: 'releases'
      upload: yes
    master:
      name: '{{ repo.version }}'
      suffix: 'releases'
      upload: yes

stages:
  build:
    node_label: ''
    environment: {}
    stash: no
    keep:
      branches: [ 'master' ] ## Sólo afecta a estas ramas
      files: '**/*.*' ## Expresión ANT de los archivos que se quieren guardar
      greedy: no  # Si no se encuentran archivos según la expresión anterior, copiar todo el directorio

    ## Pasos para hacer la construcción del artefacto
    steps:
      - label: 'Build with maven'
        use: 'maven'
        with_params:
          maven_settings: 'file: settings_spring'
          goal: 'clean install versions:set -DnewVersion={{ repo.version }}'
          with_cachelo:
            key: '{{ uuaa }}-maven-cache'
            paths: ["{{ env.WORKSPACE }}/.m2"]

      - use: 'wSonar'
        when: never
        with_params:
          parameters: ''
          qualityGate: null
          qualityProfile: null
          waitForQualityGate: false


  publish:
    environment: {}
    steps:
      - use: 'chimera'
        when: never  # Keep like this until you install the chimera credentials in your agent
        with_params:
          chimeraCredentialsId: 'chimera-work'
          chimeraEnvName: 'work'
      - use: 'curl'
        when_branch: ['feature/*', 'develop', 'release', 'master']
        with_params:
          credentials: "spring_{{country}}_{{group}}_artifactory_token"
          action: put
          url: "{{release.urlUpload}}/{{release.filePath}}"
          localFile: "{{artifact}}"

  ## Fase de TEST (normalmente tests de aceptación, regresión, smoke, etc)
  test: {}

  deploy:
    steps: []

  ## Fase de FIN de pipeline. Útil para imprimir mensajes en consola, enviar un correo, etc...
  end:
    always:
      - use: log
        with_params:
          message: 'This message is always shown'
          level: DEBUG
    error:
      - use: log
        with_params:
          message: 'An error has happened'
          level: WARNING
      - use: email
    success:
      - use: log
        with_params:
          message: 'Only shown when the pipeline has ran successfully'
    changed:
      - use: log
        with_params:
          message: "Build result changed from last build!"
          level: DEBUG
    fixed:
      - use: log
        with_params:
          message: "Build fixed!!"
          level: INFO
      - use: email
        with_params:
          title: "Congratulations, {{ repo.slug }} has been fixed!"
          body: |
            Last build of  building {{ repo.slug }}@{{ repo.branch }} has fixed previous errors.
            Check results here: : {{ env.BUILD_URL }}


```










