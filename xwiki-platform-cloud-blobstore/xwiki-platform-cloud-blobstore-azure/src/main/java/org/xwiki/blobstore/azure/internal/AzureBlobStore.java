/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.blobstore.azure.internal;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.blobstore.BlobStore;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.InputStream;

/**
 * Amazon Azure blob store implementation. This is a singleton in order to reuse as much as possible the Azure client
 * object.
 *
 * @version $Id$
 */
@Component
@Named("azure")
@Singleton
public class AzureBlobStore implements BlobStore, Initializable
{

    /**
     * Configuration.
     */
    @Inject
    @Named("cloud")
    private ConfigurationSource configurationSource;

    /**
     * Logger.
     */
    @Inject
    private Logger logger;

    /**
     * The Container client.
     */
    private BlobContainerClient containerClient;

    /**
     * The namespace where to store data.
     */
    private String namespace;

    /**
     * Setter for the configurationSource.
     * @param configurationSource the ConfigurationSource to set
     */
    public void setConfigurationSource(ConfigurationSource configurationSource) {
        this.configurationSource = configurationSource;
    }

    /**
     * Setter for the logger.
     * @param logger the Logger to set
     */
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void initialize() throws InitializationException
    {
        final String undefinedPropStr = "%s property is not defined.";

        this.logger.debug("Using {}", this.configurationSource.getClass().getName());

        String storageAccountName =
                this.configurationSource.getProperty(BlobStore.BLOBSTORE_STORAGE_ACCOUNT_NAME_PROPERTY, String.class);

        if (storageAccountName == null) {
            throw new InitializationException(String.format(undefinedPropStr,
                    BlobStore.BLOBSTORE_STORAGE_ACCOUNT_NAME_PROPERTY));
        }

        String container = this.configurationSource.getProperty(BlobStore.BLOBSTORE_CONTAINER_PROPERTY, String.class);
        if (container == null) {
            throw new InitializationException(String.format(undefinedPropStr, BlobStore.BLOBSTORE_CONTAINER_PROPERTY));
        }

        String sasToken = this.configurationSource.getProperty(BlobStore.BLOBSTORE_SAS_TOKEN_PROPERTY, String.class);
        if (sasToken == null) {
            throw new InitializationException(String.format(undefinedPropStr, BlobStore.BLOBSTORE_SAS_TOKEN_PROPERTY));
        }

        // Azure SDK client builders accept the credential as a parameter
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .endpoint("https://" + storageAccountName + ".blob.core.windows.net/")
                .sasToken(sasToken)
                .buildClient();

        this.containerClient = blobServiceClient.getBlobContainerClient(container);

        boolean containerExists = this.containerClient.exists();
        if (!containerExists) {
            throw new InitializationException(String.format("Container [%s] does not exist", container));
        }

        this.namespace = this.configurationSource.getProperty(BlobStore.BLOBSTORE_NAMESPACE_PROPERTY, String.class);

        this.logger.debug("Azure blob store initialized using namespace '{}' and container '{}'",
            this.namespace != null ? this.namespace : "no namespace specified", container);
    }

    @Override
    public void deleteBlob(String path)
    {
        String normalizedPath = normalizePath(path);

        this.logger.debug("Deleting blob '{}' from container '{}'", normalizedPath,
                this.containerClient.getBlobContainerName());

        this.containerClient.getBlobClient(normalizedPath).delete();
    }

    @Override
    public InputStream getBlob(String path)
    {
        String normalizedPath = normalizePath(path);

        this.logger.debug("Getting blob '{}' from container '{}'", normalizedPath,
                this.containerClient.getBlobContainerName());

        return this.containerClient.getBlobClient(normalizedPath).openInputStream();
    }

    @Override
    public void putBlob(String path, InputStream content)
    {
        putBlob(normalizePath(path), content, 0);
    }

    @Override
    public void putBlob(String path, InputStream content, long length)
    {
        String normalizedPath = normalizePath(path);

        this.logger.debug("Putting blob to '{}'", normalizedPath);

        this.containerClient.getBlobClient(normalizedPath).upload(content, length, true);
    }

    /**
     * Return the actual path for retrieving the blob by taking into account the namespace.
     *
     * @param path The path provided by the user.
     * @return The actual path that takes into account the namespace, if provided in the configuration.
     */
    private String normalizePath(String path)
    {
        if (StringUtils.isNotBlank(this.namespace)) {
            return String.format("%s/%s", this.namespace, path);
        }

        return path;
    }
}
