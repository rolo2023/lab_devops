#!groovy
@Library ('jenkins-workflow@x.y') _


spring { // Para aplicaciones nacar, usar el closure homónimo
    
    /* Organizacion */
    /*MUST*/ country = 'es'
    /*MUST*/ group = 'generic'  // escenia, netcash, orquidea, etc
    /*OPT*/  revision = '1.0.0' // Versión de la configuración (HEAD por defecto)

    /* Applicacion */
    //! IMPORTANTE ! : maven/java no son versiones sino los IDs establecidos en Jenkins Tool Configuration
    /*MUST*/ maven = 'MVN3.0.4'
    /*MUST*/ java = 'JDK7'
    /*MUST*/ uuaa = 'TST1'

    /* Despliegue */
    /*OPT*/ circuit = 'D_051_XYA'

    /* OPT: variables usadas dentro del custom yml. NO SE PUEDE USAR MAPAS ANIDADOS */
    vars = [
        'foo' : 'bar'
    ]
}