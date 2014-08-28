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

import org.apache.easyant.core.EasyAntEngine;
import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.easyant.core.ivy.IvyInstanceHelper;
import org.apache.ivy.Ivy;
import org.apache.ivy.ant.IvyAntSettings;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Reference;

/**
 * Base class for easyant tasks
 * 
 */
public class AbstractEasyAntTask extends Task {

    /**
     * Get the easyant ivy ant settings. Usefull if you want to bind a subtask to easyant ivy instance
     * 
     * @return an ivyAntSettings
     */
    protected IvyAntSettings getEasyAntIvyAntSettings() {
        return IvyInstanceHelper.getEasyAntIvyAntSettings(getProject());
    }

    /**
     * Get the configured ivy instance
     * 
     * @return a configured ivy instance
     */
    protected Ivy getEasyAntIvyInstance() {
        return getEasyAntIvyAntSettings().getConfiguredIvyInstance(this);
    }

    protected Ivy getProjectIvyInstance() {
        return IvyInstanceHelper.getProjectIvyAntSettings(getProject()).getConfiguredIvyInstance(this);
    }

    /**
     * Utilitary method to configure a task with the current one
     * 
     * @param task
     *            task to configure
     * @return the configured task
     */
    protected Task initTask(Task task) {
        task.setLocation(getLocation());
        task.setProject(getProject());
        task.setTaskName(getTaskName());
        task.setOwningTarget(getOwningTarget());
        return task;
    }

    /**
     * Get a reference of the project ivy instance
     * 
     * @return a reference of the project ivy instance
     */
    protected Reference getProjectIvyReference() {
        return IvyInstanceHelper.buildProjectIvyReference(getProject());
    }

    protected EasyAntEngine getEasyAntEngine() {
        EasyAntEngine easyAntEngine = getProject().getReference(EasyAntMagicNames.EASYANT_ENGINE_REF);
        if (easyAntEngine == null) {
            easyAntEngine = new EasyAntEngine();
        }
        return easyAntEngine;
    }
}