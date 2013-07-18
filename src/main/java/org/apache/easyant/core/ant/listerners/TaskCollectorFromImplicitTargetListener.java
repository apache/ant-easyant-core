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
package org.apache.easyant.core.ant.listerners;

import java.util.ArrayList;
import java.util.List;

import org.apache.easyant.tasks.Import;
import org.apache.easyant.tasks.ParameterTask;
import org.apache.easyant.tasks.PathTask;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.ComponentHelper;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Property;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;

public class TaskCollectorFromImplicitTargetListener implements BuildListener {
    public static final String ROOT_MODULE_LOCATION = "report.root.module.location";
    private List<Task> tasksCollected = new ArrayList<Task>();
    private List<Class<?>> supportedClasses = new ArrayList<Class<?>>();

    public TaskCollectorFromImplicitTargetListener() {
        supportedClasses.add(ParameterTask.class);
        supportedClasses.add(Property.class);
        supportedClasses.add(Import.class);
        supportedClasses.add(Path.class);
        supportedClasses.add(PathTask.class);
        supportedClasses.add(FileSet.class);
    }

    public void taskStarted(BuildEvent buildEvent) {
        collectRootModuleLocation(buildEvent);
        if (buildEvent.getTarget().getName().equals("")) {
            Task task = buildEvent.getTask();
            if (task.getTaskType() != null) {
                Class<?> taskClass = ComponentHelper.getComponentHelper(buildEvent.getProject()).getComponentClass(
                        task.getTaskType());
                if (taskClass != null) {
                    for (Class<?> supportedClass : supportedClasses) {
                        if (supportedClass.isAssignableFrom(taskClass)) {
                            tasksCollected.add(task);
                        }
                    }
                }
            }

        }
    }

    private void collectRootModuleLocation(BuildEvent buildEvent) {
        if (buildEvent.getProject().getProperty(ROOT_MODULE_LOCATION) == null) {
            buildEvent.getProject().setNewProperty(ROOT_MODULE_LOCATION,
                    buildEvent.getTask().getLocation().getFileName());
        }
    }

    public List<Task> getTasksCollected() {
        return tasksCollected;
    }

    public List<Class<?>> getSupportedClasses() {
        return supportedClasses;
    }

    public void buildFinished(BuildEvent buildEvent) {
        // TODO Auto-generated method stub

    }

    public void buildStarted(BuildEvent buildEvent) {
        // TODO Auto-generated method stub

    }

    public void messageLogged(BuildEvent buildEvent) {
        // TODO Auto-generated method stub

    }

    public void targetFinished(BuildEvent buildEvent) {
        // TODO Auto-generated method stub

    }

    public void targetStarted(BuildEvent buildEvent) {
        // TODO Auto-generated method stub

    }

    public void taskFinished(BuildEvent buildEvent) {
        // TODO Auto-generated method stub

    }

}
