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

import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.easyant.core.ivy.IvyInstanceHelper;
import org.apache.ivy.ant.IvyTask;
import org.apache.easyant.core.ivy.EasyAntRepositoryCacheManager;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.resolver.ChainResolver;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.FileSystemResolver;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * This task is used to configure a build scoped repository This can be particularly usefull if we are working in a
 * multi-module project with interdependencies.
 * 
 */
public class ConfigureBuildScopedRepository extends IvyTask {

    private String name;

    private String target;
    private boolean dictator = false;
    private boolean generateWrapperResoler = true;
    private String ivyPattern = "/[organisation]/[module]/[revision]/[module].ivy";
    private String artifactPattern = "/[organisation]/[module]/[revision]/[artifact](-[classifier]).[ext]";

    @Override
    public void doExecute() throws BuildException {
        String targetDirectory = getTarget();

        // be sure that we have an absolute path
        File targetDir = new File(targetDirectory);
        targetDirectory = targetDir.getAbsolutePath();

        String buildScopeRepositoryName = getName();

        final String DEFAULT_BUILD_SCOPED_REPOSITORY_DIR = targetDirectory + "/repository/" + buildScopeRepositoryName;
        final String DEFAULT_CACHE_BUILD_SCOPED_REPO = targetDirectory + "/cache/" + buildScopeRepositoryName;
        getProject().log("Registering build scoped repository in " + DEFAULT_BUILD_SCOPED_REPOSITORY_DIR,
                Project.MSG_DEBUG);
        final String CACHENAME = "build-scoped-cache-" + buildScopeRepositoryName;
        log("Registering build scope repository : " + getName() + " in " + DEFAULT_BUILD_SCOPED_REPOSITORY_DIR,
                Project.MSG_DEBUG);
        // Get the project ivy instance
        IvySettings settings = getSettings();

        // Create a cache for build scoped repository
        File cacheDir = new File(DEFAULT_CACHE_BUILD_SCOPED_REPO);
        EasyAntRepositoryCacheManager rcm = new EasyAntRepositoryCacheManager(CACHENAME, settings, cacheDir);
        rcm.setUseOrigin(true); // no need to copy temporary build artifacts
                                // into temporary cache.
        // Register the repository cache
        settings.addConfigured(rcm);

        // Create the build scoped repository
        FileSystemResolver buildRepository = new FileSystemResolver();
        buildRepository.addArtifactPattern(DEFAULT_BUILD_SCOPED_REPOSITORY_DIR + getArtifactPattern());
        buildRepository.addIvyPattern(DEFAULT_BUILD_SCOPED_REPOSITORY_DIR + getIvyPattern());

        // bind to the repocache
        buildRepository.setCache(CACHENAME);
        buildRepository.setName(buildScopeRepositoryName);
        if (isDictator()) {
            settings.setDictatorResolver(buildRepository);
        } else if (isGenerateWrapperResoler()) {

            // Search the default resolver after the build-scoped repo
            DependencyResolver dr = settings.getDefaultResolver();
            if (dr == null) {
                throw new BuildException("Unable to find a default resolver");
            }

            // replace the default resolver with a chain resolver, which first
            // searches
            // in the build repository, then in the old default.
            ChainResolver resolver = new ChainResolver();
            resolver.setName(dr.getName()); // same name as old default
            resolver.setReturnFirst(true);
            resolver.add(buildRepository);
            resolver.add(dr);
            dr.setName("delegate." + dr.getName()); // give old default a new name

            settings.addResolver(dr);
            settings.addResolver(resolver);
        }
        settings.addResolver(buildRepository);
        log(getName() + " registration succeeded", Project.MSG_DEBUG);
    }

    /**
     * Build default repository name
     * 
     * @return repository name
     */
    private String buildDefaultRepositoryName() {
        return "build." + IvyInstanceHelper.getProjectIvyInstanceName(getProject());
    }

    /**
     * Get the target directory where both build scoped repository and cache will be instanciated
     * 
     * @return a target directory
     */
    public String getTarget() {
        if (target == null) {
            target = getProject().getProperty(EasyAntMagicNames.META_TARGET);
            if (target == null) {
                target = getProject().getProperty(EasyAntMagicNames.TARGET);
            }
            if (target == null) {
                target = getProject().getBaseDir() + "/target";
            }
        }
        return target;
    }

    /**
     * Specify if this repository should be used in place of any specified resolver in {@link IvySettings}
     * 
     * @return true if acts as dictator resolver
     */
    public boolean isDictator() {
        return dictator;
    }

    /**
     * Specify if this repository should be used in place of any specified resolver in {@link IvySettings}
     * 
     * @param dictator
     *            true if acts as dictator resolver
     */
    public void setDictator(boolean dictator) {
        this.dictator = dictator;
    }

    public String getIvyPattern() {
        return ivyPattern;
    }

    public void setIvyPattern(String ivyPattern) {
        this.ivyPattern = ivyPattern;
    }

    public String getArtifactPattern() {
        return artifactPattern;
    }

    public void setArtifactPattern(String artifactPattern) {
        this.artifactPattern = artifactPattern;
    }

    /**
     * Get repository name. By default equals to "build." + ivyInstanceName
     * 
     * @return repository name
     */
    public String getName() {
        if (name == null) {
            name = buildDefaultRepositoryName();
        }
        return name;
    }

    /**
     * Set repository name
     * 
     * @param name
     *            repository name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 
     * Set the target directory where both build scoped repository and cache will be instanciated
     * 
     * @param target
     *            a target directory
     */
    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * Specify if we need to generate wrapper resolver. This is not evaluated when using as dictator. When false,
     * repository will be generated but not plugged in current {@link ChainResolver}
     * 
     * @return true if we need to generate wrapper resolver
     */
    public boolean isGenerateWrapperResoler() {
        return generateWrapperResoler;
    }

    /**
     * Specify if we need to generate wrapper resolver. This is not evaluated when using as dictator. When false,
     * repository will be generated but not plugged in current {@link ChainResolver}
     * 
     * @param generateWrapperResoler
     *            true if we need to generate wrapper resolver
     */
    public void setGenerateWrapperResoler(boolean generateWrapperResoler) {
        this.generateWrapperResoler = generateWrapperResoler;
    }
}
