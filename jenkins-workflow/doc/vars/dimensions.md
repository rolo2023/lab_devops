# dimensions
This module allows to deploy artifacts into Dimensions.

## Parameters

### Mandatory

> Warning: 'uuaa' and 'circuit' are not read from the module configuration, they come from each app's Jenkinsfile
* **uuaa**
* **circuit**

* **configuration file (default: "${country}_${uuaa}_dimensions_config")**: Json file stored in the Jenkins Master within the credentials as secret file.

## Example Configuration File
```
{"country":"","pass_type":"","petition":null,"risk":"","database":"bbva_ccr@bdibp002","host":"150.50.102.145:671","date":1592257110,"pimps":[{"title":"Automatic PIMP from pipeline execution on Jenkins Job","uuaa":"UUAA","circuit":"Dimensions_PROJECT"}]}
```

* **username and password (default: ${country}_dimensions")**: Dimensions Project credentials stored in the Jenkins Master within the credentials.
* **levels (default: [ 'NONE', 'DEVELOPMENT', 'INTEGRATED', 'QA', 'AUS', 'PRODUCTION' ])**: List of environments.

### Optionals

* **version (default: 1.1)**: Dimensions version to be used.
* **http_proxy (optional)**: if present, it will
* **verbosity (default: the global value)**: Console output level.
* **risk (default: BELOW)**: risk of implantation.
* **country**: Some countries deploy in other geographies and require to specify which country the node is in.
* **petition (default: REMOTE BANKING)**: Type of request. Review the official Serena Dimensions documentation for the complete list of request types.
* **database**: Dimensions-database's name. Each country, if not each group of applications, has its own.
* **host**: Host where the database is located.
* **pass_type**: can be EVOLUTIVE, CORRECTIVE BLOCKING or CORRECTIVE NOT BLOCKING.
  * EVOLUTIVE for CI/CD
  * CORRECTIVES for manual deployment
* **title**

## Example
```yml

  deploy:
    node_label: ldbad103
    steps:
      - use: 'dimensions'
        when_branch: [ 'develop', 'master' ]
        with_params:
          http_proxy: 'PROXYVIP.IGRUPOBBVA:8080'
          version: '1.1'
          credentials_file: /us/xpjenk1/.config/dimensions.json
          verbosity: 2
          levels: [ 'NINGUNO', 'DESARROLLO', 'INTEGRADO', 'QA', 'AUS', 'PRODUCCION' ]
          risk: 'BAJO'
          country: ESPAÑA
          petition: BANCA A DISTANCIA
          database: bgestimp@bdime001
          host: spdim001:672
          pass_type: 'EVOLUTIVO'
          title: "{{uuaa}}-{{version}} - Automatic PIMP from {{repo}} ({{branch}})"
```
