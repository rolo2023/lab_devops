#!groovy
package api.vtrack

interface IVtrack {

    /**
     * Gets a NS in Vtrack
     *
     * @param nsid The NS id
     *
     * @return INamespace A Vtrack Namespace representation
     */
    INamespace getNamespace(String nsid)

    /**
     * Request a Namespace from the Government API.
     *
     * @param uuaa      The UUAA code
     * @param geoCode   Three-char country code
     * @param nsType    Namespace Type
     *
     * @return INamespace A Vtrack Namespace representation
     */
    INamespace getNamespaceFromGovernment(String uuaa, String geoCode, String nsType)
}