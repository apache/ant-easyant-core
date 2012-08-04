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
import org.apache.ivy.Ivy;
import org.apache.ivy.ant.IvyTask;
import org.apache.ivy.core.cache.DefaultRepositoryCacheManager;
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

    private String buildScopedRepositoryPrefix = "build";

    private String target;

    /**
     * Get the build scoped repository prefix
     * 
     * @return a build scoped repository prefix
     */
    public String getBuildScopedRepositoryPrefix() {
        return buildScopedRepositoryPrefix;
    }

    /**
     * Set the build scoped repository prefix
     * 
     * @param buildScopedRepositoryPrefix
     *            a build scoped repository prefix
     */
    public void setBuildScopedRepositoryPrefix(String buildScopedRepositoryPrefix) {
        this.buildScopedRepositoryPrefix = buildScopedRepositoryPrefix;
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
     * 
     * Set the target directory where both build scoped repository and cache will be instanciated
     * 
     * @param target
     *            a target directory
     */
    public void setTarget(String target) {
        this.target = target;
    }

    @Override
    public void doExecute() throws BuildException {
        String target = getTarget();
        // be sure that we have an absolute path
        File targetDir = new File(target);
        target = targetDir.getAbsolutePath();

        final String DEFAULT_BUILD_SCOPED_REPOSITORY_DIR = target + "/repository";
        final String DEFAULT_CACHE_BUILD_SCOPED_REPO = target + "/cache";
        getProject().log("Registering build scoped repository in " + DEFAULT_BUILD_SCOPED_REPOSITORY_DIR,
                Project.MSG_DEBUG);
        final String CACHENAME = "build-scoped-cache";
        // Get the project ivy instance
        IvySettings settings = getSettings();

        // Search the default resolver after the build-scoped repo
        DependencyResolver dr = settings.getDefaultResolver();
        if (dr == null) {
            throw new BuildException("Unable to find a default resolver");
        }

        // Create a cache for build scoped repository
        File cacheDir = new File(DEFAULT_CACHE_BUILD_SCOPED_REPO);
        DefaultRepositoryCacheManager rcm = new DefaultRepositoryCacheManager(CACHENAME, settings, cacheDir);
        rcm.setUseOrigin(true); // no need to copy temporary build artifacts
                                // into temporary cache.
        // Register the repository cache
        settings.addConfigured(rcm);

        // Create the build scoped repository
        FileSystemResolver buildRepository = new FileSystemResolver();
        buildRepository.setName(buildScopedRepositoryPrefix + dr.getName());
        buildRepository.addArtifactPattern(DEFAULT_BUILD_SCOPED_REPOSITORY_DIR
                + "/[organisation]/[module]/[revision]/[artifact](-[classifier]).[ext]");
        buildRepository.addIvyPattern(DEFAULT_BUILD_SCOPED_REPOSITORY_DIR
                + "/[organisation]/[module]/[revision]/[module].ivy");
        // bind to the repocache
        buildRepository.setCache(CACHENAME);

        getProject().setProperty(EasyAntMagicNames.EASYANT_BUILD_REPOSITORY, buildRepository.getName());

        // replace the default resolver with a chain resolver, which first
        // searches
        // in the build repository, then in the old default.
        ChainResolver resolver = new ChainResolver();
        resolver.setName(dr.getName()); // same name as old default
        resolver.setReturnFirst(true);
        resolver.add(buildRepository);
        resolver.add(dr);
        dr.setName("delegate." + dr.getName()); // give old default a new name

        settings.addResolver(buildRepository);
        settings.addResolver(dr);
        settings.addResolver(resolver);

    }

}
