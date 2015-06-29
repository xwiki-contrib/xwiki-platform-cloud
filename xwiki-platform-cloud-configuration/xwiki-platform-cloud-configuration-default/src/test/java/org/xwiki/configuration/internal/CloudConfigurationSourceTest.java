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
package org.xwiki.configuration.internal;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.io.ByteArrayInputStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SystemEnvironmentConfigurationSource}.
 *
 * @version $Id$
 */
public class CloudConfigurationSourceTest
{
    @Rule
    public MockitoComponentMockingRule<ConfigurationSource> mocker =
        new MockitoComponentMockingRule<ConfigurationSource>(CloudConfigurationSource.class);

    /**
     * The remapping file.
     */
    private static final String REMAPPING_FILE = "/WEB-INF/remapping.properties";

    /**
     * Test string for properties.
     */
    private static final String FOO = "foo";

    /**
     * Test string for properties.
     */
    private static final String BAR = "bar";

    /**
     * The environment injected in the configuration source.
     */
    private Environment environment;

    /**
     * Setup the cloud configuration source for tests.
     *
     * @throws Exception If the cloud configuration source cannot be looked up.
     */
    @Before
    public void setUp() throws Exception
    {
        this.environment = this.mocker.getInstance(Environment.class);

        /* Build a fake remapping file containing a remap entry */
        final ByteArrayInputStream remappingFileContent =
            new ByteArrayInputStream(String.format("remap.%s=%s", FOO, BAR).getBytes());
        when(this.environment.getResourceAsStream(REMAPPING_FILE)).thenReturn(remappingFileContent);
    }

    /**
     * Check that all System environment properties are correctly accessible through the configuration source.
     *
     * @throws Exception If the configuration source cannot be looked up.
     */
    @Test
    public void testGetSystemEnvironmentProperties() throws Exception
    {
        ConfigurationSource system = this.mocker.getInstance(ConfigurationSource.class, "system-environment");
        when(system.containsKey("sysenvprop")).thenReturn(true);
        when(system.getProperty("sysenvprop")).thenReturn("sysvalue");
        Assert.assertEquals("sysvalue", this.mocker.getComponentUnderTest().getProperty("sysenvprop"));
    }

    /**
     * Check that all System properties are correctly accessible through the configuration source.
     *
     * @throws Exception If the configuration source cannot be looked up.
     */
    @Test
    public void testGetSystemProperties() throws Exception
    {
        ConfigurationSource system = this.mocker.getInstance(ConfigurationSource.class, "system-properties");
        when(system.containsKey("sysprop")).thenReturn(true);
        when(system.getProperty("sysprop")).thenReturn("sysvalue");
        Assert.assertEquals("sysvalue", this.mocker.getComponentUnderTest().getProperty("sysprop"));
    }

    /**
     * Check remapping. This checks that the remapping mechanism works and also that the remapping file is taken into
     * account.
     */
    @Test
    public void testRemapping() throws ComponentLookupException
    {
        ConfigurationSource system = this.mocker.getInstance(ConfigurationSource.class, "system-properties");
        when(system.containsKey(FOO)).thenReturn(true);
        when(system.getProperty(FOO)).thenReturn(FOO);
        when(system.containsKey(BAR)).thenReturn(true);
        when(system.getProperty(BAR)).thenReturn(BAR);

        Assert.assertEquals(BAR, this.mocker.getComponentUnderTest().getProperty(FOO));
    }
}
