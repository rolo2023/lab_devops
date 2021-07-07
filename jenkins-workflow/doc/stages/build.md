# Build stage

What makes this stage unique, compared to the rest, is that it does a lot of things before and after executing your
steps:

1. Clones your repo
2. Prepares [Vtrack](../modules/vtrack.md) Build information
3. Calls [Samuel](../modules/samuel.md) pre-defined behaviours
4. If your branch is eligible, it is merged with parent prior to anything
    - This can be avoided by setting the `mergeDevelop` parameter to `False` in the Git module configuration section.
5. Execute the steps you have defined
6. Register the result in a new Build in Vtrack, but **it is not uploaded yet**
7. If the branch is a candidate for [SemVer tagging](../semver.md), according to your selected/detected branching model,
   a new tag will be created now
    - This tag is pre-calculated and available in the `{{ repo.version }}` variable
8. If set in configuration (see just below) code will be stashed/kept
    - **NOTE** This functionality has not been maintained in quite some time. If you depend on it and it is not working,
      contact us ASAP.
9. And finally, if the current branch is a bugfix or hotfix, and there exists a Pull Request opened, it will create a **
   new Pull Request** for the target branch.
    - **NOTE** This behaviour overlaps with Bitbucket's _cascading-merges_ capabilities and most probably should be
      removed

## When does the Build get sent to Vtrack?

After the Test and Publish phases, even if they are empty.

This is because we add metadata to the build with the result of these phases, as we do inside
the [Sonar module](../vars/wSonar.md).

## Build-only parameters

> DEPRECATED INFO: This documentation will be overhauled, since 3.1 is long gone and these parameters are no longer maintained

This stage still holds some parameters that are only used here:

* **stash**: Array of entries that each define what _files_ to stash (ANT-like expression) and the stash _id_
* **keep**:
    - branches: Which branches' artifacts to keep
    - files: Which files to keep
    - greedy: If true, and no files found, keep everything

## Example

This is a real example of an application that first builds a Spring application with Maven, and then packs the
resulting _'.jar'_ files inside some Docker images.

Mind the use of "_substeps_" for clarity and sharing of container usage.

```yml
stages:
  build:
    pod_def: 'pod_def_orquidea.yml'
    container: maven-builder
    steps:
      - label: 'Build and package application'
        container: maven-builder
        when: "{{ form.build_artifact }}"
        steps:
          - use: 'maven'
            label: 'Maven build'
            with_params:
              java_tool: "{{ java }}"
              maven_tool: "{{ maven }}"
              goal: 'clean install'
              maven_settings: "file: spring_{{ country }}_{{ group }}_settings_file"
              with_cachelo:
                key: 'net-orquidea-co-maven-cache'
                paths: [ "{{ env.WORKSPACE }}/.m2" ]

      - label: Build docker images
        container: kaniko
        when: "{{ form.use_docker }}"
        steps:
          - use: 'kaniko'
            label: "Build and push kqco-integrado"
            with_params:
              name: orquidea/kqco-integrado
              dockerfile: "{{ env.WORKSPACE }}/.ephemeral/ephemeral/kqco/Dockerfile"
              contextPath: "{{ env.WORKSPACE }}"

          - use: 'kaniko'
            label: "Build and push kqpu-integrado"
            with_params:
              name: orquidea/kqpu-integrado
              dockerfile: "{{ env.WORKSPACE }}/.ephemeral/ephemeral/kqpu/Dockerfile"
              contextPath: "{{ env.WORKSPACE }}"

          - use: 'kaniko'
            label: "Build and push statics-integrado"
            with_params:
              name: orquidea/statics-integrado
              dockerfile: "{{ env.WORKSPACE }}/.ephemeral/ephemeral/statics/Dockerfile"
              contextPath: "{{ env.WORKSPACE }}"

````

## Appendix - Stages

* [Build](./build.md)
* [Publish](./publish.md)
* [Test](./test.md)
* [Deploy](./deploy.md)
* [End](./end.md)
