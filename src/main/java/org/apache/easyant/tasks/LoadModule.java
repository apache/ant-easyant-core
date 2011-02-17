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
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.easyant.core.BuildConfigurationHelper;
import org.apache.easyant.core.EasyAntConstants;
import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.easyant.core.descriptor.AdvancedInheritableItem;
import org.apache.easyant.core.descriptor.EasyAntModuleDescriptor;
import org.apache.easyant.core.descriptor.PhaseMappingDescriptor;
import org.apache.easyant.core.descriptor.PluginDescriptor;
import org.apache.easyant.core.descriptor.PropertyDescriptor;
import org.apache.easyant.core.ivy.InheritableScope;
import org.apache.easyant.core.ivy.IvyInstanceHelper;
import org.apache.easyant.core.parser.DefaultEasyAntXmlModuleDescriptorParser;
import org.apache.easyant.core.parser.EasyAntModuleDescriptorParser;
import org.apache.ivy.Ivy;
import org.apache.ivy.ant.IvyAntSettings;
import org.apache.ivy.ant.IvyConfigure;
import org.apache.ivy.ant.IvyInfo;
import org.apache.ivy.core.cache.DefaultRepositoryCacheManager;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.ivy.plugins.repository.url.URLResource;
import org.apache.ivy.plugins.resolver.ChainResolver;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.FileSystemResolver;
import org.apache.ivy.util.StringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.ImportTask;

/**
 * This task is the main class, used to parse module.ivy and execute the all the
 * statement behind the easyant tag.
 */
public class LoadModule extends AbstractEasyAntTask {

	private File buildFile;
	private File buildModule;
	private String easyAntMDParserClassName;
	private Boolean useBuildRepository;

	/**
	 * Get the file name that will be loaded
	 * 
	 * @return a file that represents a module descriptor
	 */
	public File getBuildModule() {
		return buildModule;
	}

	/**
	 * set the file name that will be loaded
	 * 
	 * @param file
	 *            represents a module descriptor
	 */
	public void setBuildModule(File file) {
		this.buildModule = file;
	}

	public File getBuildFile() {
		return buildFile;
	}

	public void setBuildFile(File buildFile) {
		this.buildFile = buildFile;
	}

	public void setUseBuildRepository(boolean value) {
		this.useBuildRepository = value;
	}

	/**
	 * Set the classname of the easyant parser you want to use
	 * 
	 * @param easyAntMDParserClassName
	 */
	public void setEasyAntMDParserClassName(String easyAntMDParserClassName) {
		this.easyAntMDParserClassName = easyAntMDParserClassName;
	}

	public void execute() throws BuildException {
		if (buildModule != null && buildModule.exists()) {
			IvyInfo info = new IvyInfo();
			info.setFile(buildModule);
			// Not sure we should bound IvyInfo to easyantIvyInstance
			info.setSettingsRef(IvyInstanceHelper
					.buildEasyAntIvyReference(getProject()));
			initTask(info).execute();
			getProject().setName(getProject().getProperty("ivy.module"));
		}
		if (buildModule != null && buildModule.exists()) {
			// make sure it's not a directory (this falls into the ultra
			// paranoid lets check everything category

			if (buildModule.isDirectory()) {
				System.out.println("What? buildModule: " + buildModule
						+ " is a dir!");
				throw new BuildException("Build failed");
			}
			// load override buildFile before buildModule to allow target/phase
			// override
			File f = new File(buildModule.getParent(),
					EasyAntConstants.DEFAULT_OVERRIDE_BUILD_FILE);
			if (f.exists()) {
				log("Loading override build file : "
						+ buildFile.getAbsolutePath());
				loadBuildFile(f);
			}

			log("Loading build module : " + buildModule.getAbsolutePath());
			loadBuildModule(buildModule);
		}

		// load buildFile before buildModule to allow target/phase override
		if (buildFile != null && buildFile.exists()) {
			// make sure it's not a directory (this falls into the ultra
			// paranoid lets check everything category

			if (buildFile.isDirectory()) {
				System.out.println("What? buildFile: " + buildFile
						+ " is a dir!");
				throw new BuildException("Build failed");
			}

			log("Loading build file : " + buildFile.getAbsolutePath());
			loadBuildFile(buildFile);
		}

		String projectIvyInstanceProp = IvyInstanceHelper
				.getProjectIvyInstanceName(getProject());

		// create project ivy instance except if project ivy instance is linked
		// to easyant ivy instance
		if (!EasyAntMagicNames.EASYANT_IVY_INSTANCE
				.equals(projectIvyInstanceProp)) {
			configureProjectIvyinstance(projectIvyInstanceProp);

		}

		if (shouldUseBuildRepository()) {
			configureBuildRepository(IvyInstanceHelper
					.getProjectIvyAntSettings(getProject()));
		}

		if (getProject().getDefaultTarget() == null
				&& getProject().getTargets().containsKey(
						EasyAntConstants.DEFAULT_TARGET)) {
			getProject().setDefault(EasyAntConstants.DEFAULT_TARGET);
		}
	}

	/**
	 * 
	 */
	private void configureProjectIvyinstance(String projectIvyInstanceName) {
		IvyConfigure projectIvyInstance = new IvyConfigure();
		projectIvyInstance.setSettingsId(projectIvyInstanceName);
		if (getProject()
				.getProperty(EasyAntMagicNames.PROJECT_IVY_SETTING_FILE) != null) {
			File projectIvyFile = new File(getProject().getProperty(
					EasyAntMagicNames.PROJECT_IVY_SETTING_FILE));
			projectIvyInstance.setFile(projectIvyFile);
		}
		if (getProject().getProperty(EasyAntMagicNames.PROJECT_IVY_SETTING_URL) != null) {
			String url = getProject().getProperty(
					EasyAntMagicNames.PROJECT_IVY_SETTING_URL);
			try {
				projectIvyInstance.setUrl(url);
			} catch (MalformedURLException malformedUrl) {
				throw new BuildException(
						"Unable to parse project ivysettings from the following url : "
								+ url, malformedUrl);
			}
		}
		String defaultUrl = this.getClass().getResource(
				"/org/apache/easyant/core/default-project-ivysettings.xml")
				.toExternalForm();
		getProject().setNewProperty(
				EasyAntMagicNames.PROJECT_DEFAULT_IVYSETTINGS, defaultUrl);
		if (getProject()
				.getProperty(EasyAntMagicNames.PROJECT_IVY_SETTING_FILE) == null
				&& getProject().getProperty(
						EasyAntMagicNames.PROJECT_IVY_SETTING_URL) == null) {
			File localSettings = new File(buildModule.getParent(),
					"ivysettings.xml");
			if (localSettings.exists()) {
				getProject().log("loading local project settings file...",
						Project.MSG_VERBOSE);
				projectIvyInstance.setFile(localSettings);
				getProject().setNewProperty(
						EasyAntMagicNames.PROJECT_IVY_SETTING_FILE,
						localSettings.getAbsolutePath());

			} else {
				getProject().log("no settings file found, using default...",
						Project.MSG_VERBOSE);
				getProject().setNewProperty(
						EasyAntMagicNames.PROJECT_IVY_SETTING_URL,
						defaultUrl.toString());
				try {
					projectIvyInstance.setUrl(defaultUrl);
				} catch (MalformedURLException e) {
					throw new BuildException(
							"Unable to parse project ivysettings from the following url : "
									+ defaultUrl, e);
				}
			}
		}
		initTask(projectIvyInstance).execute();
	}

	protected void loadBuildFile(File buildModule) {
		ImportTask importTask = new ImportTask();
		importTask.setFile(buildModule.getAbsolutePath());
		importTask.setOptional(true);
		initTask(importTask).execute();
	}

	protected void loadBuildModule(File buildModule) {
		EasyAntModuleDescriptorParser parser = getEasyAntModuleDescriptorParser(buildModule);
		log(
				"Loading EasyAnt module descriptor :"
						+ parser.getClass().getName(), Project.MSG_DEBUG);

		try {
			parser.setActiveBuildConfigurations(getProject().getProperty(
					EasyAntMagicNames.ACTIVE_BUILD_CONFIGURATIONS));
			parser.parseDescriptor(getEasyAntIvyInstance().getSettings(),
					buildModule.toURL(), new URLResource(buildModule.toURL()),
					true);
			EasyAntModuleDescriptor md = parser.getEasyAntModuleDescriptor();
			ModuleRevisionId currentModule = md.getIvyModuleDescriptor()
					.getModuleRevisionId();

			String buildConfigurations = null;
			for (String conf : md.getBuildConfigurations()) {
				if (buildConfigurations == null) {
					buildConfigurations = conf;
				} else {
					buildConfigurations = buildConfigurations + "," + conf;
				}
			}
			getProject().setProperty(
					EasyAntMagicNames.AVAILABLE_BUILD_CONFIGURATIONS,
					buildConfigurations);
			updateMainConfs();

			for (Iterator<PropertyDescriptor> iterator = md.getProperties()
					.values().iterator(); iterator.hasNext();) {
				PropertyDescriptor property = iterator.next();
				if (canInherit(property, currentModule)) {
					PropertyTask propTask = new PropertyTask();
					propTask.setName(property.getName());
					propTask.setValue(property.getValue());
					propTask.setBuildConfigurations(property
							.getBuildConfigurations());
					initTask(propTask).execute();
				}
			}
			if (md.getBuildType() != null) {
				Import importTask = new Import();
				importTask.setMrid(md.getBuildType());
				initTask(importTask).execute();
			}
			for (Iterator<?> iterator = md.getPlugins().iterator(); iterator
					.hasNext();) {
				PluginDescriptor plugin = (PluginDescriptor) iterator.next();

				if (canInherit(plugin, currentModule)) {
					Import importTask = new Import();
					importTask.setMrid(plugin.getMrid());
					importTask.setMode(plugin.getMode());
					importTask.setAs(plugin.getAs());
					importTask.setMandatory(plugin.isMandatory());
					importTask.setBuildConfigurations(plugin
							.getBuildConfigurations());
					initTask(importTask).execute();
				}
			}
			// Apply PhaseMapping
			for (PhaseMappingDescriptor phaseMapping : md.getPhaseMappings()) {
				BindTarget bindTarget = new BindTarget();
				bindTarget.setTarget(phaseMapping.getTarget());
				bindTarget.setToPhase(phaseMapping.getToPhase());
				bindTarget.setBuildConfigurations(phaseMapping
						.getBuildConfigurations());
				initTask(bindTarget).execute();
			}
		} catch (Exception e) {
			throw new BuildException("problem while parsing Ivy module file: "
					+ e.getMessage(), e);
		}
	}

	/**
	 * Check if an inheritable item can be inherited by verifying
	 * {@link InheritableScope}
	 * 
	 * @param inheritableItem
	 *            a given {@link AdvancedInheritableItem}
	 * @param currentModule
	 *            current module
	 * @return true if item can be inherited
	 */
	private boolean canInherit(AdvancedInheritableItem inheritableItem,
			ModuleRevisionId currentModule) {
		if (currentModule.equals(inheritableItem.getSourceModule())) {
			return !InheritableScope.CHILD.equals(inheritableItem
					.getInheritScope()); 
		} else {
			return true;
		}
	
	}

	/**
	 * This method is in charge to update the main.confs property with all the
	 * active build configuration for the current project.
	 */
	private void updateMainConfs() {
		if (getProject().getProperty(
				EasyAntMagicNames.AVAILABLE_BUILD_CONFIGURATIONS) == null
				|| getProject().getProperty(
						EasyAntMagicNames.ACTIVE_BUILD_CONFIGURATIONS) == null) {
			return;
		}

		List<String> availableBuildConfigurations = Arrays.asList(getProject()
				.getProperty(EasyAntMagicNames.AVAILABLE_BUILD_CONFIGURATIONS)
				.split(","));
		// remove spaces in active confs
		String activeConfs = getProject().getProperty(
				EasyAntMagicNames.ACTIVE_BUILD_CONFIGURATIONS);
		List<String> activeBuildConfigurations = BuildConfigurationHelper
				.buildList(activeConfs);
		List<String> mainConfsList = new ArrayList<String>();
		for (String conf : activeBuildConfigurations) {
			if (availableBuildConfigurations.contains(conf)) {
				mainConfsList.add(conf);
			} else {
				log("removing unused configuration " + conf, Project.MSG_DEBUG);
			}
		}
		if (mainConfsList.size() > 0) {
			String mainConfs = StringUtils.join(mainConfsList
					.toArray(new String[0]), ",");
			log("updating main.confs with active profile for current project :"
					+ mainConfs, Project.MSG_DEBUG);
			getProject().setProperty(EasyAntMagicNames.MAIN_CONFS, mainConfs);
		}

	}

	protected EasyAntModuleDescriptorParser getEasyAntModuleDescriptorParser(
			File file) throws BuildException {
		ModuleDescriptorParser mdp = null;
		EasyAntModuleDescriptorParser parser = null;
		try {
			mdp = ModuleDescriptorParserRegistry.getInstance().getParser(
					new URLResource(file.toURL()));
		} catch (MalformedURLException e) {
			throw new BuildException("Impossible to find a parser for "
					+ file.getName());
		}
		// If valid easyant parser is defined use it
		if (mdp != null
				&& mdp.getClass().isInstance(
						EasyAntModuleDescriptorParser.class)) {
			return (EasyAntModuleDescriptorParser) mdp;
		} else {
			// if the user has customized the loadmodule task
			if (easyAntMDParserClassName != null) {

				try {
					Class<? extends EasyAntModuleDescriptorParser> c = Class
							.forName(easyAntMDParserClassName).asSubclass(
									EasyAntModuleDescriptorParser.class);
					log("Creating instance of " + easyAntMDParserClassName,
							Project.MSG_DEBUG);
					parser = c.newInstance();
					ModuleDescriptorParserRegistry.getInstance().addParser(
							parser);
					return parser;
				} catch (Exception e) {
					throw new BuildException("Unable to load "
							+ easyAntMDParserClassName, e);
				}

			}
			// the default one
			log("Creating instance of "
					+ DefaultEasyAntXmlModuleDescriptorParser.class.getName(),
					Project.MSG_DEBUG);
			parser = new DefaultEasyAntXmlModuleDescriptorParser();

			ModuleDescriptorParserRegistry.getInstance().addParser(parser);
			return parser;

		}
	}

	/**
	 * @return true if this module should use a build-scoped repository and
	 *         cache to find artifacts generated by other modules in the same
	 *         build.
	 */
	private boolean shouldUseBuildRepository() {
		// if a value has been provided by task attribute, return it
		if (useBuildRepository != null) {
			return useBuildRepository;
		}
		// otherwise, look for a value in property configuration, defaulting to
		// false if no value.
		return Project.toBoolean(getProject().getProperty(
				EasyAntMagicNames.USE_BUILD_REPOSITORY));
	}

	/**
	 * Change the given Ivy settings to use a local build-scoped repository and
	 * cache by default. This allows submodules to access each others' artifacts
	 * before they have been published to a shared repository.
	 */
	private void configureBuildRepository(IvyAntSettings projectSettings)
			throws BuildException {
		String target = getProject().getProperty(EasyAntMagicNames.META_TARGET);
		if (target == null) {
			target = getProject().getProperty(EasyAntMagicNames.TARGET);
		}
		if (target == null) {
			target = getProject().getBaseDir() + "/target";
		}

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
		Ivy ivy = projectSettings.getConfiguredIvyInstance(this);
		IvySettings settings = ivy.getSettings();

		// Search the default resolver after the build-scoped repo
		DependencyResolver dr = settings.getDefaultResolver();
		if (dr == null) {
			throw new BuildException("Unable to find a default resolver");
		}
		resetDefaultResolver(settings);

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
		buildRepository.setName("build." + dr.getName());
		buildRepository
				.addArtifactPattern(DEFAULT_BUILD_SCOPED_REPOSITORY_DIR
						+ "/[organisation]/[module]/[revision]/[artifact](-[classifier]).[ext]");
		buildRepository.addIvyPattern(DEFAULT_BUILD_SCOPED_REPOSITORY_DIR
				+ "/[organisation]/[module]/[revision]/[module].ivy");
		// bind to the repocache
		buildRepository.setCache(CACHENAME);

		getProject().setProperty(EasyAntMagicNames.EASYANT_BUILD_REPOSITORY,
				buildRepository.getName());

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

	/**
	 * Clear the default resolver on the given IvySettings. This is a workaround
	 * for <a href="http://issues.apache.org/jira/browse/IVY-1163">Ivy issue
	 * 1163</a>. This code should be removed when the issue is resolved.
	 */
	private void resetDefaultResolver(IvySettings settings)
			throws BuildException {
		try {
			Field cachedResolver = IvySettings.class
					.getDeclaredField("defaultResolver");
			cachedResolver.setAccessible(true);
			cachedResolver.set(settings, null);
		} catch (Exception e) {
			throw new BuildException(
					"Unable to reset default resolver on IvySettings", e);
		}
	}
}
