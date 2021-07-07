# deployJboss module (optional)
Este módulo permite instalar aplicaciones en servidores jBoss

## Parameters

- **node**: Nombre del nodo agente en Jenkins que se conectará a los servidores de cada pais.
- **server_ip**: IP del servidor jBoss.
- **ssh_pk_id** Credentials ID de tipo **SSH Username with private key** en Jenkins que guarda la llave privada del servidor jBoss.
- **ssh_user** Usuario ssh del servidor jBoss.
- **server_id** Credentials ID de tipo **Username with password** en Jenkins que guarda las credenciales del servidor jBoss (previamente se debe de configurar el usuario para que tenga permisos de desistalación e instalación de aplicaciones).
- **warPath** Ruta del artefacto (WAR o EAR) que se instalará.
- **server_group** Server group de jBoss en donde se instalará la aplicación.
- **contex_root** Context root de la aplicación.
- **server_host** Opcionalmente si se tiene configurada una NAT para acceder al servidor especificamos en esta variale la ip real del servidor jBoss y en el server_ip ponemos la ip de la NAT.
- **usersApproval** Lista de personas que pueden autorizar el despliegue. 

## Example
```yml
  use: 'deployJboss'
    with_params:
      node: 'jenkins_slave_mexico'
      server_ip: '118.180.35.209'
      ssh_pk_id: 'ssh_pk_des_209'
      ssh_user: 'devops'
      server_id: 'test_jboss'
      warPath: 'target/saexWeb.war'
      server_group: 'sg-appweb01'
      contex_root: 'saexWeb'
```