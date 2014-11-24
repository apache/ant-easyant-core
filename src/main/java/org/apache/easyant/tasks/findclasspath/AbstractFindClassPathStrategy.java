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
package org.apache.easyant.tasks.findclasspath;

import org.apache.easyant.core.ivy.IvyInstanceHelper;
import org.apache.ivy.ant.IvyCachePath;
import org.apache.ivy.core.LogOptions;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

public abstract class AbstractFindClassPathStrategy extends DataType {
    private String pathid;
    private Path path;
    private AbstractFindClassPathStrategy nextStrategy;

    public boolean check() {
        boolean result = doCheck();
        if (!result) {
            if (getNextStrategy() == null) {
                return false;
            } else {
                return getNextStrategy().check();
            }

        } else {
            return true;
        }

    }

    protected abstract boolean doCheck();

    /**
     * Utilitary method to load cachepath
     * 
     * @param organisation
     *            organisation name
     * @param module
     *            module name
     * @param revision
     *            revision number
     * @param conf
     *            configuration name
     * @param ivyRef
     *            ivy instance reference
     */
    protected void loadCachePath(String organisation, String module, String revision, String conf, Reference ivyRef) {
        log("Building classpath (" + getPathid() + ")" + " with " + organisation + "#" + module + ";" + revision + " conf=" + conf, Project.MSG_DEBUG);
        IvyCachePath pluginCachePath = new IvyCachePath();
        pluginCachePath.setOrganisation(organisation);
        pluginCachePath.setModule(module);
        pluginCachePath.setRevision(revision);
        pluginCachePath.setConf(conf);
        pluginCachePath.setPathid(getPathid());
        pluginCachePath.setLog(LogOptions.LOG_DOWNLOAD_ONLY);
        pluginCachePath.setInline(true);

        pluginCachePath.setSettingsRef(ivyRef);
        initTask(pluginCachePath).execute();

    }

    /**
     * Utilitary method to build the classpath
     * 
     * @return a path
     */
    protected Path getPath() {
        if (path == null) {
            path = new Path(getProject());
            path.setPath(getPathid());
            path.setLocation(getLocation());

        }
        return path;
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
        // task.setTaskName(getTaskName());
        // task.setOwningTarget(getOwningTarget());
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

    public String getPathid() {
        return pathid;
    }

    public void setPathid(String pathid) {
        this.pathid = pathid;
    }

    public AbstractFindClassPathStrategy getNextStrategy() {
        return nextStrategy;
    }

    public void setNextStrategy(AbstractFindClassPathStrategy nextStrategy) {
        this.nextStrategy = nextStrategy;
    }

}
