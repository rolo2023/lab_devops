package workflow.vars

import org.junit.Before
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class curlTest extends BaseVarsTest {

    void setPathToScript() {
        this.pathToScript = "vars/curl.groovy"
    }

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
    }

    @Test
    void "withCurl downloads with just an url"() {
        String myUrl = 'http://www.google.es'
        systemUnderTest.call('get', myUrl)
        String curlCall = helper.callStack.find { it.methodName == 'sh' }.args[0]
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).is(0)
        assertThat(curlCall).contains("-XGET '${myUrl}'")
    }

    @Test
    void "withCurl can download an url with credentials via proxy"() {
        String myUrl = 'https://globaldevtools.bbva.com/artifactory-api'
        String fakeProxy = 'http://super.proxy:8081/'
        systemUnderTest.call(action: 'get', url: myUrl, proxy: fakeProxy)
        String curlCall = helper.callStack.find { it.methodName == 'sh' }.args[0]
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).is(0)
        assertThat(curlCall).contains("-XGET '${myUrl}'")
        assertThat(curlCall).contains("-x '${fakeProxy}'")
    }

    @Test
    void "withCurl downloads an url with credentials"() {
        String myUrl = 'https://globaldevtools.bbva.com/artifactory-api'
        systemUnderTest.call(action: 'get', url: myUrl, credentials: usernameColonPasswordValue)
        String curlCall = helper.callStack.find { it.methodName == 'sh' }.args[0]
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).is(0)
        assertThat(curlCall).contains("-XGET '${myUrl}'")
        assertThat(curlCall).contains("-u '${usernameColonPasswordValue}'")
    }

    @Test
    void "GET operation specifies a local destination file for a given remote file"() {
        // Given a local file and a URL to a resource
        String localFile = 'localFile'
        String myUrl = 'https://globaldevtools.bbva.com/artifactory-api/repository/file.extension'

        // When we do a GET request
        systemUnderTest.call(action: 'get', url: myUrl, localFile: localFile)

        //  Then we shouldn't get errors
        String curlCall = helper.callStack.find { it.methodName == 'sh' }.args[0]
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).is(0)

        // And we get data from the correct URL
        assertThat(curlCall).contains("-XGET '${myUrl}'")

         // And we write to the correct local file
        assertThat(curlCall).contains("-o '${localFile}'")
    }

    @Test
    void "PUT operation requires a  local file to be passed as argument"() {
        // Given an URL to a resource
        String myUrl = 'https://globaldevtools.bbva.com/artifactory-api/repository/file.extension'

        // When we do a PUT request
        systemUnderTest.call(action: 'put', url: myUrl)

        //  Then we should  get an error
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).is(1)
    }

    @Test
    void "PUT operation writes a local file to a given remote URL"() {
        // Given a local file and a URL to a resource
        String localFileTest = 'myFile.local'
        String myUrl = 'https://globaldevtools.bbva.com/artifactory-api/repository/file.extension'

        // When we do a PUT request
        systemUnderTest.call(action: 'put', url: myUrl, localFile: localFileTest)

        //  Then we shouldn't get errors
        String curlCall = helper.callStack.find { it.methodName == 'sh' }.args[0]
        assertThat(helper.callStack.findAll { it.methodName == 'error' }.size()).is(0)

        // And we get data from the correct URL
        assertThat(curlCall).contains("-XPUT '${myUrl}'")

         // And we write to the correct local file
        assertThat(curlCall).contains("-T '${localFileTest}'")
    }
}
