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

import org.apache.easyant.core.BuildConfigurationHelper;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Property;

/**
 * This task is similar to the Property task provided by ant except that you can specify build configurations If no
 * build configurations are specified the property will be loaded in all cases, otherwise the property will be loaded
 * only if the build configuration is active
 * 
 */
public class PropertyTask extends Property {

    private String buildConfigurations = null;

    @Override
    public void execute() throws BuildException {
        // Build the message
        StringBuilder message = new StringBuilder("property ");
        if (getName() != null) {
            message.append(getName());
        }
        if (getFile() != null) {
            message.append("file ").append(getFile());
        }

        if (BuildConfigurationHelper.isBuildConfigurationActive(getBuildConfigurations(), getProject(),
                message.toString())) {
            super.execute();
        } else {
            log("this property will be skipped ", Project.MSG_DEBUG);
        }
    }

    public String getBuildConfigurations() {
        return buildConfigurations;
    }

    public void setBuildConfigurations(String confs) {
        this.buildConfigurations = confs;
    }

    public void setConf(String conf) {
        this.buildConfigurations = conf;
    }

}
