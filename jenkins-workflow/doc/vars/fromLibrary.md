# fromLibrary

This module is used for loading libraries external to the Global Pipeline.

## Mandatory parameters

The first time the module is used, it is mandatory to load the library. Then you must place the command to execute.

```yml
  - label: 'Execute Create Package'
    use: 'fromLibrary'
    with_params:
      library:
        identify: '{{Name Library}}@{{TAG || BRANCH}}'
        remote: '{{REPO_URL}}'
        credentialsId: '{{CREDENTIAL_ID}}'
      command: '{{SL_NAME}}.{{FUNCTION_NAME}}({{PARAMS}})'
```

If you have to call another method from the library, just place the command to execute

```yml
  - label: 'Execute Create Release Structure'
    use: 'fromLibrary'
    with_params:
      command: '{{SL_NAME}}.{{FUNCTION_NAME}}({{PARAMS}})'
```

The parameters required within the Jenkinsfile of the project are the following:

 - uuaa
 - vars
    - uuaa_shared_library_version

```java
  globalPipeline { 
    uuaa = 'esia' 
    vars = [
      uuaa_shared_library_version: 'globalpipeline'
    ]
  }
```
## Example

### Simple Example:

```yml
  steps:
    - label: 'Execute Create Package'
      use: 'fromLibrary'
      with_params:
        library:
          identify: "{{ uuaa }}_shared_library@{{ vars.uuaa_shared_library_version }}"
          remote: 'ssh://git@globaldevtools.bbva.com:7999/cibesia/{{ uuaa }}_shared_library.git'
          credentialsId: '6e42f93c-0c96-47ab-b86c-83fc57d57812'
        command: 'dynamic.createPackage(x.steps, "{{ env.WORKSPACE }}", "{{ repo.slug }}", "{{ repo.branch }}")'
```

### Example with more than one command:

```yml
  steps:
    - label: 'Execute Create Package'
      use: 'fromLibrary'
      with_params:
        library:
          identify: "{{ uuaa }}_shared_library@{{ vars.uuaa_shared_library_version }}"
          remote: 'ssh://git@globaldevtools.bbva.com:7999/cibesia/{{ uuaa }}_shared_library.git'
          credentialsId: '6e42f93c-0c96-47ab-b86c-83fc57d57812'
        command: 'dynamic.createPackage(x.steps, "{{ env.WORKSPACE }}", "{{ repo.slug }}", "{{ repo.branch }}")'

    - label: 'Execute Create Release Structure'
      use: 'fromLibrary'
      with_params:
        command: 'dynamic.createReleaseStructure(x.steps, "{{ env.WORKSPACE }}", "{{ uuaa }}", "{{ repo.slug }}")'
```