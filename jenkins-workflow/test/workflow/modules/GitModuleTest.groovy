package workflow.modules

import globals.*
import org.junit.Before
import org.junit.Test
import workflow.jenkins.BasePipelineTest

import static org.assertj.core.api.Assertions.assertThat

class GitModuleTest extends BasePipelineTest {

    void setPathToScript() {
        this.pathToScript = "src/modules/GitModule.groovy"
    }

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
    }

    @Test
    void "Calling init loads default git credentials"() {
        // When we call init on the module
        systemUnderTest.init()

        // Then no errors have happened
        assertThat(helper.getCallStack().findAll { it.methodName == "error" }.size()).isEqualTo(0)

        // And the default credential set has been set
        assertThat(systemUnderTest.credentials).isEqualTo 'bot-globaldevops-pro-ssh'
    }

    @Test
    void "Calling init loads user git credentials if found in configuration"() {
        // Given a Git module config
        Settings.modules.git = [
                credentialsId: 'my-credentials'
        ]

        // When we call init on the module
        systemUnderTest.init()

        // Then no errors have happened
        assertThat(helper.getCallStack().findAll { it.methodName == "error" }.size()).isEqualTo(0)

        // And the chosencredential set has been set
        assertThat(systemUnderTest.credentials).isEqualTo 'my-credentials'
    }

    @Test
    void "we can safely recover relevant parts of a GIT url"() {
        // Given  a valid git ssh url
        def gitUrl = 'ssh://git@globaldevtools.bbva.com:7999/bbvaeseccpdevops/workflow-spring.git'

        // When get call get parts
        def parts = systemUnderTest.collectFromUrl(gitUrl)

        // Successfully get project and slug
        assertThat(parts.project).isEqualTo('bbvaeseccpdevops')
        assertThat(parts.slug).isEqualTo('workflow-spring')
    }

    @Test
    void "when we clone the repository, it is never a shallow clone"() {

        // Given a set of credentials
        systemUnderTest.credentials = 'the-credentials'

        // And a variable to capture closure params
        Map testData = [
                shallow      : true,
                credentialsId: ''
        ]

        // And a mock of the 'repo' call from workflow libs
        systemUnderTest.metaClass.repo = { Closure s ->
            s.resolveStrategy = Closure.DELEGATE_FIRST
            s.delegate = testData
            s()
        }

        // When we clone the repo
        systemUnderTest.cloneRepo()

        // Then we verify that the clone is not shallow
        assertThat(testData.shallow).isFalse()
    }

    @Test
    void "setRepositoryConfig needs cloneRepo to be called before"() {
        // Given an empty mock of the repo call methods
        systemUnderTest.metaClass.repo = [:]

        // When we set the repo config
        systemUnderTest.setRepositoryConfig()

        // Then an error must be raised
        assertThat(helper.getCallStack().findAll { it.methodName == "error" }.size()).isEqualTo(1)
    }

    @Test
    void "we can write repository config as a Settings map"() {
        // Given a mock of the repo call methods
        systemUnderTest.metaClass.repo = [
                codeInfo : [
                        GIT_URL   : 'ssh://my.server/my_repo',
                        GIT_BRANCH: 'release/2.0',
                        GIT_COMMIT: '123456789affff'
                ],
                hasSemVer: { -> false },
        ]

        // When we set that config as a settings
        systemUnderTest.setRepositoryConfig()

        // Then no error must be raised
        assertThat(helper.getCallStack().findAll { it.methodName == "error" }.size()).isEqualTo(0)

        // And the settings are now there
        String repoConfig = systemUnderTest.showRepoConfig()
        [
                "branch (complete): release/2.0",
                "branch (short): release",
                "commit (complete): 123456789affff",
                "commit (short): 1234567",
                "project: my.server",
                "repo slug: my_repo",
                "Branch parent: release",
                "Pull Request: false",
                "Comes from a merge: false",
                "Will create a PR: false",
                "Will apply Semantic Versioning? false",
                "If so, which is the next version? SNAPSHOT",
        ].each { assertThat(repoConfig).contains it }
    }

    @Test
    void "we can write repository config as a Settings map for a PR too"() {
        // Given a mock of the repo call methods
        systemUnderTest.metaClass.repo = [
                codeInfo : [
                        GIT_URL   : 'ssh://my.server/my_repo',
                        GIT_BRANCH: 'PR-124',
                        GIT_COMMIT: '123456789affff'
                ],
                hasSemVer: { -> false },
        ]

        // And a mock of Bitbucket module
        Modules.bitbucket = [
                getPrInfo: { b ->
                    [
                            toRef  : 'master',
                            fromRef: 'hotfix/ISSUE-1234-horreur'
                    ]
                }
        ]

        // When we set that config as a settings
        systemUnderTest.setRepositoryConfig()

        // Then no error must be raised
        assertThat(helper.getCallStack().findAll { it.methodName == "error" }.size()).isEqualTo(0)

        // And the settings are now there
        String repoConfig = systemUnderTest.showRepoConfig()
        [
                "Pull Request: true",
                "Branch Origin: hotfix/ISSUE-1234-horreur",
                "Branch Target: master",
                "Will create a PR: true",
        ].each { assertThat(repoConfig).contains it }
    }

    @Test
    void "we can shorten any branch name, even if non-standard"() {
        // Given an array of branch names
        def branchNames = [
                'develop-1234'           : 'develop',
                'release/1234'           : 'release',
                'feature-random-id'      : 'feature',
                'bugfix/1234-description': 'bugfix'
        ]

        // Then we correctly shorten them
        branchNames.each { realName, shortName ->
            assertThat(systemUnderTest.getGenericBranchName(realName)).isEqualTo shortName
        }
    }
}
