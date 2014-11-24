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
package org.apache.easyant.tasks;

import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.easyant.core.ant.ProjectUtils;
import org.apache.easyant.core.ivy.IvyInstanceHelper;
import org.apache.easyant.core.services.DefaultPluginService;
import org.apache.easyant.core.services.PluginService;
import org.apache.ivy.Ivy;
import org.apache.ivy.ant.IvyAntSettings;
import org.apache.ivy.ant.IvyConfigure;
import org.apache.ivy.plugins.resolver.FileSystemResolver;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class GoOfflineTest extends AntTaskBaseTest {

    private static final String EASYANT_BUILDSCOPE_REP = "easyant-buildscope-rep";
    private static final String PROJECT_BUILDSCOPE_REP = "project-buildscope-rep";

    private GoOffline goOffline;

    private File easyantBuildScopeRepoFolder;
    private File projectBuildScopeRepoFolder;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setUp() throws URISyntaxException, IOException {
        Project project = new Project();
        ProjectUtils.configureProjectHelper(project);
        configureBuildLogger(project, Project.MSG_VERBOSE);

        File cache = folder.newFolder("build-cache");
        project.setProperty("ivy.cache.dir", cache.getAbsolutePath());
        File projectCache = folder.newFolder("easyant-cache-test");
        project.setProperty("easyant.default.cache.dir", projectCache.getAbsolutePath());

        configureEasyantSettings(project);
        configureProjectSettings(project);

        project.setBaseDir(new File(this.getClass().getResource("simple").toURI()));

        configurePluginService(project);
        configureEasyantBuildScopeRepository(project);
        configureProjectBuildScopeRepository(project);

        goOffline = new GoOffline();
        goOffline.setProject(project);
        goOffline.setOwningTarget(ProjectUtils.createTopLevelTarget());
        goOffline.setLocation(new Location(ProjectUtils.emulateMainScript(project).getAbsolutePath()));

    }

    private void configureEasyantSettings(Project project) throws URISyntaxException {
        configureSettings(project, EasyAntMagicNames.EASYANT_IVY_INSTANCE, "/repositories/easyant-ivysettings-test.xml");
    }

    private void configureProjectSettings(Project project) throws URISyntaxException {
        configureSettings(project, IvyInstanceHelper.getProjectIvyInstanceName(project), "/ivysettings-test.xml");
    }

    private void configureSettings(Project project, String settingsId, String settingsFile) throws URISyntaxException {
        IvyConfigure configure = new IvyConfigure();
        configure.setSettingsId(settingsId);
        configure.setProject(project);
        configure.setFile(new File(this.getClass().getResource(settingsFile).toURI()));
        configure.execute();
    }

    private void configureEasyantBuildScopeRepository(Project project) throws IOException {
        easyantBuildScopeRepoFolder = folder.newFolder("ea-build-repo");
        FileSystemResolver resolver = newResolver(easyantBuildScopeRepoFolder, EASYANT_BUILDSCOPE_REP);

        Ivy easyantIvyInstance = IvyInstanceHelper.getEasyAntIvyAntSettings(project)
                .getConfiguredIvyInstance(goOffline);
        easyantIvyInstance.getSettings().addResolver(resolver);
    }

    private void configureProjectBuildScopeRepository(Project project) throws IOException {
        projectBuildScopeRepoFolder = folder.newFolder("project-build-repo");
        FileSystemResolver resolver = newResolver(projectBuildScopeRepoFolder, PROJECT_BUILDSCOPE_REP);

        Ivy projecttIvyInstance = IvyInstanceHelper.getProjectIvyAntSettings(project)
                .getConfiguredIvyInstance(goOffline);
        projecttIvyInstance.getSettings().addResolver(resolver);
    }

    private FileSystemResolver newResolver(File repoFolder, String resolverName) {

        FileSystemResolver resolver = new FileSystemResolver();
        resolver.setName(resolverName);
        resolver.addArtifactPattern(repoFolder.getAbsolutePath()
                + "/[organisation]/[module]/[revision]/[artifact](-[classifier]).[ext]");
        resolver.addIvyPattern(repoFolder.getAbsolutePath()
                + "/[organisation]/[module]/[revision]/[artifact](-[classifier]).[ext]");

        return resolver;
    }

    private void configurePluginService(Project project) {
        PluginService pluginService = new DefaultPluginService(
                (IvyAntSettings) project.getReference(EasyAntMagicNames.EASYANT_IVY_INSTANCE));
        project.addReference(EasyAntMagicNames.PLUGIN_SERVICE_INSTANCE, pluginService);
    }

    @Test
    public void shouldFailIfNoModuleIvyIsSpecified() {
        expectedException.expectMessage("Couldn't locate module ivy did you specified moduleivy attribute ?");
        goOffline.execute();
    }

    @Test
    public void shouldFailIfProjectResolverNameIsNotSet() throws URISyntaxException {
        expectedException.expectMessage("projectResolverName is mandatory !");
        goOffline.setModuleIvy(new File(this.getClass().getResource("simple/module.ivy").toURI()));
        goOffline.execute();
    }

    @Test
    public void shouldFailIfEasyAntResolverNameIsNotSet() throws URISyntaxException {
        expectedException.expectMessage("easyantResolverName is mandatory !");
        goOffline.setProjectResolverName("test-plugin");
        goOffline.setModuleIvy(new File(this.getClass().getResource("simple/module.ivy").toURI()));
        goOffline.execute();
    }

    @Test
    public void shouldInstallPluginsAndDependencies() throws URISyntaxException {
        goOffline.setEasyantResolverName(EASYANT_BUILDSCOPE_REP);
        goOffline.setProjectResolverName("test-plugin");
        goOffline.setModuleIvy(new File(this.getClass().getResource("simple/module.ivy").toURI()));
        goOffline.execute();

        assertLogContaining("installing mycompany#simpleplugin;0.1");

        File ivyFile = new File(easyantBuildScopeRepoFolder.getAbsolutePath() + "/mycompany/simpleplugin/0.1/ivy.xml");
        assertThat(ivyFile.exists(), is(true));

        File antFile = new File(easyantBuildScopeRepoFolder.getAbsolutePath()
                + "/mycompany/simpleplugin/0.1/simpleplugin.ant");
        assertThat(antFile.exists(), is(true));
    }

    @Test
    public void shouldInstallPluginsAndDependenciesTransitive() throws URISyntaxException {
        goOffline.setEasyantResolverName(EASYANT_BUILDSCOPE_REP);
        goOffline.setProjectResolverName(PROJECT_BUILDSCOPE_REP);
        goOffline.setModuleIvy(new File(this.getClass().getResource("dependencies/module.ivy").toURI()));
        goOffline.execute();

        assertLogContaining("installing mycompany#simpleplugin;0.1");
        assertLogContaining("installing junit#junit;4.4");
        assertLogContaining("installing org.mortbay.jetty#jetty;6.1.14");

        assertFileExists(easyantBuildScopeRepoFolder, "/mycompany/simpleplugin/0.1/ivy.xml");
        assertFileExists(easyantBuildScopeRepoFolder, "/mycompany/simpleplugin/0.1/simpleplugin.ant");

        assertFileExists(projectBuildScopeRepoFolder, "/junit/junit/4.4/junit.jar");
        assertFileExists(projectBuildScopeRepoFolder, "/org.mortbay.jetty/jetty/6.1.14/jetty.jar");
        assertFileExists(projectBuildScopeRepoFolder, "/org.mortbay.jetty/jetty-util/6.1.14/jetty-util.jar");
    }


}
