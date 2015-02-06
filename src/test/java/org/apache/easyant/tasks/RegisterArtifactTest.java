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
import org.apache.easyant.core.ivy.IvyInstanceHelper;
import org.apache.easyant.core.parser.DefaultEasyAntXmlModuleDescriptorParser;
import org.apache.ivy.ant.IvyConfigure;
import org.apache.ivy.ant.IvyPublish;
import org.apache.ivy.ant.IvyResolve;
import org.apache.easyant.core.ivy.EasyAntRepositoryCacheManager;
import org.apache.easyant.core.ivy.EasyantResolutionCacheManager;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Reference;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class RegisterArtifactTest extends AntTaskBaseTest {

    private static final String MY_LOCAL_RESOLVER = "my-local-resolver";

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

    @Test
    public void shouldRegisterArtifactAndPublish() throws IOException {
        configureParserAndCacheManagers();

        Reference easyAntIvyReference = IvyInstanceHelper.buildEasyAntIvyReference(registerArtifact.getProject());
        configureLocalRepository();

        resolveModule(easyAntIvyReference, new File(this.getClass().getResource("simple/module.ivy").getFile()));

        String artifactName = "my-artifact-name";
        File artifact = folder.newFile(artifactName + ".jar");
        folder.newFile("standard-java-app.jar");

        registerArtifact.setSettingsRef(easyAntIvyReference);
        registerArtifact.setName(artifactName);
        registerArtifact.setExt("jar");
        registerArtifact.execute();

        publishToLocalRepository(easyAntIvyReference, artifact.getParent());
        ResolveReport resolveReport = registerArtifact.getProject().getReference("ivy.resolved.report");
        assertNotNull(resolveReport);
        assertEquals(2, resolveReport.getModuleDescriptor().getAllArtifacts().length);
        assertEquals("standard-java-app", resolveReport.getModuleDescriptor().getAllArtifacts()[0].getName());
        assertEquals("jar", resolveReport.getModuleDescriptor().getAllArtifacts()[0].getExt());
        assertEquals("jar", resolveReport.getModuleDescriptor().getAllArtifacts()[0].getType());
        assertEquals("my-artifact-name", resolveReport.getModuleDescriptor().getAllArtifacts()[1].getName());
        assertEquals("jar", resolveReport.getModuleDescriptor().getAllArtifacts()[1].getExt());
        assertEquals("jar", resolveReport.getModuleDescriptor().getAllArtifacts()[1].getType());

    }

    private void configureParserAndCacheManagers() {
        ModuleDescriptorParserRegistry.getInstance().addParser(new DefaultEasyAntXmlModuleDescriptorParser());
        IvySettings settings = IvyInstanceHelper.getEasyAntIvyAntSettings(registerArtifact.getProject())
                .getConfiguredIvyInstance(registerArtifact).getSettings();
        // FIXME: hack as ResolutionCacheManager use XmlModuleDescriptorParser under the hood
        EasyAntRepositoryCacheManager cacheManager = new EasyAntRepositoryCacheManager("default-project-cache",
                settings, settings.getDefaultCache());
        settings.setDefaultRepositoryCacheManager(cacheManager);

        EasyantResolutionCacheManager resolutionCacheManager = new EasyantResolutionCacheManager();
        resolutionCacheManager.setBasedir(settings.getDefaultResolutionCacheBasedir());
        resolutionCacheManager.setSettings(settings);
        settings.setResolutionCacheManager(resolutionCacheManager);
    }

    private void resolveModule(Reference easyAntIvyReference, File ivyFile) {
        IvyResolve resolve = new IvyResolve();
        resolve.setSettingsRef(easyAntIvyReference);
        resolve.setProject(registerArtifact.getProject());
        resolve.setFile(ivyFile);
        resolve.execute();
    }

    private void publishToLocalRepository(Reference easyAntIvyReference, String artifactFolder) {
        IvyPublish publish = new IvyPublish();
        publish.setSettingsRef(easyAntIvyReference);
        publish.setProject(registerArtifact.getProject());
        publish.setResolver(MY_LOCAL_RESOLVER);
        publish.setArtifactspattern(artifactFolder + "/[artifact].[ext]");
        publish.setHaltonmissing(true);
        publish.setStatus("integration");
        publish.execute();
    }

    private void configureLocalRepository() throws IOException {
        ConfigureBuildScopedRepository localRepository = new ConfigureBuildScopedRepository();
        localRepository.setSettingsRef(IvyInstanceHelper.buildEasyAntIvyReference(registerArtifact.getProject()));
        localRepository.setProject(registerArtifact.getProject());
        localRepository.setName(MY_LOCAL_RESOLVER);
        localRepository.setTarget(folder.newFolder("local-repo").getAbsolutePath());
        localRepository.execute();
    }
}
