import binascii

clusterName = sys.argv[0]
appName = sys.argv[1]
earUrl = sys.argv[2]
script = binascii.a2b_base64(sys.argv[3])[:-1]

if __name__ == "__main__":
    if appName in AdminApp.list().splitlines():
        print ("Desinstalando aplicación" + appName)
        AdminApp.uninstall(appName)
        print ("Guardando cambios")
        AdminConfig.save()
        print ("Sincronizando nodos")
        AdminNodeManagement.syncActiveNodes()
    print ("Instalando aplicación")
    AdminApp.install(earUrl, script)
    print ("Guardando cambios")
    AdminConfig.save()
    print ("Sincronizando nodos")
    AdminNodeManagement.syncActiveNodes()
    print ("Iniciando aplicación" + appName)
    AdminApplication.startApplicationOnCluster(appName, clusterName)
    print ("Finalizó despliegue")