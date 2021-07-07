# Standard Pipeline

This **_shared library_** covers **all life cycle stages** for any architecture component:

* Building (with maven, docker, shell scripts)
* Testing & Quality (Sonar)
* Publishing (Artifactory)
* Deployment (Dimensions, WAS, jBoss)

It also auto-includes features such as:
* Git Flow
* Enforcement (Samuel)
* Automatic Semantic Versioning (optional)
* Tracking (vTrack)

## What does 'standard' stand for?

Since it covers all the **recommended** stages in the Life Cycle of an application, any application that is built with
will meet the recommended standards

## What is the latest stable release?

> [Check out tag 4.1 in Bitbucket][latest_version_url]

## Quickstart

In order to start using this, you need to adjust your [Jenkinsfile](doc/jenkinsfile.md) and create a [specific configuration file](doc/setting-up-group-file.md).

### Example Jenkinsfile

```groovy
#!groovy
@Library ('std-pipeline@4.0') _

globalPipeline {
    country = 'global'
    group = 'simple'
    architecture = 'spring.r1'  
    verbosity = 1               
    revision = 'develop'
    uuaa = 'bnet'       
}
```

Check out more information on what can be used here and what not in the [specific section for Jenkinsfile](./doc/jenkinsfile.md).

### Local configuration file

```yml
---
verbosity: 1
branching_model: release

modules:
  bitbucket:
    url: https://globaldevtools.bbva.com/bitbucket
    credentialsId: "spring_es_escenia_bitbucket_token"

  artifactory:
    server_id: 'Artifactory globaldevtools'
    credentialsId: "spring_es_escenia_artifactory_token"

# simplest possible pipeline: do nothing
stages:
  build:
    steps: []

  test:
    steps: []

  publish:
    steps: []

  deploy:
    steps: []
```

In the [setting up the group's configuration file](./doc/setting-up-group-file.md) you have all the information you need in order to create a much more detailed example.


## Support
If you have any request (doubts, bot users, a new master, resources, ...) let us know via [**Service Desk**][sd_link].

## Index
* [Jenkins requirements](./doc/jenkins-requirements.md)
* [Jenkinsfile](./doc/jenkinsfile.md)
* [Group files](./doc/setting-up-group-file.md)
* [CHANGELOG](CHANGELOG.md) 
* [How to collaborate](COLLABORATE.md)
* **Advanced configuration** 
  * [Variable replacement](./doc/replace-variables.md)
  * [Semantic Versioning](./doc/semver.md)
  * [Worklflow Steps](./doc/ci/workflow-step.md)
  * [Custom Pod Definition](./doc/ci/custom-pods.md)
* **Modules**
  * [bitbucket](./doc/modules/bitbucket.md)
  * [artifactory](./doc/modules/artifactory.md)
  * [Samuel](./doc/modules/samuel.md)
  * [vtrack](./doc/modules/vtrack.md)
* **Stages**
  * [build](./doc/stages/build.md)
  * [test](./doc/stages/test.md)
  * [deploy](./doc/stages/deploy.md)
  * [end](./doc/stages/end.md) 
* **vars**
  * [chimera](./doc/vars/chimera.md)
  * [curl](./doc/vars/curl.md)
  * [dimensions](./doc/vars/dimensions.md)
  * [libRunTests](./doc/vars/libRunTests.md)
  * [maven](./doc/vars/maven.md)
  * [sftp](./doc/vars/sftp.md)
  * [wDocker](./doc/vars/wDocker.md)
  * [wSonar](./doc/vars/wSonar.md)
  * [deployOnServer](./doc/vars/deployOnServer.md)
  * [deployJboss](./doc/vars/deployJboss.md)
  * [publishArtifactoryMvn](./doc/vars/publishArtifactoryMvn.md)


[sd_link]: https://globaldevtools.bbva.com/jira/servicedesk/customer/portal/26/create/333?q=pipeline
[latest_version_url]: https://globaldevtools.bbva.com/bitbucket/projects/GPIPE/repos/jenkins-workflow/browse?at=refs%2Ftags%2F4.1
