# kaniko

Abstraction for docker build and docker push inside a Jenkins Kubernetes master

> It only supports using Dockerfile's directory as context

For it to work, a specific container has to be added to your [Kubernetes Pod Template]():

```yml
  containers:
    - name: kaniko
      image: gcr.io/kaniko-project/executor:debug-v0.16.0
      imagePullPolicy: Always
      securityContext:
        runAsUser: 0
      command:
        - /busybox/cat
      tty: true
      volumeMounts:
        - name: jenkins-docker-cfg
          mountPath: /kaniko/.docker
```

And a new volume as well:

```yml
  volumes:
    - name: jenkins-docker-cfg
      secret:
        secretName: registrypullsecret
        items:
          - key: .dockerconfigjson
            path: config.json
```

Check [official PIAAS docs](http://live-jenkinsenterprise-docs.s3-website-eu-west-1.amazonaws.com/index.html) for more info.

## Mandatory parameters

 - name: Name of the image to be created. It **SHOULD** be a 'project/image' string, such as _'gcp.io/kubernetes'_

## Optional paramaters

- dockerfile: path to dockerfile, either absolute or relative
- version: version of the image, if not set will default to the computed one for the artifact
- registry: registry path for the image (final path will be registry/name:version)
- container: name of the container with the Kaliko image, defined alongside your application pod
- contextPath: if set, the Docker context will be set to it

## Example

1. Build and push using default container 'kaniko'

    ```yml
      use: 'kaniko'
      with_params:
        name: 'orquidea/kqco'
    ```

2. Build and push using a custom container name and custom Dockerfile

    ```yml
      use: 'kaniko'
      with_params:
        name: 'orquidea'
        container: 'my-kaniko-container'
        dockerfile: '.ephemerals/test/Dockerfile'
    ```

3. Build and push using a custom container name and custom Dockerfile + Context

    ```yml
      use: 'kaniko'
      with_params:
        name: 'orquidea'
        container: 'my-kaniko-container'
        dockerfile: '.ephemerals/test/Dockerfile'
        contextPath: '.ephemerals/tests'
    ```