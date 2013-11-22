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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.ivy.ant.IvyConfigure;
import org.apache.ivy.ant.IvyDependency;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.tools.ant.Project;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ResolvePluginsTest {

    private ResolvePlugins resolvePlugins;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setUp() throws URISyntaxException, IOException {
        Project project = new Project();

        File cache = folder.newFolder("build-cache");
        project.setProperty("ivy.cache.dir", cache.getAbsolutePath());

        IvyConfigure configure = new IvyConfigure();
        configure.setProject(project);

        File f = new File(this.getClass().getResource("/repositories/easyant-ivysettings-test.xml").toURI());
        configure.setFile(f);

        configure.setSettingsId(EasyAntMagicNames.EASYANT_IVY_INSTANCE);
        configure.execute();

        resolvePlugins = new ResolvePlugins();
        resolvePlugins.setProject(project);
    }

    @Test
    public void shouldCreateEmptyResolveReport() {
        resolvePlugins.execute();
        ResolveReport report = resolvePlugins.getProject().getReference(
                EasyAntMagicNames.IMPORTED_MODULES_RESOLVE_REPORT_REF);
        assertNotNull(report);
        assertEquals(0, report.getDependencies().size());
    }

    @Test
    public void shouldContainUnresolvedDependency() {
        IvyDependency dependency = resolvePlugins.createDependency();
        dependency.setOrg("does");
        dependency.setName("not");
        dependency.setRev("exist");
        resolvePlugins.execute();
        ResolveReport report = resolvePlugins.getProject().getReference(
                EasyAntMagicNames.IMPORTED_MODULES_RESOLVE_REPORT_REF);
        assertNotNull(report);
        assertEquals(1, report.getDependencies().size());
        assertEquals(1, report.getUnresolvedDependencies().length);
    }

    @Test
    public void shouldContainResolvedDependency() {
        IvyDependency dependency = resolvePlugins.createDependency();
        dependency.setOrg("mycompany");
        dependency.setName("simpleplugin");
        dependency.setRev("0.1");
        resolvePlugins.execute();
        ResolveReport report = resolvePlugins.getProject().getReference(
                EasyAntMagicNames.IMPORTED_MODULES_RESOLVE_REPORT_REF);
        assertNotNull(report);
        assertEquals(1, report.getDependencies().size());
        assertEquals(0, report.getUnresolvedDependencies().length);
    }

}
