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

import org.apache.commons.cli.*;
import org.apache.easyant.core.ant.ProjectUtils;
import org.apache.easyant.core.configuration.EasyAntConfiguration;
import org.apache.easyant.core.configuration.EasyantConfigurationFactory;
import org.apache.easyant.man.*;
import org.apache.tools.ant.*;
import org.apache.tools.ant.launch.AntMain;
import org.apache.tools.ant.util.FileUtils;

import java.io.*;
import java.util.*;

/**
 * Command line entry point into EasyAnt. This class is entered via the canonical `public static void main` entry point
 * and reads the command line arguments. It then assembles and executes an Ant project.
 * <p>
 * If you integrating EasyAnt into some other tool, this is not the class to use as an entry point. Instead you should
 * have a look at {@link EasyAntEngine}.
 * </p>
 */
public class EasyAntMain implements AntMain {
    /**
     * A Set of args are are handled by the launcher and should not be seen by Main.
     */
    private static final Set<String> LAUNCH_COMMANDS = new HashSet<String>();
    private boolean isLogFileUsed;

    static {
        LAUNCH_COMMANDS.add("-lib");
        LAUNCH_COMMANDS.add("-cp");
        LAUNCH_COMMANDS.add("-noclasspath");
        LAUNCH_COMMANDS.add("--noclasspath");
        LAUNCH_COMMANDS.add("-nouserlib");
        LAUNCH_COMMANDS.add("-main");
    }

    private EasyAntConfiguration easyAntConfiguration;
    private boolean projectHelp;

    private Options options = new Options();

    /**
     * Whether or not this instance has successfully been constructed and is ready to run.
     */
    private boolean readyToRun;
    private List<String> propertyFiles = new ArrayList<String>(1);

    /**
     * Prints the message of the Throwable if it (the message) is not <code>null</code>.
     *
     * @param t Throwable to print the message of. Must not be <code>null</code>.
     */
    private static void printMessage(Throwable t) {
        String message = t.getMessage();
        if (message != null) {
            System.err.println(message);
        }
    }

    /**
     * Creates a new instance of this class using the arguments specified, gives it any extra user properties which have
     * been specified, and then runs the build using the classloader provided.
     *
     * @param args                     Command line arguments. Must not be <code>null</code>.
     * @param additionalUserProperties Any extra properties to use in this build. May be <code>null</code>, which is the equivalent to
     *                                 passing in an empty set of properties.
     * @param coreLoader               Classloader used for core classes. May be <code>null</code> in which case the system classloader is
     *                                 used.
     */
    public static void start(String[] args, Properties additionalUserProperties, ClassLoader coreLoader) {
        EasyAntMain m = new EasyAntMain();
        m.startAnt(args, additionalUserProperties, coreLoader);
    }

    /**
     * Start Ant
     *
     * @param args                     command line args
     * @param additionalUserProperties properties to set beyond those that may be specified on the args list
     * @param coreLoader               - not used
     * @since Ant 1.6
     */
    public void startAnt(String[] args, Properties additionalUserProperties, ClassLoader coreLoader) {
        easyAntConfiguration.setCoreLoader(coreLoader);
        configureOptions();
        CommandLineParser parser = new GnuParser();
        CommandLine line;
        try {
            line = parser.parse(options, args);
            processArgs(line);
        } catch (ParseException exc) {
            if (easyAntConfiguration.getMsgOutputLevel() >= Project.MSG_VERBOSE) {
                exc.printStackTrace();
            }
            handleLogfile();
            printMessage(exc);
            exit(1);
            return;
        }

        if (additionalUserProperties != null) {
            Enumeration<?> properties = additionalUserProperties.propertyNames();
            while (properties.hasMoreElements()) {
                String key = (String) properties.nextElement();
                String property = additionalUserProperties.getProperty(key);
                easyAntConfiguration.getDefinedProps().put(key, property);
            }
        }

        // expect the worst
        int exitCode = 1;
        try {
            try {
                runBuild(line);
                exitCode = 0;
            } catch (ExitStatusException ese) {
                exitCode = ese.getStatus();
                if (exitCode != 0) {
                    throw ese;
                }
            }
        } catch (BuildException be) {
            // do nothing they have been already logged by our logger
        } catch (Throwable exc) {
            exc.printStackTrace();
            printMessage(exc);
        } finally {
            handleLogfile();
        }
        exit(exitCode);
    }

    /**
     * This operation is expected to call {@link System#exit(int)}, which is what the base version does. However, it is
     * possible to do something else.
     *
     * @param exitCode code to exit with
     */
    protected void exit(int exitCode) {
        System.exit(exitCode);
    }

    /**
     * Close logfiles, if we have been writing to them.
     *
     * @since Ant 1.6
     */
    private void handleLogfile() {
        if (isLogFileUsed && easyAntConfiguration != null) {
            FileUtils.close(easyAntConfiguration.getOut());
            FileUtils.close(easyAntConfiguration.getErr());
        }
    }

    /**
     * Command line entry point. This method kicks off the building of a project object and executes a build using
     * either a given target or the default target.
     *
     * @param args Command line arguments. Must not be <code>null</code>.
     */
    public static void main(String[] args) {
        start(args, null, null);
    }

    /**
     * Constructor used when creating Main for later arg processing and startup
     */
    public EasyAntMain() {
        easyAntConfiguration = EasyantConfigurationFactory.getInstance().createDefaultConfiguration();
    }

    /**
     * Process command line arguments. When ant is started from Launcher, launcher-only arguments do not get passed
     * through to this routine.
     *
     * @since Ant 1.6
     */
    private void processArgs(CommandLine line) {
        String searchForThis;
        PrintStream logTo = null;

        if (line.hasOption("help")) {
            printUsage();
            return;
        }
        if (easyAntConfiguration.getMsgOutputLevel() >= Project.MSG_VERBOSE || line.hasOption("version")) {
            printVersion();
            if (line.hasOption("version")) {
                return;
            }
        }
        if (line.hasOption("showMemoryDetails")) {
            easyAntConfiguration.setShowMemoryDetails(true);
        }
        if (line.hasOption("diagnostics")) {
            Diagnostics.doReport(System.out, easyAntConfiguration.getMsgOutputLevel());
            return;
        }
        if (line.hasOption("quiet")) {
            easyAntConfiguration.setMsgOutputLevel(Project.MSG_WARN);
        }
        if (line.hasOption("verbose")) {
            easyAntConfiguration.setMsgOutputLevel(Project.MSG_VERBOSE);
        }
        if (line.hasOption("debug")) {
            easyAntConfiguration.setMsgOutputLevel(Project.MSG_DEBUG);
        }
        if (line.hasOption("noinput")) {
            easyAntConfiguration.setAllowInput(false);
        }
        if (line.hasOption("logfile")) {
            try {
                File logFile = new File(line.getOptionValue("logfile"));
                logTo = new PrintStream(new FileOutputStream(logFile));
                isLogFileUsed = true;
            } catch (IOException ioe) {
                String msg = "Cannot write on the specified log file. "
                        + "Make sure the path exists and you have write " + "permissions.";
                throw new BuildException(msg);
            } catch (ArrayIndexOutOfBoundsException aioobe) {
                String msg = "You must specify a log file when " + "using the -log argument";
                throw new BuildException(msg);
            }
        }
        if (line.hasOption("buildmodule")) {
            File buildModule = new File(line.getOptionValue("buildmodule").replace('/', File.separatorChar));
            easyAntConfiguration.setBuildModule(buildModule);
        }
        if (line.hasOption("buildfile")) {
            File buildFile = new File(line.getOptionValue("buildfile").replace('/', File.separatorChar));
            easyAntConfiguration.setBuildFile(buildFile);
        }
        if (line.hasOption("buildconf")) {
            easyAntConfiguration.getActiveBuildConfigurations().add(line.getOptionValue("buildconf"));
        }

        File easyantConfFile = null;

        if (line.hasOption("configfile")) {
            easyantConfFile = new File(line.getOptionValue("configfile").replace('/', File.separatorChar));
        } else {
            // if no command line switch is specified check the default location

            File easyantHome = new File(System.getProperty(EasyAntMagicNames.EASYANT_HOME).replace('/',
                    File.separatorChar));
            File defaultGlobalEasyantConfFile = new File(easyantHome, EasyAntConstants.DEFAULT_GLOBAL_EASYANT_CONF_FILE);

            if (defaultGlobalEasyantConfFile.exists()) {
                easyantConfFile = defaultGlobalEasyantConfFile;
            }
        }

        if (easyantConfFile != null) {
            try {
                easyAntConfiguration = EasyantConfigurationFactory.getInstance().createConfigurationFromFile(
                        easyAntConfiguration, easyantConfFile.toURI().toURL());
            } catch (Exception e) {
                throw new BuildException(e);
            }
        }

        if (line.hasOption("listener")) {
            easyAntConfiguration.getListeners().add(line.getOptionValue("listener"));
        }
        if (line.hasOption("D")) {
            easyAntConfiguration.getDefinedProps().putAll(line.getOptionProperties("D"));
        }
        if (line.hasOption("logger")) {
            if (easyAntConfiguration.getLoggerClassname() != null) {
                throw new BuildException("Only one logger class may be specified.");
            }
            easyAntConfiguration.setLoggerClassname(line.getOptionValue("logger"));
        }
        if (line.hasOption("inputhandler")) {
            if (easyAntConfiguration.getInputHandlerClassname() != null) {
                throw new BuildException("Only one input handler class may " + "be specified.");
            }
            easyAntConfiguration.setInputHandlerClassname(line.getOptionValue("inputhandler"));
        }
        if (line.hasOption("emacs")) {
            easyAntConfiguration.setEmacsMode(true);
        }
        if (line.hasOption("projecthelp")) {
            // set the flag to display the targets and quit
            projectHelp = true;
        }
        if (line.hasOption("find")) {
            // eat up next arg if present, default to module.ivy
            if (line.getOptionValues("find").length > 0) {
                searchForThis = line.getOptionValue("find");

            } else {
                searchForThis = EasyAntConstants.DEFAULT_BUILD_MODULE;
            }
            easyAntConfiguration.setBuildModule(new File(searchForThis));
            easyAntConfiguration.setBuildModuleLookupEnabled(true);
        }
        if (line.hasOption("propertyfile")) {
            propertyFiles.add(line.getOptionValue("propertyfile"));
        }
        if (line.hasOption("keep-going")) {
            easyAntConfiguration.setKeepGoingMode(true);
        }
        if (line.hasOption("offline")) {
            easyAntConfiguration.setOffline(true);
        }
        if (line.hasOption("nice")) {
            easyAntConfiguration.setThreadPriority(Integer.decode(line.getOptionValue("nice")));

            if (easyAntConfiguration.getThreadPriority() < Thread.MIN_PRIORITY
                    || easyAntConfiguration.getThreadPriority() > Thread.MAX_PRIORITY) {
                throw new BuildException("Niceness value is out of the range 1-10");
            }
        }
        if (line.hasOption("autoproxy")) {
            easyAntConfiguration.setProxy(true);
        }
        if (!line.getArgList().isEmpty()) {
            for (Object o : line.getArgList()) {
                String target = (String) o;
                easyAntConfiguration.getTargets().add(target);
            }
        }

        // Load the property files specified by -propertyfile
        loadPropertyFiles();

        if (logTo != null) {
            easyAntConfiguration.setOut(logTo);
            easyAntConfiguration.setErr(logTo);
            System.setOut(easyAntConfiguration.getOut());
            System.setErr(easyAntConfiguration.getErr());
        }
        readyToRun = true;
    }

    // --------------------------------------------------------
    // other methods
    // --------------------------------------------------------

    /**
     * Load the property files specified by -propertyfile
     */
    private void loadPropertyFiles() {
        for (String filename : propertyFiles) {
            Properties props = new Properties();
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(filename);
                props.load(fis);
            } catch (IOException e) {
                System.out.println("Could not load property file " + filename + ": " + e.getMessage());
            } finally {
                FileUtils.close(fis);
            }

            // ensure that -D properties take precedence
            Enumeration<?> properties = props.propertyNames();
            while (properties.hasMoreElements()) {
                String name = (String) properties.nextElement();
                if (easyAntConfiguration.getDefinedProps().getProperty(name) == null) {
                    easyAntConfiguration.getDefinedProps().put(name, props.getProperty(name));
                }
            }
        }
    }

    /**
     * Executes the build. If the constructor for this instance failed (e.g. returned after issuing a warning), this
     * method returns immediately.
     *
     * @throws BuildException if the build fails
     */
    private void runBuild(CommandLine line) throws BuildException {
        if (!readyToRun) {
            return;
        }
        if (projectHelp) {
            displayProjectHelp();
        } else {
            EasyAntEngine eaEngine = new EasyAntEngine(easyAntConfiguration);
            Project project = new Project();
            eaEngine.configureEasyAnt(project);
            eaEngine.loadProject(project);
            // handle other easyant option (-listTargets,-describe,etc..)
            for (int i = 0; i < line.getOptions().length; i++) {
                if (line.getOptions()[i] instanceof EasyantOption) {
                    EasyantOption eaoption = (EasyantOption) line.getOptions()[i];
                    eaoption.setProject(project);
                    eaoption.execute();
                    if (eaoption.isStopBuild()) {
                        return;
                    }
                }
            }
            eaEngine.doBuild(project);
        }

    }

    /**
     * Prints the description of a project (if there is one) to <code>System.out</code>.
     *
     * @param project The project to display a description of. Must not be <code>null</code>.
     */
    protected void printDescription(Project project) {
        if (project.getDescription() != null) {
            project.log(project.getDescription());
        }
    }

    /**
     * Searches for the correct place to insert a name into a list so as to keep the list sorted alphabetically.
     *
     * @param names The current list of names. Must not be <code>null</code>.
     * @param name  The name to find a place for. Must not be <code>null</code>.
     * @return the correct place in the list for the given name
     */
    private static int findTargetPosition(List<String> names, String name) {
        int res = names.size();
        for (int i = 0; i < names.size() && res == names.size(); i++) {
            if (name.compareTo(names.get(i)) < 0) {
                res = i;
            }
        }
        return res;
    }

    /**
     * Writes a formatted list of target names to <code>System.out</code> with an optional description.
     *
     * @param project      the project instance.
     * @param names        The names to be printed. Must not be <code>null</code>.
     * @param descriptions The associated target descriptions. May be <code>null</code>, in which case no descriptions are
     *                     displayed. If non- <code>null</code>, this should have as many elements as <code>names</code>.
     * @param heading      The heading to display. Should not be <code>null</code>.
     * @param maxlen       The maximum length of the names of the targets. If descriptions are given, they are padded to this
     *                     position so they line up (so long as the names really <i>are</i> shorter than this).
     */
    private static void printTargets(Project project, List<String> names, List<String> descriptions, String heading,
                                     int maxlen) {
        if (!names.isEmpty()) {
            // now, start printing the targets and their descriptions
            String lSep = System.getProperty("line.separator");
            String spaces = String.format("%" + maxlen + "s", ' ');
            StringBuilder msg = new StringBuilder();
            msg.append(lSep).append(heading).append(lSep).append(lSep);
            for (int i = 0; i < names.size(); i++) {
                msg.append(" ");
                msg.append(names.get(i));
                if (descriptions != null) {
                    msg.append(spaces.substring(0, maxlen - (names.get(i)).length() + 2));
                    msg.append(descriptions.get(i));
                }
                msg.append(lSep);
            }
            project.log(msg.toString(), Project.MSG_WARN);
        }
    }

    /**
     * Prints a list of all targets in the specified project to <code>System.out</code>, optionally including
     * subtargets.
     *
     * @param project         The project to display a description of. Must not be <code>null</code>.
     * @param printSubTargets Whether or not subtarget names should also be printed.
     */
    protected static void printTargets(Project project, boolean printSubTargets) {
        // find the target with the longest name
        int maxLength = 0;
        Map<String, Target> ptargets = ProjectUtils.removeDuplicateTargets(project.getTargets());
        String targetName;
        String targetDescription;
        // split the targets in top-level and sub-targets depending
        // on the presence of a description
        List<String> topNames = new ArrayList<String>();
        List<String> topDescriptions = new ArrayList<String>();
        List<String> subNames = new ArrayList<String>();

        List<String> highLevelTargets = new ArrayList<String>();
        List<String> highLevelTargetsDescriptions = new ArrayList<String>();
        for (Target currentTarget : ptargets.values()) {
            targetName = currentTarget.getName();
            if (targetName.equals("")) {
                continue;
            }
            targetDescription = currentTarget.getDescription();
            // maintain a sorted list of targets
            if (currentTarget instanceof ExtensionPoint && !currentTarget.getName().contains(":")) {
                int pos = findTargetPosition(highLevelTargets, targetName);
                highLevelTargets.add(pos, targetName);
                highLevelTargetsDescriptions.add(pos, targetDescription);
            } else if (targetDescription != null) {
                int pos = findTargetPosition(topNames, targetName);
                topNames.add(pos, targetName);
                topDescriptions.add(pos, targetDescription);
            } else {
                int pos = findTargetPosition(subNames, targetName);
                subNames.add(pos, targetName);
            }
            if (targetName.length() > maxLength) {
                maxLength = targetName.length();
            }

        }

        printTargets(project, highLevelTargets, highLevelTargetsDescriptions, "High level targets:", maxLength);
        printTargets(project, topNames, topDescriptions, "Main targets:", maxLength);
        // if there were no main targets, we list all subtargets
        // as it means nothing has a description
        if (topNames.isEmpty()) {
            printSubTargets = true;
        }
        if (printSubTargets) {
            printTargets(project, subNames, null, "Other targets:", maxLength);
        } else {
            project.log("Run easyant with '-v' or '--verbose' option to have the whole list of available targets / extension points");
        }

        String defaultTarget = project.getDefaultTarget();
        if (defaultTarget != null && !"".equals(defaultTarget)) {
            // shouldn't need to check but...
            project.log("Default target: " + defaultTarget);
        }
    }

    private void displayProjectHelp() {
        final Project project = new Project();
        Throwable error = null;

        try {

            EasyAntEngine.configureAndLoadProject(project, easyAntConfiguration);
            printDescription(project);
            printTargets(project, easyAntConfiguration.getMsgOutputLevel() > Project.MSG_INFO);

        } catch (RuntimeException exc) {
            error = exc;
            throw exc;
        } finally {
            if (error != null) {
                project.log(error.toString(), Project.MSG_ERR);
            }
        }
    }

    /**
     * Configure command line options
     */
    @SuppressWarnings("static-access")
    public void configureOptions() {
        options.addOption("h", "help", false, "print this message");

        options.addOption("p", "projecthelp", false, "print project help information");
        options.addOption("version", false, "print the version information and exit");
        options.addOption("diagnostics", false,
                "print information that might be helpful to diagnose or report problems");
        options.addOption("showMemoryDetails", false, "print memory details (used/free/total)");
        options.addOption("q", "quiet", false, "be extra quiet");
        options.addOption("v", "verbose", false, "be extra verbose");
        options.addOption("d", "debug", false, "print debugging information");
        options.addOption("e", "emacs", false, "produce logging information without adornments");
        Option lib = OptionBuilder.withArgName("path").hasArg()
                .withDescription("specifies a path to search for jars and classes").create("lib");
        options.addOption(lib);
        Option logfile = OptionBuilder.withArgName("file").hasArg().withDescription("use given file for log")
                .create("logfile");
        options.addOption(logfile);
        Option logger = OptionBuilder.withArgName("classname").hasArg()
                .withDescription("the class which it to perform " + "logging").create("logger");
        options.addOption(logger);
        Option listener = OptionBuilder.withArgName("classname").hasArg()
                .withDescription("add an instance of class as " + "a project listener").create("listener");
        options.addOption(listener);
        Option buildfile = OptionBuilder.withArgName("file").hasArg().withDescription("use given buildfile")
                .create("buildfile");
        options.addOption(buildfile);
        Option find = OptionBuilder.withArgName("file").hasOptionalArg()
                .withDescription("search for buildfile towards the " + "root of the filesystem and use it")
                .withLongOpt("find").create("s");
        options.addOption(find);
        options.addOption("noinput", false, "do not allow interactive input");
        Option buildmodule = OptionBuilder.withArgName("file").hasArg().withDescription("use given buildmodule")
                .withLongOpt("buildmodule").create("f");
        options.addOption(buildmodule);

        Option buildconf = OptionBuilder.withArgName("confs").hasArg()
                .withDescription("specify build configurations (profiles)").withLongOpt("buildconf").create("C");
        options.addOption(buildconf);
        Option configFile = OptionBuilder.withArgName("file").hasArg()
                .withDescription("use given easyant configuration").create("configfile");
        options.addOption(configFile);
        Option property = OptionBuilder.withArgName("property=value").hasArgs(2).withValueSeparator()
                .withDescription("use value for given property").create("D");
        options.addOption(property);
        options.addOption("k", "keep-going", false, "execute all targets that do not depend on failed target(s)");
        Option propertiesfile = OptionBuilder.withArgName("file").hasArg()
                .withDescription("load all properties from file with -D properties taking precedence")
                .create("propertyfile");
        options.addOption(propertiesfile);
        Option inputhandler = OptionBuilder.withArgName("classname").hasArg()
                .withDescription("the class which will handle input requests").create("inputhandler");
        options.addOption(inputhandler);
        Option nice = OptionBuilder.withArgName("number").hasArg()
                .withDescription("A niceness value for the main thread: 1 (lowest) to 10 (highest); 5 is the default")
                .create("nice");
        options.addOption(nice);
        options.addOption("nouserlib", false, "Run ant without using the jar files from ${user.home}/.ant/lib");
        options.addOption("noclasspath", false, "Run ant without using CLASSPATH");
        options.addOption("autoproxy", false, "Java1.5+: use the OS proxy settings");
        Option main = OptionBuilder.withArgName("classname").hasArg()
                .withDescription("override EasyAnt's normal entry point").create("main");
        options.addOption(main);
        options.addOption("o", "offline", false, "turns EasyAnt in offline mode");
        options.addOption(new Describe());
        options.addOption(new ListExtensionPoints());
        options.addOption(new ListTargets());
        options.addOption(new ListProps());
        options.addOption(new ListParameters());
        options.addOption(new ListPlugins());
    }

    /**
     * Prints the usage information for this class to <code>System.out</code>.
     */
    private void printUsage() {

        HelpFormatter help = new HelpFormatter();
        help.printHelp("easyant [options] [target [target2 [target3] ...]]", options);

    }

    /**
     * Prints the EasyAnt version information to <code>System.out</code>.
     *
     * @throws BuildException if the version information is unavailable
     */
    private static void printVersion() throws BuildException {
        System.out.println(EasyAntEngine.getEasyAntVersion());
        System.out.println(getAntVersion());
    }

    /**
     * Cache of the Ant version information when it has been loaded.
     */
    private static String antVersion = null;

    /**
     * Returns the Ant version information, if available. Once the information has been loaded once, it's cached and
     * returned from the cache on future calls.
     *
     * @return the Ant version information as a String (always non- <code>null</code>)
     * @throws BuildException if the version information is unavailable
     */
    public static synchronized String getAntVersion() throws BuildException {
        if (antVersion == null) {
            InputStream in = null;
            try {
                Properties props = new Properties();
                in = Main.class.getResourceAsStream("/org/apache/tools/ant/version.txt");
                if (in == null) {
                    throw new BuildException("Could not load the version information.");
                }
                props.load(in);

                antVersion = "Apache Ant version " + props.getProperty("VERSION") + " compiled on " + props.getProperty("DATE");
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
        return antVersion;
    }

}
