---
title: Create Mirror
---

In addition to creating a new repository and importing an existing repository, the SCM Manager offers a third mode `Mirroring Repository`.

The form can be used to define a URL, possible access data and a synchronization interval.
In addition, the SCM Manager usual information about the new repository is requested.
Based on this data, a new repository is created in the SCM Manager and the content is mirrored from the external source.

Mirrored repositories are identified by the `Mirror` tag.
A gray tag indicates ongoing synchronization with the external source.
If the data was successfully synchronized, the tag is green, in case of an error it is red.

[!Create_Mirror]("screenshot")

The mirror settings like source, credentials and synchronization interval can also be changed later 
using the corresponding settings menu in the repository.
