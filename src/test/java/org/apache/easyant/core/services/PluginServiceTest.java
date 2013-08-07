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

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.apache.easyant.core.descriptor.PropertyDescriptor;
import org.apache.easyant.core.report.EasyAntReport;
import org.apache.easyant.core.report.ExtensionPointReport;
import org.apache.easyant.core.report.ImportedModuleReport;
import org.apache.easyant.core.report.TargetReport;
import org.apache.easyant.core.services.impl.DefaultPluginServiceImpl;
import org.apache.ivy.ant.IvyAntSettings;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.tools.ant.Project;
import org.junit.BeforeClass;
import org.junit.Test;

public class PluginServiceTest {

    private static PluginService pluginService;

    @BeforeClass
    public static void setUp() throws ParseException, IOException {
        // configure the ivyinstance
        Project p = new Project();
        IvyAntSettings ivyAntSettings = new IvyAntSettings();
        ivyAntSettings.setUrl(PluginServiceTest.class.getResource("/ivysettings-test.xml"));
        ivyAntSettings.setProject(p);
        pluginService = new DefaultPluginServiceImpl(ivyAntSettings);

    }

    @Test
    public void testDefaultResolverSearch() throws Exception {
        ModuleRevisionId[] mrids = pluginService.search("org.apache.easyant.buildtypes", "build-std-java");
        // the module should be found once in easyant repo default resolver
        Assert.assertEquals(1, mrids.length);
    }

    @Test
    public void testSearchAllResolvers() throws Exception {
        ModuleRevisionId[] mrids = pluginService.search("org.apache.easyant.buildtypes", "build-std-java", null, null,
                PatternMatcher.EXACT, "*");
        // the module should be found once each in easyant repo and in chained
        // resolver
        Assert.assertEquals(2, mrids.length);
        Assert.assertEquals(mrids[0], mrids[1]);
    }

    @Test
    public void testSearchModule() throws Exception {
        String[] mrids = pluginService.searchModule("org.apache.easyant.buildtypes", "build-std-java");
        // the module should be found once in easyant repo default resolver
        Assert.assertEquals(1, mrids.length);
    }

    private EasyAntReport generateReport() throws Exception {
        File module = new File(this.getClass().getResource("module.ivy").toURI());
        File moduleAnt = new File(this.getClass().getResource("module.ant").toURI());
        return pluginService.generateEasyAntReport(module, moduleAnt, null);
    }

    @Test
    public void testGenerateReport() throws Exception {
        EasyAntReport eaReport = generateReport();
        Assert.assertNotNull(eaReport);

        Assert.assertNotNull(eaReport.getImportedModuleReport("org.apache.easyant.buildtypes#build-std-java;0.9"));
        Assert.assertNotNull(eaReport.getImportedModuleReport("org.apache.easyant.plugins#run-java;0.9"));

        ImportedModuleReport buildType = eaReport
                .getImportedModuleReport("org.apache.easyant.buildtypes#build-std-java;0.9");
        // global importedModule size should be equals to :
        // importedModule size of buildtype + 2 (buildtype itself +run-java plugin)
        Assert.assertEquals(buildType.getEasyantReport().getImportedModuleReports().size() + 2, eaReport
                .getImportedModuleReports().size());

        checkPropertyValueEquals(eaReport.getPropertyDescriptors().get("run.main.classname"),
                "org.apache.easyant.example.Example");

        checkPropertyValueEquals(eaReport.getPropertyDescriptors().get("src.main.java"), "${basedir}/src/main/java");
        checkPropertyValueEquals(eaReport.getPropertyDescriptors().get("target.main.classes"), "${target}/main/classes");
        checkPropertyValueEquals(eaReport.getPropertyDescriptors().get("target"), "${basedir}/target");
        checkPropertyValueEquals(eaReport.getPropertyDescriptors().get("test.framework"), "junit");

        checkPropertyDefaultValueEquals(eaReport.getPropertyDescriptors().get("target.main.artifact"),
                "${target.artifacts}/${target.artifacts.main.jar.name}");

        // the property should also be contained in getAvailableProperties which
        // list all properties (those for the current module and those in
        // imported modules)
        checkPropertyValueEquals(eaReport.getAvailableProperties().get("run.main.classname"),
                "org.apache.easyant.example.Example");

        // check that package ExtensionPoint exists and targets are bound to
        // this extension-point
        ExtensionPointReport packageEP = eaReport.getExtensionPointReport("package");
        Assert.assertNotNull(packageEP);

        Assert.assertEquals("compile,abstract-package:package,hello-world", packageEP.getDepends());

        List<TargetReport> targets = packageEP.getTargetReports();
        Set<String> expectedTargets = new HashSet<String>(Arrays.asList("hello-world"));
        Assert.assertEquals(expectedTargets.size(), targets.size());

        for (TargetReport target : packageEP.getTargetReports()) {
            Assert.assertTrue("expected to find " + target.getName(), expectedTargets.remove(target.getName()));
        }

        TargetReport helloWorld = eaReport.getTargetReport("hello-world");
        Assert.assertNotNull(helloWorld);
        Assert.assertTrue("package".equals(helloWorld.getExtensionPoint()));
    }

    @Test
    public void testGetDescription() throws Exception {
        String description = pluginService.getPluginDescription("org.apache.easyant.plugins#run-java;0.9");
        Assert.assertEquals("This module provides java bytecode execution feature.", description);
    }

    @Test
    public void testGetPluginInfo() throws Exception {
        EasyAntReport pluginInfo = pluginService.getPluginInfo("org.apache.easyant.plugins#compile-java;0.9");
        Assert.assertNotNull(pluginInfo);
        Assert.assertEquals(2, pluginInfo.getImportedModuleReports().size());
        Assert.assertNotNull(pluginInfo.getImportedModuleReport("abstract-provisioning"));
        Assert.assertNotNull(pluginInfo.getImportedModuleReport("abstract-compile"));

        checkPropertyDefaultValueEquals(pluginInfo.getPropertyDescriptors().get("compile.java.includes.pattern"),
                "**/*.java");
        checkPropertyDefaultValueEquals(pluginInfo.getPropertyDescriptors().get("target.test.integration.classes"),
                "${target}/integration-test/classes");
    }

    @Test
    public void testGetPluginInfoOnlyForCurrentPlugin() throws Exception {
        ModuleRevisionId mrid = ModuleRevisionId.parse("org.apache.easyant.plugins#compile-java;0.9");
        EasyAntReport pluginInfo = pluginService.getPluginInfo(mrid, "default");
        Assert.assertNotNull(pluginInfo);
        Assert.assertEquals(1, pluginInfo.getImportedModuleReportsFromCurrentModule().size());
        ImportedModuleReport abstractCompile = pluginInfo.getImportedModuleReport("abstract-compile");
        Assert.assertNotNull(abstractCompile);
        Assert.assertEquals(1, abstractCompile.getEasyantReport().getImportedModuleReportsFromCurrentModule().size());
        Assert.assertNotNull(abstractCompile.getEasyantReport().getImportedModuleReport("abstract-provisioning"));
        checkPropertyDefaultValueEquals(
                pluginInfo.getPropertyReportsFromCurrentModule().get("compile.java.includes.pattern"), "**/*.java");
        checkPropertyDefaultValueEquals(
                abstractCompile.getEasyantReport().getPropertyReportsFromCurrentModule()
                        .get("target.test.integration.classes"), "${target}/integration-test/classes");
    }

    @Test
    public void testGetPluginInfoWithNestedPlugin() throws Exception {
        EasyAntReport pluginInfo = pluginService.getPluginInfo("org.apache.easyant.plugins#compile-java;0.9");
        Assert.assertNotNull(pluginInfo);

        // verify abstract-provisioning is imported in abstract-compile
        ImportedModuleReport importedModuleReport = pluginInfo.getImportedModuleReport("abstract-compile");
        Assert.assertNotNull(importedModuleReport);
        Assert.assertNotNull(importedModuleReport.getEasyantReport());
        Assert.assertEquals(1, importedModuleReport.getEasyantReport().getImportedModuleReports().size());
        Assert.assertNotNull(importedModuleReport.getEasyantReport().getImportedModuleReport("abstract-provisioning"));

        checkPropertyDefaultValueEquals(
                importedModuleReport.getEasyantReport().getPropertyDescriptors().get("target.test.integration.classes"),
                "${target}/integration-test/classes");
    }

    @Test
    public void shouldGetPluginReportWithoutRootlevelTasks() throws Exception {
        EasyAntReport pluginInfo = pluginService.getPluginInfo(
                new File(this.getClass().getResource("plugins/simple-plugin-without-rootlevel-tasks.ivy").toURI()),
                new File(this.getClass().getResource("plugins").toURI()), "default");

        Assert.assertEquals(0, pluginInfo.getImportedModuleReports().size());
        Assert.assertEquals(1, pluginInfo.getPropertyDescriptors().size());
        checkPropertyDefaultValueEquals(pluginInfo.getPropertyDescriptors().get("src.main.java"),
                "${basedir}/src/main/java");
        Assert.assertEquals(1, pluginInfo.getTargetReports().size());

        TargetReport helloWorld = pluginInfo.getTargetReport("simple-plugin-without-rootlevel-tasks:hello-world");
        Assert.assertNotNull(helloWorld);
        Assert.assertEquals("hello-world description", helloWorld.getDescription());

        ExtensionPointReport pluginReadyEP = pluginInfo
                .getExtensionPointReport("simple-plugin-without-rootlevel-tasks:plugin-ready");
        Assert.assertNotNull(pluginReadyEP);
        Assert.assertEquals("plugin-ready description", pluginReadyEP.getDescription());
    }

    public void checkPropertyDefaultValueEquals(PropertyDescriptor propertyDescriptor, String expectedValue) {
        Assert.assertNotNull(propertyDescriptor);
        Assert.assertEquals(expectedValue, propertyDescriptor.getDefaultValue());
    }

    public void checkPropertyValueEquals(PropertyDescriptor propertyDescriptor, String expectedValue) {
        Assert.assertNotNull(propertyDescriptor);
        Assert.assertEquals(expectedValue, propertyDescriptor.getValue());
    }
}
