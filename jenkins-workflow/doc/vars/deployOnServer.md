# deployWasJboss module (optional)
Este módulo permite instalar aplicaciones en servidores Websphere Application Server o en servidores Jboss EAP

## Parameters

- **node**: Nombre del nodo agente en Jenkins que se conectará a los servidores de cada pais.
- **identityApp**: Identificador de la aplicacion, generalmente es el nombre del repositorio {{repo.slug}}. *OPTIONAL (default: repo.slug)
- **environmentToDeploy** Ambiente a instalar cuyos valores pueden ser: TEST o QA. *OPTIONAL (default: TEST)
- **remoteGit** Repositorio - vía ssh - donde se alojan los archivos de configuraciones. Estos archivos siguen la sgte. nomenclatura 'confAppsTEST.yml' && 'confServersTEST.yml'. Se forma según el parámetro **environmentToDeploy**. *OPTIONAL (default: repositorio configs_{{country}})
- **subjectEmail** Si el archivo confAppsTEST.yml contiene un grupo de "aprobadores". Se envía por correo el input para que aprueben el despliegue. *OPTIONAL (default: Despliegue Automatico Devops)
- **messageEmail** Mensaje que se enviará por email. *OPTIONAL (default: *revisar las fuentes*)
- **waitMinutes** Tiempo de espera en minutos que el job realizará para esperar la aprobación del despliegue. *OPTIONAL (default: 10 minutos)
- **urlArtifacts** URL de artifactory donde se alojan el/los archivo(s) que se desplegarán. *OPTIONAL (si no se usa, la lógica es hacer unstash)
- **deployablefile** Nombre del archivo que se hizo STASH durante el CI. Si se usa *urlArtifacts*, sería el nombre del archivo donde se guardará la descarga
  
- **ssh_pk_id, server_id, etc** ver archivo confAppsTEST.yml y confServersTEST.yml para ver los parámetros que se necesitan para el despliegue. Generalmente son credenciales que deben existir en Jenkins

## Example
```yml
  - label: 'Deploy Test'
        when_branch: [ 'develop', 'devops' ]
        use: 'deployWasJboss'
        with_params:
          node: "jenkins_slave_mexico"
          environmentDeploy: TEST
          urlArtifacts: "{{ artifact.urlUpload }}/{{ artifact.version }}/{{artifact}}"
          deployablefile: artifactsToDeployment.zip 
          messageEmail: | 
            <h3>Despliegue automático a ambiente previo Test</h3>
            <p>Un despliegue requiere su aprobación</p>
            <p>Para aprobar el despliegue hacer clic&nbsp; <a href="{{ env.BUILD_URL }}input/"> aquí</a>.</p>
```

## Example confAppsTEST.yml
```yml
  applications:
 -  applicationRepo: bnet-pri-zona-privada #value of repo.slug
    authTeam: william.marchan@bbva.com
    components:
    - name: bdntux-ear
      hostname: 118.180.34.111
      artifact: bdntux_pe_web.ear
      clusterName: cluster85des3
      scriptDeploy: "[ -nopreCompileJSPs -installed.ear.destination /pr/bdntux/online/pe/web/J2EE/ -distributeApp -nouseMetaDataFromBinary -nodeployejb -appname bdntux-ear -createMBeansForResources -noreloadEnabled -nodeployws -validateinstall warn -processEmbeddedConfig -filepermission .*\\.dll=755#.*\\.so=755#.*\\.a=755#.*\\.sl=755 -noallowDispatchRemoteInclude -noallowServiceRemoteInclude -asyncRequestDispatchType DISABLED -nouseAutoLink -noenableClientModule -clientMode isolated -novalidateSchema -MapModulesToServers [[ bdntux_pe_web.war bdntux_pe_web.war,WEB-INF/web.xml WebSphere:cell=TLAPLVWAS85DMCell01,cluster=cluster85des3+WebSphere:cell=TLAPLVWAS85DMCell01,node=TLAPLVWAS8501Node,server=webserver1+WebSphere:cell=TLAPLVWAS85DMCell01,node=TLAPLVWAS8502Node,server=webserver1 ]] -MapWebModToVH [[ bdntux_pe_web.war bdntux_pe_web.war,WEB-INF/web.xml default_host ]]]"
```

## Example confServersTEST.yml
```yml
servers:
 - hostname: 118.180.34.111
   ip: 118.180.34.111
   ssh_pk_id: pe-was-test
   ssh_user: test
   server_id: server_des_111
   server: was

 - hostname: 118.180.34.213
   ip: 118.180.34.213
   ssh_pk_id: ssh_pk_des_213
   ssh_user: devops
   server_id: was_qa_devops
   server: was
```