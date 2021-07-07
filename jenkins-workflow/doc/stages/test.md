# Test stage

This stage is executed AFTER the Build Stage, but BEFORE the Publish stage.

In Kubernetes, it uses the same Pod Template as the Build Stage.

## Vtrack information

Upon finish, current Vtrack Build's metadata is updated with the result of the testing (success or failure).

## Appendix - Stages

* [Build](./build.md)
* [Publish](./publish.md)
* [Test](./test.md)
* [Deploy](./deploy.md)
* [End](./end.md)
