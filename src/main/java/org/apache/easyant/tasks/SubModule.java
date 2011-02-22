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
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.apache.easyant.core.EasyAntConstants;
import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.easyant.core.ant.listerners.MultiModuleLogger;
import org.apache.easyant.core.ivy.IvyInstanceHelper;
import org.apache.ivy.ant.IvyAntSettings;
import org.apache.ivy.ant.IvyPublish;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.StringUtils;

/**
 * This task is used to manage orchestration of submodules.
 */
public class SubModule extends Task {

    private boolean failOnError = true;
    private boolean verbose = false;
    private String moduleFile = EasyAntConstants.DEFAULT_BUILD_MODULE;

    private Path buildpath;
    private TargetList targets = null;
    private boolean useBuildRepository = false;
    private boolean overwrite = true;

    public void execute() throws BuildException {
        if (buildpath == null) {
            throw new BuildException("No buildpath specified");
        }
        final String[] filenames = buildpath.list();
        final int count = filenames.length;
        if (count < 1) {
            log("No sub-builds to iterate on", Project.MSG_WARN);
            return;
        }

        // Change the default output logger
        PrintStream out = System.out;
        PrintStream err = System.err;
        int currentLogLevel = Project.MSG_INFO;
        log("removing current logger", Project.MSG_DEBUG);
        // since BuildLogger doesn't offer any way to get the out / err print
        // streams we should use reflection
        // TODO: we should find a better way to do this
        for (Iterator<?> i = getProject().getBuildListeners().iterator(); i
                .hasNext();) {
            BuildListener l = (BuildListener) i.next();
            if (l instanceof DefaultLogger) {
                Field fields[];
                //case of classes extending DefaultLogger
                if (l.getClass().getSuperclass() == DefaultLogger.class) {
                    fields = l.getClass().getSuperclass().getDeclaredFields();
                } else {
                    fields = l.getClass().getDeclaredFields();
                }
                
                for (int j = 0; j < fields.length; j++) {
                    try {
                        if (fields[j].getType().equals(PrintStream.class)
                                && fields[j].getName().equals("out")) {
                            fields[j].setAccessible(true);
                            out = (PrintStream) fields[j].get(l);
                            fields[j].setAccessible(false);
                        }
                        if (fields[j].getType().equals(PrintStream.class)
                                && fields[j].getName().equals("err")) {
                            fields[j].setAccessible(true);
                            err = (PrintStream) fields[j].get(l);
                            fields[j].setAccessible(false);
                        }
                        if (fields[j].getName().equals("msgOutputLevel")) {
                            fields[j].setAccessible(true);
                            currentLogLevel = (Integer) fields[j].get(l);
                            fields[j].setAccessible(false);
                        }
                    } catch (IllegalAccessException ex) {
                        throw new BuildException(ex);
                    }
                }
            }
            getProject().removeBuildListener(l);

        }
        log("Initializing BigProjectLogger", Project.MSG_DEBUG);
        // Intanciate the new logger
        BuildLogger bl = new MultiModuleLogger();
        bl.setOutputPrintStream(out);
        bl.setErrorPrintStream(err);
        bl.setMessageOutputLevel(currentLogLevel);
        getProject().setProjectReference(bl);
        getProject().addBuildListener(bl);

        BuildException buildException = null;
        for (int i = 0; i < count; ++i) {
            File file = null;
            String subdirPath = null;
            Throwable thrownException = null;
            try {
                File directory = null;
                file = new File(filenames[i]);
                if (file.isDirectory()) {
                    if (verbose) {
                        subdirPath = file.getPath();
                        log("Entering directory: " + subdirPath + "
",
                                Project.MSG_INFO);
                    }
                    file = new File(file, moduleFile);
                }
                directory = file.getParentFile();
                execute(file, directory);
                if (verbose && subdirPath != null) {
                    log("Leaving directory: " + subdirPath + "
",
                            Project.MSG_INFO);
                }
            } catch (RuntimeException ex) {
                if (!(getProject().isKeepGoingMode())) {
                    if (verbose && subdirPath != null) {
                        log("Leaving directory: " + subdirPath + "
",
                                Project.MSG_INFO);
                    }
                    throw ex; // throw further
                }
                thrownException = ex;
            } catch (Throwable ex) {
                if (!(getProject().isKeepGoingMode())) {
                    if (verbose && subdirPath != null) {
                        log("Leaving directory: " + subdirPath + "
",
                                Project.MSG_INFO);
                    }
                    throw new BuildException(ex);
                }
                thrownException = ex;
            }
            if (thrownException != null) {
                if (thrownException instanceof BuildException) {
                    log("File '" + file + "' failed with message '"
                            + thrownException.getMessage() + "'.",
                            Project.MSG_ERR);
                    // only the first build exception is reported
                    if (buildException == null) {
                        buildException = (BuildException) thrownException;
                    }
                } else {
                    log("Target '" + file + "' failed with message '"
                            + thrownException.getMessage() + "'.",
                            Project.MSG_ERR);
                    thrownException.printStackTrace(System.err);
                    if (buildException == null) {
                        buildException = new BuildException(thrownException);
                    }
                }
                if (verbose && subdirPath != null) {
                    log("Leaving directory: " + subdirPath + "
",
                            Project.MSG_INFO);
                }
            }
        }
        // check if one of the builds failed in keep going mode
        if (buildException != null) {
            throw buildException;
        }
    }

    /**
     * Runs the given target on the provided build file.
     * 
     * @param file
     *            the build file to execute
     * @param directory
     *            the directory of the current iteration
     * @throws BuildException
     *             is the file cannot be found, read, is a directory, or the
     *             target called failed, but only if <code>failOnError</code> is
     *             <code>true</code>. Otherwise, a warning log message is simply
     *             output.
     */
    private void execute(File file, File directory) throws BuildException {
        if (!file.exists() || file.isDirectory() || !file.canRead()) {
            String msg = "Invalid file: " + file;
            if (failOnError) {
                throw new BuildException(msg);
            }
            log(msg, Project.MSG_WARN);
            return;
        }

        Project subModule = getProject().createSubProject();

        subModule.setJavaVersionProperty();

        for (int i = 0; i < getProject().getBuildListeners().size(); i++) {
            BuildListener buildListener = (BuildListener) getProject()
                    .getBuildListeners().elementAt(i);
            subModule.addBuildListener(buildListener);
        }

        subModule.setName(file.getName());
        subModule.setBaseDir(directory);

        subModule.fireSubBuildStarted();

        try {
            // Emulate an empty project
            // import task check that projectHelper is at toplevel by checking
            // the
            // size of projectHelper.getImportTask()
            ProjectHelper projectHelper = ProjectHelper.getProjectHelper();
            File mainscript;
            try {
                mainscript = File.createTempFile(
                        EasyAntConstants.EASYANT_TASK_NAME, null);
                mainscript.deleteOnExit();
            } catch (IOException e1) {
                throw new BuildException("Can't create temp file", e1);
            }

            @SuppressWarnings("unchecked")
            Vector<File> imports = projectHelper.getImportStack();
            imports.addElement(mainscript);
            subModule.addReference(ProjectHelper.PROJECTHELPER_REFERENCE,
                    projectHelper);

            // copy all User properties
            addAlmostAll(getProject().getUserProperties(), subModule,
                    PropertyType.USER);

            // copy easyantIvyInstance
            IvyAntSettings ivyAntSettings = IvyInstanceHelper.getEasyAntIvyAntSettings(getProject());
            subModule.addReference(EasyAntMagicNames.EASYANT_IVY_INSTANCE,
                    ivyAntSettings);

            // buildFile should be in the same directory of buildModule
            File buildfile = new File(directory,
                    EasyAntConstants.DEFAULT_BUILD_FILE);
            if (buildfile.exists()) {
                subModule.setNewProperty(MagicNames.ANT_FILE, buildfile
                        .getAbsolutePath());
            }
            subModule.setNewProperty(EasyAntMagicNames.EASYANT_FILE, file
                    .getAbsolutePath());

            // inherit meta.target directory, for shared build repository.
            String metaTarget = getProject().getProperty("meta.target");
            if (metaTarget != null) {
                File metaDir = getProject().resolveFile(metaTarget);
                subModule.setNewProperty("meta.target", metaDir
                        .getAbsolutePath());
            }

            // Used to emulate top level target
            Target topLevel = new Target();
            topLevel.setName("");

            LoadModule lm = new LoadModule();
            lm.setBuildModule(file);
            lm.setBuildFile(buildfile);
            lm.setTaskName(EasyAntConstants.EASYANT_TASK_NAME);
            lm.setProject(subModule);
            lm.setOwningTarget(topLevel);
            lm.setLocation(new Location(mainscript.toString()));
            lm.setUseBuildRepository(useBuildRepository);
            lm.execute();

            filterTargets(subModule);
            printExecutingTargetMsg(subModule);

            if (targets != null && !targets.isEmpty()) {
                subModule.executeTargets(targets);
                if (useBuildRepository) {

                    File artifactsDir = subModule.resolveFile(subModule
                            .getProperty("target.artifacts"));
                    if (artifactsDir.isDirectory()) {

                        // this property set by LoadModule task when it
                        // configures the build repo
                        String resolver = subModule
                                .getProperty(EasyAntMagicNames.EASYANT_BUILD_REPOSITORY);

                        subModule.log("Publishing in build scoped repository",
                                Project.MSG_INFO);
                        // Publish on build scoped repository
                        IvyPublish ivyPublish = new IvyPublish();
                        ivyPublish.setSettingsRef(IvyInstanceHelper.buildProjectIvyReference(subModule));
                        ivyPublish.setResolver(resolver);
                        // TODO: this should be more flexible!
                        ivyPublish
                                .setArtifactspattern("${target.artifacts}/[artifact](-[classifier]).[ext]");
                        // not all sub-build targets will generate ivy
                        // artifacts. we don't want to fail
                        // a successful build just because there's nothing to
                        // publish.
                        ivyPublish.setWarnonmissing(false);
                        ivyPublish.setHaltonmissing(false);
                        ivyPublish.setProject(subModule);
                        ivyPublish.setOwningTarget(getOwningTarget());
                        ivyPublish.setLocation(getLocation());
                        ivyPublish.setOverwrite(overwrite);
                        ivyPublish.execute();
                    } else {
                        subModule.log("Skipping publish because "
                                + artifactsDir.getPath()
                                + " is not a directory", Project.MSG_VERBOSE);
                    }
                }
            } else {
                subModule
                        .log(
                                "Skipping sub-project build because no matching targets were found",
                                Project.MSG_VERBOSE);
            }
            subModule.fireSubBuildFinished(null);
        } catch (BuildException e) {
            subModule.fireSubBuildFinished(e);
            throw e;
        }

    }

    /**
     * Filter the active set of targets to only those defined in the given
     * project.
     */
    private void filterTargets(Project subProject) {
        Set<?> keys = subProject.getTargets().keySet();
        for (Iterator<String> it = targets.iterator(); it.hasNext();) {
            String target = it.next();
            if (!keys.contains(target) && target.trim().length() > 0) {
                subProject.log("Skipping undefined target '" + target + "'",
                        Project.MSG_VERBOSE);
                it.remove();
            }
        }
    }

    /**
     * Print a message when executing the target
     * 
     * @param subProject
     *            a subproject where the log will be printed
     */
    private void printExecutingTargetMsg(Project subProject) {
        final String HEADER = "======================================================================";
        StringBuilder sb = new StringBuilder();
        sb.append(HEADER).append(StringUtils.LINE_SEP);
        sb.append("Executing ").append(targets).append(" on ").append(
                subProject.getName());
        sb.append(StringUtils.LINE_SEP).append(HEADER);
        subProject.log(sb.toString());
    }

    /**
     * Copies all properties from the given table to the new project - omitting
     * those that have already been set in the new project as well as properties
     * named basedir or ant.file.
     * 
     * @param props
     *            properties <code>Hashtable</code> to copy to the new project.
     * @param the
     *            type of property to set (a plain Ant property, a user property
     *            or an inherited property).
     * @since Ant 1.8.0
     */
    private void addAlmostAll(Hashtable<?, ?> props, Project subProject,
            PropertyType type) {
        Enumeration<?> e = props.keys();
        while (e.hasMoreElements()) {
            String key = e.nextElement().toString();
            if (MagicNames.PROJECT_BASEDIR.equals(key)
                    || MagicNames.ANT_FILE.equals(key)) {
                // basedir and ant.file get special treatment in execute()
                continue;
            }

            String value = props.get(key).toString();
            if (type == PropertyType.PLAIN) {
                // don't re-set user properties, avoid the warning message
                if (subProject.getProperty(key) == null) {
                    // no user property
                    subProject.setNewProperty(key, value);
                }
            } else if (type == PropertyType.USER) {
                subProject.setUserProperty(key, value);
            } else if (type == PropertyType.INHERITED) {
                subProject.setInheritedProperty(key, value);
            }
        }
    }

    private static final class PropertyType {
        private PropertyType() {
        }

        private static final PropertyType PLAIN = new PropertyType();
        private static final PropertyType INHERITED = new PropertyType();
        private static final PropertyType USER = new PropertyType();
    }

    /**
     * The target to call on the different sub-builds. Set to "" to execute the
     * default target.
     * 
     * @param target
     *            the target
     *            <p>
     */
    // REVISIT: Defaults to the target name that contains this task if not
    // specified.
    public void setTarget(String target) {
        setTargets(new TargetList(target));
    }

    /**
     * The targets to call on the different sub-builds.
     * 
     * @param target
     *            a list of targets to execute
     */
    public void setTargets(TargetList targets) {
        this.targets = targets;
    }

    /**
     * Set the buildpath to be used to find sub-projects.
     * 
     * @param s
     *            an Ant Path object containing the buildpath.
     */
    public void setBuildpath(Path s) {
        getBuildpath().append(s);
    }

    /**
     * Gets the implicit build path, creating it if <code>null</code>.
     * 
     * @return the implicit build path.
     */
    private Path getBuildpath() {
        if (buildpath == null) {
            buildpath = new Path(getProject());
        }
        return buildpath;
    }

    /**
     * Buildpath to use, by reference.
     * 
     * @param r
     *            a reference to an Ant Path object containing the buildpath.
     */
    public void setBuildpathRef(Reference r) {
        createBuildpath().setRefid(r);
    }

    /**
     * Creates a nested build path, and add it to the implicit build path.
     * 
     * @return the newly created nested build path.
     */
    public Path createBuildpath() {
        return getBuildpath().createPath();
    }

    /**
     * Enable/ disable verbose log messages showing when each sub-build path is
     * entered/ exited. The default value is "false".
     * 
     * @param on
     *            true to enable verbose mode, false otherwise (default).
     */
    public void setVerbose(boolean on) {
        this.verbose = on;
    }

    /**
     * Sets whether to fail with a build exception on error, or go on.
     * 
     * @param failOnError
     *            the new value for this boolean flag.
     */
    public void setFailonerror(boolean failOnError) {
        this.failOnError = failOnError;
    }

    /**
     * Sets whether a submodule should use build repository or not
     * 
     * @param useBuildRepository
     *            the new value for this boolean flag
     */
    public void setUseBuildRepository(boolean useBuildRepository) {
        this.useBuildRepository = useBuildRepository;
    }

    /**
     * Set whether publish operations for the
     * {@link #setUseBuildRepository(boolean) build-scoped repository} should
     * overwrite existing artifacts. Defaults to <code>true</code> if
     * unspecified.
     */
    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    /**
     * A Vector or target names, which can be constructed from a simple
     * comma-separated list of values.
     */
    public static class TargetList extends Vector<String> {
        private static final long serialVersionUID = 2302999727821991487L;

        public TargetList(String commaSeparated) {
            this(commaSeparated.split(","));
        }

        public TargetList(String... targets) {
            for (String target : targets)
                add(target);
        }
    }
}
