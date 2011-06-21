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

import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.easyant.core.descriptor.PropertyDescriptor;
import org.apache.easyant.core.report.EasyAntReport;
import org.apache.easyant.core.report.ImportedModuleReport;
import org.apache.easyant.core.report.PhaseReport;
import org.apache.easyant.core.report.TargetReport;
import org.apache.easyant.core.services.impl.DefaultPluginServiceImpl;
import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.junit.BeforeClass;
import org.junit.Test;

public class PluginServiceTest {

    private static PluginService pluginService;

    @BeforeClass
    public static void setUp() throws ParseException, IOException {
        // configure the ivyinstance
        Ivy ivy = IvyContext.pushNewContext().getIvy();
        ivy.setVariable(
                EasyAntMagicNames.EASYANT_CORE_REPO_URL,
                PluginServiceTest.class.getResource(
                        "/org/apache/easyant/core/repository/modules")
                        .toExternalForm());
        ivy.configure(PluginServiceTest.class
                .getResource("/org/apache/easyant/core/default-easyant-ivysettings.xml"));
        pluginService = new DefaultPluginServiceImpl(ivy);

    }

    @Test
    public void testDefaultResolverSearch() throws Exception {
        ModuleRevisionId[] mrids = pluginService.search(
                "org.apache.easyant.buildtypes", "build-std-java");
        // the module should be found once in easyant repo default resolver
        Assert.assertEquals(1, mrids.length);
    }

    @Test
    public void testSearchAllResolvers() throws Exception {
        ModuleRevisionId[] mrids = pluginService.search(
                "org.apache.easyant.buildtypes", "build-std-java", null, null,
                PatternMatcher.EXACT, "*");
        // the module should be found once each in easyant repo and in chained
        // resolver
        Assert.assertEquals(2, mrids.length);
        Assert.assertEquals(mrids[0], mrids[1]);
    }

    @Test
    public void testSearchModule() throws Exception {
        String[] mrids = pluginService.searchModule(
                "org.apache.easyant.buildtypes", "build-std-java");
        // the module should be found once in easyant repo default resolver
        Assert.assertEquals(1, mrids.length);
    }

    private EasyAntReport generateReport() throws Exception {
        File module = new File(this.getClass().getResource("module.ivy")
                .toURI());
        File moduleAnt = new File(this.getClass().getResource("module.ant")
                .toURI());
        return pluginService.generateEasyAntReport(module, moduleAnt, null);
    }

    @Test
    public void testGenerateReport() throws Exception {
        EasyAntReport eaReport = generateReport();
        Assert.assertNotNull(eaReport);

        // the report should contain the run-java plugin
        boolean containsBuildType = false;
        boolean containsPlugin = true;
        for (ImportedModuleReport importedModule : eaReport
                .getImportedModuleReports()) {
            if (importedModule.getModuleMrid().equals(
                    "org.apache.easyant.buildtypes#build-std-java;0.2")) {
                containsBuildType = true;
            }
            if (importedModule.getModuleMrid().equals(
                    "org.apache.easyant.plugins#run;0.1")
                    && importedModule.getAs().equals("run")) {
                containsPlugin = true;
            }
        }
        Assert.assertTrue(containsBuildType);
        Assert.assertTrue(containsPlugin);

        // be sure that the property exist
        PropertyDescriptor property = eaReport.getPropertyDescriptors().get(
                "run.main.classname");
        Assert.assertNotNull(property);
        // check the value of the property
        Assert.assertEquals("org.apache.easyant.example.Example",
                property.getValue());

        // be sure that the property exist
        PropertyDescriptor srcMainJava = eaReport.getAvailableProperties().get(
                "src.main.java");
        Assert.assertNotNull(srcMainJava);
        // check the value of the property
        Assert.assertEquals("${basedir}/src/main/java", srcMainJava.getValue());

        // the property should also be contained in getAvailableProperties which
        // list all properties (those for the current module and those in
        // imported modules)
        property = eaReport.getAvailableProperties().get("run.main.classname");
        Assert.assertNotNull(property);
        // check the value of the property
        Assert.assertEquals("org.apache.easyant.example.Example",
                property.getValue());

        // check that package phase exists and that jar:jar target is bound to
        // this phase
        PhaseReport packagePhase = null;
        for (PhaseReport phase : eaReport.getAvailablePhases()) {
            if ("package".equals(phase.getName())) {
                packagePhase = phase;
                break;
            }
        }

        Assert.assertNotNull(packagePhase);
        List<TargetReport> targets = packagePhase.getTargetReports();
        Set<String> expectedTargets = new HashSet<String>(Arrays.asList(
                "jar:jar", "test-jar:jar"));
        Assert.assertEquals("test and main jars included in package phase",
                expectedTargets.size(), targets.size());

        for (TargetReport target : packagePhase.getTargetReports()) {
            Assert.assertTrue("expected to find " + target.getName(),
                    expectedTargets.remove(target.getName()));
        }

    }

    @Test
    public void shouldHaveModuleAntFile() throws Exception {
        EasyAntReport eaReport = generateReport();
        boolean hasHelloWorldTarget = false;
        for (TargetReport targetReport : eaReport.getAvailableTargets()) {
            if ("hello-world".equals(targetReport.getName())) {
                Assert.assertTrue("process-sources".equals(targetReport
                        .getPhase()));
                hasHelloWorldTarget = true;
                break;
            }
        }
        Assert.assertTrue(hasHelloWorldTarget);
    }

    @Test
    public void testGetDescription() throws Exception {
        String description = pluginService
                .getPluginDescription("org.apache.easyant.plugins#run-java;0.1");
        Assert.assertEquals(
                "This module provides java bytecode execution feature.",
                description);
    }
}
