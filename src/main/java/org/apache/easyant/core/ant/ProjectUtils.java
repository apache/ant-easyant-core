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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Iterator;

import org.apache.easyant.core.EasyAntConstants;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;

/**
 * Utilitary class to manipulate ant's project (such as creating toplevel
 * target)
 * 
 */
public class ProjectUtils {

    /**
     * emulates a top level target
     * @return a top level target
     */
    public static Target createTopLevelTarget() {
        Target topLevel = new Target();
        topLevel.setName("");
        return topLevel;
    }

    /**
     * Emulate an empty project import task check that projectHelper is at
     * toplevel by checking the size of projectHelper.getImportTask()
     * 
     * @return a temporary file acting as a mainscript
     */
    public static File emulateMainScript(Project project) {
        return project.getBaseDir();
    }
    
    /**
     * Replace main logger implementation
     * @param project a given project
     * @param logger {@link BuildLogger} implementation to use
     */
    public static void replaceMainLogger(Project project, BuildLogger logger) {
        // Change the default output logger
        PrintStream out = System.out;
        PrintStream err = System.err;
        int currentLogLevel = Project.MSG_INFO;
        project.log("removing current logger", Project.MSG_DEBUG);
        // since BuildLogger doesn't offer any way to get the out / err print
        // streams we should use reflection
        // TODO: we should find a better way to do this
        for (Iterator<?> i = project.getBuildListeners().iterator(); i
                .hasNext();) {
            BuildListener l = (BuildListener) i.next();
            if (l instanceof BuildLogger) {
                Field fields[];
                // case of classes extending DefaultLogger
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
            project.removeBuildListener(l);

        }
        project.log("Initializing new logger " + logger.getClass().getName(), Project.MSG_DEBUG);
        logger.setOutputPrintStream(out);
        logger.setErrorPrintStream(err);
        logger.setMessageOutputLevel(currentLogLevel);
        project.setProjectReference(logger);
        project.addBuildListener(logger);

    }

}
