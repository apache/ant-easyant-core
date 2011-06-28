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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.easyant.core.ant.Phase;
import org.apache.easyant.core.ant.listerners.DefaultEasyAntLogger;
import org.apache.easyant.core.descriptor.PluginDescriptor;
import org.apache.easyant.core.factory.EasyantConfigurationFactory;
import org.apache.easyant.core.ivy.IvyInstanceHelper;
import org.apache.easyant.core.services.PluginService;
import org.apache.easyant.core.services.impl.DefaultPluginServiceImpl;
import org.apache.easyant.tasks.Import;
import org.apache.easyant.tasks.LoadModule;
import org.apache.ivy.Ivy;
import org.apache.ivy.ant.IvyAntSettings;
import org.apache.ivy.ant.IvyConfigure;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.DemuxInputStream;
import org.apache.tools.ant.DemuxOutputStream;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Main;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.input.InputHandler;
import org.apache.tools.ant.util.ClasspathUtils;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.ProxySetup;

/**
 * This class provides everything you need to run easyant. This class should be
 * used to bootstrap easyant from IDE for example
 */
public class EasyAntEngine {

    private static final long MEGABYTE = 1024 * 1024;;

    private final EasyAntConfiguration configuration;

    private PluginService pluginService = null;

    /**
     * Default constructor will initialize the default configuration
     */
    public EasyAntEngine() {
        this(EasyantConfigurationFactory.getInstance()
                .createDefaultConfiguration());
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
     * @param project
     *            project instance
     * @return a configured {@link Ivy} instance
     */
    public Ivy configureEasyAntIvyInstance(Project project) {
        IvyConfigure easyantIvyConfigure = new IvyConfigure();
        easyantIvyConfigure
                .setSettingsId(EasyAntMagicNames.EASYANT_IVY_INSTANCE);

        project
                .setNewProperty(
                        EasyAntMagicNames.EASYANT_DEFAULT_IVYSETTINGS,
                        this
                                .getClass()
                                .getResource(
                                        "/org/apache/easyant/core/default-easyant-ivysettings.xml")
                                .toExternalForm());
        project
                .setNewProperty(
                        EasyAntMagicNames.EASYANT_EXTRA_IVYSETTINGS,
                        this
                                .getClass()
                                .getResource(
                                        "/org/apache/easyant/core/extra-easyant-ivysettings.xml")
                                .toExternalForm());

        project.setNewProperty(EasyAntMagicNames.EASYANT_CORE_REPO_URL, this
                .getClass().getResource(
                        "/org/apache/easyant/core/repository/modules")
                .toExternalForm());
        if (this.getClass().getResource(
                "/org/apache/easyant/repository/extra-modules") != null) {
            project.setNewProperty(EasyAntMagicNames.EASYANT_EXTRA_REPO_URL,
                    this.getClass().getResource(
                            "/org/apache/easyant/repository/extra-modules")
                            .toExternalForm());
        }

        File userSettings = getUserEasyAntIvySettings(project);
        String globalSettings = getGlobalEasyAntIvySettings(project);
        boolean isIgnoringUserIvysettings=Project.toBoolean(project.getProperty(EasyAntMagicNames.IGNORE_USER_IVYSETTINGS));

        if (userSettings.exists() && !isIgnoringUserIvysettings) {
            project.log("loading user's easyant ivysettings file from "
                    + userSettings.getAbsolutePath(),Project.MSG_DEBUG);
            easyantIvyConfigure.setFile(userSettings);
        } else if (globalSettings != null) {
            project.log("loading global easyant ivysettings file from "
                    + globalSettings,Project.MSG_DEBUG);
            try {
                easyantIvyConfigure.setUrl(globalSettings);
            } catch (MalformedURLException malformedUrl) {
                throw new BuildException(
                        "Unable to parse easyant ivysettings from the following url : "
                                + globalSettings, malformedUrl);
            }

        } else {
            project.log("using easyant default ivy settings file",
                    Project.MSG_VERBOSE);
            String url = project
                    .getProperty(EasyAntMagicNames.EASYANT_DEFAULT_IVYSETTINGS);
            try {
                easyantIvyConfigure.setUrl(url);
            } catch (MalformedURLException malformedUrl) {
                throw new BuildException(
                        "Unable to parse easyant ivysettings from the following url : "
                                + url, malformedUrl);
            }
        }
        easyantIvyConfigure.setProject(project);
        easyantIvyConfigure.setTaskName("configure-easyant");
        easyantIvyConfigure.execute();

        IvyAntSettings ivyAntSettings = IvyInstanceHelper
                .getEasyAntIvyAntSettings(project);
        return ivyAntSettings.getConfiguredIvyInstance(easyantIvyConfigure);
    }

    /**
     * Get user easyant-ivysettings file
     * 
     * @param project
     * @return the configured user easyant-ivysettings.file
     */
    private File getUserEasyAntIvySettings(Project project) {
        // path can be specified through a property
        String path = project
                .getProperty(EasyAntMagicNames.USER_EASYANT_IVYSETTINGS);
        // if no property is set check the default location
        if (path == null) {
            path = PropertyHelper.getPropertyHelper(project).replaceProperties(
                    EasyAntConstants.DEFAULT_USER_EASYANT_IVYSETTINGS);
        }
        project.log("user's easyant-ivysettings file : " + path,
                Project.MSG_DEBUG);
        return new File(path);
    }

    /**
     * Get global easyant-ivysettings file
     * 
     * @param project
     * @return the configured global easyant-ivysettings.file
     */
    private String getGlobalEasyAntIvySettings(Project project) {
        PropertyHelper helper = PropertyHelper.getPropertyHelper(project);
        String path=null;
        if (configuration.getEasyantIvySettingsFile() != null) {
            File f = new File(helper.replaceProperties(configuration.getEasyantIvySettingsFile()));
            try {
                path = f.toURL().toExternalForm();
            } catch (MalformedURLException e) {
                throw new BuildException("Can't load easyant ivysettings file from "+ f.getAbsolutePath(),e);
            }
        }
        if (configuration.getEasyantIvySettingsUrl() != null) {
            path = helper.replaceProperties(configuration.getEasyantIvySettingsUrl());
        }
        // path can be specified through a property
        if (path==null && project.getProperty(EasyAntMagicNames.GLOBAL_EASYANT_IVYSETTINGS) != null) {
            path = project
                    .getProperty(EasyAntMagicNames.GLOBAL_EASYANT_IVYSETTINGS);
        }
        // if no property is set check the default location
        if (path == null) {
        	File defaultGlboalEasyAntIvySettings =  new File(helper.replaceProperties(
                    EasyAntConstants.DEFAULT_GLOBAL_EASYANT_IVYSETTINGS));
        	if (!defaultGlboalEasyAntIvySettings.exists()) {
        		return null;
        	}
        	try {
				path = defaultGlboalEasyAntIvySettings.toURL().toExternalForm();
			} catch (MalformedURLException e) {
				throw new BuildException("Can't load easyant ivysettings file from "+ defaultGlboalEasyAntIvySettings.getAbsolutePath(),e);
			}
        }
        project.log("global easyant-ivysettings file : " + path,
                Project.MSG_DEBUG);
        return path;
    }

    protected void configurePluginService(Project project,
            Ivy easyantIvyInstance) {
        pluginService = new DefaultPluginServiceImpl(easyantIvyInstance);
        project.addReference(EasyAntMagicNames.PLUGIN_SERVICE_INSTANCE,
                pluginService);

    }

    /**
     * Adds the listeners specified in the command line arguments, along with
     * the default listener, to the specified project.
     * 
     * @param project
     *            The project to add listeners to. Must not be <code>null</code>
     *            .
     */
    protected void addBuildListeners(Project project) {

        // Add the default listener
        project.addBuildListener(createLogger());

        for (int i = 0; i < configuration.getListeners().size(); i++) {
            String className = (String) configuration.getListeners().elementAt(
                    i);
            BuildListener listener = (BuildListener) ClasspathUtils
                    .newInstance(className, EasyAntEngine.class
                            .getClassLoader(), BuildListener.class);
            project.setProjectReference(listener);

            project.addBuildListener(listener);
        }
    }

    /**
     * Creates the InputHandler and adds it to the project.
     * 
     * @param project
     *            the project instance.
     * 
     * @exception BuildException
     *                if a specified InputHandler implementation could not be
     *                loaded.
     */
    protected void addInputHandler(Project project) {
        InputHandler handler = null;

        if (configuration.getInputHandlerClassname() == null) {
            handler = new DefaultInputHandler();
        } else {
            handler = (InputHandler) ClasspathUtils.newInstance(configuration
                    .getInputHandlerClassname(), Main.class.getClassLoader(),
                    InputHandler.class);
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
        BuildLogger logger = null;
        if (configuration.getLoggerClassname() != null) {
            try {
                logger = (BuildLogger) ClasspathUtils.newInstance(configuration
                        .getLoggerClassname(), EasyAntEngine.class
                        .getClassLoader(), BuildLogger.class);
            } catch (BuildException e) {
                throw new RuntimeException("The specified logger class "
                        + configuration.getLoggerClassname()
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
     * <p>
     * Takes the given target as a suffix to append to each parent directory in
     * search of a build file. Once the root of the file-system has been reached
     * an exception is thrown.
     * 
     * @param start
     *            Leaf directory of search. Must not be <code>null</code>.
     * @param suffix
     *            Suffix filename to look for in parents. Must not be
     *            <code>null</code>.
     * 
     * @return A handle to the build file if one is found
     * 
     * @exception BuildException
     *                if no build file is found
     */
    protected File findBuildModule(String start, String suffix)
            throws BuildException {
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
     * configure a given project with current configuration
     * 
     * @param project
     *            a given project
     * @throws BuildException
     */
    public void configureProject(Project project) throws BuildException {

        addBuildListeners(project);
        addInputHandler(project);

        // set the thread priorities
        if (configuration.getThreadPriority() != null) {
            try {
                project.log("Setting Ant's thread priority to "
                        + configuration.getThreadPriority(),
                        Project.MSG_VERBOSE);
                Thread.currentThread().setPriority(
                        configuration.getThreadPriority().intValue());
            } catch (SecurityException swallowed) {
                // we cannot set the priority here.
                project
                        .log("A security manager refused to set the -nice value");
            }
        }

        project.setKeepGoingMode(configuration.isKeepGoingMode());
        if (configuration.isProxy()) {
            // proxy setup if enabledcoreLoader
            ProxySetup proxySetup = new ProxySetup(project);
            proxySetup.enableProxies();
        }

        project.setName("EasyAnt");

    }

    /**
     * Initialize an easyant Project
     * 
     * @param project
     */
    public void initProject(Project project) {
        project.init();
        // set user-define properties
        Enumeration e = configuration.getDefinedProps().keys();
        while (e.hasMoreElements()) {
            String arg = (String) e.nextElement();
            String value = (String) configuration.getDefinedProps().get(arg);
            project.setUserProperty(arg, value);
        }

        project.setUserProperty(EasyAntMagicNames.EASYANT_OFFLINE, Boolean.toString(configuration.isOffline()));

        // Emulate an empty project
        // import task check that projectHelper is at toplevel by checking the
        // size of projectHelper.getImportTask()
        ProjectHelper helper = ProjectHelper.getProjectHelper();
        File mainscript = null;
        try {
            mainscript = File.createTempFile(
                    EasyAntConstants.EASYANT_TASK_NAME, null);
            mainscript.deleteOnExit();
        } catch (IOException e1) {
            throw new BuildException("Can't create temp file", e1);
        }

        Location mainscriptLocation = new Location(mainscript.toString());
        helper.getImportStack().addElement(mainscript);
        project.addReference(ProjectHelper.PROJECTHELPER_REFERENCE, helper);

        // Used to emulate top level target
        Target topLevel = new Target();
        topLevel.setName("");

        // Validate Phase is used by several system plugin so we should
        // initialize it
        Phase validatePhase = new Phase();
        validatePhase.setName("validate");
        validatePhase
                .setDescription("validate the project is correct and all necessary information is available");
        project.addTarget("validate", validatePhase);

        Ivy easyantIvyInstance = configureEasyAntIvyInstance(project);
        configurePluginService(project, easyantIvyInstance);

        // Profile
        if (configuration.getActiveBuildConfigurations().size() != 0) {
            String buildConfigurations = null;
            for (String conf : configuration.getActiveBuildConfigurations()) {
                if (buildConfigurations == null) {
                    buildConfigurations = conf;
                } else {
                    buildConfigurations = buildConfigurations + "," + conf;
                }

            }
            project.log("Active build configurations : " + buildConfigurations,
                    Project.MSG_INFO);
            project.setProperty(EasyAntMagicNames.ACTIVE_BUILD_CONFIGURATIONS,
                    buildConfigurations);
        }

        // Load system plugins
        if (configuration.getSystemPlugins().size() > 0) {
            project.log("Loading System Plugins...");
        }
        for (PluginDescriptor systemPlugin : configuration.getSystemPlugins()) {
            // import/include system plugin
            Import importTask = new Import();
            importTask.setMrid(systemPlugin.getMrid());
            importTask.setOrganisation(systemPlugin.getOrganisation());
            importTask.setModule(systemPlugin.getModule());
            importTask.setRevision(systemPlugin.getRevision());
            importTask.setAs(systemPlugin.getAs());
            importTask.setMode(systemPlugin.getMode());
            importTask.setMandatory(systemPlugin.isMandatory());
            importTask.setProject(project);
            importTask.setTaskName(EasyAntConstants.EASYANT_TASK_NAME);
            importTask.setOwningTarget(topLevel);
            importTask.setLocation(mainscriptLocation);
            importTask.execute();
        }
        File buildModule = configuration.getBuildModule();
        File buildFile = configuration.getBuildFile();
        
        if (project.getProperty("project.basedir")!=null) {
            project.setBaseDir(new File(project.getProperty("project.basedir")));
        }

        if (buildModule == null) {
            buildModule = new File(project.getBaseDir(),EasyAntConstants.DEFAULT_BUILD_MODULE);
        }

        if (!buildModule.exists() && configuration.isBuildModuleLookupEnabled()) {
            buildModule = findBuildModule(System.getProperty("user.dir"),
                    buildModule.toString());
        }

        // calculate buildFile location based on buildModule directory
        if (buildModule.exists() && buildFile == null) {
            buildFile = new File(buildModule.getParentFile(),
                    EasyAntConstants.DEFAULT_BUILD_FILE);
        }

        if (buildFile == null && configuration.isBuildModuleLookupEnabled()) {
            buildFile = findBuildModule(System.getProperty("user.dir"),
                    EasyAntConstants.DEFAULT_BUILD_FILE);
        }

        // Normalize buildFile for re-import detection
        if (buildModule != null) {
            buildModule = FileUtils.getFileUtils().normalize(
                    buildModule.getAbsolutePath());
            project.setNewProperty(EasyAntMagicNames.EASYANT_FILE, buildModule
                    .getAbsolutePath());

        }

        if (buildFile != null) {
            buildFile = FileUtils.getFileUtils().normalize(
                    buildFile.getAbsolutePath());
            project.setNewProperty(MagicNames.ANT_FILE, buildFile
                    .getAbsolutePath());

        }

        configuration.setBuildFile(buildFile);
        configuration.setBuildModule(buildModule);


        if (configuration.getBuildModule() != null
                || configuration.getBuildFile() != null) {
            LoadModule lm = new LoadModule();
            lm.setBuildModule(configuration.getBuildModule());
            lm.setBuildFile(configuration.getBuildFile());
            lm.setTaskName(EasyAntConstants.EASYANT_TASK_NAME);
            lm.setProject(project);
            lm.setOwningTarget(topLevel);
            lm.setLocation(mainscriptLocation);
            lm.execute();
        }
    }

    /**
     * this method run the build process
     * 
     * @throws BuildException
     */
    public void doBuild() throws BuildException {
        final Project project = new Project();
        project.fireBuildStarted();

        Throwable error = null;

        try {

            PrintStream savedErr = System.err;
            PrintStream savedOut = System.out;
            InputStream savedIn = System.in;

            // use a system manager that prevents from System.exit()
            SecurityManager oldsm = null;
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
                System.setOut(new PrintStream(new DemuxOutputStream(project,
                        false)));
                System.setErr(new PrintStream(new DemuxOutputStream(project,
                        true)));
                configureProject(project);
                initProject(project);

                // make sure that we have a target to execute
                if (configuration.getTargets().size() == 0) {
                    if (project.getDefaultTarget() != null) {
                        configuration.getTargets().addElement(
                                project.getDefaultTarget());
                    }
                }

                project.executeTargets(configuration.getTargets());
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
            try {
                project.fireBuildFinished(error);
            } catch (Throwable t) {
                // yes, I know it is bad style to catch Throwable,
                // but if we don't, we lose valuable information
                System.err.println("Caught an exception while logging the"
                        + " end of the build.  Exception was:");
                t.printStackTrace();
                if (error != null) {
                    System.err.println("There has been an error prior to"
                            + " that:");
                    error.printStackTrace();
                }
                throw new BuildException(t);
            }
        }
        if (configuration.isShowMemoryDetails()
                || configuration.getMsgOutputLevel() >= Project.MSG_VERBOSE) {
            printMemoryDetails(project);
        }

    }

    /**
     * This is a static method used to run build process
     * 
     * @param eaConfig
     *            an easyant configuration
     * @throws BuildException
     */
    public static void runBuild(EasyAntConfiguration eaConfig)
            throws BuildException {
        EasyAntEngine eaEngine = new EasyAntEngine(eaConfig);
        eaEngine.doBuild();
    }

    /**
     * This is a static method used to configure and initialize an existing
     * project
     * 
     * @param project
     *            a given project
     * @param eaConfiguration
     *            an easyant configuration
     * @return configured project
     * @throws BuildException
     */
    public static Project configureAndInitProject(Project project,
            EasyAntConfiguration eaConfiguration) throws BuildException {
        EasyAntEngine eaEngine = new EasyAntEngine(eaConfiguration);
        eaEngine.configureProject(project);
        eaEngine.initProject(project);
        return project;
    }

    /**
     * Print memory details
     * 
     * @param project
     *            a given project
     */
    public static void printMemoryDetails(Project project) {
        project.log("---- Memory Details ----");
        project.log("  Used Memory  = "
                + (Runtime.getRuntime().totalMemory() / MEGABYTE - Runtime
                        .getRuntime().freeMemory()
                        / MEGABYTE) + "MB");
        project.log("  Free Memory  = "
                + (Runtime.getRuntime().freeMemory() / MEGABYTE) + "MB");
        project.log("  Total Memory = "
                + (Runtime.getRuntime().totalMemory() / MEGABYTE) + "MB");
        project.log("-----------------------");
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
                String value = (String) configuration.getDefinedProps()
                        .get(arg);
                project.setUserProperty(arg, value);
            }
            project.setName("EasyAnt");
            // not sure we need to invoke init here
            project.init();
            Ivy ivy = configureEasyAntIvyInstance(project);
            configurePluginService(project, ivy);
        }
        return pluginService;
    }

    /**
     * Cache of the EasyAnt version information when it has been loaded.
     */
    private static String easyantVersion = null;

    /**
     * Returns the EasyAnt version information, if available. Once the
     * information has been loaded once, it's cached and returned from the cache
     * on future calls.
     * 
     * @return the Ant version information as a String (always non-
     *         <code>null</code>)
     * 
     * @exception BuildException
     *                if the version information is unavailable
     */
    public static String getEasyAntVersion() {

        if (easyantVersion == null) {
            try {
                Properties props = new Properties();
                InputStream in = Main.class
                        .getResourceAsStream("/META-INF/version.properties");
                props.load(in);
                in.close();

                StringBuffer msg = new StringBuffer();
                msg.append("EasyAnt version ");
                msg.append(props.getProperty("VERSION"));
                msg.append(" compiled on ");
                msg.append(props.getProperty("DATE"));
                easyantVersion = msg.toString();
            } catch (IOException ioe) {
                throw new BuildException(
                        "Could not load the version information:"
                                + ioe.getMessage());
            } catch (NullPointerException npe) {
                throw new BuildException(
                        "Could not load the version information.");
            }
        }
        return easyantVersion;
    }

}
