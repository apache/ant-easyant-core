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

import org.apache.easyant.core.report.ExtensionPointReport;
import org.apache.easyant.core.report.ImportedModuleReport;
import org.apache.easyant.core.report.TargetReport;

/**
 * Implements the '-listTargets' switch for project Manual.
 * 
 * This command lists all targets belonging to the specified extension-point / plugin.
 * 
 * If no extension point / plugin name is specified, then this command lists all targets available in the project
 * (module.ivy)
 */
public class ListTargets extends EasyantOption {

    private static final long serialVersionUID = 1L;

    public ListTargets() {
        super("listTargets", true,
                "List all targets available or associated with a given extension-point or plugin as argument");
        setOptionalArg(true);
        setStopBuild(true);
    }

    /**
     * defining some convenient string constants
     */
    private static final String NONE = "NONE";

    /**
     * simply look up for all targets belonging to a extension point named <container>, is such an extension point
     * exists. then list all targets listed in a module named <container>, is such a module exists.
     * 
     * however, if the this.container variable has not been initialized then simply list down all targets in the current
     * module and all imported sub-modules.
     */
    public void execute() {

        getProject().log(
                LINE_SEP + "--- Available Targets for current project: " + getProject().getName() + " ---" + LINE_SEP);
        String container = getValue();
        if (container == null || container.trim().isEmpty()) {
            getProject().log(
                    LINE_SEP + "No ExtensionPoint / Plugin specified. Listing all targets available in the project.");

            List<TargetReport> targets = getEareport().getTargetReports();
            printTargets(targets);
        } else {
            ExtensionPointReport extensionPointRep = getEareport().getExtensionPointReport(container);

            if (extensionPointRep != null) {
                getProject().log("Targets for ExtensionPoint: " + container);
                List<TargetReport> targets = extensionPointRep.getTargetReports();
                printTargets(targets);
            } else {
                getProject().log("\tNo ExtensionPoint found by name: " + container);
            }

            ImportedModuleReport selectedModule = getEareport().getImportedModuleReport(container);
            if (selectedModule != null) {
                getProject().log(LINE_SEP + "Targets for Module: " + container);
                List<TargetReport> targets = selectedModule.getEasyantReport().getTargetReports();
                printTargets(targets);
            } else {
                getProject().log(LINE_SEP + "\tNo Module / Plugin found by name: " + container);
            }
        }
        getProject().log(
                LINE_SEP + LINE_SEP + "For more information on an target, run:" + LINE_SEP
                        + "\t easyant -describe <TARGET>");
        getProject().log(LINE_SEP + "--- End Of (Targets Listing) ---");
    }

    /**
     * common method to output a list of targets.
     * 
     * re-used multiple times in this class.
     * 
     * @param targets
     *            list of targets to print
     */
    private void printTargets(List<TargetReport> targets) {
        if (targets.isEmpty()) {
            getProject().log("\tNo targets found.");
            return;
        }
        for (TargetReport targetRep : targets) {
            getProject().log("\tTarget: " + targetRep.getName());
            getProject().log(
                    "\t\tExtension-Point: "
                            + (targetRep.getExtensionPoint() == null ? NONE : targetRep.getExtensionPoint()));
            getProject().log(
                    "\t\tDescription: " + (targetRep.getDescription() == null ? NONE : targetRep.getDescription()));
            getProject().log("\t\tDepends: " + (targetRep.getDepends() == null ? NONE : targetRep.getDepends()));
            getProject().log("\t\tIF: " + (targetRep.getIfCase() == null ? NONE : targetRep.getIfCase()));
            getProject().log("\t\tUNLESS: " + (targetRep.getUnlessCase() == null ? NONE : targetRep.getUnlessCase()));
        }
    }
}
