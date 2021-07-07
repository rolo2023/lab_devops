# How to collaborate

* Every new feature **should come** from a JIRA Story:
  * The title must be clear and self-explanatory
  * The user story must be aligned with the different Build & Deploy Guild Leaders. That’s the requirement in order to change the status to Ready
* Create a feature branch from the user story (using the UI or keeping the naming convention):

  ```sh
  # If branch should start from develop...
  git checkout develop
  git pull
  git checkout -b feature/GDAD-xxx-TITLE
  ```

* Make sure [CHANGELOG.md](./CHANGELOG.md) file is updated, instructions on how to fill it can be found there.
* **Please, for the love of all gods, _squash & rebase_ your commits**: a commit should be an atomic contribution that can be reverted at any time.
  * From experience, this is the best way to do it, in order to achieve a clean history:

    ```bash
    # If target branch is 'develop'
    git fetch origin  # update sources
    git rebase -i origin/develop  # Reduce all your small commits to one or two meaningful, revertable changesets
    git pull --rebase origin develop  # Rewind your work on top of target branch, easier the less commits there are
    # resolve any conflicts that may arise
    git push --force  # force push your branch back

    ```

* Your commit message **should** follow this easy convention:

    ```
    [JIRA-ID (if any)] Short summary of work done
    (blank)
    Long description, if needed (details welcome)
    ```

* We are using **Gitflow branching model**
  * **develop**
    * Integrates new features
  * **release** or **release/x.y**:
    * Freezes next release.
    * **release branches** branch off from **develop** one week before in order to stabilize next version
    * Only bug fixes allowed, cascaded back to develop
  * **master**
    * Only hot fixes allowed
  * Following feature branching model, _direct commits into develop are never allowed into these branches, only PRs_

* Any kind of main branch evolution (develop, master, release) will be via **Pull Request**
* Include working Unit Testing:
  * Add the test cases covering the use cases and the error situations of our task in the corresponding Test file (test/workflow/... folder).

  > You can see an example at https://globaldevtools.bbva.com/bitbucket/projects/GPIPE/repos/jenkins-workflow/pull-requests/9/overview

  * The library we use is https://github.com/jenkinsci/JenkinsPipelineUnit
  * Tests can be easily run from within IntelliJ IDEA by importing the project as a Gradle Project, or directly from the CLI using the provided gradle wrapper:

      ```sh
      ./gradlew test -i  # -i flag outputs very usable information in case of error
      ```


## Code structure

* _vars/globalPipeline.groovy_ is our main entrypoint
* _globals/*.groovy_: Static classes used indiscriminately everywhere. They implement pipeline configuration, global helpers, and give access to modules from the Stages. **Helpers.groovy** is especially useful since it allows the rest of the classes to access Jenkins functionalities, and on testing it allows to mock the requited methods.
* _ci/stages/*.groovy_: Each executed stage using the activated modules.
* _modules/*.groovy_: Core functionality, such as Vtrack, Samuel,bitbucket API...
* _vars/*.groovy_: generally available modules and functionality, this is where we accept collaboration!


### Develop a new pluggable functionality

Modules are the set of tools applied in each of the different stages. They are optional and repeatable, usable in one or more stages, one or more times

### Quickstart your new module

`./utils/generator/create_var.sh --name newModuleName`

Calling this script will create the necessary files for your new functionality to get started.

After that script runs, you will have:
* a new file under `vars/newModuleName.groovy`
* a test file for you to fill up in `tests/workflow/vars/newModuleNameTest.groovy`,
* and a sample documentation entry in `docs/vars/newModuleName.md`

## List of available modules (OUTDATED)

| Module | Practice | Short description
| ----------|---------|-----
[libRunTests](./vars/libRunTests.md) | utility | Wrapper for the 'runTests' ephemeral testing library
[dimensions](./vars/dimensions.md)| Deploy | Deploy to dimensions
[sftp](./vars/sftp.md)| Deploy | Deploy to a WAS server
[maven](./vars/maven.md)| Build & Deploy | Abstraction on Maven goals
[curl](./vars/curl.md)| Utility | Curl wrapper, currently used to get/put data into Artifactory
[wDocker](./vars/wDocker.md)| Utility | Docker wrapper, currently only supports 'build', 'run, 'push'
[wSonar](./vars/wSonar.md)| Utility | Sonar wrapper, allows custom sonarqube runs in your stages
[chimera](./vars/chimera.md)| Utility | Chimera wrapper, allowing codeReview to be run in your stages.

## Module's configuration

Modules get tneir configuration with an Ansible-like syntax:

```yml
stages:
  deploy:
    environment: {}
    node_label: 'maven-jdk8'
    steps:
      - when_branch: ['develop']
        use: {{module_A_id}}
        with_params:
          {{param_1}}: {{param_1_value}}
          {{param_2}}: {{param_2_value}}
          {{param_3}}: {{param_3_value}}
      - when_branch: ['feature/*', 'PR-*']
        use: 'chimera'
        environment:
          credentials: 'chimera-credentials'
        with_params:
          {{param_1}}: {{param_1_value}}
          {{param_2}}: {{param_2_value}}
          {{param_3}}: {{param_3_value}}
```
