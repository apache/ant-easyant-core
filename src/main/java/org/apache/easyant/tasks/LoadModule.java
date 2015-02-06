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

import org.apache.easyant.core.BuildConfigurationHelper;
import org.apache.easyant.core.EasyAntConstants;
import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.easyant.core.descriptor.*;
import org.apache.easyant.core.ivy.InheritableScope;
import org.apache.easyant.core.ivy.IvyInstanceHelper;
import org.apache.easyant.core.parser.DefaultEasyAntXmlModuleDescriptorParser;
import org.apache.easyant.core.parser.EasyAntModuleDescriptorParser;
import org.apache.ivy.ant.IvyConfigure;
import org.apache.ivy.ant.IvyDependency;
import org.apache.ivy.ant.IvyInfo;
import org.apache.ivy.core.IvyContext;
import org.apache.easyant.core.ivy.EasyAntRepositoryCacheManager;
import org.apache.easyant.core.ivy.EasyantResolutionCacheManager;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.ivy.plugins.repository.url.URLResource;
import org.apache.ivy.util.StringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.ImportTask;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This task is the main class, used to parse module.ivy and execute the all the statement behind the easyant tag.
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
     * @param file represents a module descriptor
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

            // make sure it's not a directory (this falls into the ultra
            // paranoid lets check everything category

            if (buildModule.isDirectory()) {
                throw new BuildException("What? buildModule: " + buildModule + " is a dir!");
            }

            IvyInfo info = new IvyInfo();
            info.setFile(buildModule);
            // Not sure we should bound IvyInfo to easyantIvyInstance
            info.setSettingsRef(IvyInstanceHelper.buildEasyAntIvyReference(getProject()));
            initTask(info).execute();
            getProject().setName(getProject().getProperty("ivy.module"));

            // load override buildFile before buildModule to allow target/extension-point
            // override
            File f = new File(buildModule.getParent(), EasyAntConstants.DEFAULT_OVERRIDE_BUILD_FILE);
            if (f.exists()) {
                log("Loading override build file : " + buildFile.getAbsolutePath());
                loadBuildFile(f);
            }

            log("Loading build module : " + buildModule.getAbsolutePath());
            loadBuildModule(buildModule);
        }

        if (buildFile != null && buildFile.exists()) {
            // make sure it's not a directory (this falls into the ultra
            // paranoid lets check everything category

            if (buildFile.isDirectory()) {
                throw new BuildException("What? buildFile: " + buildFile + " is a dir!");
            }

            log("Loading build file : " + buildFile.getAbsolutePath());
            loadBuildFile(buildFile);
        }

        String projectIvyInstanceProp = IvyInstanceHelper.getProjectIvyInstanceName(getProject());

        // create project ivy instance except if project ivy instance is linked
        // to easyant ivy instance
        if (!EasyAntMagicNames.EASYANT_IVY_INSTANCE.equals(projectIvyInstanceProp)) {
            configureProjectIvyinstance(projectIvyInstanceProp);
        }

        configureProjectOfflineResolver();

        if (shouldUseBuildRepository()) {
            configureBuildRepository();
        }

        if (getProject().getDefaultTarget() == null
                && getProject().getTargets().containsKey(EasyAntConstants.DEFAULT_TARGET)) {
            getProject().setDefault(EasyAntConstants.DEFAULT_TARGET);
        }
    }

    /**
     * Configure project offline repository If offline mode is enabled, it will acts as dictator resolver
     */
    private void configureProjectOfflineResolver() {
        if (EasyAntMagicNames.EASYANT_IVY_INSTANCE.equals(IvyInstanceHelper.getProjectIvyInstanceName(getProject()))) {
            getProject().setProperty(EasyAntMagicNames.OFFLINE_PROJECT_RESOLVER,
                    getProject().getProperty(EasyAntMagicNames.OFFLINE_EASYANT_RESOLVER));
        } else {

            getProject().setProperty(EasyAntMagicNames.OFFLINE_PROJECT_RESOLVER,
                    EasyAntConstants.DEFAULT_OFFLINE_PROJECT_RESOLVER);
            ConfigureBuildScopedRepository projectOfflineRepository = new ConfigureBuildScopedRepository();
            projectOfflineRepository.setGenerateWrapperResoler(false);
            projectOfflineRepository.setName(getProject().getProperty(EasyAntMagicNames.OFFLINE_PROJECT_RESOLVER));
            projectOfflineRepository.setDictator(Project.toBoolean(getProject().getProperty(
                    EasyAntMagicNames.EASYANT_OFFLINE)));
            projectOfflineRepository.setSettingsRef(IvyInstanceHelper.buildProjectIvyReference(getProject()));
            projectOfflineRepository.setTarget(getProject().getProperty(EasyAntMagicNames.OFFLINE_BASE_DIRECTORY));
            initTask(projectOfflineRepository).execute();
        }
    }

    /**
     *
     */
    private void configureProjectIvyinstance(String projectIvyInstanceName) {
        IvyConfigure projectIvyInstance = new IvyConfigure();
        projectIvyInstance.setSettingsId(projectIvyInstanceName);
        boolean ivysettingsConfigured = false;
        // project ivy settings can be specified by properties
        if (getProject().getProperty(EasyAntMagicNames.PROJECT_IVY_SETTING_FILE) != null) {
            File projectIvyFile = new File(getProject().getProperty(EasyAntMagicNames.PROJECT_IVY_SETTING_FILE));

            if (projectIvyFile.exists()) {
                projectIvyInstance.setFile(projectIvyFile);
                ivysettingsConfigured = true;
            }

        }
        if (getProject().getProperty(EasyAntMagicNames.PROJECT_IVY_SETTING_URL) != null) {
            String url = getProject().getProperty(EasyAntMagicNames.PROJECT_IVY_SETTING_URL);
            try {
                projectIvyInstance.setUrl(url);
                ivysettingsConfigured = true;
            } catch (MalformedURLException malformedUrl) {
                throw new BuildException("Unable to parse project ivysettings from the following url : " + url,
                        malformedUrl);
            }
        }
        // if no property is set check the default user location
        if (!ivysettingsConfigured) {
            File userProjectIvyFile = new File(getProject().replaceProperties(
                    EasyAntConstants.DEFAULT_USER_PROJECT_IVYSETTINGS));
            if (userProjectIvyFile.exists()) {
                projectIvyInstance.setFile(userProjectIvyFile);
                ivysettingsConfigured = true;
            }
        }
        // set default project ivy settings location accessible through properties,
        // then users can import it if they don't want to use it directly
        String defaultUrl = this.getClass().getResource("/org/apache/easyant/core/default-project-ivysettings.xml")
                .toExternalForm();
        getProject().setNewProperty(EasyAntMagicNames.PROJECT_DEFAULT_IVYSETTINGS, defaultUrl);
        if (!ivysettingsConfigured) {
            File localSettings = new File(getProject().getBaseDir(), "ivysettings.xml");
            if (localSettings.exists()) {
                getProject().log("loading local project settings file...", Project.MSG_VERBOSE);
                projectIvyInstance.setFile(localSettings);
                getProject()
                        .setNewProperty(EasyAntMagicNames.PROJECT_IVY_SETTING_FILE, localSettings.getAbsolutePath());

            } else {
                getProject().log("no settings file found, using default...", Project.MSG_VERBOSE);
                getProject().setNewProperty(EasyAntMagicNames.PROJECT_IVY_SETTING_URL, defaultUrl);
                try {
                    projectIvyInstance.setUrl(defaultUrl);
                } catch (MalformedURLException e) {
                    throw new BuildException("Unable to parse project ivysettings from the following url : "
                            + defaultUrl, e);
                }
            }
        }

        initTask(projectIvyInstance).perform();

        // FIXME: hack as ResolutionCacheManager use XmlModuleDescriptorParser under the hood
        EasyAntRepositoryCacheManager cacheManager = new EasyAntRepositoryCacheManager("default-project-cache",
                getProjectIvyInstance().getSettings(), getProjectIvyInstance().getSettings().getDefaultCache());
        getProjectIvyInstance().getSettings().setDefaultRepositoryCacheManager(cacheManager);

        EasyantResolutionCacheManager resolutionCacheManager = new EasyantResolutionCacheManager();
        resolutionCacheManager.setBasedir(getProjectIvyInstance().getSettings().getDefaultResolutionCacheBasedir());
        resolutionCacheManager.setSettings(getProjectIvyInstance().getSettings());
        getProjectIvyInstance().getSettings().setResolutionCacheManager(resolutionCacheManager);

    }

    protected void loadBuildFile(File buildModule) {
        ImportTask importTask = new ImportTask();
        importTask.setFile(buildModule.getAbsolutePath());
        importTask.setOptional(true);
        initTask(importTask).perform();
    }

    protected void loadBuildModule(File buildModule) {
        IvyContext.pushNewContext().setIvy(getEasyAntIvyInstance());
        EasyAntModuleDescriptorParser parser = getEasyAntModuleDescriptorParser(buildModule);
        log("Loading EasyAnt module descriptor :" + parser.getClass().getName(), Project.MSG_DEBUG);

        try {
            parser.parseDescriptor(getEasyAntIvyInstance().getSettings(), buildModule.toURI().toURL(), new URLResource(
                    buildModule.toURI().toURL()), true);
            EasyAntModuleDescriptor md = parser.getEasyAntModuleDescriptor();
            ModuleRevisionId currentModule = md.getIvyModuleDescriptor().getModuleRevisionId();

            String buildConfigurations = null;
            for (String conf : md.getBuildConfigurations()) {
                if (buildConfigurations == null) {
                    buildConfigurations = conf;
                } else {
                    buildConfigurations = buildConfigurations + "," + conf;
                }
            }
            getProject().setProperty(EasyAntMagicNames.AVAILABLE_BUILD_CONFIGURATIONS, buildConfigurations);
            updateMainConfs();

            for (PropertyDescriptor property : md.getProperties().values()) {
                if (canInherit(property, currentModule)) {
                    PropertyTask propTask = new PropertyTask();
                    propTask.setName(property.getName());
                    propTask.setValue(property.getValue());
                    propTask.setBuildConfigurations(property.getBuildConfigurations());
                    propTask.setTaskType("antlib:org.apache.easyant:property");
                    getOwningTarget().addTask(propTask);
                    initTask(propTask).perform();
                }
            }
            if (md.getConfigureProjectDescriptor() != null) {
                ConfigureProjectDescriptor descriptor = md.getConfigureProjectDescriptor();
                ConfigureProject configureProject = new ConfigureProject();
                configureProject.setDefaultTarget(descriptor.getDefaultTarget());
                configureProject.setBasedir(descriptor.getBasedir());
                configureProject.setTaskType("antlib:org.apache.easyant:configure-project");
                getOwningTarget().addTask(configureProject);
                initTask(configureProject).perform();
            }

            ResolvePlugins resolvePlugins = new ResolvePlugins();

            if (md.getBuildType() != null) {
                if (canInherit(md.getBuildType(), currentModule)) {
                    // Import importTask = new Import();
                    // importTask.setMrid(md.getBuildType().getMrid());
                    // importTask.setMode(md.getBuildType().getMode());
                    // importTask.setAs(md.getBuildType().getAs());
                    // importTask.setMandatory(md.getBuildType().isMandatory());
                    // importTask.setBuildConfigurations(md.getBuildType().getBuildConfigurations());
                    // importTask.setDependencies(md.getBuildType().getDependencies());
                    // importTask.setConflicts(md.getBuildType().getConflicts());
                    // importTask.setExcludes(md.getBuildType().getExcludes());
                    //
                    // importTask.setTaskType("antlib:org.apache.easyant:import");
                    // getOwningTarget().addTask(importTask);
                    // initTask(importTask).perform();
                    IvyDependency buildtype = resolvePlugins.createDependency();
                    buildtype.setOrg(md.getBuildType().getModuleRevisionId().getOrganisation());
                    buildtype.setName(md.getBuildType().getModuleRevisionId().getName());
                    buildtype.setRev(md.getBuildType().getModuleRevisionId().getRevision());
                }
            }
            for (PluginDescriptor plugin : md.getPlugins()) {
                if (canInherit(plugin, currentModule)) {
                    // Import importTask = new Import();
                    // importTask.setMrid(plugin.getMrid());
                    // importTask.setMode(plugin.getMode());
                    // importTask.setAs(plugin.getAs());
                    // importTask.setMandatory(plugin.isMandatory());
                    // importTask.setBuildConfigurations(plugin.getBuildConfigurations());
                    // importTask.setDependencies(plugin.getDependencies());
                    // importTask.setConflicts(plugin.getConflicts());
                    // importTask.setExcludes(plugin.getExcludes());
                    // importTask.setTaskType("antlib:org.apache.easyant:import");
                    // getOwningTarget().addTask(importTask);
                    // initTask(importTask).perform();
                    IvyDependency pluginDependency = resolvePlugins.createDependency();
                    pluginDependency.setOrg(plugin.getModuleRevisionId().getOrganisation());
                    pluginDependency.setName(plugin.getModuleRevisionId().getName());
                    pluginDependency.setRev(plugin.getModuleRevisionId().getRevision());
                }
            }
            initTask(resolvePlugins).execute();

            if (md.getBuildType() != null) {
                if (canInherit(md.getBuildType(), currentModule)) {
                    ImportDeferred importDeferredTask = new ImportDeferred();
                    importDeferredTask.setOrganisation(md.getBuildType().getModuleRevisionId().getOrganisation());
                    importDeferredTask.setModule(md.getBuildType().getModuleRevisionId().getName());
                    importDeferredTask.setMode(md.getBuildType().getMode());
                    importDeferredTask.setAs(md.getBuildType().getAs());
                    importDeferredTask.setMandatory(md.getBuildType().isMandatory());
                    importDeferredTask.setBuildConfigurations(md.getBuildType().getBuildConfigurations());

                    importDeferredTask.setTaskType("antlib:org.apache.easyant:import-deferred");
                    getOwningTarget().addTask(importDeferredTask);
                    initTask(importDeferredTask).perform();
                }
            }
            for (PluginDescriptor plugin : md.getPlugins()) {
                if (canInherit(plugin, currentModule)) {
                    ImportDeferred importDeferredTask = new ImportDeferred();
                    importDeferredTask.setOrganisation(plugin.getModuleRevisionId().getOrganisation());
                    importDeferredTask.setModule(plugin.getModuleRevisionId().getName());
                    importDeferredTask.setMode(plugin.getMode());
                    importDeferredTask.setAs(plugin.getAs());
                    importDeferredTask.setMandatory(plugin.isMandatory());
                    importDeferredTask.setBuildConfigurations(plugin.getBuildConfigurations());
                    importDeferredTask.setTaskType("antlib:org.apache.easyant:import-deferred");
                    getOwningTarget().addTask(importDeferredTask);
                    initTask(importDeferredTask).perform();

                }
            }

            // Apply ExtensionPointMapping
            for (ExtensionPointMappingDescriptor epMapping : md.getExtensionPointsMappings()) {
                BindTarget bindTarget = new BindTarget();
                bindTarget.setTarget(epMapping.getTarget());
                bindTarget.setExtensionOf(epMapping.getExtensionPoint());
                bindTarget.setBuildConfigurations(epMapping.getBuildConfigurations());
                initTask(bindTarget).perform();
            }
        } catch (Exception e) {
            throw new BuildException("problem while parsing Ivy module file: " + e.getMessage(), e);
        }
        IvyContext.popContext();
    }

    /**
     * Check if an inheritable item can be inherited by verifying {@link InheritableScope}
     *
     * @param inheritableItem a given {@link AdvancedInheritableItem}
     * @param currentModule   current module
     * @return true if item can be inherited
     */
    private boolean canInherit(AdvancedInheritableItem inheritableItem, ModuleRevisionId currentModule) {
        if (currentModule.equals(inheritableItem.getSourceModule())) {
            return !InheritableScope.CHILD.equals(inheritableItem.getInheritScope());
        } else {
            return true;
        }

    }

    /**
     * This method is in charge to update the main.confs property with all the active build configuration for the
     * current project.
     */
    private void updateMainConfs() {
        if (getProject().getProperty(EasyAntMagicNames.AVAILABLE_BUILD_CONFIGURATIONS) == null
                || getProject().getProperty(EasyAntMagicNames.ACTIVE_BUILD_CONFIGURATIONS) == null) {
            return;
        }

        List<String> availableBuildConfigurations = Arrays.asList(getProject().getProperty(
                EasyAntMagicNames.AVAILABLE_BUILD_CONFIGURATIONS).split(","));
        // remove spaces in active confs
        String activeConfs = getProject().getProperty(EasyAntMagicNames.ACTIVE_BUILD_CONFIGURATIONS);
        List<String> activeBuildConfigurations = BuildConfigurationHelper.buildList(activeConfs);
        List<String> mainConfsList = new ArrayList<String>();
        for (String conf : activeBuildConfigurations) {
            if (availableBuildConfigurations.contains(conf)) {
                mainConfsList.add(conf);
            } else {
                log("removing unused configuration " + conf, Project.MSG_DEBUG);
            }
        }
        if (!mainConfsList.isEmpty()) {
            String mainConfs = StringUtils.join(mainConfsList.toArray(new String[mainConfsList.size()]), ",");
            log("updating main.confs with active profile for current project :" + mainConfs, Project.MSG_DEBUG);
            getProject().setProperty(EasyAntMagicNames.MAIN_CONFS, mainConfs);
        }

    }

    protected EasyAntModuleDescriptorParser getEasyAntModuleDescriptorParser(File file) throws BuildException {
        ModuleDescriptorParser mdp;
        EasyAntModuleDescriptorParser parser;
        try {
            mdp = ModuleDescriptorParserRegistry.getInstance().getParser(new URLResource(file.toURI().toURL()));
        } catch (MalformedURLException e) {
            throw new BuildException("Impossible to find a parser for " + file.getName());
        }
        // If valid easyant parser is defined use it
        if (mdp != null && mdp.getClass().isInstance(EasyAntModuleDescriptorParser.class)) {
            return (EasyAntModuleDescriptorParser) mdp;
        } else {
            // if the user has customized the loadmodule task
            if (easyAntMDParserClassName != null) {

                try {
                    Class<? extends EasyAntModuleDescriptorParser> c = Class.forName(easyAntMDParserClassName)
                            .asSubclass(EasyAntModuleDescriptorParser.class);
                    log("Creating instance of " + easyAntMDParserClassName, Project.MSG_DEBUG);
                    parser = c.newInstance();
                    ModuleDescriptorParserRegistry.getInstance().addParser(parser);
                    return parser;
                } catch (Exception e) {
                    throw new BuildException("Unable to load " + easyAntMDParserClassName, e);
                }

            }
            // the default one
            log("Creating instance of " + DefaultEasyAntXmlModuleDescriptorParser.class.getName(), Project.MSG_DEBUG);
            parser = new DefaultEasyAntXmlModuleDescriptorParser();
            ModuleDescriptorParserRegistry.getInstance().addParser(parser);
            return parser;

        }
    }

    /**
     * @return true if this module should use a build-scoped repository and cache to find artifacts generated by other
     * modules in the same build.
     */
    private boolean shouldUseBuildRepository() {
        // if a value has been provided by task attribute, return it
        if (useBuildRepository != null) {
            return useBuildRepository;
        }
        // otherwise, look for a value in property configuration, defaulting to
        // false if no value.
        return Project.toBoolean(getProject().getProperty(EasyAntMagicNames.USE_BUILD_REPOSITORY));
    }

    /**
     * Change the given Ivy settings to use a local build-scoped repository and cache by default. This allows submodules
     * to access each others' artifacts before they have been published to a shared repository.
     */
    private void configureBuildRepository() throws BuildException {
        ConfigureBuildScopedRepository configureBuildScopedRepository = new ConfigureBuildScopedRepository();
        configureBuildScopedRepository.setName(EasyAntConstants.BUILD_SCOPE_REPOSITORY);
        configureBuildScopedRepository.setSettingsRef(IvyInstanceHelper.buildProjectIvyReference(getProject()));
        configureBuildScopedRepository.setGenerateWrapperResoler(true);
        getProject().setProperty(EasyAntMagicNames.EASYANT_BUILD_REPOSITORY, EasyAntConstants.BUILD_SCOPE_REPOSITORY);
        initTask(configureBuildScopedRepository).perform();
    }

}
