# publishArtifactoryMvn module (optional)
Este módulo permite publicar los artefactos en el build info de Artifactory.

## Parameters mandatory

- **path_pom**: Ruta del pom.xml, en caso de no declararlo toma la ubicación del pom.xml en la raiz.
- **artifactory_id** Credentials ID de tipo **Username with password** en Jenkins que guarda las credenciales del repositorio de Artifactory.
- **command**: Comando maven para instalar el artefacto "clean install -"

## Parameters mandatory. One of them must be declared, but only one
- **artifactory_repo**: UUAA de repositorio de Artifactory.
- **custom_repo**: repositorio de Artifactory
	- *release*: respositorio donde se cargaran los objetos release
	- *snapshot*: respositorio donde se cargaran los objetos snapshot
	- *repository*: respositorio desde donde se descargarán las dependencias

## Example
```yml
    - label: 'Publish Artifactory'
    use: 'publishArtifactoryMvn'
    with_params:
        path_pom: '{{vars.path_pom}}'
        artifactory_id: 'artifactory_devops'
        artifactory_repo: 'devops-pe-mvn'
```

```yml
    - label: 'Publish Artifactory'
    use: 'publishArtifactoryMvn'
    with_params:
        path_pom: '{{vars.path_pom}}'
        artifactory_id: 'artifactory_devops'
        custom_repo: 
            release: 'app-release'
            snapshot: 'app-snapshot'
            repository: 'repository-app'
```

## 

## Plugins

- [Artifactory Plugin](https://wiki.jenkins.io/display/JENKINS/Artifactory+Plugin)