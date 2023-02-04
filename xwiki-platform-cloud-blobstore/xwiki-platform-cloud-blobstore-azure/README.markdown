Introduction
============

This module contains a blobstore implementation for Azure Blob Storage.

There are several parameters that must be defined in `WEB-INF/xwiki.properties` file in order to configure the Azure Blob Storage blobstore:

* `xwiki.store.attachments.blobstore=azure` to select the Microsoft Azure Blob Storage blobstore.
* `xwiki.store.attachments.blobstore.container=CONTAINER_NAME` where `CONTAINER_NAME` is the container where you want to store your attachments. 
* `xwiki.store.attachments.blobstore.storageAccountName=STORAGE_ACCOUNT_NAME`. Your Microsoft Azure Blob Storage account name. 
* `xwiki.store.attachments.blobstore.sasToken=SAS_TOKEN`. Your Microsoft Azure Blob Storage SAS Token for your container. https://learn.microsoft.com/en-us/azure/storage/common/storage-sas-overview. Go to the `Shared access tokens` section under your Container setting in Azure Storage to create it. 
* `xwiki.store.attachments.blobstore.namespace=NAMESPACE` where `NAMESPACE` is a string that will be used as the first path component for storing all the attachments of this wiki. This is used to host multiple wikis or farms in the same bucket. Keep blank for regular user.
