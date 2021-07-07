package api.vtrack.v0

class Routes implements Serializable {
    static String archs = 'v0/archs'
    static String namespaces = '_service/v0/ns'
    static String components = "v0/ns/%s/components"
    static String builds = "%s/builds" // %s will append component_id
    static String deploys = "%s/deploys" // %s will append component_id + build_id
}