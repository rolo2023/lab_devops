# artifactory module (mandatory)
This module is initialized in the **prepare stage**, before vtrack, and serves to identify itself in Artifactory for the load (build stage) and download (deploy stage) of the current job.

## Parameters

- **server_id (default: globaldevtools)**: Name given to the Artifactory server through the plugin of the same name for Jenkins.
- **credentialsId (default: spring_{{country}}_{group}_artifactory_token)**: Credentials ID stored in Jenkins, **type user with password**, for authentication in Artifactory. These credentials must satisfy the repository for artifacts AND the docker registry (if needed).
- **registry (default: _globaldevtools.bbva.com:5000_)** Docker Registry to use by default 

## Example
```yml
  artifactory:
    server_id: artifactoryServer
    credentialsId: bot-arquitectura-spring
    registry: globaldevtools.bbva.com:5000
```