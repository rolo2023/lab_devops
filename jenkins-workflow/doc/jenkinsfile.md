# Jenkinsfile syntax

The easiest, most basic `Jenkisfile` file for your STD-pipeline-powered application would be:

```groovy
#!groovy
@Library ('std-pipeline@tags/4.2') _

globalPipeline {
    country = 'global'
    group = 'simple'
}
``` 

These settings would pull a configuration file located in 

 https://globaldevtools.bbva.com/bitbucket/projects/GPIPE/configs_${country}/{{group}}.yml
* **country**: 2-digit country id
* **group**: In that repository you have to store a yaml file with the name of your "group"

In our example, we would look for the following file to govern our CI flow:

    https://globaldevtools.bbva.com/bitbucket/projects/GPIPE/repos/configs_global/browse/simple.yml
 
## Optional parameters

The following parameters are optional, and may have defined default values when missing.

|Name  | Description | Default value | Notes |
|:------:|:----------|:------------:|--------|
| uuaa | Identifies UUAA, if needed | _'NO_UUAA')_   | Currently used by the [Samuel module](./modules/samuel.md) |
| revision | Branch of the remote config to pull | HEAD (master) | Can point to Commits, tags or branches|
| architecture | Defines the Ether application architecture value | _generic_ |  Must be an Ether compatible architecture: spring.r1, nacar.r1, ...<br />Currently used by the [Vtrack Module](./modules/vtrack.md) |
| verbosity | Defines log level of your application | 2 (INFO) | Possible values:<br />0 (ERROR), 1 (WARNING), 2 (INFO), 3 (DEBUG) |
| agentTag | Set tag for build/deploy Pod Templates | [see own section](./ci/agent-tag.md) | (**Kubernetes-only**)  |
| vars | Custom variables for your application | {} | Mostly used in [variable substitution](./replace-variables.md) |
