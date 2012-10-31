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

import org.apache.tools.ant.BuildException;

/**
 * Configure current project
 * 
 */
public class ConfigureProject extends AbstractEasyAntTask {

    private String defaultTarget;
    private String basedir;

    @Override
    public void execute() throws BuildException {
        if (defaultTarget != null) {
            getProject().setDefault(defaultTarget);
        }

        if (basedir != null) {
            getProject().setBasedir(basedir);
        }
    }

    public String getDefaultTarget() {
        return defaultTarget;
    }

    public void setDefaultTarget(String defaultTarget) {
        this.defaultTarget = defaultTarget;
    }

    public String getBasedir() {
        return basedir;
    }

    public void setBasedir(String basedir) {
        this.basedir = basedir;
    }

}
