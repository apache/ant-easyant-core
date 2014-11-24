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
package org.apache.easyant.core.ant;

import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.easyant.core.ant.listerners.MultiModuleLogger;
import org.apache.tools.ant.*;
import org.apache.tools.ant.util.ClasspathUtils;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Utilitary class to manipulate ant's project (such as creating toplevel target)
 */
public class ProjectUtils {

    private ProjectUtils() {
    }

    private static final long MEGABYTE = 1024 * 1024;

    /**
     * emulates a top level target
     *
     * @return a top level target
     */
    public static Target createTopLevelTarget() {
        Target topLevel = new Target();
        topLevel.setName("");
        return topLevel;
    }

    /**
     * Emulate an empty project import task check that projectHelper is at toplevel by checking the size of
     * projectHelper.getImportTask()
     *
     * @return a temporary file acting as a mainscript
     */
    public static File emulateMainScript(Project project) {
        return project.getBaseDir();
    }

    /**
     * Replace main logger implementation
     *
     * @param project a given project
     * @param logger  {@link BuildLogger} implementation to use
     */
    public static void replaceMainLogger(Project project, BuildLogger logger) {
        // Change the default output logger
        PrintStream out = System.out;
        PrintStream err = System.err;
        int currentLogLevel = Project.MSG_INFO;
        project.log("removing current logger", Project.MSG_DEBUG);
        // since DefaultLogger doesn't offer any way to get the out / err print
        // streams we should use reflection
        // TODO: we should find a better way to do this
        for (BuildListener l : project.getBuildListeners()) {
            if (l instanceof DefaultLogger) {
                try {
                    Field fieldOut = DefaultLogger.class.getDeclaredField("out");
                    fieldOut.setAccessible(true);
                    out = (PrintStream) fieldOut.get(l);
                    Field fieldErr = DefaultLogger.class.getDeclaredField("err");
                    fieldErr.setAccessible(true);
                    err = (PrintStream) fieldErr.get(l);
                    Field fieldMsgLevel = DefaultLogger.class.getDeclaredField("msgOutputLevel");
                    fieldMsgLevel.setAccessible(true);
                    currentLogLevel = (Integer) fieldMsgLevel.get(l);
                } catch (IllegalAccessException ex) {
                    throw new BuildException(ex);
                } catch (SecurityException e) {
                    throw new BuildException(e);
                } catch (NoSuchFieldException e) {
                    throw new BuildException(e);
                }
                project.removeBuildListener(l);
            }
        }
        project.log("Initializing new logger " + logger.getClass().getName(), Project.MSG_DEBUG);
        logger.setOutputPrintStream(out);
        logger.setErrorPrintStream(err);
        logger.setMessageOutputLevel(currentLogLevel);
        project.setProjectReference(logger);
        project.addBuildListener(logger);

    }

    /**
     * Install multi module logger Multi module logger specified through {@link EasyAntMagicNames}.MULTIMODULE_LOGGER
     * property. If this property is not set use default implementation
     *
     * @param project a given project where multi module logger should be installed
     */
    public static void installMultiModuleLogger(Project project) {
        String multiModuleLoggerClassName = project.getProperty(EasyAntMagicNames.MULTIMODULE_LOGGER);
        if (multiModuleLoggerClassName == null) {
            multiModuleLoggerClassName = MultiModuleLogger.class.getCanonicalName();
        }
        BuildLogger buildLogger = (BuildLogger) ClasspathUtils.newInstance(multiModuleLoggerClassName,
                ProjectUtils.class.getClassLoader(), BuildLogger.class);
        replaceMainLogger(project, buildLogger);
    }

    /**
     * Print memory details
     *
     * @param project a given project
     */
    public static void printMemoryDetails(Project project) {
        project.log("---- Memory Details ----");
        project.log("  Used Memory  = "
                + (Runtime.getRuntime().totalMemory() / MEGABYTE - Runtime.getRuntime().freeMemory() / MEGABYTE) + "MB");
        project.log("  Free Memory  = " + (Runtime.getRuntime().freeMemory() / MEGABYTE) + "MB");
        project.log("  Total Memory = " + (Runtime.getRuntime().totalMemory() / MEGABYTE) + "MB");
        project.log("-----------------------");
    }

    /**
     * Targets in imported files with a project name and not overloaded by the main build file will be in the target map
     * twice. This method removes the duplicate target.
     *
     * @param targets the targets to filter.
     * @return the filtered targets.
     */
    public static Map<String, Target> removeDuplicateTargets(Map<?, ?> targets) {
        Map<Location, Target> locationMap = new HashMap<Location, Target>();
        for (Map.Entry<?, ?> entry : targets.entrySet()) {
            String name = (String) entry.getKey();
            Target target = (Target) entry.getValue();
            Target otherTarget = locationMap.get(target.getLocation());
            // Place this entry in the location map if
            // a) location is not in the map
            // b) location is in map, but it's name is longer
            // (an imported target will have a name. prefix)
            if (otherTarget == null || otherTarget.getName().length() > name.length()) {
                locationMap.put(target.getLocation(), target); // Smallest name
                // wins
            }
        }
        Map<String, Target> ret = new HashMap<String, Target>();
        for (Target target : locationMap.values()) {
            ret.put(target.getName(), target);
        }
        return ret;
    }

    public static ProjectHelper configureProjectHelper(Project project) {
        ProjectHelper helper = ProjectHelper.getProjectHelper();
        helper.getImportStack().addElement(ProjectUtils.emulateMainScript(project));
        project.addReference(ProjectHelper.PROJECTHELPER_REFERENCE, helper);
        return helper;
    }

    public static ProjectHelper getConfiguredProjectHelper(Project project) {
        return (ProjectHelper) project.getReference(ProjectHelper.PROJECTHELPER_REFERENCE);
    }
}
