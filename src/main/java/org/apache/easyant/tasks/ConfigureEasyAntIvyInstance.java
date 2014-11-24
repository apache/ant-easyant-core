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
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.easyant.core.EasyAntEngine;
import org.apache.easyant.core.configuration.EasyAntConfiguration;
import org.apache.easyant.core.configuration.EasyantConfigurationFactory;
import org.apache.ivy.ant.IvyAntSettings;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Configure easyant ivy instance in current project
 * 
 */
public class ConfigureEasyAntIvyInstance extends Task {
    private EasyAntConfiguration easyantConfiguration = new EasyAntConfiguration();

    @Override
    public void execute() throws BuildException {
        EasyAntEngine eaEngine = new EasyAntEngine(getEasyantConfiguration());
        IvyAntSettings easyantIvyInstance = eaEngine.configureEasyAntIvyInstance(getProject());
        eaEngine.configurePluginService(getProject(), easyantIvyInstance);
    }

    public void setConfigurationFile(String configurationFile) {
        File f = new File(configurationFile);
        try {
            EasyantConfigurationFactory.getInstance().createConfigurationFromFile(getEasyantConfiguration(),
                    f.toURI().toURL());
        } catch (Exception e) {
            throw new BuildException("Can't create easyantConfiguration from File " + configurationFile, e);
        }
    }

    public void setConfigurationUrl(String configurationUrl) {
        try {
            URL url = new URL(configurationUrl);
            EasyantConfigurationFactory.getInstance().createConfigurationFromFile(getEasyantConfiguration(), url);

        } catch (Exception e) {
            throw new BuildException("Can't create easyantConfiguration from URL " + configurationUrl, e);
        }
    }

    public void setBuildConfiguration(String buildConfiguration) {
        String[] buildConfs = buildConfiguration.split(",");
        Set<String> buildConfigurations = new HashSet<String>();
        Collections.addAll(buildConfigurations, buildConfs);
        getEasyantConfiguration().setActiveBuildConfigurations(buildConfigurations);
    }

    public EasyAntConfiguration getEasyantConfiguration() {
        return easyantConfiguration;
    }

    public void setEasyantConfiguration(EasyAntConfiguration easyantConfiguration) {
        this.easyantConfiguration = easyantConfiguration;
    }

}
