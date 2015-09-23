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
package org.apache.easyant.core;

import org.apache.easyant.core.ant.ProjectUtils;
import org.apache.easyant.core.ant.listerners.DefaultEasyAntLogger;
import org.apache.easyant.core.configuration.EasyAntConfiguration;
import org.apache.easyant.core.configuration.EasyantConfigurationFactory;
import org.apache.easyant.core.descriptor.PluginDescriptor;
import org.apache.easyant.core.ivy.InheritableScope;
import org.apache.easyant.core.ivy.IvyInstanceHelper;
import org.apache.easyant.core.services.DefaultPluginService;
import org.apache.easyant.core.services.PluginService;
import org.apache.easyant.tasks.ConfigureBuildScopedRepository;
import org.apache.easyant.tasks.Import;
import org.apache.easyant.tasks.LoadModule;
import org.apache.ivy.Ivy;
import org.apache.ivy.ant.IvyAntSettings;
import org.apache.ivy.ant.IvyConfigure;
import org.apache.easyant.core.ivy.EasyAntRepositoryCacheManager;
import org.apache.easyant.core.ivy.EasyantResolutionCacheManager;
import org.apache.tools.ant.*;
import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.input.InputHandler;
import org.apache.tools.ant.util.ClasspathUtils;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.ProxySetup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

/**
 * This class provides everything you need to run easyant. This class should be used to bootstrap easyant from IDE for
 * example
 */
public class EasyAntEngine {

    private final EasyAntConfiguration configuration;

    private PluginService pluginService = null;

    /**
     * Default constructor will initialize the default configuration
     */
    public EasyAntEngine() {
        this(EasyantConfigurationFactory.getInstance().createDefaultConfiguration());
    }

    /**
     * Constructor if you want to use a custom configuration
     *
     * @param configuration
     */
    public EasyAntEngine(final EasyAntConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Configure easyant ivy instance
     *
     * @param project project instance
     * @return a configured {@link IvyAntSettings} instance
     */
    public IvyAntSettings configureEasyAntIvyInstance(Project project) {
        IvyConfigure easyantIvyConfigure = new IvyConfigure();
        easyantIvyConfigure.setSettingsId(EasyAntMagicNames.EASYANT_IVY_INSTANCE);

        project.setNewProperty(EasyAntMagicNames.EASYANT_DEFAULT_IVYSETTINGS,
                this.getClass().getResource("/org/apache/easyant/core/default-easyant-ivysettings.xml")
                        .toExternalForm());

        project.setNewProperty(EasyAntMagicNames.EASYANT_CORE_JAR_URL, guessEasyantCoreJarUrl().toExternalForm());

        try {
            File userSettings = getUserEasyAntIvySettings(project);
            URL globalSettings = getGlobalEasyAntIvySettings(project);
            boolean isIgnoringUserIvysettings = Project.toBoolean(project
                    .getProperty(EasyAntMagicNames.IGNORE_USER_IVYSETTINGS));

            if (userSettings.exists() && !isIgnoringUserIvysettings) {
                project.log("loading user's easyant ivysettings file from " + userSettings.getAbsolutePath(),
                        Project.MSG_DEBUG);
                easyantIvyConfigure.setFile(userSettings);
            } else if (globalSettings != null) {
                project.log("loading global easyant ivysettings file from " + globalSettings.toExternalForm(),
                        Project.MSG_DEBUG);
                easyantIvyConfigure.setUrl(globalSettings);

            } else {
                project.log("using easyant default ivy settings file", Project.MSG_VERBOSE);
                String url = project.getProperty(EasyAntMagicNames.EASYANT_DEFAULT_IVYSETTINGS);
                easyantIvyConfigure.setUrl(url);
            }
        } catch (MalformedURLException malformedUrl) {
            throw new BuildException("Unable to parse easyant ivysettings from given url", malformedUrl);
        }

        executeTask(easyantIvyConfigure, "configure-easyant", project);

        configureEasyAntOfflineRepository(project);

        IvyAntSettings easyantIvySettings = IvyInstanceHelper.getEasyAntIvyAntSettings(project);

        // FIXME: hack as ResolutionCacheManager and RepositoryCacheManger use XmlModuleDescriptorParser under the hood
        Ivy easyantIvyInstance = easyantIvySettings.getConfiguredIvyInstance(easyantIvyConfigure);

        EasyAntRepositoryCacheManager cacheManager = new EasyAntRepositoryCacheManager("default-easyant-cache",
                easyantIvyInstance.getSettings(), easyantIvyInstance.getSettings().getDefaultCache());
        easyantIvyInstance.getSettings().setDefaultRepositoryCacheManager(cacheManager);

        EasyantResolutionCacheManager resolutionCacheManager = new EasyantResolutionCacheManager();
        resolutionCacheManager.setBasedir(easyantIvyInstance.getSettings().getDefaultResolutionCacheBasedir());
        resolutionCacheManager.setSettings(easyantIvyInstance.getSettings());
        easyantIvyInstance.getSettings().setResolutionCacheManager(resolutionCacheManager);

        return easyantIvySettings;
    }

    private static Method getLocalURL;

    public static synchronized URL guessEasyantCoreJarUrl() {
        URL url = EasyAntEngine.class.getResource("/org/apache/easyant/antlib.xml");
        try {
            if ("jar".equals(url.getProtocol())) {
                return getJarUrl(url);
            } else if ("bundleresource".equals(url.getProtocol())) {
                URLConnection conn = url.openConnection();
                try {
                    if (getLocalURL == null
                            && "org.eclipse.osgi.framework.internal.core.BundleURLConnection".equals(conn.getClass()
                            .getName())) {
                        EasyAntEngine.getLocalURL = conn.getClass().getMethod("getLocalURL", null);
                        getLocalURL.setAccessible(true);
                    }
                    if (getLocalURL != null && conn != null) {
                        URL localJarUrl = (URL) getLocalURL.invoke(conn,  null);
                        return getJarUrl(localJarUrl);
                    }
                } catch (Throwable throwable) {
                    IOException ioe = new IOException("Cannot get jar url from Equinox OSGi bundle");
                    ioe.initCause(throwable);
                    throw ioe;
                }
            }
        } catch (IOException ioe) {
            throw new RuntimeException("Easyant jar cannot be guessed", ioe);
        }
        return url;
    }

    private static URL getJarUrl(URL url) throws IOException {
        JarURLConnection conn = (JarURLConnection) url.openConnection();
        return conn.getJarFileURL();
    }

    /**
     * Get user easyant-ivysettings file
     *
     * @param project
     * @return the configured user easyant-ivysettings.file
     */
    protected File getUserEasyAntIvySettings(Project project) {
        // path can be specified through a property
        String path = project.getProperty(EasyAntMagicNames.USER_EASYANT_IVYSETTINGS);
        // if no property is set check the default location
        if (path == null) {
            path = PropertyHelper.getPropertyHelper(project).replaceProperties(
                    EasyAntConstants.DEFAULT_USER_EASYANT_IVYSETTINGS);
        }
        project.log("user's easyant-ivysettings file : " + path, Project.MSG_DEBUG);
        return new File(path);
    }

    /**
     * Get global easyant-ivysettings file
     *
     * @param project
     * @return the configured global easyant-ivysettings.file
     * @throws MalformedURLException
     */
    protected URL getGlobalEasyAntIvySettings(Project project) throws MalformedURLException {
        PropertyHelper helper = PropertyHelper.getPropertyHelper(project);
        URL path = null;
        if (configuration.getEasyantIvySettingsFile() != null) {
            File f = new File(helper.replaceProperties(configuration.getEasyantIvySettingsFile()));
            path = f.toURI().toURL();
        }
        if (configuration.getEasyantIvySettingsUrl() != null) {
            path = new URL(helper.replaceProperties(configuration.getEasyantIvySettingsUrl()));
        }
        // path can be specified through a property
        if (path == null && project.getProperty(EasyAntMagicNames.GLOBAL_EASYANT_IVYSETTINGS) != null) {
            path = new URL(project.getProperty(EasyAntMagicNames.GLOBAL_EASYANT_IVYSETTINGS));
        }
        // if no property is set check the default location
        if (path == null) {
            File defaultGlboalEasyAntIvySettings = new File(
                    helper.replaceProperties(EasyAntConstants.DEFAULT_GLOBAL_EASYANT_IVYSETTINGS));
            if (!defaultGlboalEasyAntIvySettings.exists()) {
                return null;
            }
            path = defaultGlboalEasyAntIvySettings.toURI().toURL();
        }
        project.log("global easyant-ivysettings file : " + path.toExternalForm(), Project.MSG_DEBUG);
        return path;
    }

    public void configurePluginService(Project project, IvyAntSettings easyantIvyInstance) {
        pluginService = new DefaultPluginService(easyantIvyInstance);
        String property = project.getProperty(EasyAntMagicNames.EASYANT_OFFLINE);
        pluginService.setOfflineMode(Project.toBoolean(property));
        project.addReference(EasyAntMagicNames.PLUGIN_SERVICE_INSTANCE, pluginService);

    }

    /**
     * Adds the listeners specified in the command line arguments, along with the default listener, to the specified
     * project.
     *
     * @param project The project to add listeners to. Must not be <code>null</code> .
     */
    protected void addBuildListeners(Project project) {

        // Add the default listener
        project.addBuildListener(createLogger());

        for (String className : configuration.getListeners()) {
            BuildListener listener = (BuildListener) ClasspathUtils.newInstance(className,
                    EasyAntEngine.class.getClassLoader(), BuildListener.class);
            project.setProjectReference(listener);

            project.addBuildListener(listener);
        }
    }

    /**
     * Creates the InputHandler and adds it to the project.
     *
     * @param project the project instance.
     * @throws BuildException if a specified InputHandler implementation could not be loaded.
     */
    protected void addInputHandler(Project project) {
        InputHandler handler;

        if (configuration.getInputHandlerClassname() == null) {
            handler = new DefaultInputHandler();
        } else {
            handler = (InputHandler) ClasspathUtils.newInstance(configuration.getInputHandlerClassname(),
                    Main.class.getClassLoader(), InputHandler.class);
            project.setProjectReference(handler);
        }
        project.setInputHandler(handler);
    }

    /**
     * Creates the default build logger for sending build events to the ant log.
     *
     * @return the logger instance for this build.
     */
    protected BuildLogger createLogger() {
        BuildLogger logger;
        if (configuration.getLoggerClassname() != null) {
            try {
                logger = (BuildLogger) ClasspathUtils.newInstance(configuration.getLoggerClassname(),
                        EasyAntEngine.class.getClassLoader(), BuildLogger.class);
            } catch (BuildException e) {
                throw new RuntimeException("The specified logger class " + configuration.getLoggerClassname()
                        + " could not be used because " + e.getMessage(), e);
            }
        } else {
            logger = new DefaultEasyAntLogger();
        }

        logger.setMessageOutputLevel(configuration.getMsgOutputLevel());
        logger.setOutputPrintStream(configuration.getOut());
        logger.setErrorPrintStream(configuration.getErr());
        logger.setEmacsMode(configuration.isEmacsMode());

        return logger;
    }

    /**
     * Search parent directories for the build file.
     * <p/>
     * Takes the given target as a suffix to append to each parent directory in search of a build file. Once the root of
     * the file-system has been reached an exception is thrown.
     *
     * @param start  Leaf directory of search. Must not be <code>null</code>.
     * @param suffix Suffix filename to look for in parents. Must not be <code>null</code>.
     * @return A handle to the build file if one is found
     * @throws BuildException if no build file is found
     */
    protected File findBuildModule(String start, String suffix) throws BuildException {
        if (configuration.getMsgOutputLevel() >= Project.MSG_INFO) {
            System.out.println("Searching for " + suffix + " ...");
        }

        File parent = new File(new File(start).getAbsolutePath());
        File file = new File(parent, suffix);

        // check if the target file exists in the current directory
        while (!file.exists()) {
            // change to parent directory
            parent = parent.getParentFile();

            // if parent is null, then we are at the root of the fs,
            // complain that we can't find the build file.
            if (parent == null) {
                throw new BuildException("Could not locate a build file!");
            }

            // refresh our file handle
            file = new File(parent, suffix);
        }

        return file;
    }


    /**
     * configure easyant (listeners, inputhandlers, proxy, easyantIvyInstance, systems plugins etc...)
     *
     * @param project a project to configure
     */
    public void configureEasyAnt(Project project) {

        project.setCoreLoader(configuration.getCoreLoader());

        addBuildListeners(project);
        addInputHandler(project);

        // set the thread priorities
        if (configuration.getThreadPriority() != null) {
            try {
                project.log("Setting Ant's thread priority to " + configuration.getThreadPriority(),
                        Project.MSG_VERBOSE);
                Thread.currentThread().setPriority(configuration.getThreadPriority());
            } catch (SecurityException swallowed) {
                // we cannot set the priority here.
                project.log("A security manager refused to set the -nice value");
            }
        }

        project.setKeepGoingMode(configuration.isKeepGoingMode());
        if (configuration.isProxy()) {
            // proxy setup if enabledcoreLoader
            ProxySetup proxySetup = new ProxySetup(project);
            proxySetup.enableProxies();
        }

        project.setName("EasyAnt");

        try {
            project.init();
            project.addReference(EasyAntMagicNames.EASYANT_ENGINE_REF, this);

            // set user-define properties
            Enumeration<?> properties = configuration.getDefinedProps().propertyNames();
            while (properties.hasMoreElements()) {
                String arg = (String) properties.nextElement();
                String value = (String) configuration.getDefinedProps().get(arg);
                project.setUserProperty(arg, value);
            }

            project.setUserProperty(EasyAntMagicNames.EASYANT_OFFLINE, Boolean.toString(configuration.isOffline()));

            ProjectUtils.configureProjectHelper(project);

            IvyAntSettings easyantIvySettings = configureEasyAntIvyInstance(project);
            configurePluginService(project, easyantIvySettings);

            // Profile
            if (!configuration.getActiveBuildConfigurations().isEmpty()) {
                String buildConfigurations = null;
                for (String conf : configuration.getActiveBuildConfigurations()) {
                    if (buildConfigurations == null) {
                        buildConfigurations = conf;
                    } else {
                        buildConfigurations = buildConfigurations + "," + conf;
                    }

                }
                project.log("Active build configurations : " + buildConfigurations, Project.MSG_INFO);
                project.setProperty(EasyAntMagicNames.ACTIVE_BUILD_CONFIGURATIONS, buildConfigurations);
            }
            loadSystemPlugins(project, true);
        } catch (RuntimeException exc) {
            fireBuildFinished(project, exc);
            throw exc;
        }
    }

    /**
     * Load an easyant project and resolve extension points
     *
     * @param project
     */
    public void loadProject(Project project) {
        try {
            locateBuildModuleAndBuildFile(project);

            if (configuration.getBuildModule() != null || configuration.getBuildFile() != null) {
                LoadModule lm = new LoadModule();
                lm.setBuildModule(configuration.getBuildModule());
                lm.setBuildFile(configuration.getBuildFile());
                executeTask(lm, "load-module", project);
            }
            ProjectUtils.getConfiguredProjectHelper(project).resolveExtensionOfAttributes(project);
        } catch (RuntimeException exc) {
            fireBuildFinished(project, exc);
            throw exc;
        }

    }

    private void fireBuildFinished(Project project, Throwable error) {
        try {
            project.fireBuildFinished(error);
        } catch (Throwable t) {
            // yes, I know it is bad style to catch Throwable,
            // but if we don't, we lose valuable information
            System.err.println("Caught an exception while logging the" + " end of the build.  Exception was:");
            t.printStackTrace();
            if (error != null) {
                System.err.println("There has been an error prior to" + " that:");
                error.printStackTrace();
            }
            throw new BuildException(t);
        }
    }

    public void loadSystemPlugins(Project project, boolean isRootProject) {
        if (isRootProject && !configuration.getSystemPlugins().isEmpty()) {
            project.log("Loading System Plugins...");
        }
        for (PluginDescriptor systemPlugin : configuration.getSystemPlugins()) {
            if (isRootProject && InheritableScope.BOTH == systemPlugin.getInheritScope()
                    || systemPlugin.isInheritable()) {
                // import/include system plugin
                Import importTask = new Import();
                importTask.setMrid(systemPlugin.getMrid());
                importTask.setOrganisation(systemPlugin.getOrganisation());
                importTask.setModule(systemPlugin.getModule());
                importTask.setRevision(systemPlugin.getRevision());
                importTask.setAs(systemPlugin.getAs());
                importTask.setMode(systemPlugin.getMode());
                importTask.setMandatory(systemPlugin.isMandatory());
                executeTask(importTask, "configure-system-plugins", project);
            }
        }
    }

    private void locateBuildModuleAndBuildFile(Project project) {
        File buildModule = configuration.getBuildModule();
        File buildFile = configuration.getBuildFile();

        if (project.getProperty("project.basedir") != null) {
            project.setBaseDir(new File(project.getProperty("project.basedir")));
        }

        if (buildModule == null) {
            buildModule = new File(project.getBaseDir(), EasyAntConstants.DEFAULT_BUILD_MODULE);
        }

        if (!buildModule.exists() && configuration.isBuildModuleLookupEnabled()) {
            buildModule = findBuildModule(System.getProperty("user.dir"), buildModule.toString());
        }

        // calculate buildFile location based on buildModule directory
        if (buildModule.exists() && buildFile == null) {
            buildFile = new File(buildModule.getParentFile(), EasyAntConstants.DEFAULT_BUILD_FILE);
        }

        if (buildFile == null && configuration.isBuildModuleLookupEnabled()) {
            buildFile = findBuildModule(System.getProperty("user.dir"), EasyAntConstants.DEFAULT_BUILD_FILE);
        }

        // Normalize buildFile for re-import detection
        if (buildModule != null) {
            buildModule = FileUtils.getFileUtils().normalize(buildModule.getAbsolutePath());
            project.setNewProperty(EasyAntMagicNames.EASYANT_FILE, buildModule.getAbsolutePath());

        }

        if (buildFile != null) {
            buildFile = FileUtils.getFileUtils().normalize(buildFile.getAbsolutePath());
            project.setNewProperty(MagicNames.ANT_FILE, buildFile.getAbsolutePath());

        }

        configuration.setBuildFile(buildFile);
        configuration.setBuildModule(buildModule);
    }

    /**
     * Configure easyant offline repository If offline mode is enabled, it will acts as dictator resolver
     *
     * @param project {@link Project} where repositories will be configured
     */
    private void configureEasyAntOfflineRepository(Project project) {
        // assign default value if not already set
        project.setProperty(EasyAntMagicNames.OFFLINE_EASYANT_RESOLVER,
                EasyAntConstants.DEFAULT_OFFLINE_EASYANT_RESOLVER);
        project.setProperty(EasyAntMagicNames.OFFLINE_BASE_DIRECTORY, project.getBaseDir().getAbsolutePath()
                + "/offline/");

        ConfigureBuildScopedRepository easyantOfflineRepository = new ConfigureBuildScopedRepository();
        easyantOfflineRepository.setGenerateWrapperResoler(false);
        easyantOfflineRepository.setName(project.getProperty(EasyAntMagicNames.OFFLINE_EASYANT_RESOLVER));
        easyantOfflineRepository.setDictator(Project.toBoolean(project.getProperty(EasyAntMagicNames.EASYANT_OFFLINE)));
        easyantOfflineRepository.setSettingsRef(IvyInstanceHelper.buildEasyAntIvyReference(project));
        easyantOfflineRepository.setTarget(project.getProperty(EasyAntMagicNames.OFFLINE_BASE_DIRECTORY));
        executeTask(easyantOfflineRepository, "configure-offline-easyant-resolver", project);
    }

    private void executeTask(Task task, String operationName, Project project) {
        Location location = new Location(ProjectUtils.emulateMainScript(project).getAbsolutePath());
        task.setLocation(location);
        task.setOwningTarget(ProjectUtils.createTopLevelTarget());
        task.setProject(project);
        task.setTaskName(EasyAntConstants.EASYANT_TASK_NAME + "-" + operationName);
        task.execute();

    }

    /**
     * this method run the build process
     *
     * @throws BuildException
     */
    public void doBuild() throws BuildException {
        final Project project = new Project();
        configureEasyAnt(project);
        loadProject(project);
        doBuild(project);

    }

    public void doBuild(final Project project) {
        project.fireBuildStarted();

        Throwable error = null;

        try {

            PrintStream savedErr = System.err;
            PrintStream savedOut = System.out;
            InputStream savedIn = System.in;

            // use a system manager that prevents from System.exit()
            SecurityManager oldsm;
            oldsm = System.getSecurityManager();

            // SecurityManager can not be installed here for backwards
            // compatibility reasons (PD). Needs to be loaded prior to
            // ant class if we are going to implement it.
            // System.setSecurityManager(new NoExitSecurityManager());
            try {
                if (configuration.isAllowInput()) {
                    project.setDefaultInputStream(System.in);
                }
                System.setIn(new DemuxInputStream(project));
                System.setOut(new PrintStream(new DemuxOutputStream(project, false)));
                System.setErr(new PrintStream(new DemuxOutputStream(project, true)));

                // make sure that we have a target to execute
                if (configuration.getTargets().isEmpty() && project.getDefaultTarget() != null) {
                    configuration.getTargets().add(project.getDefaultTarget());
                }
                project.executeTargets(new Vector<String>(configuration.getTargets()));
            } finally {
                // put back the original security manager
                // The following will never eval to true. (PD)
                if (oldsm != null) {
                    System.setSecurityManager(oldsm);
                }

                System.setOut(savedOut);
                System.setErr(savedErr);
                System.setIn(savedIn);
            }
        } catch (RuntimeException exc) {
            error = exc;
            throw exc;
        } catch (Error e) {
            error = e;
            throw e;
        } finally {
            fireBuildFinished(project, error);
        }
        if (configuration.isShowMemoryDetails() || configuration.getMsgOutputLevel() >= Project.MSG_VERBOSE) {
            ProjectUtils.printMemoryDetails(project);
        }
    }

    /**
     * This is a static method used to run build process
     *
     * @param eaConfig an easyant configuration
     * @throws BuildException
     */
    public static void runBuild(EasyAntConfiguration eaConfig) throws BuildException {
        EasyAntEngine eaEngine = new EasyAntEngine(eaConfig);
        eaEngine.doBuild();
    }

    /**
     * This is a static method used to configure and load an existing project
     *
     * @param project         a given project
     * @param eaConfiguration an easyant configuration
     * @return configured project
     * @throws BuildException
     */
    public static Project configureAndLoadProject(Project project, EasyAntConfiguration eaConfiguration)
            throws BuildException {
        EasyAntEngine eaEngine = new EasyAntEngine(eaConfiguration);
        eaEngine.configureEasyAnt(project);
        eaEngine.loadProject(project);
        return project;
    }

    /**
     * Return the configured plugin service instance
     *
     * @return the configured plugin service instance
     */
    public PluginService getPluginService() {
        // hack for IDE integration
        if (pluginService == null) {
            Project project = new Project();
            project.setCoreLoader(configuration.getCoreLoader());
            Enumeration<?> e = configuration.getDefinedProps().keys();
            while (e.hasMoreElements()) {
                String arg = (String) e.nextElement();
                String value = (String) configuration.getDefinedProps().get(arg);
                project.setUserProperty(arg, value);
            }
            project.setName("EasyAnt");
            // not sure we need to invoke init here
            project.init();
            IvyAntSettings ivyAntSettings = configureEasyAntIvyInstance(project);
            configurePluginService(project, ivyAntSettings);
        }
        return pluginService;
    }

    /**
     * Cache of the EasyAnt version information when it has been loaded.
     */
    private static String easyantVersion = null;

    /**
     * Returns the EasyAnt version information, if available. Once the information has been loaded once, it's cached and
     * returned from the cache on future calls.
     *
     * @return the Ant version information as a String (always non- <code>null</code>)
     * @throws BuildException if the version information is unavailable
     */
    public static String getEasyAntVersion() {

        if (easyantVersion == null) {
            InputStream in = null;

            try {
                Properties props = new Properties();
                in = Main.class.getResourceAsStream("/META-INF/version.properties");
                if (in == null) {
                    throw new BuildException("Could not load the version information.");

                }
                props.load(in);

                easyantVersion = "EasyAnt version " + props.getProperty("VERSION") + " compiled on " + props.getProperty("DATE");
            } catch (IOException ioe) {
                throw new BuildException("Could not load the version information", ioe);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        // do nothing
                    }
                }
            }
        }
        return easyantVersion;
    }

}
