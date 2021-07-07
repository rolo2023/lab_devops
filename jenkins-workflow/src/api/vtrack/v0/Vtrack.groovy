#!groovy
package api.vtrack.v0

import api.requests.RestJsonClient
import api.vtrack.INamespace
import api.vtrack.IVtrack

class Vtrack implements Serializable, IVtrack {
    Script script
    RestJsonClient session
    List namespaces = []
    String architecture

    private String getRegionFromUrl() {
        def regionMatcher = (this.session.baseUrl =~ /.*vtrack.(.*).(ether|platform).*/)
        if (!regionMatcher.matches()) {
            throw new Exception("VtrackError: cannot read Vtrack region from ${this.session.baseUrl}")
        }
        return regionMatcher[0][1] as String
    }

    Vtrack(Script script, Map args) {
        this.script = script
        this.session = new RestJsonClient(script, args)
    }

    void exception(String task, String message) {
        this.script.echo "*** Unable to perform task ${task}.\n*** Server response: ${message}\n***"
        throw new Exception("VtrackErrorOn${task.capitalize()}")
    }

    INamespace getNamespace(String nsid) {
        Map ns = [
                _id: nsid
        ]
        INamespace namespace = new Namespace(this.script, this.session, ns)
        return namespace
    }

    INamespace getNamespaceFromGovernment(String uuaa, String geoCode, String nsType = 'DESIGN') {
        // We need to know which region we're in, in order to call government API there
        String etherRegion = this.getRegionFromUrl()

        //* IMPORTANT: We are reusing Ether credentials from Vtrack here --> Make sure they are valid in Government!!
        String governmentApiUrl = "https://gov.${etherRegion}.platform.bbva.com"
        governmentApiUrl = "${governmentApiUrl}/v1/namespaces/search?uaAcronym=${uuaa}&geographyCode=${geoCode}&namespaceType=${nsType}"

        // We use 'curl' rather than GET, so we do not get the vtrack URL as base
        Map response = this.session.curl("-XGET '${governmentApiUrl}'")
        if (response._error || !response.ok) {
            def msg = response._error ? response.get("_error")["message"] : "${response}"
            exception('getNamespaceFromGovernment', msg)
        }

        // Check result, we might not get a value back
        def namespaceId = response.data ? response.data['name'] : null
        if (!namespaceId) {
            return null
        }
        return this.getNamespace(namespaceId)
    }
}
