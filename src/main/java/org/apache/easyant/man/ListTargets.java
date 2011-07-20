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

import java.util.List;

import org.apache.easyant.core.report.ImportedModuleReport;
import org.apache.easyant.core.report.PhaseReport;
import org.apache.easyant.core.report.TargetReport;
import org.apache.tools.ant.Project;

/**
 * Implements the '-listTargets' switch for project Manual.
 * 
 * This command lists all targets belonging to the specified 
 * phase / plugin. 
 * 
 * If no phase / plugin name is specified, then this command
 * lists all targets available in the project (module.ivy)
 */
public class ListTargets extends EasyantOption {
    
    
    public ListTargets() {
        super("listTargets", false, "List all targets available or associated with a given phase or plugin as argument");
        setOptionalArg(true);
        setStopBuild(true);
    }


    /*
     * defining some convenient string constants
     */
    private static final String NONE = "NONE";
    
    /*
     * simply look up for all targets belonging to a phase named <container>, is such
     * a phase exists. then list all targets listed in a module named <container>, is
     * such a module exists.
     * 
     * however, if the this.container variable has not been initialized then simply list
     * down all targets in the current module and all imported sub-modules.
     */
    public void execute() {
        
        getProject().log(LINE_SEP + "--- Available Targets for current project: " + getProject().getName() + " ---" + LINE_SEP);
        String container = getValue();
        if(container == null || container.trim().length() == 0) {
            getProject().log(LINE_SEP+ "No Phase / Plugin specified. Listing all targets available in the project.");
            
            List<TargetReport> targets = getEareport().getAvailableTargets();
            printTargets(targets, getProject());
        } else {
            PhaseReport phase = getEareport().getPhaseReport(container, true);
            
            if(phase != null) {
                getProject().log("Targets for Phase: " + container);
                List<TargetReport> targets = phase.getTargetReports();
                printTargets(targets, getProject());
            } else {
                getProject().log("\tNo Phase found by name: " + container);
            }
            
            List<ImportedModuleReport> modules = getEareport().getImportedModuleReports();
            ImportedModuleReport selected = null;
            for(int i = 0; i<modules.size(); i++) {
                selected = modules.get(i);
                if(container.equals(selected.getModuleMrid())) {
                    break;
                }
            }
            if(selected != null) {
                getProject().log(LINE_SEP + "Targets for Module: " + container);
                List<TargetReport> targets = selected.getEasyantReport().getTargetReports();
                printTargets(targets, getProject());
            } else {
                getProject().log(LINE_SEP + "\tNo Module / Plugin found by name: " + container);
            }
                
            getProject().log(LINE_SEP+LINE_SEP+ "For more information on a Phase, run:" + LINE_SEP + "\t easyant -describe <PHASE>");
        }
        getProject().log(LINE_SEP + "--- End Of (Phases Listing) ---");
    }

    /*
     * common method to output a list of targets.
     * 
     * re-used multiple times in this class.
     */
    private void printTargets(List<TargetReport> targets, Project project) {
        if(targets.size() == 0) {
            project.log("\tNo targets found.");
            return;
        }
        for(int i = 0; i<targets.size(); i++) {
            TargetReport targetRep = targets.get(i);
            project.log("\tTarget: " + targetRep.getName());
            project.log("\t\tPhase: " + (targetRep.getPhase() == null ? NONE : targetRep.getPhase()));
            project.log("\t\tDescription: " + (targetRep.getDescription() == null ? NONE : targetRep.getDescription()));
            project.log("\t\tDepends: " + (targetRep.getDepends() == null ? NONE : targetRep.getDepends()));
            project.log("\t\tIF: " + (targetRep.getIfCase() == null ? NONE : targetRep.getIfCase()));
            project.log("\t\tUNLESS: " + (targetRep.getUnlessCase() == null ? NONE : targetRep.getUnlessCase()));
        }
    }
}
