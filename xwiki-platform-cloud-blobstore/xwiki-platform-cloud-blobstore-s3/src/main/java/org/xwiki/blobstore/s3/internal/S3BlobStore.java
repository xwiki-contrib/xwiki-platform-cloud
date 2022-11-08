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
package org.xwiki.blobstore.s3.internal;

import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.blobstore.BlobStore;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

/**
 * Amazon S3 blob store implementation. This is a singleton in order to reuse as much as possible the Amazon S3 client
 * object (https://forums.aws.amazon.com/thread.jspa?threadID=50723)
 *
 * @version $Id$
 */
@Component
@Named("s3")
@Singleton
public class S3BlobStore implements BlobStore, Initializable
{
    /**
     * The bucket to be used for storing data.
     */
    private String bucket;

    /**
     * The optional region for the bucket.
     */
    private Regions region;

    /**
     * The S3 client. No particular mechanisms are used in the code to deal with multiple thread interactions because
     * the Amazon S3 client is thread safe: https://forums.aws.amazon.com/thread.jspa?threadID=50723
     */
    private AmazonS3 client;

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
     * The namespace where to store data.
     */
    private String namespace;

    @Override
    public void initialize() throws InitializationException
    {
        final String undefinedPropStr = "%s property is not defined.";

        this.logger.debug("Using {}", this.configurationSource.getClass().getName());

        this.bucket = this.configurationSource.getProperty(BlobStore.BLOBSTORE_BUCKET_PROPERTY, String.class);
        if (this.bucket == null) {
            throw new InitializationException(String.format(undefinedPropStr, BlobStore.BLOBSTORE_BUCKET_PROPERTY));
        }

        specifyRegionIfNecessary();

        String accessKey = this.configurationSource.getProperty(BlobStore.BLOBSTORE_IDENTITY_PROPERTY, String.class);
        if (accessKey == null) {
            throw new InitializationException(String.format(undefinedPropStr, BlobStore.BLOBSTORE_IDENTITY_PROPERTY));
        }

        String secretKey = this.configurationSource.getProperty(BlobStore.BLOBSTORE_CREDENTIAL_PROPERTY, String.class);
        if (secretKey == null) {
            throw new InitializationException(String.format(undefinedPropStr, BlobStore.BLOBSTORE_CREDENTIAL_PROPERTY));
        }

        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        final AmazonS3ClientBuilder clientBuilder = AmazonS3ClientBuilder
            .standard()
            .withCredentials(new AWSStaticCredentialsProvider(credentials));

        if (this.region != null) {
            clientBuilder.withRegion(this.region);
        }

        this.client = clientBuilder.build();

        boolean bucketExists = this.client.doesBucketExistV2(this.bucket);
        if (!bucketExists) {
            this.client.createBucket(this.bucket);
        }

        this.namespace = this.configurationSource.getProperty(BlobStore.BLOBSTORE_NAMESPACE_PROPERTY, String.class);

        this.logger.debug("S3 blob store initialized using namespace '{}' and bucket '{}'",
            this.namespace != null ? this.namespace : "no namespace specified",
            this.bucket);
    }

    /**
     * Try to retrieve the region if explicitly specified.
     *
     * @throws InitializationException iff the specified region is not valid
     */
    private void specifyRegionIfNecessary() throws InitializationException {
        final String invalidRegStr = "The region %s specified by %s is invalid: %s";
        final String regStr = this.configurationSource.getProperty(BlobStore.BLOBSTORE_REGION_PROPERTY, String.class);
        if (StringUtils.isNotBlank(regStr)) {
            try {
                this.region = Regions.fromName(regStr);
            } catch (Exception ex) {
                throw new InitializationException(
                    String.format(invalidRegStr, regStr, BlobStore.BLOBSTORE_REGION_PROPERTY, ex.getMessage())
                );
            }
        }
    }

    @Override
    public void deleteBlob(String path)
    {
        String normalizedPath = normalizePath(path);

        this.logger.debug("Deleting blob '{}' from bucket '{}'", normalizedPath, this.bucket);

        this.client.deleteObject(this.bucket, normalizedPath);
    }

    @Override
    public InputStream getBlob(String path)
    {
        String normalizedPath = normalizePath(path);

        this.logger.debug("Getting blob '{}' from bucket '{}'", normalizedPath, this.bucket);

        S3Object object = this.client.getObject(this.bucket, normalizedPath);
        if (object != null) {
            return object.getObjectContent();
        }

        return null;
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

        ObjectMetadata objectMetadata = new ObjectMetadata();
        if (length > 0) {
            objectMetadata.setContentLength(length);
        }

        this.client.putObject(this.bucket, normalizedPath, content, objectMetadata);
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
