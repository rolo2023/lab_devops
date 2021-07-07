@Library ('jenkins-workflow@master') _

spring {
    verbosity = 1 // Opcional. Puede ir de 0 (sólo errores/warnings) a 2 (debug)
    country = 'global'
    group = 'generic' // Escenia, netcash, etc
    uuaa = 'bnet' // nombre de la uuaa
    build = [
        node_label: 'node-agent-label', // nodo donde se realiza la construcción
        artifact_path: '.', // ubicación del artefacto a desplegar (relpath desde WORKSPACE)
        artifact_file: 'uuaa.ear', // nombre del artefacto. Acepta la variable {{version}} si el artefacto cambia de nombre según compilación
        maven_settings: 'env:ARTIFACTORY_CREDENTIALS', // env:id si el nodo dispone de un settings.xml al que se pasa una variable de credenciales, file:id si se provee de un settings.xml a través de Jenkins
        maven_args: '-P mavenProfile' // argumentos que se pasan a maven. Puede quedarse vacío
    ]
}
