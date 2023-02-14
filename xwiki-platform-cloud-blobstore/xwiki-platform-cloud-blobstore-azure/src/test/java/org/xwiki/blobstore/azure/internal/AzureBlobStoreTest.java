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

import com.azure.core.implementation.logging.DefaultLogger;
import com.azure.core.util.Configuration;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.internal.MemoryConfigurationSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;

public class AzureBlobStoreTest {

    @Ignore
    @Test
    public void testAzureBlobStore() throws InitializationException, IOException {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "DEBUG");

        MemoryConfigurationSource configurationSource = new MemoryConfigurationSource();
        Properties properties = getProperties();
        properties.stringPropertyNames()
                .forEach(k -> configurationSource.setProperty(k, properties.getProperty(k)));

        Configuration.getGlobalConfiguration().put("ROOT_LOG_LEVEL", "DEBUG");
        Logger logger = new DefaultLogger(AzureBlobStoreTest.class);
        AzureBlobStore azureBlobStore = new AzureBlobStore();
        azureBlobStore.setConfigurationSource(configurationSource);
        azureBlobStore.setLogger(logger);
        azureBlobStore.initialize();

        String stringContent = "Hello World!";
        byte[] byteArray = stringContent.getBytes(StandardCharsets.UTF_8);
        long length = byteArray.length;
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);

        azureBlobStore.putBlob("xwiki/storage/test.txt", byteArrayInputStream, length);
    }

    /**
     * Create a file under the src/test/resources folder called azure.application.properties and put the value of the
     * sasToken in it, and all other needed parameters
     * This is done in order to make it easy to test the connection but not commit the token to github (this file is
     * added to the .gitignore file).
     *
     */
    private static Properties getProperties() throws IOException {
        Properties properties = new Properties();
        properties.load(Objects.requireNonNull(AzureBlobStoreTest.class.getClassLoader()
                .getResourceAsStream("azure.application.properties")));
        return properties;
    }
}
