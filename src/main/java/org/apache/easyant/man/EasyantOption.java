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
package org.apache.easyant.man;

import java.io.File;

import org.apache.commons.cli.Option;
import org.apache.easyant.core.EasyAntConstants;
import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.easyant.core.report.EasyAntReport;
import org.apache.easyant.core.services.PluginService;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * Base class for all classes implementing functionality for project switches that are accepted on command line when
 * invoking easyant.
 * 
 * For example, <br>
 * easyant -listTargets
 * 
 * <p />
 * The -listTargets and similar switches (like -describe etc.) are all extending this class.
 * 
 * For each manual switch that is intended to be supported by easyant, a new implementing class for this class must be
 * added that implements the switch functionality.
 * 
 */
public abstract class EasyantOption extends Option {

    private static final long serialVersionUID = 1L;

    public static final String LINE_SEP = System.getProperty("line.separator");
    private Project project;
    private EasyAntReport eareport;
    private boolean stopBuild = false;

    public EasyantOption(String opt, boolean hasArg, String description) throws IllegalArgumentException {
        super(opt, hasArg, description);
    }

    public EasyantOption(String opt, String longOpt, boolean hasArg, String description)
            throws IllegalArgumentException {
        super(opt, longOpt, hasArg, description);
    }

    public EasyantOption(String opt, String description) throws IllegalArgumentException {
        super(opt, description);
    }

    public abstract void execute();
    
    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public EasyAntReport getEareport() {
        if (eareport == null) {
            try {
                File moduleDescriptor = new File(project.getProperty(EasyAntMagicNames.EASYANT_FILE));
                File optionalAntModule = new File(moduleDescriptor.getParent(), EasyAntConstants.DEFAULT_BUILD_FILE);
                File overrideAntModule = new File(moduleDescriptor.getParent(),
                        EasyAntConstants.DEFAULT_OVERRIDE_BUILD_FILE);

                PluginService pluginService = project
                        .getReference(EasyAntMagicNames.PLUGIN_SERVICE_INSTANCE);
                setEareport(pluginService.generateEasyAntReport(moduleDescriptor, optionalAntModule, overrideAntModule));
            } catch (Exception e) {
                throw new BuildException("EasyAntMan could not be initialized. Details: " + e.getMessage(), e);
            }
        }
        return eareport;
    }

    public void setEareport(EasyAntReport eareport) {
        this.eareport = eareport;
    }

    public boolean isStopBuild() {
        return stopBuild;
    }

    public void setStopBuild(boolean stopBuild) {
        this.stopBuild = stopBuild;
    }

}
