# libRunTests

This module is used as a wrapper for the [Testing team library 'runTests'](https://globaldevtools.bbva.com/bitbucket//projects/bgt/repos/test-legacy-workflowlib)

It is currently being used by:
* [Colombia Orquidea](https://globaldevtools.bbva.com/bitbucket/projects/gpipe/repos/configs_co/browse) in order to drive their ephemeral tests.

## Usage

This wrapper supports the following frameworks:
   1.- Backend Testing (mvn commands) [https://globaldevtools.bbva.com/bitbucket/projects/BGT/repos/backend-testing/browse]
   2.- Acis Framework (npm commands) [https://globaldevtools.bbva.com/bitbucket/projects/BGT/repos/e2e-js-framework/browse]
         - require: 'npm file credentials'
         - require: 'export DATAMANAGER_HEADERS="{\"Authorization\":\"<API-KEY>\"}"'

### Mandatory params

- context:
 - artifactoryNpmCredentials: 'file' type credentials with .npmrc configured with Artifactory access
 - dockerRegistryCredentials: 'user/password' type credentials used to log to Docker Registry
 - testManagerCredentials: 'secret' type credential with an identified URL for test manager
 - dataManagerCredentials: 'secret' type credential for Data Manager
 - namespace
 - application

### Optional

- libraryVersion: Defaults to Settings.libRunTest
- context:
  - saucelabsCredentials: 'userpass' type credential for Saucelabs (backend testing not use it)
  - polyfemoApiCredentials: 'userpass' type credential for endpoint polyfemo (backend testing prod use it)
  - dockerRegistry: Defaults to ${Settings.ether.artifactory.registry}*   - saucelabsCredentials: 'user/password' credentials for Saucelabs, defaults to 'bot-saucelabs'
  - gitUrl: Repo to test, defaults to the current one
  - branchName: branch of that repo to test, defaults to current one


### The 'spring' runner

The only type of test runner available currently is labeled as 'spring' type.
What we actually do is create a decorated Closure that will be sent to, and executed, by the runTests library.

We have decided to go this way, since we need to both support custom parameters from user side (configuration file) while keeping the generic enough to send any kind of runner code to the library,

The Closure is something like this, with the *ctx* being provided by the 'runTests' library:

```groovy

return { ctx ->
  sh "mvn --batch-mode -Dstyle.color=always -V -U clean verify -f acceptance -Dsaucelabs.username=${ctx.deviceHub.username} -Dsaucelabs.password=${ctx.deviceHub.password} -Dsaucelabs.tunnelid=${ctx.deviceHub.tunnelid} -Dsaucelabs.endpoint=${ctx.deviceHub.endpoint} -Dapplication.endpoint=${ctx.endpoints.orquidea}"
}
```

## Example
```yml
    - use: libRunTests
      label: 'Run Ephemeral tests'
      environment:
        DOCKER_IMAGE: "globaldevtools.bbva.com:5000/hub/orquidea-test/kqco-integrado:{{ artifact.version }}"
      with_params:
        context:
          application: orquidea
          namespace: colombia
          saucelabsCredentials: bot-saucelabs
          dockerRegistryCredentials: spring_co_orquidea_artifactory_token
          testManagerCredentials: test-manager-url
          artifactoryNpmCredentials: bot-artifactory-npm
          dataManagerCredentials: data-man-credentials
        tests:
          - type: e2e
            runner:
              type: spring
              command: 'mvn --batch-mode -Dstyle.color=always -V -U clean verify -f acceptance'
              parameters: '-Dcucumber.options="--tags @Misproductos --tags @OtrosQuick"'
              credentials: 'spring_co_orquidea_settings_file'
            results:
              pattern: acceptance/target/cucumber.json
            environment:
              shared:
                name: qa
              ephemeral:
                compose: acceptance/src/test/resources/docker-compose.yml
                endpoints:
                  orquidea: https://nuevaversion.bbvanet.com.co
```

