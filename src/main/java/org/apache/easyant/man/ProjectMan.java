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

import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.easyant.core.report.EasyAntReport;
import org.apache.easyant.core.services.PluginService;
import org.apache.tools.ant.Project;

// TODO: move the list of supported switches and their implementation
// classes to easyant-conf.xml

/**
 * This class controls the Project specific manual / documentation / help
 * functionality.
 * 
 * The getCommand method supports a set of switches, that may be added to.
 * Each switch must also have a corresponding implementation class, that
 * is expected to implement the ManCommand interface.
 */
public class ProjectMan {
    private boolean inited = false;
    private Throwable initErr = null;
    
    private Project project = null;
    private EasyAntReport earep = null;
    private ManCommand command = null;
    
    /**
     * Factory method that returns an appropriate ManCommand implementation
     * depending on the parameter passed to it.
     * 
     * @param cmd
     *          Switch name for which ManCommand implementation is required.
     *          Example, -describe etc.
     * @return
     */
    private ManCommand getCommand(String cmd) {
        if("-listPhases".equals(cmd)) {
            return new ListPhases();
        } else if("-describe".equals(cmd)) {
            return new Describe();
        } else if("-listTargets".equals(cmd)) {
            return new ListTargets();
        } else if("-listProps".equals(cmd)) {
            return new ListProps();
        } else if("-listPlugins".equals(cmd)) {
            return new ListPlugins();
        }
        throw new IllegalArgumentException("Unknown Manual Command.");
    }
    
    /**
     * Sets the context for this ProjectMan instance. Once a coxtext is provided
     * this object is in a state to support manual commands / switches.
     * 
     * The context essentially comprises of:
     * <ol>
     *  <li>Configured Project instance</li>
     *  <li>The build module itself.</li>
     * Assumes the passed project object to be a 
     * configured project.
     * 
     * @param p
     *          Configured project instance.
     * @param moduleDescriptor
     *          The build module file. This MUST not be left unspecified. 
     *          This value does not default to module.ivy in current directory.
     * @param optionalAntModule The optional build file
     * @param overrideAntModule The optional override build file.
     */
    public boolean setContext(Project p, File moduleDescriptor, File optionalAntModule, File overrideAntModule) {
        project = p;
        try {
            PluginService pluginService = (PluginService)project.getReference(EasyAntMagicNames.PLUGIN_SERVICE_INSTANCE);
            earep = pluginService.generateEasyAntReport(moduleDescriptor,optionalAntModule,overrideAntModule);
            inited = true;
        } catch (Throwable t) {
            project.log("EasyAntMan could not be initialized. Details: " + t.getMessage());
            initErr = t;
        }
        return inited;
    }

    /**
     * Sets the command to be executed by ProjectMan. ProjectMan only keeps
     * track of the last command supplied to it through this method.
     * 
     * @param command
     *          The switch (e.g. -listTargets) with which easyant was invoked.
     */
    public void setCommand(String command) {
        // if there are multiple commands specified the last one overrides all the previous ones
        this.command = getCommand(command);
    }
    
    /**
     * Used to add a parameter to ManCommand instance. The ManCommand instance
     * will decide how to handle the object.
     * 
     * @param param
     *          Additional input to the ManCommand. This may be specify the plugin
     *          name if the the command is -listProps, or the phase name if the 
     *          command is -describe.
     */
    public void addParam(String param) {
        command.addParam(param);
    }
    
    /**
     * Executes the Project Manual with the given command name, and supplied
     * parameters.
     */
    public void execute() {
        if(!inited)
            throw new RuntimeException(initErr);
        
        if(command == null)
            throw new RuntimeException("Available options: -listAll, -describe, -listTargets, -listProps. " +
                    "ProjectHelp can not be run without one of these.");
        String lineSep = System.getProperty("line.separator");
        
        project.log(lineSep + "Project Manual");
        project.log("--------------");
        command.execute(earep, project);
    }
}
