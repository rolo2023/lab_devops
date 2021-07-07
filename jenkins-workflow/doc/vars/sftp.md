# SFTP
This module allows operations on remote files.

## Parameters

### Mandatory

* **localDir**: Local localization of artifacts
* **remoteDir**: Remote localization where artifacts will be deployment
* **remoteUser**: User server
* **remoteIp**: Ip destination server
* **remoteKey**: ID of the SSH key for connection to the destination server
* **deployableFile**: Name for unstash
* **remoteCommand**: Command line that will be executed
* **userCredential**: ID credential in Jenkins

## Example
```yml

deploy:
    environment: {}
    node_label: slave_co_ccr # Mexico node
    steps:
      # -------------------------- DES 1 82.250.88.96  --------------------#
      #------------------ de ------------------#
      # Public Resource          
      - when: "{{ form.DES }}"
        label: 'Public Resource 82.250.88.96'
        use: 'sftp'
        with_params:
          localDir: './publicresources/target/'
          remoteDir: '/home/deploy_user/pipeline_deploy'
          remoteUser: 'deploy_user' 
          remoteIp: '82.250.88.96'
          remoteKey: 'DES_01'
          deployableFile: 'publicResources-01-static.tar'
          remoteCommand: tar -xvf /home/deploy_user/pipeline_deploypublicResources-01-static.tar -C /de/kqco/online/co/web --overwrite --transform 'spublicResources-01/pub/'
          userCredential: Desarrollo_WAS_CON
      # KQPU      
      - when: "{{ form.DES }}"
        label: 'KQPU 82.250.88.96'          
        use: 'sftp'
        with_params:
          localDir: './kqpu/target/'
          remoteDir: '/home/deploy_user/pipeline_deploy'
          remoteUser: 'deploy_user' 
          remoteIp: '82.250.88.96'
          remoteKey: 'DES_01'
          deployableFile: 'kqpu_mult_web_front-01.war'
          remoteCommand: '/opt/IBM/WebSphere/AppServer/profiles/Dmgr01/bin/wsadmin.sh -user {USER_WASC_USR} -password ${USER_WASC_PSW} -f /home/deploy_user/pipeline_deployupdateKQPU.py -lang jython '
          userCredential: Desarrollo_WAS_CON
      # KQCO 
      - when: "{{ form.DES }}"
        label: 'KQCO 82.250.88.96'                    
        use: 'sftp'
        with_params:
          localDir: './kqco/target/'
          remoteDir: '/home/deploy_user/pipeline_deploy'
          remoteUser: 'deploy_user' 
          remoteIp: '82.250.88.96'
          remoteKey: 'DES_01'
          deployableFile: 'kqco_mult_web_front-01.war'
          remoteCommand: '/opt/IBM/WebSphere/AppServer/profiles/Dmgr01/bin/wsadmin.sh -user {USER_WASC_USR} -password ${USER_WASC_PSW} -f /home/deploy_user/pipeline_deployupdateKQCO.py -lang jython '
          userCredential: Desarrollo_WAS_CON
```
