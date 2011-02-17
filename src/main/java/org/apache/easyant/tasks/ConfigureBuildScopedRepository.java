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
import org.apache.ivy.ant.IvyTask;
import org.apache.ivy.core.cache.DefaultRepositoryCacheManager;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.resolver.ChainResolver;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.FileSystemResolver;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * This task is used to configure a build scoped repository This can be
 * particularly usefull if we are working in a multi-module project with
 * interdependencies.
 * 
 */
public class ConfigureBuildScopedRepository extends IvyTask {

	private String buildScopedRepositoryName = "build";
	private boolean dictator=false;

	private String target;

	/**
	 * Get the build scoped repository name
	 * 
	 * @return a build scoped repository name
	 */
	public String getBuildScopedRepositoryName() {
		return buildScopedRepositoryName;
	}

	/**
	 * Set the build scoped repository name
	 * 
	 * @param buildScopedRepositoryName
	 *            a build scoped repository name
	 */
	public void setBuildScopedRepositoryName(String buildScopedRepositoryName) {
		this.buildScopedRepositoryName = buildScopedRepositoryName;
	}

	/**
	 * Get the target directory where both build scoped repository and cache
	 * will be instanciated
	 * 
	 * @return a target directory
	 */
	public String getTarget() {
		if (target == null) {
			target = getProject().getProperty(EasyAntMagicNames.TARGET);
			if (target == null) {
				target = getProject().getBaseDir() + "/target";
			}
		}
		return target;
	}

	/**
	 * 
	 * Set the target directory where both build scoped repository and cache
	 * will be instanciated
	 * 
	 * @param target
	 *            a target directory
	 */
	public void setTarget(String target) {
		this.target = target;
	}
	
	/**
	 * Is the build scoped repository a dictator resolver?
	 * @return true if build scoped repository is a dictator resolver
	 */
	public boolean isDictator() {
		return dictator;
	}

	/**
	 * Set the build scoped repository as a dictator resolver
	 * @param dictator true if build scoped repository is a dictator resolver
	 */
	public void setDictator(boolean dictator) {
		this.dictator = dictator;
	}

	@Override
	public void doExecute() throws BuildException {
		String target = getTarget();
		// be sure that we have an absolute path
		File targetDir = new File(target);
		target = targetDir.getAbsolutePath();

		final String DEFAULT_BUILD_SCOPED_REPOSITORY_DIR = target
				+ "/repository";
		final String DEFAULT_CACHE_BUILD_SCOPED_REPO = target + "/cache";
		getProject().log(
				"Registering build scoped repository in "
						+ DEFAULT_BUILD_SCOPED_REPOSITORY_DIR,
				Project.MSG_DEBUG);
		final String CACHENAME = "build-scoped-cache";
		// Get the project ivy instance
		IvySettings settings = getSettings();

		// Create a cache for build scoped repository
		File cacheDir = new File(DEFAULT_CACHE_BUILD_SCOPED_REPO);
		DefaultRepositoryCacheManager rcm = new DefaultRepositoryCacheManager(
				CACHENAME, settings, cacheDir);
		rcm.setUseOrigin(true); // no need to copy temporary build artifacts
								// into temporary cache.
		// Register the repository cache
		settings.addConfigured(rcm);

		// Create the build scoped repository
		FileSystemResolver buildRepository = new FileSystemResolver();
		buildRepository.setName(buildScopedRepositoryName);
		buildRepository
				.addArtifactPattern(DEFAULT_BUILD_SCOPED_REPOSITORY_DIR
						+ "/[organisation]/[module]/[revision]/[artifact](-[classifier]).[ext]");
		buildRepository.addIvyPattern(DEFAULT_BUILD_SCOPED_REPOSITORY_DIR
				+ "/[organisation]/[module]/[revision]/[module].ivy");
		// bind to the repocache
		buildRepository.setCache(CACHENAME);
		settings.addResolver(buildRepository);

		// Bind to the default resolver
		DependencyResolver dr = settings.getDefaultResolver();
		if (dr == null) {
			throw new BuildException("Unable to find a default resolver");
		}

		// wrap the default resolver in a chain resolver, which first checks
		// the build-scoped repository.
		String globalResolver = buildScopedRepositoryName + ".wrapper";

		ChainResolver resolver = new ChainResolver();
		resolver.setName(globalResolver);
		resolver.setReturnFirst(true);
		resolver.add(buildRepository);
		resolver.add(dr);

		settings.addResolver(resolver);
		if (dictator)
			settings.setDictatorResolver(resolver);

	}

}
