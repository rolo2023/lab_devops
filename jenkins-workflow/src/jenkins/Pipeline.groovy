package jenkins

/*
Esta clase vacía tiene 2 propósitos:

1) Poder llevar todos los archivos .groovy a una declaración más test-friendly,
sin que se pierda el ámbito GroovyScript de Jenkins (se conservan las funciones
sh, withCredentials, etc). Ejemplo:

```
// newCps() hace el equivalente a new() si se llama desde otros archivos de src/
static CpsClass newCps() = {
    X cps = new CpsClass()
    cps.init()
    return cps
}

// declaración de la clase "classic-style", con init como constructor.
class CpsClass extends Pipeline {
    void init() {}
    [...]
}
```

2) Simplifica el mock de tests gracias a la herencia de Pipeline.
*/

return this
