# End stage

This stage is mean to emulate what a declarative pipeline allows developers to do:

## Jenkins Declarative Syntax

```
pipeline {
    agent any
    stages {
        stage('Example') {
            steps {
                echo 'Hello World'
            }
        }
    }
    post {
        always {
            echo 'I will always say Hello again!'
        }
        error {
          echo 'Only shown when an error has happened'
        }
        success {
          echo 'Only shown when the pipeline has ran successfully'
        }
        changed {
            echo "Only shown when the build result changes from last build"
        }
        fixed {
            echo "Only shown when the build fixes last build's errors"
        }
    }
}
```

## Standard pipeline syntax

```
  end:
    always:
      - use: log
        with_params:
          message: 'I will always say Hello again!'
    error:
      - use: log
        with_params:
          message: 'Only shown when an error has happened'
      - use: email
        with_params:
          body: |
            Hello, {{ repo.author_name }}.

            An error has happened building {{ repo.slug }}@{{ repo.branch }}
          title: |
            An error has happened building {{ repo.slug }}@{{ repo.branch }}
          additional_recipient_list:
            - your_team.group@bbva.com
    success:
      - use: log
        with_params:
          message: 'Only shown when the pipeline has ran successfully'
    changed:
      - use: log
        with_params:
              message: "Only shown when the build result changes from last build"
    fixed:
      - use: log
        with_params:
          message: "Only shown when the build fixes last build's errors"

```

## Appendix - Stages

* [Build](./build.md)
* [Publish](./publish.md)
* [Test](./test.md)
* [Deploy](./deploy.md)
* [End](./end.md)

