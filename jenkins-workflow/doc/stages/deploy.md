# Deploy stage

Now executed standalone, with it's own Pod Template definition.

**It does not** contain the cloned repository, so if you want something from your repo in order to deploy, we would need
to allow you to clone it again.

It now automatically calls Vtrack which, if enabled and configured, will register deploy information for you.

> This information will be quite basic, and only really valid for the **Dimensions** modules, the rest need to be updated.

## Appendix - Stages

* [Build](./build.md)
* [Publish](./publish.md)
* [Test](./test.md)
* [Deploy](./deploy.md)
* [End](./end.md)
