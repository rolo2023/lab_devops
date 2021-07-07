# Defining Custom Containers in Custom Pods

At the Kubernetes Jenkins pipelines, in order to give the flexibility and the autonomy to allow each team to define their custom execution containers, a functionality which consists in the ability to define a custom Pod template has been added.

## What is that functionality based on?

Software engineers can define their own containers, within a pod definition file, where the stages of their pipelines can be executed.

## Where can I define pod templates?

Pod templates can be defined at `build` stage level (Build Template) and `deploy` stage level (deployment templates).

They could be the same, and in that case **they would share the workspace**.

## Custom Pod definition file

The custom Pod definition is a yml file that should be placed in the same repository as the country related configuration files:

  * https://globaldevtools.bbva.com/bitbucket/projects/GPIPE/repos/configs_${country}/${custom_pod_def_filename}.yml

The syntax of this pod definitions file should be something like this:

```yml
  ---
  apiVersion: "v1"
  kind: "Pod"
  metadata:
    name: "${your_pod_name}"
  spec:
    securityContext:
      runAsUser: 1000
      fsGroup: 1000
    imagePullSecrets:
      - name: "${your_pull_secret}"
    containers:
      - name: "${your_container_name_1}"
        image: "${your_image_url_1}:${version}"
        tty: true
        resources:
          limits:
            memory: ${your_memory_limit}
            cpu: ${your_cpu_limit}
          requests:
            memory: ${your_memory_request}
            cpu: ${your_cpu_request}
        command:
          - cat
      - name: "${your_container_name_2}"
        image: "${your_image_url_2}:${version}"
        tty: true
        resources:
          limits:
            memory: ${your_memory_limit}
            cpu: ${your_cpu_limit}
          requests:
            memory: ${your_memory_request}
            cpu: ${your_cpu_request}
        command:
          - cat
      ...
```

Please, for more specific information, visit the official Kubernetes documentation site:

  * https://kubernetes.io/docs/concepts/workloads/pods/pod-overview/

## Using your defined Custom Container

Since the pod definitions file is in the country configuration repository, the use of the custom containers is ready.

To configure the execution container add the following syntax within the desired stage at the pipeline configuration file:

```yml
  stages:
    build:
      container: '${your_container_name_1}'
      pod_def: '${${custom_pod_def_filename}.yml}'
      steps:
        - use: 'maven'
        ...
    deploy:
      container: '${your_container_name_2}'
      pod_def: '${${custom_pod_def_filename}.yml}'
      steps:
        - use: 'maven'
        ...
```


## Examples

  * Pod Definition File:
    **pod_def-build.yml**
    ```yml
      ---
      apiVersion: "v1"
      kind: "Pod"
      metadata:
        name: "devops_global"
      spec:
        securityContext:
          runAsUser: 1000
          fsGroup: 1000
        imagePullSecrets:
          - name: "${pull_secret}"
        containers:
          - name: "devops-global-generic"
            image: "globaldevtools.bbva.com:5000/piaas/gpipe/devops_global:latest"
            tty: true
            resources:
              limits:
                memory: 2Gi
                cpu: 2
              requests:
                memory: 1Gi
                cpu: 1
            command:
              - cat
          - name: "maven-agent"
            image: "globaldevtools.bbva.com:5000/piaas/gpipe/devops_global_maven:latest"
            tty: true
            resources:
              limits:
                memory: 2Gi
                cpu: 2
              requests:
                memory: 1Gi
                cpu: 1
            command:
              - cat
    ```
  
  * Pipeline Configuration File:
    **simple_pipeline.yml**
    ```yml
      ---
      verbosity: 1
      modules:
        bitbucket:
          credentialsId: "${bitbucket_credential}"

        artifactory:
          credentialsId: "${artifactory_credential}"

        vtrack:
          namespace:  'enax_console_prod'
          ether_certificate: 'vtrack_enax_cert'
          ether_key: 'vtrack_enax_key'

      stages:
        build:
          container: 'maven-agent-specific'
          pod_def: 'pod_def-build.yml'
          steps:
            - use: 'maven'
              with_params:
                maven_settings: 'file: settings_spring'
                goal: 'clean install versions:set -DnewVersion={{ artifact.version }}'
                with_cachelo:
                  key: '{{ uuaa }}-maven-cache'
                  paths: ["{{ env.WORKSPACE }}/.m2"]

            - use: 'wSonar'
              with_params:
                parameters: '-Dsonar.sources=src'

            - use: 'maven'
              when_branch: ['develop', 'master']
              with_params:
                maven_settings: 'file: settings_spring'
                goal: 'deploy'

        publish:
          steps: []
        deploy:
          steps: [] 
    ```
