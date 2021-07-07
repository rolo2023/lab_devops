# jfrogCli

This module encapsulates the use of the Jfrog CLI

**Important** to be able to use this module it is necessary to use an agent with the Jfrog CLI installed, such as `globaldevtools.bbva.com:5000/piaas/cib-global/jfrog-cli:1.0.3`

The module is capable of both uploading compressed artifacts and folders with uncompressed artifacts, this is automatically detected according to configured parameters

## Parameters

| Name | Mandatory? | Description | Default value |
|------|:----------:|:------------|----------------|
| server |  No | Server Artifactory URL | https://globaldevtools.bbva.com/artifactory/ |
| credentialsId | SI | Credential id of user Artifactory | - |
| repo | SI | Artifactory repository | - |
| pathRepo | SI | Primary path where artifacts are stored | - |
| dirRepo | NO | Secondary path where artifacts are stored | ‘’ (void String)|
| fileName | NO | Name of the artifact with extension included | ‘’ (void String) |
| dirPath | SI| Path where the artifact is located | - |


## Example

Example of parameters to upload a folder with the unzipped artifact to Artifactory. The upload URL is `https://globaldevtools.bbva.com/artifactory/cib-uuaa-generic/CIR_UUAA_DE_PR/feature_any--69/`

```yml
  publish:
    container: 'jfrog-cli'
    pod_def: 'uuaa_pod_template.yaml'
    steps: 
      - label: 'Publish'
        use: 'jfrogCli'
        with_params:
          credentialsId: '7859839f-cf30-4adc-a71a-1f89ae9174ec'
          repo: "cib-uuaa-generic"
          pathRepo: "CIR_UUAA_DE_PR"
          dirRepo: "feature/any--69"
          dirPath: "{{WORKSPACE}}/releaseUpload/"
```

Example of parameters to upload a compressed artifact on a subpath to Artifactory. The upload URL is `https://globaldevtools.bbva.com/artifactory/cib-uuaa-generic/CIR_UUAA_DE_PR/artifact.zip`

```yml
  publish:
    container: 'jfrog-cli'
    pod_def: 'uuaa_pod_template.yaml'
    steps: 
      - label: 'Publish'
        use: 'jfrogCli'
        with_params:
          credentialsId: '7859839f-cf30-4adc-a71a-1f89ae9174ec'
          repo: "cib-uuaa-generic"
          pathRepo: "CIR_UUAA_DE_PR"
          fileName: "artifact.zip"
          dirPath: "{{WORKSPACE}}/releaseUpload/"
```

Example of parameters to upload a compressed artifact to Artifactory. The upload URL is `https://globaldevtools.bbva.com/artifactory/cib-uuaa-generic/CIR_UUAA_DE_PR/feature_any--69/artifact.zip`

```yml
  publish:
    container: 'jfrog-cli'
    pod_def: 'uuaa_pod_template.yaml'
    steps: 
      - label: 'Publish'
        use: 'jfrogCli'
        with_params:
          credentialsId: '7859839f-cf30-4adc-a71a-1f89ae9174ec'
          repo: "cib-uuaa-generic"
          pathRepo: "CIR_UUAA_DE_PR"
          dirRepo: "feature/any--69"
          fileName: "artifact.zip"
          dirPath: "{{WORKSPACE}}/releaseUpload/"
```