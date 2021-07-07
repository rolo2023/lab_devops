---
# Changelog

All notable changes to this project will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

# 4.3.0

### Updated

- Bump [Cachelo version to 1.3.0](https://globaldevtools.bbva.com/bitbucket/projects/BGP/repos/cachelo/browse?at=refs%2Ftags%2F1.3.0)

### Changed

- Deploy information is inserted in vTrack from the [Deploy stage](./src/ci/stages/Deploy.groovy). This means that deploy modules, such as [dimensions](./vars/dimensions.groovy), are now responsible of calling `Modules.vtrack.updateVtrackDeployResult` with the information to be stored as Deploy information.
    - NOTE: In future releases, the only accepted input for a deploy module will be a Vtrack Locator

### Fixed

- `vars/publishArtifactoryMvn.groovy` will allow the assignment of custom artifactory repositories.

```yml
    - label: 'Publish Artifactory'
    use: 'publishArtifactoryMvn'
    with_params:
        path_pom: '{{vars.path_pom}}'
        artifactory_id: 'artifactory_devops'
        custom_repo: 
            release: 'app-release'
            snapshot: 'app-snapshot'
            repository: 'repository-app'
```

- `vars/publishArtifactoryMvn.groovy` will now use container's _MAVEN_HOME_ environment variable.
- We now can execute code inside a node or a container as expected in JE Kubernetes.
- Refactor logic that evaluates 'when' conditions, so that all these evaluate to `True`:
  
  ```yml
      - use: 'maven'
        when:
          true: "{{ form.value }}"  # value is True
  
      # Evaluation works fine again
      - use: docker
          when: "{{ vars.packageType }} == 'jar' && {{ vars.release }} == true"
          with_params: {}
  
      # ALL MUST MATCH!! This is equivalent to &&
      - use: kaniko
        when:
          - "{{ vars.packageType }} == 'jar'"
          - "{{ vars.release }} == true"
        with_params: {}
  ```

- Refactor Build stage sub-step handling. Now we can properly use steps inside steps, and this is legal:

  ```yml
    build:
      pod_def: 'pod_def_orquidea.yml'
      container: maven-builder
      steps:
        - label: 'Build and package application'
          container: maven-builder
          steps:
          - use: 'maven'
            label: 'Maven build' #Etiqueta
            with_params: #Paramentro a usar
              goal: 'clean install' #Se llama el id especificado en el form (clean install).
              maven_settings: "file: spring_{{country}}_{{group}}_settings_file"
          - label: 'Create .tar file'
            bin: sh
            run: "tar -cvzf artifact-{{ version }} target/*.jar"
  
        - use: 'wSonar'
          container: 'sonar-container'
          label: 'Sonar Analysis'
          with_params:
              parameters: '-Dsonar.inclusions=**/*.java -Dsonar.sources=. -Dsonar.java.binaries=. -Dsonar.sourceEncoding=UTF-8 -Dsonar.ws.timeout=720'
              versionLibSonar: lts
  ```
  > This change serves as a preparation for an upcoming change to variable substitution.


### Added

- Kaniko module, so we can build and push Docker images in JE Kubernetes. [Docs](./doc/vars/kaniko.md)
- [Migration script](./utils/generator/pod_template_generator.sh) that transalates MESOS agent definitons into Kubernetes Pod Templates
- Samuel module, we now can specified country entity. [Docs](./doc/modules/samuel.md)

## 4.2.1

### Fixed

- [GLEP-115372] We can now choose an agentTag for Pod Templates, but it's [better to read the docs](doc/ci/agent-tag.md).

## 4.2.0

### Added
- [GDAD-1025] Mandatory Samuel execution, check samuel doc.
- [GDAD-1149] Inside a master, if the config is the same, the same agent will be used for both the build and deploy phases.
- [NO-ISSUE] Added the 'fromLibrary' module to add libraries dynamically.
- [NO-ISSUE] Added the 'jfrogCli' module to execute jfrog-cli from steps.
- [GDHL-158] It's added It's describe Dimensions json config file content and credentials for authentication in Dimensions. Credentials and secret file are stored in Jenkins Master node of country.
- [GDHL-158] It's added dimensionsAmerica module in vars directory for America Project:

        steps:
          - label: Deploy to Dimensions in Lago Esmeralda
              use: dimensionsAmerica
              when_branch: [ 'develop', 'master' ]
              with_params:
                http_proxy: 'PROXYVIP.IGRUPOBBVA:8080'
                version: '1.1'
                verbosity: 2
                levels: [ 'NINGUNO', 'DESARROLLO', 'INTEGRADO', 'QA', 'AUS', 'PRODUCCION' ]

### Changed
- [NO-ISSUE] Do not reserve as many cores upfront during Load stage

### Fixed
- [NO-ISSUE] Bump support workflowlibs library to 1.25.0, which alleviates the problem with the Bitbucket 7 PR merge messages.

## 4.1.0

### Added
- [GDAD-917] Allow disabling branching model by setting it to 'none' in configuration file
- [GDAD-950] Add a new, working "End" stage
- [NO-ISSUE] Allow 'libRunTests' to set a custom version for the test-legacy library
- [NO-ISSUE] Added email module, by default we will try to send emails on failed and fixed builds

### Fixed
- BUGFIX: Build step in Kubernetes execution without a specified container does not fallback to 'generic' node but to an existing defined container 'devops-global-generic'.
- [GDAD-952] The branch can now generate an artifact with a different name depending on the configuration of the store.release
- HOTFIX: Do not crash while rendering a crash
- BUGFIX: Do not crash when a stage build environment is empty or null, and we use a 'bin' statement in a step
- FEAT: Rewrote 'log' module so it can be used to dump anything, considering variables like "This is a build of {{ repo.project }}"
- BUGFIX: Avoid spawning unnecessary `withCredentials` block, to avoid deadlocks in pre-2.80 versions of the 'Pipeline:Groovy' plugin (see ./doc/jenkins-requirements.md)
- BUGFIX: Assign correct null value to node_label durig step parsing
    This caused the `Step definition error: you cannot define both a container and an agent` error with a config like:


      test:
        container: 'maven-agent'
        steps: []


### Special mention

This change is specially important, and may impact your work: Configuration merges will ALWAYS overwrite default sections with application-specific sections.
In some cases it may result in loss of data, but it was an inconsistent behaviour.

## 4.0.5

### Fixed
- [NO-ISSUES] Enabled vTrack 'type' field for generated artifacts

## 4.0.0

### Added
- [GDAD-650] Custom Pods definition
    - Add the ability to define custom containers in a custom pod definition file in the country configuration repository for kubernetes Pipeline
    - Add the ability to use the custom containers in each pipeline stage

- [GDAD-434] Hybrid pipeline code
    - Add pod definition for Kubernetes Pipeline
    - Code can seamslessly work in both Mesos or Kubernetes versions of Jenkins Enterprise
    - GREATLY simplified flow, now it should be clearer what we do

- [GDAD-437] Vtrack Library points to Ether endpoint
    - [GDAD-611]: Refactor to Get Namespace from Settings or Ether Government API
    - [GDAD-617] Add new Vtrack architecture values
    - [GDAD-437] Rethink where and how we gather Vtrack information: Only register artifacts when they are successfully generated
    - [GDAD-437] Enable or Disable Vtrack via flag

- [GDAD-436] Enable Samuel integration
- [SDYO-1945]: Updated CHANGELOG.md to apply Release Best Practices
- Form parameters can now be conditionals.

### Changed

- [SDYO-1945]: Allowed snapshot package to deploy with Dimensions
- Bitbucket module now counts as mandatory
- Sonar library version is now 'LTS'. It upgrades Samuel compatibility, but needs Git 2.6+. Check [the documentation](doc/vars/wSonar.md) for more details.
- Updated libChimera 2.3.0


## [3.4.1](https://globaldevtools.bbva.com/bitbucket/projects/GPIPE/repos/jenkins-workflow/commits/0f0441bc420f92a107df4d668e736d5001732da1) - 2019-12-13

### Fixed
- [NO-ISSUES] Point to latest stable version of WFL (workflowlibs 1.22)

## [3.4.0](https://globaldevtools.bbva.com/bitbucket/projects/GPIPE/repos/jenkins-workflow/browse?at=8ebb60757bf72c0e720f6162edf6ce47cf3cf053) - 2019-10-22

### Added
- [GDAD-154] Módulos de despliegue para Orquídea
- [GDAD-279] Perfilado de usuarios en jobs de Jenkins
- [PAE-304] Despliegues con ficheros de configuración
- [PAE-289] Utilidad para verificar existencia de un plugin
- [PAE-258] Habilitar uso de credenciales en módulo Sonar
- [PAE-241] Módulo para publicar Build Info en Artifactory

### Fixed
- [GDAD-399] Fix para orden de parámetros Sonar

## [3.3.1](https://globaldevtools.bbva.com/bitbucket/projects/GPIPE/repos/jenkins-workflow/commits/a52f934e59322d23fd673059c4e9e3f7a8795b7c) - 2019-07-30

### Updated

- [NO-ISSUES] Updated workflowlibs to 1.15.0 version


## [3.3.0](https://globaldevtools.bbva.com/bitbucket/projects/GPIPE/repos/jenkins-workflow/browse?at=5072900534f0775ce9c6ae32cadbcb8beb906fa1) - 2019-07-23

### Added
- [GDAD-269] Enable parametric builds via configuration. [Look this Example](https://globaldevtools.bbva.com/bitbucket/projects/GPIPE/repos/jenkins-workflow/browse/doc/replace-variables.md?at=refs%2Fheads%2Fdevelop)
- [NO-ISSUES] Allows to use a fully configurable global config, to make available functionality immediately
  - Templated credentials always get values now
  - Simplify Artifact configuration. Added Docker registry
  - Use the Afrtifact Module as artifact properties store
  - Little make-up for Artifactory Module + test
  - Do not allow templated credentials in credentialCheck

### Fixed
- [GDAD-253] Ephemeral init was done no matter what
- [GDAD-253] Ephemeral module config was invalid
- [GDAD-306] Test library refactor

## [3.2.1](https://globaldevtools.bbva.com/bitbucket/projects/GPIPE/repos/jenkins-workflow/commits/a188ced5502cf98b690f9250e6a7cc932b4cad57) - 2019-07-31

### Fixed
- [NO-ISSUES] Bitbucket API client initialization bug

## [3.2.0](https://globaldevtools.bbva.com/bitbucket/projects/GPIPE/repos/jenkins-workflow/browse?at=6715be25c024d0767eb7f6a06d26c4b95d2bf511) - 2019-06-27

### Added
- [GDAD-180] Habilitar integración Docker
- [GDAD-209] Integración de Chimera para poder realizar análisis de seguridad de código estático.
- [GDAD-158] Integración con librería de Testing
- [GDAD-179] Integración de Cachelo para gestión de caché

### Fixed
- [GDAD-275] Cuando dos o mas jobs intentaban desplegar en dimensions a la vez, debido a que una ejecución borrase la caché de artefactos de otro
- [GDAD-277] Debido al fix anterior [GDAD-275] hay que realizar un refactor del módulo de Dimensions
- [NO-ISSUES]
  - Se ha mejorado el uso de Semantic Versioning: tanto el código de la propia Pipeline como de las aplicaciones que se construyen con ella
  - Simplificación carga inicial. Se ha simplificado la carga inicial, con la finalidad de que la fase de "Load" sea lo mas inteligente posible en el futuro

## [3.1](https://globaldevtools.bbva.com/bitbucket/projects/GPIPE/repos/jenkins-workflow/commits?until=refs%2Ftags%2F3.1&merges=include) - 2019-05-08

### Added
- Colors! Los mensajes de información, debug, warning y error salen colorizados en la consola
- Publish stage: permite elegir la herramienta de publicación (actualmente, curl y maven)
- Sonar stage: (des)activa la llamada a sonar
- Tanto los stages como los futuros steps podrán utilizar el condicional 'when_branch', que permite elegir condiciones de ejecución según rama.

## [3.0](https://globaldevtools.bbva.com/bitbucket/projects/GPIPE/repos/jenkins-workflow/commits?until=refs%2Ftags%2F3.0&merges=include) - 2019-04-24

### Updated
- Se lleva a un proyecto global

## 2.3

### Added
- Asegurar que todas las credentialsId existen en el Master donde se está ejecutando el pipeline
- Workflow 'generic' para uso general (no YAML):
  - build: mvn clean install [-P profile] [-s settings.xml]
  - test y deploy desactivados
- Mecanismo para que vtrack registre builds provocadas por el mismo commit, aunque sólo para ramas de no-despliegue
- Los yaml heredan de los parámetros de template: modules y stages en nivel 2; store en nivel 1. Por ejemplo, en el yaml template disponemos de lo siguiente:
```yaml
  modules:
    artifactory:
      enabled: yes
      server_id: globadevtools
      credentialsId: genericCredentials
  ...
```
y en el custom yaml lo siguiente:
```yaml
  modules:
    artifactory:
      credentialsId: customCredentials
  ...
```

el yaml resultante sería:
```yaml
  modules:
    artifactory:
      enabled: yes
      server_id: globadevtools
      credentialsId: customCredentials
  ...
```

### Updated
- Todas las referencias a credenciales se llaman credentialsId para una búsqueda más inmediata, salvo maven_settings_id por tratamiento interno
- Sonar se considera un stage posterior al prepare dentro del YML, al igual que lo está dentro del CI.
- Git, bitbucket, artifactory y vtrack se consideran módulos obligatorios. Esto provoca la desaparición del prepare como stage.


## [2.2](https://globaldevtools.bbva.com/bitbucket/projects/GPIPE/repos/jenkins-workflow/browse?at=6a82f001a135912ca580ab10f05be334446c049d) - 2018-12-10

### Updated
- Incorporación de ar-jupiter

### Fixed
- Arreglos en el yml de es-escenia

## 2.1
### Added
- Incorporación de los settings de orquidea (Colombia)
- Dado que cada país puede tener grupos de UUAA (escenia, orquidea), se agrega un campo "group" al Jenkinsfile para que gestione las credenciales de Bitbucket/Artifactory

### Updated
- Actualizar librería de global tools a 1.9

## 2.0
### Updated
- Refactor completo. Se intenta generalizar el caso de España en un ámbito más global

## 1.0
### Added
- Versión inicial. Se contempla únicamente el caso de spring/nacar España
