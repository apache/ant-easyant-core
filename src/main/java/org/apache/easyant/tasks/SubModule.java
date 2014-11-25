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

import org.apache.easyant.core.EasyAntConstants;
import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.easyant.core.ant.ProjectUtils;
import org.apache.easyant.core.ant.listerners.ExecutionResult;
import org.apache.easyant.core.ant.listerners.MultiModuleLogger;
import org.apache.easyant.core.ivy.IvyInstanceHelper;
import org.apache.ivy.ant.IvyPublish;
import org.apache.ivy.ant.IvyResolve;
import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.Ant;
import org.apache.tools.ant.taskdefs.Property;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.PropertySet;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.CollectionUtils;
import org.apache.tools.ant.util.StringUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;

/**
 * This task is used to manage orchestration of submodules.
 */
public class SubModule extends AbstractEasyAntTask {

    private boolean failOnError = true;
    private boolean verbose = false;
    private String moduleFile = EasyAntConstants.DEFAULT_BUILD_MODULE;

    private Path buildpath;
    private TargetList targets = new TargetList();
    private boolean useBuildRepository = false;
    private boolean overwrite = true;

    private boolean inheritRefs = false;
    private List<Property> properties = new ArrayList<Property>();
    private List<Ant.Reference> references = new ArrayList<Ant.Reference>();
    private List<PropertySet> propertySets = new ArrayList<PropertySet>();

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

        BuildException buildException = null;
        for (String filename : filenames) {
            File file = null;
            String subdirPath = null;
            Throwable thrownException = null;
            try {
                File directory = null;
                file = new File(filename);
                if (file.isDirectory()) {
                    if (verbose) {
                        subdirPath = file.getPath();
                        log("Entering directory: " + subdirPath + "\n", Project.MSG_INFO);
                    }
                    file = new File(file, moduleFile);
                }
                directory = file.getParentFile();
                execute(file, directory);
                if (verbose && subdirPath != null) {
                    log("Leaving directory: " + subdirPath + "\n", Project.MSG_INFO);
                }
            } catch (RuntimeException ex) {
                if (!(getProject().isKeepGoingMode())) {
                    if (verbose && subdirPath != null) {
                        log("Leaving directory: " + subdirPath + "\n", Project.MSG_INFO);
                    }
                    throw ex; // throw further
                }
                thrownException = ex;
            } catch (Throwable ex) {
                if (!(getProject().isKeepGoingMode())) {
                    if (verbose && subdirPath != null) {
                        log("Leaving directory: " + subdirPath + "\n", Project.MSG_INFO);
                    }
                    throw new BuildException(ex);
                }
                thrownException = ex;
            }
            if (thrownException != null) {
                if (thrownException instanceof BuildException) {
                    log("File '" + file + "' failed with message '" + thrownException.getMessage() + "'.",
                            Project.MSG_ERR);
                    // only the first build exception is reported
                    if (buildException == null) {
                        buildException = (BuildException) thrownException;
                    }
                } else {
                    log("Target '" + file + "' failed with message '" + thrownException.getMessage() + "'.",
                            Project.MSG_ERR);
                    thrownException.printStackTrace(System.err);
                    if (buildException == null) {
                        buildException = new BuildException(thrownException);
                    }
                }
                if (verbose && subdirPath != null) {
                    log("Leaving directory: " + subdirPath + "\n", Project.MSG_INFO);
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
     * @param file      the build file to execute
     * @param directory the directory of the current iteration
     * @throws BuildException is the file cannot be found, read, is a directory, or the target called failed, but only if
     *                        <code>failOnError</code> is <code>true</code>. Otherwise, a warning log message is simply output.
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

        Project subModule = configureSubModule(file, directory);
        subModule.fireSubBuildStarted();

        try {
            // buildFile should be in the same directory of buildModule
            File buildfile = new File(directory, EasyAntConstants.DEFAULT_BUILD_FILE);
            if (buildfile.exists()) {
                subModule.setNewProperty(MagicNames.ANT_FILE, buildfile.getAbsolutePath());
            }
            subModule.setNewProperty(EasyAntMagicNames.EASYANT_FILE, file.getAbsolutePath());

            ProjectHelper helper = ProjectUtils.configureProjectHelper(subModule);

            getEasyAntEngine().loadSystemPlugins(subModule, false);

            LoadModule lm = new LoadModule();
            lm.setBuildModule(file);
            lm.setBuildFile(buildfile);
            lm.setTaskName(EasyAntConstants.EASYANT_TASK_NAME);
            lm.setProject(subModule);
            lm.setOwningTarget(ProjectUtils.createTopLevelTarget());
            lm.setLocation(new Location(ProjectUtils.emulateMainScript(getProject()).getAbsolutePath()));
            lm.setUseBuildRepository(useBuildRepository);
            lm.execute();

            helper.resolveExtensionOfAttributes(subModule);

            String targetsToRun = filterTargets(subModule);
            printExecutingTargetMsg(subModule);

            if (targetsToRun != null && !"".equals(targetsToRun.trim())) {
                subModule.setNewProperty(EasyAntMagicNames.PROJECT_EXECUTED_TARGETS, targetsToRun);
                subModule.executeTargets(new TargetList(targetsToRun));
                if (useBuildRepository) {
                    String targetArtifacts = subModule.getProperty("target.artifacts");
                    if (targetArtifacts == null) {
                        targetArtifacts = "target/artifacts";
                    }
                    File artifactsDir = subModule.resolveFile(targetArtifacts);
                    if (artifactsDir.isDirectory()) {
                        IvyResolve ivyResolve = new IvyResolve();
                        ivyResolve.setFile(file);
                        ivyResolve.setProject(subModule);
                        ivyResolve.setOwningTarget(getOwningTarget());
                        ivyResolve.setLocation(getLocation());
                        ivyResolve.setTaskName("publish-buildscoped-repository");
                        ivyResolve.setSettingsRef(IvyInstanceHelper.buildProjectIvyReference(subModule));
                        ivyResolve.execute();

                        // this property set by LoadModule task when it
                        // configures the build repo
                        String resolver = subModule.getProperty(EasyAntMagicNames.EASYANT_BUILD_REPOSITORY);

                        subModule.log("Publishing in build scoped repository", Project.MSG_INFO);
                        // Publish on build scoped repository
                        IvyPublish ivyPublish = new IvyPublish();
                        ivyPublish.setSettingsRef(IvyInstanceHelper.buildProjectIvyReference(subModule));
                        ivyPublish.setResolver(resolver);
                        // TODO: this should be more flexible!
                        ivyPublish.setArtifactspattern(artifactsDir.getAbsolutePath()
                                + "/[artifact](-[classifier]).[ext]");
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
                        ivyPublish.setForcedeliver(true);
                        ivyPublish.setTaskName("publish-buildscoped-repository");
                        ivyPublish.execute();
                    } else {
                        subModule.log("Skipping publish because " + artifactsDir.getPath() + " is not a directory",
                                Project.MSG_VERBOSE);
                    }
                }
            } else {
                subModule.log("Skipping sub-project build because no matching targets were found", Project.MSG_VERBOSE);
            }
            subModule.fireSubBuildFinished(null);
        } catch (BuildException e) {
            subModule.fireSubBuildFinished(e);
            throw e;
        } finally {
            // add execution times for the current submodule to parent
            // project references for access from MetaBuildExecutor
            storeExecutionTimes(getProject(), subModule);
        }

    }

    private Project configureSubModule(File file, File directory) {
        Project subModule = getProject().createSubProject();

        subModule.setNewProperty(EasyAntMagicNames.SUBMODULE, "true");

        subModule.setJavaVersionProperty();
        for (BuildListener buildListener : getProject().getBuildListeners()) {
            subModule.addBuildListener(buildListener);
        }
        // copy all User properties
        addAlmostAll(getProject().getUserProperties(), subModule, PropertyType.USER);
        // inherit meta.target directory, for shared build repository.
        String metaTarget = getProject().getProperty(EasyAntMagicNames.META_TARGET);
        if (metaTarget != null) {
            File metaDir = getProject().resolveFile(metaTarget);
            subModule.setNewProperty(EasyAntMagicNames.META_TARGET, metaDir.getAbsolutePath());
        }
        // inherit easyant offline base
        String offlineBaseDir = getProject().getProperty(EasyAntMagicNames.OFFLINE_BASE_DIRECTORY);
        if (offlineBaseDir != null) {
            subModule.setInheritedProperty(EasyAntMagicNames.OFFLINE_BASE_DIRECTORY, offlineBaseDir);
        }

        subModule.initProperties();

        // copy nested properties
        for (PropertySet ps : propertySets) {
            addAlmostAll(ps.getProperties(), subModule, PropertyType.PLAIN);
        }

        overrideProperties(subModule);
        addReferences(subModule);

        getEasyAntEngine().configureEasyAntIvyInstance(subModule);
        subModule.addReference(EasyAntMagicNames.PLUGIN_SERVICE_INSTANCE,
                getProject().getReference(EasyAntMagicNames.PLUGIN_SERVICE_INSTANCE));

        subModule.setName(file.getName());
        subModule.setBaseDir(directory);
        return subModule;
    }

    @SuppressWarnings("unchecked")
    private void storeExecutionTimes(Project parent, Project child) {
        List<ExecutionResult> allresults = parent
                .getReference(MultiModuleLogger.EXECUTION_TIMER_BUILD_RESULTS);
        if (allresults == null) {
            allresults = new ArrayList<ExecutionResult>();
            parent.addReference(MultiModuleLogger.EXECUTION_TIMER_BUILD_RESULTS, allresults);
        }
        List<ExecutionResult> childResults = child
                .getReference(MultiModuleLogger.EXECUTION_TIMER_BUILD_RESULTS);
        if (childResults != null) {
            allresults.addAll(childResults);
        }
    }

    /**
     * Filter the active set of targets to only those defined in the given project.
     */
    private String filterTargets(Project subProject) {
        List<String> filteredTargets = new ArrayList<String>();
        Set<?> keys = subProject.getTargets().keySet();

        for (String target : targets) {
            if (keys.contains(target)) {
                filteredTargets.add(target);
            } else {
                subProject.log("Skipping undefined target '" + target + "'" + " on " + subProject.getName(), Project.MSG_VERBOSE);
            }
        }
        return CollectionUtils.flattenToString(filteredTargets);
    }

    /**
     * Print a message when executing the target
     *
     * @param subProject a subproject where the log will be printed
     */
    private void printExecutingTargetMsg(Project subProject) {
        final String HEADER = "======================================================================";
        subProject.log(HEADER + StringUtils.LINE_SEP + "Executing " + targets + " on " + subProject.getName() + StringUtils.LINE_SEP + HEADER);
    }

    /**
     * Copies all properties from the given table to the new project - omitting those that have already been set in the
     * new project as well as properties named basedir or ant.file.
     *
     * @param props properties to copy to the new project.
     * @since Ant 1.8.0
     */
    private void addAlmostAll(Map<?, ?> props, Project subProject, PropertyType type) {
        for (Entry<?, ?> prop : props.entrySet()) {
            String key = prop.getKey().toString();
            if (MagicNames.PROJECT_BASEDIR.equals(key) || MagicNames.ANT_FILE.equals(key)) {
                // basedir and ant.file get special treatment in execute()
                continue;
            }

            String value = prop.getValue().toString();
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
     * The target to call on the different sub-builds. Set to "" to execute the default target.
     *
     * @param target the target
     *               <p/>
     */
    // REVISIT: Defaults to the target name that contains this task if not
    // specified.
    public void setTarget(String target) {
        setTargets(new TargetList(target));
    }

    /**
     * The targets to call on the different sub-builds.
     */
    public void setTargets(TargetList targets) {
        this.targets = targets;
    }

    /**
     * Set the buildpath to be used to find sub-projects.
     *
     * @param s an Ant Path object containing the buildpath.
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
     * @param r a reference to an Ant Path object containing the buildpath.
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
     * Enable/ disable verbose log messages showing when each sub-build path is entered/ exited. The default value is
     * "false".
     *
     * @param on true to enable verbose mode, false otherwise (default).
     */
    public void setVerbose(boolean on) {
        this.verbose = on;
    }

    /**
     * Sets whether to fail with a build exception on error, or go on.
     *
     * @param failOnError the new value for this boolean flag.
     */
    public void setFailonerror(boolean failOnError) {
        this.failOnError = failOnError;
    }

    /**
     * Sets whether a submodule should use build repository or not
     *
     * @param useBuildRepository the new value for this boolean flag
     */
    public void setUseBuildRepository(boolean useBuildRepository) {
        this.useBuildRepository = useBuildRepository;
    }

    /**
     * Set whether publish operations for the {@link #setUseBuildRepository(boolean) build-scoped repository} should
     * overwrite existing artifacts. Defaults to <code>true</code> if unspecified.
     */
    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    /**
     * Corresponds to <code>&lt;ant&gt;</code>'s <code>inheritrefs</code> attribute.
     *
     * @param b the new value for this boolean flag.
     */
    public void setInheritrefs(boolean b) {
        this.inheritRefs = b;
    }

    /**
     * Corresponds to <code>&lt;ant&gt;</code>'s nested <code>&lt;property&gt;</code> element.
     *
     * @param p the property to pass on explicitly to the sub-build.
     */
    public void addProperty(Property p) {
        properties.add(p);
    }

    /**
     * Corresponds to <code>&lt;ant&gt;</code>'s nested <code>&lt;reference&gt;</code> element.
     *
     * @param r the reference to pass on explicitly to the sub-build.
     */
    public void addReference(Ant.Reference r) {
        references.add(r);
    }

    /**
     * Corresponds to <code>&lt;ant&gt;</code>'s nested <code>&lt;propertyset&gt;</code> element.
     *
     * @param ps the propertset
     */
    public void addPropertyset(PropertySet ps) {
        propertySets.add(ps);
    }

    /**
     * Override the properties in the new project with the one explicitly defined as nested elements here.
     *
     * @param subproject a subproject
     * @throws BuildException under unknown circumstances.
     */
    private void overrideProperties(Project subproject) throws BuildException {
        // remove duplicate properties - last property wins
        // Needed for backward compatibility
        Set<String> set = new HashSet<String>();
        for (int i = properties.size() - 1; i >= 0; --i) {
            Property p = properties.get(i);
            if (p.getName() != null && !p.getName().equals("")) {
                if (set.contains(p.getName())) {
                    properties.remove(i);
                } else {
                    set.add(p.getName());
                }
            }
        }
        for (Property p : properties) {
            p.setProject(subproject);
            p.execute();
        }

        getProject().copyInheritedProperties(subproject);
    }

    /**
     * Add the references explicitly defined as nested elements to the new project. Also copy over all references that
     * don't override existing references in the new project if inheritrefs has been requested.
     *
     * @param subproject a subproject
     * @throws BuildException if a reference does not have a refid.
     */
    private void addReferences(Project subproject) throws BuildException {
        @SuppressWarnings("unchecked")
        Map<String, Object> thisReferences = (Map<String, Object>) getProject().getReferences().clone();
        Map<String, Object> newReferences = subproject.getReferences();
        for (Ant.Reference ref : references) {
            String refid = ref.getRefId();
            if (refid == null) {
                throw new BuildException("the refid attribute is required" + " for reference elements");
            }
            if (!thisReferences.containsKey(refid)) {
                log("Parent project doesn't contain any reference '" + refid + "'", Project.MSG_WARN);
                continue;
            }

            thisReferences.remove(refid);
            String toRefid = ref.getToRefid();
            if (toRefid == null) {
                toRefid = refid;
            }
            copyReference(subproject, refid, toRefid);
        }

        // Now add all references that are not defined in the
        // subproject, if inheritRefs is true
        if (inheritRefs) {
            for (String key : thisReferences.keySet()) {
                if (newReferences.containsKey(key)) {
                    continue;
                }
                copyReference(subproject, key, key);
                subproject.inheritIDReferences(getProject());
            }
        }
    }

    /**
     * Try to clone and reconfigure the object referenced by oldkey in the parent project and add it to the new project
     * with the key newkey.
     * <p/>
     * <p>
     * If we cannot clone it, copy the referenced object itself and keep our fingers crossed.
     * </p>
     *
     * @param oldKey the reference id in the current project.
     * @param newKey the reference id in the new project.
     */
    private void copyReference(Project subproject, String oldKey, String newKey) {
        Object orig = getProject().getReference(oldKey);
        if (orig == null) {
            log("No object referenced by " + oldKey + ". Can't copy to " + newKey, Project.MSG_WARN);
            return;
        }

        Class<?> c = orig.getClass();
        Object copy = orig;
        Method cloneM;
        try {
            cloneM = c.getMethod("clone", new Class[0]);
            if (cloneM != null) {
                copy = cloneM.invoke(orig, new Object[0]);
                log("Adding clone of reference " + oldKey, Project.MSG_DEBUG);
            }
        } catch (NoSuchMethodException e) {
            // not clonable
        } catch (IllegalAccessException e) {
            // not clonable
        } catch (InvocationTargetException e) {
            // not clonable
        }

        if (copy instanceof ProjectComponent) {
            ((ProjectComponent) copy).setProject(subproject);
        } else {
            try {
                Method setProjectM = c.getMethod("setProject", new Class[]{Project.class});
                if (setProjectM != null) {
                    setProjectM.invoke(copy, subproject);
                }
            } catch (NoSuchMethodException e) {
                // ignore this if the class being referenced does not have
                // a set project method.
            } catch (Exception e2) {
                String msg = "Error setting new project instance for " + "reference with id " + oldKey;
                throw new BuildException(msg, e2, getLocation());
            }
        }
        subproject.addReference(newKey, copy);
    }

    /**
     * A Vector or target names, which can be constructed from a simple comma-separated list of values.
     */
    public static class TargetList extends Vector<String> {
        private static final long serialVersionUID = 2302999727821991487L;

        public TargetList(String commaSeparated) {
            this(commaSeparated.split(","));
        }

        public TargetList(String... targets) {
            for (String target : targets) {
                add(target);
            }
        }
    }
}
