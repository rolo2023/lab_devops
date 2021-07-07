import globals.Helpers
import globals.Settings

/**
 * email
 *
 * Wrapper over jenkins 'mail' feature.
 * This depends on the server having the right SMTP configuration!!
 * {@see docs/vars/email.md}
 *
 * params:
 * - title
 * - body
 * - additional_recipient_list
 */
def call(Map map) {
    String defaultTitle = "An error has happened building {{ repo.slug }}@{{ repo.branch }}"
    String defaultBody = """
Hello, {{ repo.author_name }}.
   
An error has happened building {{ repo.slug }}@{{ repo.branch }}: {{ env.BUILD_URL }}
"""
    List defaultRecipients = [Settings.repo.author_email]

    Map config = map ?: [:]
    String title = config.get('title', defaultTitle)
    String body = config.get('body', defaultBody)
    List recipientList = defaultRecipients + config.get('additional_recipient_list', []) as List

    try {
        mail(
                body: Helpers.subst(body),
                subject: Helpers.subst(title),
                to: Helpers.substituteList(recipientList).join(',')
        )
    } catch (smtpError) {
        Helpers.error "Unable to send email: make sure your SMTP settings are working"
    }

}

/**
 * When called from within a step, this will be invoked AUTOMATICALLY.
 * Use it to massage parameters before sending them to the 'call' method
 *
 * @param stepArgs branch dependent parameters
 */
def fromStep(Map stepArgs) {
    call(stepArgs)
}

//* Used during testing to return a callable script
return this
