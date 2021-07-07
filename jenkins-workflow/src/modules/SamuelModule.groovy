package modules

import globals.Helpers
import globals.Settings
import jenkins.Pipeline

static SamuelCps newCps() { new SamuelCps() }

/**
 * Wrapper for executing Samuel stage-related behaviours for the project
 *
 * @see "docs/modules/samuel.md"
 */
class SamuelCps extends Pipeline {

    Map samuelSettings = [:]

    static SUPPORTED_TYPES = ['spring', 'nacar', 'generic']
    static SUPPORTED_COUNTRIES = ['AR','CIB','CO','ES','GLOBAL','MX','PE','PY','TR','US','UY','VE']

    void init() {

        this.samuelSettings = Helpers.substituteTree(Settings.modules.samuel as Map)

        if (!Helpers.pluginExists('samuel-jenkins-plugin')) {
            Helpers.error """
                Samuel Plugin is not installed in this Jenkins instance.
                Please download, install and configure it: https://globaldevtools.bbva.com/samuel/web/docs/tasks/samuel-plugins/jenkins-plugin.html
            """
            return
        }

        if (this.samuelSettings) {
            Helpers.log.warning "Samuel :: init :: Samuel settings are deprecated, only owners will be used"
        }

        // Samuel plugin manages entity existance based on repository, no need to send in any parameters
        if (samuelEntityExists()) {
            Helpers.log.info "Samuel :: init :: entity exists!"
            return
        }else{
            Helpers.log.info "Samuel :: init :: entity does not exist for this repository"
            createSamuelEntity(entityMap: createDefaultEntityMap(this.samuelSettings?.owners))

        }


    }

    /**
     * Instruct Samuel to execute whatever it has defined for a given stage
     *
     * @param stage name defined in the Samuel Console
     */
    void executeSamuelStage(String stage) {
        if (!this.samuelSettings.enabled) {
            Helpers.log.warning "Samuel :: init :: Samuel parameter enabled is deprecated because now the execution of samuel is mandatory"
        }

        def samuelResult = runSamuel(applyCtx: [pipeline: [stage: stage]])
        Helpers.log.debug "Samuel :: executeSamuelStage(${stage}) --> ${samuelResult}"
    }

    /**
     * Given some samuel settings from configuration, return a map of settings to be
     * used when creating an Entity.
     */
    private Map createDefaultEntityMap(def owners = []) {
        return [
            'country': getDefaultCountry(),
            'coordinates': getDefaultCoordinates(),
            'name': getDefaultName(),
            'type': getDefaultType(),
            'uuaa': getDefaultUUAA(),
            'kind': getKind(),
// TODO: Uncomment when available
//            'framework': getFramework(),
//            'language': getLanguage(),
            'owners': owners
        ]
    }

    private String getDefaultCountry() {
        String country = this.samuelSettings.country?:Settings.country

        if (!country)  {
            Helpers.error "Samuel :: validateCountry :: $country does not exist"
        } else {
            country = country.toUpperCase()
        }
        if (!(country in SUPPORTED_COUNTRIES))  {
            Helpers.error "Samuel :: validateCountry :: $country is not a valid value for Country"
        }
        return country  
    }

    private static List getDefaultCoordinates() {
        if (Settings.uuaa)
            return [Settings.uuaa, Settings.repo.slug]
        else
            return [Settings.repo.project, Settings.repo.slug]
    }

    private static String getDefaultUUAA() {
        return Settings.uuaa?:'none'
    }

    private static String getDefaultName() {
        return Settings.repo.slug
    }

    private static String getDefaultType() {
        return validateType(Settings.architecture.split('\\.')[0])
    }

    private static String validateType(type) {
        if(!type || !(type in SUPPORTED_TYPES) ){
            Helpers.error "Samuel :: validateType :: $type is not a supported architecture. Review configuration"
        }
        return type
    }

    private static String getKind() {
        def parts = Settings.architecture.split('\\.')
        return parts.length > 1 ? parts[1]: null
    }

    private static String getLanguage() {
        return Settings.language
    }

    private static String getFramework() {
        return Settings.framework
    }
}

return this
