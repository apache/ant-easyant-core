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
import org.apache.ivy.core.report.ResolveReport;
import org.apache.tools.ant.Project;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class RegisterArtifactTest {

    private RegisterArtifact registerArtifact;

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

        registerArtifact = new RegisterArtifact();
        registerArtifact.setProject(project);
    }

    @Test
    public void shouldRegisterArtifact() {
        registerArtifact.setInline(true);
        registerArtifact.setOrganisation("mycompany");
        registerArtifact.setModule("simpleplugin");
        registerArtifact.setRevision("0.1");
        registerArtifact.setResolveId("myResolve");

        registerArtifact.setName("my-artifact-name");
        registerArtifact.setExt("my-ext");
        registerArtifact.setType("my-type");

        registerArtifact.setClassifier("my-classifier");
        registerArtifact.execute();

        ResolveReport resolveReport = registerArtifact.getProject().getReference("ivy.resolved.report.myResolve");
        assertNotNull(resolveReport);
        assertEquals(1, resolveReport.getModuleDescriptor().getAllArtifacts().length);
        assertEquals("my-artifact-name", resolveReport.getModuleDescriptor().getAllArtifacts()[0].getName());
        assertEquals("my-ext", resolveReport.getModuleDescriptor().getAllArtifacts()[0].getExt());
        assertEquals("my-type", resolveReport.getModuleDescriptor().getAllArtifacts()[0].getType());
        String classifierAttribute = resolveReport.getModuleDescriptor().getAllArtifacts()[0]
                .getExtraAttribute("classifier");
        assertNotNull(classifierAttribute);

        assertEquals("my-classifier", classifierAttribute);
    }
}
