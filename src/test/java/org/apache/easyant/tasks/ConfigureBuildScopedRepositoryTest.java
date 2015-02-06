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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.easyant.core.ant.ProjectUtils;
import org.apache.easyant.core.ivy.IvyInstanceHelper;
import org.apache.ivy.Ivy;
import org.apache.ivy.ant.IvyConfigure;
import org.apache.easyant.core.ivy.EasyAntRepositoryCacheManager;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.FileSystemResolver;
import org.apache.tools.ant.Project;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ConfigureBuildScopedRepositoryTest {
    private ConfigureBuildScopedRepository configureBuildScopeRepository = new ConfigureBuildScopedRepository();
    private Ivy configuredIvyInstance;
    private int originalNbResolvers;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException, URISyntaxException {
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

        project.setProperty(EasyAntMagicNames.PROJECT_IVY_INSTANCE, EasyAntMagicNames.EASYANT_IVY_INSTANCE);

        configureBuildScopeRepository = new ConfigureBuildScopedRepository();
        configureBuildScopeRepository.setSettingsRef(IvyInstanceHelper.buildEasyAntIvyReference(project));
        configureBuildScopeRepository.setProject(project);

        configuredIvyInstance = IvyInstanceHelper.getIvyAntSettings(configureBuildScopeRepository.getProject(),
                EasyAntMagicNames.EASYANT_IVY_INSTANCE).getConfiguredIvyInstance(configureBuildScopeRepository);
        originalNbResolvers = configuredIvyInstance.getSettings().getResolvers().size();

    }

    @Test
    public void shouldCreateBuildScopeRepository() {
        configureBuildScopeRepository.execute();
        String resolverName = buildDefaultResolverName();

        verifyWrapper(configuredIvyInstance, originalNbResolvers);

        verifyResolver(configuredIvyInstance, resolverName);
    }

    @Test
    public void shouldCreateBuildScopeRepositoryWithGivenName() {
        String resolverName = "my-build-scope-repository";

        configureBuildScopeRepository.setName(resolverName);
        configureBuildScopeRepository.execute();

        verifyWrapper(configuredIvyInstance, originalNbResolvers);
        verifyResolver(configuredIvyInstance, resolverName);
    }

    @Test
    public void shouldCreateBuildScopeRepositoryWithoutWrapper() {
        String resolverName = buildDefaultResolverName();
        configureBuildScopeRepository.setGenerateWrapperResoler(false);
        configureBuildScopeRepository.execute();

        assertThat(configuredIvyInstance.getSettings().getDefaultResolver().getName(), equalTo("test-plugin"));
        assertThat(configuredIvyInstance.getSettings().getResolvers().size(), equalTo(originalNbResolvers + 1));

        verifyResolver(configuredIvyInstance, resolverName);
    }

    @Test
    public void shouldCreateDictatorBuildScopeRepository() {
        String resolverName = buildDefaultResolverName();
        configureBuildScopeRepository.setDictator(true);
        configureBuildScopeRepository.execute();

        assertThat(configuredIvyInstance.getSettings().getDefaultResolver().getName(), equalTo(resolverName));
        assertThat(configuredIvyInstance.getSettings().getResolvers().size(), equalTo(originalNbResolvers + 1));

        verifyResolver(configuredIvyInstance, resolverName);
    }

    @Test
    public void shouldCreateBuildScopeRepositoryWithCustomPattern() {
        String resolverName = buildDefaultResolverName();
        configureBuildScopeRepository.setArtifactPattern("a-custom-pattern");
        configureBuildScopeRepository.setIvyPattern("a-custom-pattern");
        configureBuildScopeRepository.execute();

        verifyWrapper(configuredIvyInstance, originalNbResolvers);
        verifyResolver(configuredIvyInstance, resolverName);
    }

    private String buildDefaultResolverName() {
        return "build."
                + IvyInstanceHelper.getProjectIvyInstanceName(configureBuildScopeRepository.getProject());
    }

    @Test
    public void shouldCreateBuildScopeRepositoryWithGivenTarget() {
        String resolverName = buildDefaultResolverName();
        configureBuildScopeRepository.setTarget(configureBuildScopeRepository.getProject().getBaseDir()
                .getAbsolutePath()
                + "a-target-directory");
        configureBuildScopeRepository.execute();

        verifyWrapper(configuredIvyInstance, originalNbResolvers);
        verifyResolver(configuredIvyInstance, resolverName);
    }

    @Test
    public void shouldCreateBuildScopeRepositoryWithDefaultTarget() {
        String resolverName = buildDefaultResolverName();
        configureBuildScopeRepository.execute();

        assertThat(configureBuildScopeRepository.getTarget(), equalTo(configureBuildScopeRepository.getProject()
                .getBaseDir() + "/target"));

        verifyWrapper(configuredIvyInstance, originalNbResolvers);
        verifyResolver(configuredIvyInstance, resolverName);
    }

    @Test
    public void shouldCreateBuildScopeRepositoryWithTargetSetAsProjectAttribute() {
        Ivy configuredIvyInstance = IvyInstanceHelper.getIvyAntSettings(configureBuildScopeRepository.getProject(),
                EasyAntMagicNames.EASYANT_IVY_INSTANCE).getConfiguredIvyInstance(configureBuildScopeRepository);
        int originalNbResolvers = configuredIvyInstance.getSettings().getResolvers().size();
        String resolverName = buildDefaultResolverName();

        configureBuildScopeRepository.getProject().setNewProperty(EasyAntMagicNames.TARGET,
                configureBuildScopeRepository.getProject().getBaseDir() + "/mytarget");
        configureBuildScopeRepository.execute();

        assertThat(configureBuildScopeRepository.getTarget(), equalTo(configureBuildScopeRepository.getProject()
                .getBaseDir() + "/mytarget"));

        verifyWrapper(configuredIvyInstance, originalNbResolvers);
        verifyResolver(configuredIvyInstance, resolverName);
    }

    @Test
    public void shouldCreateBuildScopeRepositoryWithMetaTargetSetAsProjectAttribute() {
        Ivy configuredIvyInstance = IvyInstanceHelper.getIvyAntSettings(configureBuildScopeRepository.getProject(),
                EasyAntMagicNames.EASYANT_IVY_INSTANCE).getConfiguredIvyInstance(configureBuildScopeRepository);
        int originalNbResolvers = configuredIvyInstance.getSettings().getResolvers().size();
        String resolverName = buildDefaultResolverName();

        configureBuildScopeRepository.getProject().setNewProperty(EasyAntMagicNames.META_TARGET,
                configureBuildScopeRepository.getProject().getBaseDir() + "/metatarget");
        configureBuildScopeRepository.execute();

        assertThat(configureBuildScopeRepository.getTarget(), equalTo(configureBuildScopeRepository.getProject()
                .getBaseDir() + "/metatarget"));

        verifyWrapper(configuredIvyInstance, originalNbResolvers);
        verifyResolver(configuredIvyInstance, resolverName);
    }

    private void verifyResolver(Ivy configuredIvyInstance, String resolverName) {
        DependencyResolver resolver = configuredIvyInstance.getSettings().getResolver(resolverName);
        assertThat(resolver, notNullValue());
        assertThat(resolver, instanceOf(FileSystemResolver.class));

        FileSystemResolver fsr = (FileSystemResolver) resolver;
        String repositoryBasedir = configureBuildScopeRepository.getTarget() + "/repository/" + resolverName;

        assertThat((String) fsr.getArtifactPatterns().get(0),
                equalTo(repositoryBasedir + configureBuildScopeRepository.getArtifactPattern()));
        assertThat((String) fsr.getIvyPatterns().get(0),
                equalTo(repositoryBasedir + configureBuildScopeRepository.getIvyPattern()));

        assertThat(resolver.getRepositoryCacheManager(), instanceOf(EasyAntRepositoryCacheManager.class));
        assertThat(resolver, notNullValue());
    }

    private void verifyWrapper(Ivy configuredIvyInstance, int originalNbResolvers) {
        assertThat(configuredIvyInstance.getSettings().getDefaultResolver().getName(), equalTo("delegate.test-plugin"));
        // buildscope repository + wrapper
        assertThat(configuredIvyInstance.getSettings().getResolvers().size(), equalTo(originalNbResolvers + 2));
    }

}
