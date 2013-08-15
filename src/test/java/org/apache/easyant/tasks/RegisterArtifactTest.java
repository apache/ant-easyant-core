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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.ivy.ant.IvyConfigure;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RegisterArtifactTest {
    private File cache;

    private RegisterArtifact registerArtifact;

    @Before
    public void setUp() throws MalformedURLException, URISyntaxException {
        createCache();
        Project project = new Project();

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

    private void createCache() {
        cache = new File("build/cache");
        cache.mkdirs();
    }

    @After
    public void tearDown() throws Exception {
        cleanCache();
    }

    private void cleanCache() {
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(cache);
        del.execute();
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
        Assert.assertNotNull(resolveReport);
        Assert.assertEquals(1, resolveReport.getModuleDescriptor().getAllArtifacts().length);
        Assert.assertEquals("my-artifact-name", resolveReport.getModuleDescriptor().getAllArtifacts()[0].getName());
        Assert.assertEquals("my-ext", resolveReport.getModuleDescriptor().getAllArtifacts()[0].getExt());
        Assert.assertEquals("my-type", resolveReport.getModuleDescriptor().getAllArtifacts()[0].getType());
        String classifierAttribute = resolveReport.getModuleDescriptor().getAllArtifacts()[0]
                .getExtraAttribute("classifier");
        Assert.assertNotNull(classifierAttribute);

        Assert.assertEquals("my-classifier", classifierAttribute);
    }
}
