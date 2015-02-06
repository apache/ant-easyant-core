/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.easyant.core;

import org.apache.easyant.core.ant.listerners.MultiModuleLogger;
import org.apache.easyant.core.configuration.EasyAntConfiguration;
import org.apache.easyant.core.ivy.IvyInstanceHelper;
import org.apache.easyant.core.services.DefaultPluginService;
import org.apache.ivy.Ivy;
import org.apache.ivy.ant.IvyAntSettings;
import org.apache.easyant.core.ivy.EasyAntRepositoryCacheManager;
import org.apache.easyant.core.ivy.EasyantResolutionCacheManager;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.input.PropertyFileInputHandler;
import org.apache.tools.ant.util.ProxySetup;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;

import static org.apache.easyant.CollectionTestUtil.containsClass;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class EasyAntEngineTest {
    private EasyAntConfiguration easyAntConfiguration = new EasyAntConfiguration();
    private EasyAntEngine easyantEngine = new EasyAntEngine(easyAntConfiguration);
    private Project project = new Project();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        File cache = temporaryFolder.newFolder("build-cache");
        project.setProperty("ivy.cache.dir", cache.getAbsolutePath());

    }

    private void configureEasyAntIvyInstanceForTests() {
        easyAntConfiguration.setEasyantIvySettingsUrl(this.getClass().getResource(
                "/repositories/easyant-ivysettings-test.xml"));
        project.setProperty(EasyAntMagicNames.IGNORE_USER_IVYSETTINGS, "true");
    }

    @Test
    public void shouldAddBuildListener() {
        easyAntConfiguration.getListeners().add(MultiModuleLogger.class.getCanonicalName());
        easyantEngine.addBuildListeners(project);
        assertTrue(containsClass(project.getBuildListeners(), MultiModuleLogger.class));
    }

    @Test
    public void shouldSetInputHandler() {
        easyAntConfiguration.setInputHandlerClassname(PropertyFileInputHandler.class.getCanonicalName());
        easyantEngine.addInputHandler(project);
        assertThat(project.getInputHandler(), instanceOf(PropertyFileInputHandler.class));
    }

    @Test
    public void shouldSetDefaultInputHandler() {
        easyantEngine.addInputHandler(project);
        assertThat(project.getInputHandler(), instanceOf(DefaultInputHandler.class));
    }

    @Test
    public void shouldCreateLogger() {
        easyAntConfiguration.setLoggerClassname(MultiModuleLogger.class.getCanonicalName());
        BuildLogger logger = easyantEngine.createLogger();
        assertThat(logger, instanceOf(MultiModuleLogger.class));
    }

    @Test
    public void shouldFailIfLoggerDoesntExists() {
        expectedException
                .expectMessage("The specified logger class a-missing-logger could not be used because Class not found");
        easyAntConfiguration.setLoggerClassname("a-missing-logger");
        easyantEngine.createLogger();
    }

    @Test
    public void shouldFindFileInCurrentDirectory() throws URISyntaxException {
        File startFile = new File(this.getClass().getResource("multimodule/myapp-core").toURI());
        File foundFile = easyantEngine.findBuildModule(startFile.getAbsolutePath(), "module.ivy");
        assertThat(foundFile.getName(), is("module.ivy"));
        assertThat(foundFile.getParentFile().getName(), is("myapp-core"));
    }

    @Test
    public void shouldFindFileInParentDirectory() throws URISyntaxException {
        File startFile = new File(this.getClass().getResource("multimodule/myapp-core").toURI());
        File foundFile = easyantEngine.findBuildModule(startFile.getAbsolutePath(), "parent.ivy");
        assertThat(foundFile.getName(), is("parent.ivy"));
        assertThat(foundFile.getParentFile().getName(), is("multimodule"));
    }

    @Test
    public void shouldFailIfNotFound() throws URISyntaxException {
        expectedException.expectMessage("Could not locate a build file!");
        File startFile = new File(this.getClass().getResource("multimodule/myapp-core").toURI());
        easyantEngine.findBuildModule(startFile.getAbsolutePath(), "a-missing-file");
    }

    @Test
    public void shouldConfigurePluginService() throws URISyntaxException {
        IvyAntSettings ivyAntSettings = new IvyAntSettings();
        ivyAntSettings.setProject(project);
        ivyAntSettings.setFile(new File(this.getClass().getResource("/repositories/easyant-ivysettings-test.xml")
                .toURI()));

        easyantEngine.configurePluginService(project, ivyAntSettings);
        assertThat(project.getReference(EasyAntMagicNames.PLUGIN_SERVICE_INSTANCE),
                instanceOf(DefaultPluginService.class));
        assertThat(easyantEngine.getPluginService(), instanceOf(DefaultPluginService.class));
        assertThat(easyantEngine.getPluginService(),
                sameInstance(project.getReference(EasyAntMagicNames.PLUGIN_SERVICE_INSTANCE)));
    }

    @Test
    public void shouldConfigureEasyAnt() {
        configureEasyAntIvyInstanceForTests();
        easyantEngine.configureEasyAnt(project);
        assertThat(Thread.currentThread().getPriority(), is(Thread.NORM_PRIORITY));
        assertThat(easyAntConfiguration.getCoreLoader(), nullValue());
        assertThat(easyAntConfiguration.isProxy(), is(false));
        assertEasyAntIsConfigured();
    }

    @Test
    public void shouldConfigureEasyAntWithCustomPriority() {
        configureEasyAntIvyInstanceForTests();
        easyAntConfiguration.setThreadPriority(10);
        easyantEngine.configureEasyAnt(project);
        assertThat(Thread.currentThread().getPriority(), is(easyAntConfiguration.getThreadPriority()));
        assertEasyAntIsConfigured();
    }

    @Test
    public void shouldConfigureEasyAntWithCustomCoreLoader() {
        configureEasyAntIvyInstanceForTests();
        easyAntConfiguration.setCoreLoader(this.getClass().getClassLoader());
        easyantEngine.configureEasyAnt(project);
        assertEasyAntIsConfigured();
        assertThat(project.getCoreLoader(), is(this.getClass().getClassLoader()));
    }

    @Test
    public void shouldConfigureEasyAntWhenKeepGoingModeIsTrue() {
        configureEasyAntIvyInstanceForTests();
        easyAntConfiguration.setKeepGoingMode(true);
        easyantEngine.configureEasyAnt(project);
        assertEasyAntIsConfigured();
    }

    @Test
    public void shouldEasyAntProjectWhenProxyIsTrue() {
        configureEasyAntIvyInstanceForTests();
        String oldValue = System.getProperty(ProxySetup.USE_SYSTEM_PROXIES);
        System.getProperties().remove(ProxySetup.USE_SYSTEM_PROXIES);

        easyAntConfiguration.setProxy(true);
        easyantEngine.configureEasyAnt(project);

        assertEasyAntIsConfigured();

        if (oldValue != null) {
            System.setProperty(ProxySetup.USE_SYSTEM_PROXIES, oldValue);
        } else {
            System.getProperties().remove(ProxySetup.USE_SYSTEM_PROXIES);
        }
    }

    private void assertEasyAntIsConfigured() {
        assertThat(project.getCoreLoader(), is(easyAntConfiguration.getCoreLoader()));
        assertThat(project.isKeepGoingMode(), is(easyAntConfiguration.isKeepGoingMode()));
        assertThat(Boolean.parseBoolean(System.getProperty(ProxySetup.USE_SYSTEM_PROXIES)),
                is(easyAntConfiguration.isProxy()));
        assertThat(project.getName(), is("EasyAnt"));
    }

    @Test
    public void shouldConfigureEasyAntIvyInstanceWithSettingsFileFromConfiguration() throws URISyntaxException {
        File f = new File(this.getClass().getResource("/repositories/easyant-ivysettings-test.xml").toURI());
        easyAntConfiguration.setEasyantIvySettingsFile(f.getAbsolutePath());
        IvyAntSettings configuredEasyAntIvyInstance = easyantEngine.configureEasyAntIvyInstance(project);

        assertThat(project.getReference(EasyAntMagicNames.EASYANT_IVY_INSTANCE), instanceOf(IvyAntSettings.class));
        assertThat(project.getProperty(EasyAntMagicNames.EASYANT_DEFAULT_IVYSETTINGS),
                is(this.getClass().getResource("/org/apache/easyant/core/default-easyant-ivysettings.xml")
                        .toExternalForm()));
        assertThat(Boolean.valueOf(project.getProperty(EasyAntMagicNames.IGNORE_USER_IVYSETTINGS)), is(false));

        assertThat(project.getProperty(EasyAntMagicNames.OFFLINE_EASYANT_RESOLVER),
                is(EasyAntConstants.DEFAULT_OFFLINE_EASYANT_RESOLVER));
        assertThat(project.getProperty(EasyAntMagicNames.OFFLINE_BASE_DIRECTORY), is(project.getBaseDir()
                .getAbsolutePath() + "/offline/"));

        IvyAntSettings easyantIvySettings = IvyInstanceHelper.getEasyAntIvyAntSettings(project);
        Ivy ivyInstance = easyantIvySettings.getConfiguredIvyInstance(null);
        assertThat(ivyInstance.getResolutionCacheManager(), instanceOf(EasyantResolutionCacheManager.class));
        assertTrue(containsClass(Arrays.asList(ivyInstance.getSettings().getRepositoryCacheManagers()), EasyAntRepositoryCacheManager.class));

        assertThat(configuredEasyAntIvyInstance, sameInstance(easyantIvySettings));
    }


    @Test
    public void shouldReturnDefaultUserEasyAntIvySettingsLocation() {
        File userEasyAntIvySettings = easyantEngine.getUserEasyAntIvySettings(project);
        assertThat(userEasyAntIvySettings.getAbsolutePath(),
                endsWith(EasyAntConstants.DEFAULT_USER_EASYANT_IVYSETTINGS));
    }

    @Test
    public void shouldReturnUserEasyAntIvySettingsLocationSpecifiedByProperty() {
        String fakeLocation = "/path/to/userSettings.xml";
        project.setNewProperty(EasyAntMagicNames.USER_EASYANT_IVYSETTINGS, fakeLocation);
        File userEasyAntIvySettings = easyantEngine.getUserEasyAntIvySettings(project);
        assertThat(userEasyAntIvySettings.getAbsolutePath(), is(fakeLocation));
    }

    @Test
    public void shouldReturnNullGlobalEasyAntIvySettingsLocationIfNoDefaultGlobalExists() throws MalformedURLException {
        // configure default global to missing directory
        project.setNewProperty(EasyAntMagicNames.EASYANT_HOME, "/fake/path");
        URL globalEasyAntIvySettings = easyantEngine.getGlobalEasyAntIvySettings(project);
        assertThat(globalEasyAntIvySettings, nullValue());
    }

    @Test
    public void shouldReturnDefaultGlobalEasyAntIvySettingsLocationIfExists() throws IOException {
        File f = temporaryFolder.newFile("easyant-ivysettings.xml");
        FileOutputStream fos = null;
        try {
            // write file
            fos = new FileOutputStream(f);
            project.setNewProperty(EasyAntMagicNames.EASYANT_HOME, f.getParent());
            URL globalEasyAntIvySettings = easyantEngine.getGlobalEasyAntIvySettings(project);
            assertThat(globalEasyAntIvySettings, notNullValue());
            assertThat(globalEasyAntIvySettings, is(f.toURI().toURL()));
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }

    @Test
    public void shouldReturnGlobalEasyAntIvySettingsLocationSpecifiedByConfigurationFile() throws MalformedURLException {
        easyAntConfiguration.setEasyantIvySettingsFile("/path/to/fake/easyantIvySettingsFile.xml");
        URL globalEasyAntIvySettings = easyantEngine.getGlobalEasyAntIvySettings(project);
        assertThat(globalEasyAntIvySettings.toString(), endsWith(easyAntConfiguration.getEasyantIvySettingsFile()));
    }

    @Test
    public void shouldReturnGlobalEasyAntIvySettingsLocationSpecifiedByConfigurationURLAsString()
            throws MalformedURLException {
        easyAntConfiguration.setEasyantIvySettingsUrl("file:/path/to/fake/easyantIvySettingsFile.xml");
        URL globalEasyAntIvySettings = easyantEngine.getGlobalEasyAntIvySettings(project);
        assertThat(globalEasyAntIvySettings.toString(), is(easyAntConfiguration.getEasyantIvySettingsUrl()));
    }

    @Test
    public void shouldReturnGlobalEasyAntIvySettingsLocationSpecifiedByConfigurationURL() throws MalformedURLException {
        easyAntConfiguration.setEasyantIvySettingsUrl(this.getClass().getResource(
                "/repositories/easyant-ivysettings-test.xml"));
        URL globalEasyAntIvySettings = easyantEngine.getGlobalEasyAntIvySettings(project);
        assertThat(globalEasyAntIvySettings.toString(), is(easyAntConfiguration.getEasyantIvySettingsUrl()));
    }

}
