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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.easyant.core.ant.ProjectUtils;
import org.apache.ivy.ant.IvyConfigure;
import org.apache.ivy.ant.IvyDependency;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class ImportDeferredTest {

    private ImportDeferred importTask;

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

        ResolvePlugins resolvePlugins = new ResolvePlugins();
        resolvePlugins.setProject(project);
        IvyDependency simplePluginDependency = resolvePlugins.createDependency();
        simplePluginDependency.setOrg("mycompany");
        simplePluginDependency.setName("simpleplugin");
        simplePluginDependency.setRev("0.1");
        IvyDependency simplePluginWithPropertiesDependency = resolvePlugins.createDependency();
        simplePluginWithPropertiesDependency.setOrg("mycompany");
        simplePluginWithPropertiesDependency.setName("simplepluginwithproperties");
        simplePluginWithPropertiesDependency.setRev("0.1");

        resolvePlugins.execute();

        importTask = new ImportDeferred();
        importTask.setProject(project);
        importTask.setOwningTarget(ProjectUtils.createTopLevelTarget());
        importTask.setLocation(new Location(ProjectUtils.emulateMainScript(project).getAbsolutePath()));
    }

    @Test
    public void shouldFailIfNoMandatoryAttributeAreSet() {
        expectedException
                .expectMessage("The module to import is not properly specified, you must set set organisation / module attributes");
        importTask.execute();
    }

    @Test
    public void shouldIncludeSimplePlugin() {
        importTask.setOrg("mycompany");
        importTask.setModule("simpleplugin");
        importTask.setMode("include");
        importTask.execute();

        Path pluginClasspath = importTask.getProject().getReference("mycompany#simpleplugin.classpath");
        org.junit.Assert.assertNotNull(pluginClasspath);
        assertNotNull(pluginClasspath);
        assertEquals(0, pluginClasspath.list().length);
    }

    @Test
    public void shouldImportSimplePlugin() {
        importTask.setOrg("mycompany");
        importTask.setModule("simpleplugin");
        importTask.execute();

        Path pluginClasspath = importTask.getProject().getReference("mycompany#simpleplugin.classpath");
        assertNotNull(pluginClasspath);
        assertEquals(0, pluginClasspath.list().length);
    }

    @Test
    public void shouldImportSimplePluginWithProperties() {
        importTask.setOrg("mycompany");
        importTask.setModule("simplepluginwithproperties");
        importTask.execute();

        verifySimplePluginWithPropertiesIsImported();
    }

    private void verifySimplePluginWithPropertiesIsImported() {
        Path pluginClasspath = importTask.getProject().getReference("mycompany#simplepluginwithproperties.classpath");
        assertNotNull(pluginClasspath);
        assertEquals(0, pluginClasspath.list().length);

        String propertiesFileLocation = importTask.getProject().getProperty(
                "mycompany#simplepluginwithproperties.properties.file");
        assertNotNull(propertiesFileLocation);

        String srcMainJavaProperty = importTask.getProject().getProperty("src.main.java");
        assertNotNull(srcMainJavaProperty);
        assertEquals(importTask.getProject().getBaseDir() + "/src/main/java", srcMainJavaProperty);

        // imported from property file
        String aProperty = importTask.getProject().getProperty("aproperty");
        assertNotNull(aProperty);
        assertEquals("value", aProperty);

        String anotherJavaProperty = importTask.getProject().getProperty("anotherproperty");
        assertNotNull(anotherJavaProperty);
        assertEquals("value", anotherJavaProperty);
    }

    @Test
    public void shouldSkipSimplePlugin() {
        importTask.getProject().setNewProperty("skip.mycompany#simplepluginwithproperties", "true");
        importTask.setOrg("mycompany");
        importTask.setModule("simplepluginwithproperties");
        importTask.execute();

        verifySimplePluginWithPropertiesIsSkipped();

    }

    private void verifySimplePluginWithPropertiesIsSkipped() {
        Path pluginClasspath = importTask.getProject().getReference("mycompany#simplepluginwithproperties.classpath");
        Assert.assertNull(pluginClasspath);

        String propertiesFileLocation = importTask.getProject().getProperty(
                "mycompany#simplepluginwithproperties.properties.file");
        assertNull(propertiesFileLocation);

        String srcMainJavaProperty = importTask.getProject().getProperty("src.main.java");
        assertNull(srcMainJavaProperty);

        // imported from property file
        String aProperty = importTask.getProject().getProperty("aproperty");
        assertNull(aProperty);

        String anotherJavaProperty = importTask.getProject().getProperty("anotherproperty");
        assertNull(anotherJavaProperty);
    }

    @Test
    public void shouldSkipSimplePluginWithAsAttribute() {
        importTask.getProject().setNewProperty("skip.myalias", "true");
        importTask.setOrg("mycompany");
        importTask.setModule("simplepluginwithproperties");
        importTask.setAs("myalias");
        importTask.execute();

        verifySimplePluginWithPropertiesIsSkipped();

    }

    @Test
    public void shouldNotSkipMandatoryPlugin() {
        importTask.getProject().setNewProperty("skip.mycompany#simplepluginwithproperties;0.1", "true");
        importTask.setOrg("mycompany");
        importTask.setModule("simplepluginwithproperties");
        importTask.setMandatory(true);
        importTask.execute();

        verifySimplePluginWithPropertiesIsImported();
    }

    @Test
    public void shouldImportMandatoryPlugin() {
        importTask.setOrg("mycompany");
        importTask.setModule("simplepluginwithproperties");
        importTask.setMandatory(true);
        importTask.execute();

        verifySimplePluginWithPropertiesIsImported();
    }

    @Test
    public void shouldComputeAsAttributeOnInclude() {
        importTask.setOrg("mycompany");
        importTask.setModule("simpleplugin");
        importTask.setMode("include");
        importTask.execute();

        // as attribute is preconfigured with module name
        assertNotNull(importTask.getAs());
        assertEquals("simpleplugin", importTask.getAs());
    }

    @Test
    public void shouldFailBuildConfAreFound() {
        expectedException.expectMessage("there is no available build configuration");
        importTask.setOrg("mycompany");
        importTask.setModule("simplepluginwithproperties");
        importTask.setBuildConfigurations("amissingConf");
        importTask.execute();
    }

    @Test
    public void shouldNotImportIfBuildConfDoesntMatch() {
        importTask.getProject().setProperty(EasyAntMagicNames.AVAILABLE_BUILD_CONFIGURATIONS, "aBuildConfNotActive");
        importTask.setOrg("mycompany");
        importTask.setModule("simplepluginwithproperties");
        importTask.setBuildConfigurations("aBuildConfNotActive");
        importTask.execute();

        verifySimplePluginWithPropertiesIsSkipped();
    }

    @Test
    public void shouldImportIfBuildConfMatch() {
        importTask.getProject().setProperty(EasyAntMagicNames.AVAILABLE_BUILD_CONFIGURATIONS, "aBuildConfActive");

        importTask.getProject().setProperty(EasyAntMagicNames.MAIN_CONFS, "aBuildConfActive");

        importTask.setOrg("mycompany");
        importTask.setModule("simplepluginwithproperties");
        importTask.setBuildConfigurations("aBuildConfActive");
        importTask.execute();

        verifySimplePluginWithPropertiesIsImported();
    }

}
