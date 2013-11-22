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

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.easyant.core.EasyAntConstants;
import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.easyant.core.ant.ProjectUtils;
import org.apache.easyant.core.ivy.IvyInstanceHelper;
import org.apache.easyant.core.parser.DefaultEasyAntXmlModuleDescriptorParser;
import org.apache.ivy.Ivy;
import org.apache.ivy.ant.IvyAntSettings;
import org.apache.ivy.ant.IvyConfigure;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class LoadModuleTest extends AntTaskBaseTest {

    private LoadModule loadModule;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() throws URISyntaxException, IOException {
        Project project = new Project();
        ProjectUtils.configureProjectHelper(project);

        File cache = folder.newFolder("build-cache");
        project.setProperty("ivy.cache.dir", cache.getAbsolutePath());

        IvyConfigure configure = new IvyConfigure();
        configure.setProject(project);

        File f = new File(this.getClass().getResource("/repositories/easyant-ivysettings-test.xml").toURI());
        configure.setFile(f);

        configure.setSettingsId(EasyAntMagicNames.EASYANT_IVY_INSTANCE);
        configure.execute();

        project.setBaseDir(new File(this.getClass().getResource("simple").toURI()));

        loadModule = new LoadModule();
        loadModule.setProject(project);
        loadModule.setOwningTarget(ProjectUtils.createTopLevelTarget());
        loadModule.setLocation(new Location(ProjectUtils.emulateMainScript(project).getAbsolutePath()));
    }

    @Test
    public void shouldLoadModuleWithCustomBuildModule() throws URISyntaxException {
        Project project = loadModule.getProject();
        configureBuildLogger(project, Project.MSG_DEBUG);
        File moduleIvyFile = new File(this.getClass().getResource("simple/module.ivy").toURI());
        loadModule.setBuildModule(moduleIvyFile);
        loadModule.execute();

        assertThat(project.getName(), is(project.getProperty("ivy.module")));

        assertLogContaining("Loading build module : " + moduleIvyFile.getAbsolutePath());
        assertLogContaining("Loading EasyAnt module descriptor :"
                + DefaultEasyAntXmlModuleDescriptorParser.class.getName());
        assertLogNotContaining("Loading build file :");

        assertThat(project.getProperty(EasyAntMagicNames.AVAILABLE_BUILD_CONFIGURATIONS), nullValue());

        assertThat(project.getProperty("run.main.classname"), is("org.apache.easyant.example.Example"));

        ResolveReport resolveReport = project.getReference(EasyAntMagicNames.IMPORTED_MODULES_RESOLVE_REPORT_REF);
        assertThat(resolveReport, notNullValue());
        ModuleRevisionId buildType = ModuleRevisionId.parse("mycompany#simpleplugin;0.1");
        ModuleRevisionId plugin = ModuleRevisionId.parse("mycompany#simplepluginwithproperties;0.1");
        @SuppressWarnings("unchecked")
        List<ModuleId> resolvedModuleIds = resolveReport.getModuleIds();
        assertThat(resolvedModuleIds, hasItems(buildType.getModuleId(), plugin.getModuleId()));

        assertThat(project.getUserProperty("ant.file.mycompany#simpleplugin"), notNullValue());
        assertThat(project.getUserProperty("ant.file.mycompany#simplepluginwithproperties"), notNullValue());

        Ivy configuredIvyInstance = verifyOfflineResolverIsConfigured(project);

        assertThat(configuredIvyInstance.getSettings().getResolver(EasyAntConstants.BUILD_SCOPE_REPOSITORY),
                nullValue());

    }

    @Test
    public void shouldNotFailWithNonExistingBuildModule() throws URISyntaxException {
        Project project = loadModule.getProject();
        configureBuildLogger(project, Project.MSG_DEBUG);
        File moduleIvyFile = new File("missingmodule.ivy");
        loadModule.setBuildModule(moduleIvyFile);
        loadModule.execute();

        assertThat(project.getName(), is(project.getProperty("ivy.module")));

        assertLogNotContaining("Loading build module : " + moduleIvyFile.getAbsolutePath());
        assertLogNotContaining("Loading build file :");

        assertThat(project.getProperty(EasyAntMagicNames.AVAILABLE_BUILD_CONFIGURATIONS), nullValue());
        assertThat(project.getReference(EasyAntMagicNames.IMPORTED_MODULES_RESOLVE_REPORT_REF), nullValue());

        Ivy configuredIvyInstance = verifyOfflineResolverIsConfigured(project);

        assertThat(configuredIvyInstance.getSettings().getResolver(EasyAntConstants.BUILD_SCOPE_REPOSITORY),
                nullValue());

    }

    @Test
    public void shouldFailIfBuildModuleIsADirectory() throws URISyntaxException {
        File moduleIvyFile = new File(this.getClass().getResource("simple").toURI());
        expectedException.expectMessage("What? buildModule: " + moduleIvyFile.getAbsolutePath() + " is a dir!");

        loadModule.setBuildModule(moduleIvyFile);
        loadModule.execute();
    }

    @Test
    public void shouldLoadModuleWithCustomBuildFile() throws URISyntaxException {
        Project project = loadModule.getProject();
        configureBuildLogger(project, Project.MSG_DEBUG);
        File moduleAntFile = new File(this.getClass().getResource("simple/module.ant").toURI());
        loadModule.setBuildFile(moduleAntFile);
        loadModule.execute();

        assertLogNotContaining("Loading build module :");
        assertLogContaining("Loading build file : " + moduleAntFile.getAbsolutePath());

        assertThat(project.getTargets().get("hello-world"), notNullValue());

        assertThat(project.getProperty(EasyAntMagicNames.AVAILABLE_BUILD_CONFIGURATIONS), nullValue());
        assertThat(project.getReference(EasyAntMagicNames.IMPORTED_MODULES_RESOLVE_REPORT_REF), nullValue());

        Ivy projectIvyInstance = verifyOfflineResolverIsConfigured(project);

        assertThat(projectIvyInstance.getSettings().getResolver(EasyAntConstants.BUILD_SCOPE_REPOSITORY), nullValue());

    }

    @Test
    public void shouldNotFailWithNonExistingBuildFile() throws URISyntaxException {
        Project project = loadModule.getProject();
        configureBuildLogger(project, Project.MSG_DEBUG);
        File moduleAntFile = new File("missingmodule.ant");
        loadModule.setBuildFile(moduleAntFile);
        loadModule.execute();

        assertLogNotContaining("Loading build module :");
        assertLogNotContaining("Loading build file : " + moduleAntFile.getAbsolutePath());

        assertThat(project.getProperty(EasyAntMagicNames.AVAILABLE_BUILD_CONFIGURATIONS), nullValue());
        assertThat(project.getReference(EasyAntMagicNames.IMPORTED_MODULES_RESOLVE_REPORT_REF), nullValue());

        Ivy projectIvyInstance = verifyOfflineResolverIsConfigured(project);

        assertThat(projectIvyInstance.getSettings().getResolver(EasyAntConstants.BUILD_SCOPE_REPOSITORY), nullValue());

    }

    @Test
    public void shouldFailIfBuildFileIsADirectory() throws URISyntaxException {
        File moduleAntFile = new File(this.getClass().getResource("simple").toURI());
        expectedException.expectMessage("What? buildFile: " + moduleAntFile.getAbsolutePath() + " is a dir!");
        loadModule.setBuildFile(moduleAntFile);
        loadModule.execute();

    }

    @Test
    public void shouldNotFailWithNoArgument() throws URISyntaxException {
        Project project = loadModule.getProject();
        configureBuildLogger(project, Project.MSG_DEBUG);
        loadModule.execute();

        assertLogNotContaining("Loading build module :");
        assertLogNotContaining("Loading build file : ");

        assertThat(project.getProperty(EasyAntMagicNames.AVAILABLE_BUILD_CONFIGURATIONS), nullValue());
        assertThat(project.getReference(EasyAntMagicNames.IMPORTED_MODULES_RESOLVE_REPORT_REF), nullValue());

        Ivy projectIvyInstance = verifyOfflineResolverIsConfigured(project);

        assertThat(projectIvyInstance.getSettings().getResolver(EasyAntConstants.BUILD_SCOPE_REPOSITORY), nullValue());

    }

    @Test
    public void shouldConfigureBuildScopeRepository() throws URISyntaxException {
        Project project = loadModule.getProject();
        configureBuildLogger(project, Project.MSG_DEBUG);
        loadModule.setUseBuildRepository(true);
        loadModule.execute();

        assertLogNotContaining("Loading build module :");
        assertLogNotContaining("Loading build file : ");

        assertThat(project.getProperty(EasyAntMagicNames.AVAILABLE_BUILD_CONFIGURATIONS), nullValue());
        assertThat(project.getReference(EasyAntMagicNames.IMPORTED_MODULES_RESOLVE_REPORT_REF), nullValue());

        Ivy projectIvyInstance = verifyOfflineResolverIsConfigured(project);

        assertThat(projectIvyInstance.getSettings().getResolver(EasyAntConstants.BUILD_SCOPE_REPOSITORY),
                notNullValue());

    }

    private Ivy verifyOfflineResolverIsConfigured(Project project) {
        IvyAntSettings projectIvyInstanceRef = project.getReference(IvyInstanceHelper
                .getProjectIvyInstanceName(project));
        assertThat(projectIvyInstanceRef, notNullValue());
        assertThat(project.getProperty(EasyAntMagicNames.OFFLINE_PROJECT_RESOLVER),
                is(EasyAntConstants.DEFAULT_OFFLINE_PROJECT_RESOLVER));
        Ivy configuredIvyInstance = projectIvyInstanceRef.getConfiguredIvyInstance(loadModule);
        assertThat(configuredIvyInstance.getSettings().getResolver(EasyAntConstants.DEFAULT_OFFLINE_PROJECT_RESOLVER),
                notNullValue());
        return configuredIvyInstance;
    }
}
