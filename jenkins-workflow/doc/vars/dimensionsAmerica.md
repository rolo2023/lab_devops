# dimensionsAmerica
This module allows to deploy artifacts into Dimensions.

## Parameters

### Mandatory

> Warning: 'uuaa' and 'circuit' are not read from the module configuration, they come from each app's Jenkinsfile
* **uuaa**
* **circuit**

* **configuration file (default: "${country}_${uuaa}_dimensions_config")**: Json file stored in the Jenkins Master node of country within the credentials as secret file.

## Example Configuration File
```
{"country":"","pass_type":"","petition":null,"risk":"","database":"bbva_ccr@bdibp002","host":"150.50.102.145:671","date":1592257110,"pimps":[{"title":"Automatic PIMP from pipeline execution on Jenkins Job","uuaa":"UUAA","circuit":"Dimensions_PROJECT"}]}
```

* **username and password (default: ${country}_dimensions")**: Dimensions Project credentials stored in the Jenkins Master within the credentials.
* **levels (default: [ 'NONE', 'DEVELOPMENT', 'INTEGRATED', 'QA', 'AUS', 'PRODUCTION' ])**: List of environments for Dimensions.

### Optionals

* **version (default: 1.1)**: Dimensions version to be used.
* **http_proxy (optional)**: if present, it will
* **verbosity (default: the global pipeline value)**: Console output level.
* **risk (default: BELOW)**: risk of implantation.
* **country**: Some countries deploy in other geographies and require to specify which country the node is in.
* **petition (default: REMOTE BANKING)**: Type of request. Review the official Serena Dimensions documentation for the complete list of request types.
* **database**: Dimensions-database's name. Each country, if not each group of applications, has its own.
* **host**: Host where the database is located.
* **pass_type**: can be EVOLUTIVE, CORRECTIVE BLOCKING or CORRECTIVE NOT BLOCKING.
  * EVOLUTIVE for CI/CD
  * CORRECTIVES for manual deployment
* **title**
* **when_branch (array of strigs)**: The module is executed if currect branch exists in the given array

## Example
```yml

  deploy:
    node_label: ldbad103
    steps:
      - use: 'dimensionsAmerica'
        when_branch: [ 'develop', 'master' ]
        with_params:
          http_proxy: 'PROXYVIP.IGRUPOBBVA:8080'
          version: '1.1'
          verbosity: 2
          levels: [ 'NINGUNO', 'DESARROLLO', 'INTEGRADO', 'QA', 'AUS', 'PRODUCCION' ]
```
