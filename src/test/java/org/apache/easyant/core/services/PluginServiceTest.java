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
package org.apache.easyant.core.services;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.easyant.core.descriptor.PropertyDescriptor;
import org.apache.easyant.core.ivy.IvyInstanceHelper;
import org.apache.easyant.core.report.EasyAntReport;
import org.apache.easyant.core.report.ExtensionPointReport;
import org.apache.easyant.core.report.ImportedModuleReport;
import org.apache.easyant.core.report.TargetReport;
import org.apache.ivy.ant.IvyConfigure;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.tools.ant.Project;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PluginServiceTest {

    private static PluginService pluginService;
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException, URISyntaxException {
        // configure the ivyinstance
        Project p = new Project();

        File cache = folder.newFolder("build-cache");
        p.setProperty("ivy.cache.dir", cache.getAbsolutePath());

        IvyConfigure configure = new IvyConfigure();
        configure.setProject(p);
        configure.setFile(new File(this.getClass().getResource("/repositories/easyant-ivysettings-test.xml").toURI()));
        configure.setSettingsId(EasyAntMagicNames.EASYANT_IVY_INSTANCE);
        configure.execute();

        pluginService = new DefaultPluginService(IvyInstanceHelper.getEasyAntIvyAntSettings(p));
    }

    @Test
    public void shouldFindExistingModuleWithDefaultResolverSearch() throws Exception {
        ModuleRevisionId[] mrids = pluginService.search("mycompany", "simpleplugin");
        // the module should be found once in easyant repo default resolver
        assertThat(mrids.length, is(1));
    }

    @Test
    public void shouldFindExistingModuleWithAllResolversSearch() throws Exception {
        ModuleRevisionId[] mrids = pluginService.search("mycompany", "simpleplugin", null, null, PatternMatcher.EXACT,
                "*");
        // the module should be found once each in easyant repo and in chained
        // resolver
        assertThat(mrids.length, is(2));
        assertThat(mrids[0], equalTo(mrids[1]));
    }

    @Test
    public void shouldFindExistingModule() throws Exception {
        String[] mrids = pluginService.searchModule("mycompany", "simpleplugin");
        // the module should be found once in easyant repo default resolver
        assertThat(mrids.length, is(1));
    }

    @Test
    public void testGenerateReport() throws Exception {
        EasyAntReport eaReport = generateReport();
        assertThat(eaReport, is(notNullValue()));
        assertThat(eaReport.getImportedModuleReport("mycompany#complexplugin;0.1"), is(notNullValue()));
        assertThat(eaReport.getImportedModuleReport("mycompany#simpleplugin;0.1"), is(notNullValue()));

        ImportedModuleReport complexPlugin = eaReport.getImportedModuleReport("mycompany#complexplugin;0.1");
        assertThat(complexPlugin, is(notNullValue()));

        // global importedModule size should be equals to :
        // importedModule size of complexplugin + 2 (complexplugin itself +simpleplugin)
        Assert.assertEquals(complexPlugin.getEasyantReport().getImportedModuleReports().size() + 2, eaReport
                .getImportedModuleReports().size());

        checkPropertyValueEquals(eaReport.getPropertyDescriptors().get("run.main.classname"),
                "org.apache.easyant.example.Example");

        checkPropertyDefaultValueEquals(eaReport.getPropertyDescriptors().get("myproperty"), "foobar");
        checkPropertyDefaultValueEquals(eaReport.getPropertyDescriptors().get("anotherproperty"), "foo");

        // the property should also be contained in getAvailableProperties which
        // list all properties (those for the current module and those in
        // imported modules)
        checkPropertyValueEquals(eaReport.getAvailableProperties().get("run.main.classname"),
                "org.apache.easyant.example.Example");

        // check that package ExtensionPoint exists and targets are bound to
        // this extension-point
        ExtensionPointReport packageEP = eaReport.getExtensionPointReport("package");
        assertThat(packageEP, is(notNullValue()));
        assertThat(packageEP.getDepends(), is("complexplugin:mytarget,hello-world"));

        List<TargetReport> targets = packageEP.getTargetReports();
        Set<String> expectedTargets = new HashSet<String>(Arrays.asList("hello-world", "complexplugin:mytarget"));
        assertThat(targets.size(), is(expectedTargets.size()));

        for (TargetReport target : packageEP.getTargetReports()) {
            assertTrue("expected to find " + target.getName(), expectedTargets.remove(target.getName()));
        }

        TargetReport helloWorld = eaReport.getTargetReport("hello-world");
        assertThat(helloWorld, is(notNullValue()));
        assertThat(helloWorld.getExtensionPoint(), is("package"));
    }

    @Test
    public void shouldGetDescriptionFromExistingPlugin() throws Exception {
        String description = pluginService.getPluginDescription("mycompany#abstractplugin;0.1");
        assertThat(description, is("an abstract plugin"));
    }

    @Test
    public void shouldFindPluginInfoForExistingModule() throws Exception {
        EasyAntReport pluginInfo = pluginService.getPluginInfo("mycompany#abstractplugin;0.1");
        assertThat(pluginInfo, is(notNullValue()));
        assertThat(pluginInfo.getImportedModuleReports().size(), is(0));
        assertThat(pluginInfo.getPropertyDescriptors().size(), is(2));
        assertThat(pluginInfo.getTargetReport("abstractplugin:init"), is(notNullValue()));
        checkPropertyDefaultValueEquals(pluginInfo.getPropertyDescriptors().get("myproperty"), "foobar");
        checkPropertyDefaultValueEquals(pluginInfo.getPropertyDescriptors().get("anotherproperty"), "foo");
    }

    @Test
    public void shouldFindGivenPluginInfoAsNestedPlugin() throws Exception {
        ModuleRevisionId mrid = ModuleRevisionId.parse("mycompany#complexplugin;0.1");
        EasyAntReport pluginInfo = pluginService.getPluginInfo(mrid, "default");
        assertThat(pluginInfo, is(notNullValue()));
        assertThat(pluginInfo.getImportedModuleReportsFromCurrentModule().size(), is(1));
        ImportedModuleReport abstractPlugin = pluginInfo.getImportedModuleReport("abstractplugin");

        assertThat(abstractPlugin, is(notNullValue()));
        EasyAntReport abstractPluginReport = abstractPlugin.getEasyantReport();
        assertThat(abstractPluginReport.getTargetReport("abstractplugin:init"), is(notNullValue()));
        assertThat(abstractPluginReport.getImportedModuleReportsFromCurrentModule().size(), is(0));
        assertThat(abstractPluginReport.getPropertyDescriptors().size(), is(2));
        checkPropertyDefaultValueEquals(abstractPluginReport.getPropertyDescriptors().get("myproperty"), "foobar");
    }

    @Test
    public void shouldGetPluginReportWithoutRootlevelTasks() throws Exception {
        EasyAntReport pluginInfo = pluginService.getPluginInfo(
                new File(this.getClass().getResource("plugins/simple-plugin-without-rootlevel-tasks.ivy").toURI()),
                new File(this.getClass().getResource("plugins").toURI()), "default");
        assertThat(pluginInfo.getImportedModuleReports().size(), is(0));
        assertThat(pluginInfo.getPropertyDescriptors().size(), is(1));
        checkPropertyDefaultValueEquals(pluginInfo.getPropertyDescriptors().get("src.main.java"),
                "${basedir}/src/main/java");
        assertThat(pluginInfo.getTargetReports().size(), is(2));

        TargetReport helloWorld = pluginInfo.getTargetReport("simple-plugin-without-rootlevel-tasks:hello-world");
        assertThat(helloWorld, is(notNullValue()));
        assertThat(helloWorld.getDescription(), is("hello-world description"));

        ExtensionPointReport pluginReadyEP = pluginInfo
                .getExtensionPointReport("simple-plugin-without-rootlevel-tasks:plugin-ready");
        assertThat(pluginReadyEP, is(notNullValue()));
        assertThat(pluginReadyEP.getDescription(), is("plugin-ready description"));
    }

    public void checkPropertyDefaultValueEquals(PropertyDescriptor propertyDescriptor, String expectedValue) {
        assertThat(propertyDescriptor, is(notNullValue()));
        assertThat(propertyDescriptor.getDefaultValue(), is(expectedValue));
    }

    public void checkPropertyValueEquals(PropertyDescriptor propertyDescriptor, String expectedValue) {
        assertThat(propertyDescriptor, is(notNullValue()));
        assertThat(propertyDescriptor.getValue(), is(expectedValue));
    }

    private EasyAntReport generateReport() throws Exception {
        File module = new File(this.getClass().getResource("module.ivy").toURI());
        File moduleAnt = new File(this.getClass().getResource("module.ant").toURI());
        return pluginService.generateEasyAntReport(module, moduleAnt, null);
    }

}
