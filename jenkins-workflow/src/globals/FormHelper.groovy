package globals

static Form newCps() { return new Form() }

class Form implements Serializable {
    List currentForm = []
    Map hiddenParams = [:]

    def fromYaml(List yaml) {
        yaml.each { Map config ->
            if(!checkMandatories(config)) {
                Helpers.error "FormHelper :: Unable to parse all parameters"
                return
            }
            if (Helpers.canDoWhen(config)) {
                addParamsFromYaml(config)
            } else {
                addHiddenParameter(config)
            }
        }

        Helpers.log.debug("""
        FormHelper :: fromYaml
        ${yaml}) ->

            currentForm = ${this.currentForm}

            hiddenParams = ${this.hiddenParams}
        """)
    }

    private void addString(String id, String defaultValue, String description='') {
        defaultValue = Helpers.subst(defaultValue)
        this.currentForm << Helpers.jenkins.string(
                name: id,
                defaultValue: defaultValue,
                description: description)
    }

    private void addChoice(String id, List choices, String defaultValue, String description='') {
        defaultValue = Helpers.subst(defaultValue)
        this.currentForm << Helpers.jenkins.choice(
                name: id,
                choices: choiceGen(choices, defaultValue),
                description: description
        )
    }

    private void addBoolean(id, defaultValue, description='') {
        this.currentForm << Helpers.jenkins.booleanParam(
                name: id,
                defaultValue: defaultValue as Boolean,
                description: description
        )
    }

    private def addParamsFromYaml(Map config) {
        switch (config.type) {
            case ["string"]:
                addString(config.id, config.default, config.description); break
            case ["choice", "selection"]:
                addChoice(config.id, config.choices, config.default, config.description); break
            case ["boolean", "booleanParam"]:
                addBoolean(config.id, config.default, config.description); break
        }

    }

    private Boolean checkMandatories(Map config) {
        if (!config.id || !config.type) {
            Helpers.log.error "FormHelper :: 'id' and 'type' are mandatory for a parametric form entry: ${config}"
            return false
        } else if (config.type in ["choice", "selection"]) {
            if (!config.choices) {
                Helpers.log.error "FormHelper :: 'choices' array is mandatory for a dropdown parameter: ${config}"
                return false
            }
        }

        // Common mistype
        if (config.defaults || config.defaultValue) {
            Helpers.log.warn "FormHelper :: in order to define a default value, use the 'default' key: ${config} (this is not an error, yet)"
            config.default = config.defaultValue ?: config.defaults ?: null
        }

        // Conditionals must have an alternative default value
        def keys = config.keySet()
        if ('when' in keys || 'when_branch' in keys) {
            if (!('default_when_false' in keys)) {
                Helpers.log.error "FormHelper :: 'default_when_false' missing on conditional form entry"
                return false
            }
        }

        return true
    }

    private String choiceGen(List choices, String defaultValue = '', Boolean sorted = true) {
        if (sorted) {
            choices.sort()
        }
        def first = ''
        if (defaultValue) {
            first = "${defaultValue}\n"
            choices = choices.findAll { it != defaultValue }
        }
        return "${first}${choices.join('\n')}"
    }

    /**
     * Hidden parameters are those which conditions are not matched.
     * They need to be stored, because substitutions might rely on these values
     *
     * @param config
     */
    private void addHiddenParameter(Map config) {
        hiddenParams[config.id] = Helpers.subst(config.default_when_false as String)
    }
}
