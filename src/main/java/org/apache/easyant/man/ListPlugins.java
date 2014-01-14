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

import java.util.Set;

import org.apache.easyant.core.report.ImportedModuleReport;

/**
 * Lists all available plugins (imported modules) for the specified build module.
 */
public class ListPlugins extends EasyantOption {
    private static final long serialVersionUID = 1L;

    public ListPlugins() throws IllegalArgumentException {
        super("listPlugins", false, "List all plugins used by the project");
        setStopBuild(true);
    }

    public void execute() {
        getProject().log(
                LINE_SEP + "--- Available Plugins for current project: " + getProject().getName() + " ---" + LINE_SEP);

        Set<ImportedModuleReport> moduleReps = getEareport().getImportedModuleReports();
        for (ImportedModuleReport importedModuleReport : moduleReps) {
            getProject()
                    .log("\t"
                            + importedModuleReport.getModuleMrid()
                            + (importedModuleReport.getAs() == null ? "" : ": Known as " + importedModuleReport.getAs()));
        }

        getProject().log(
                LINE_SEP + LINE_SEP + "For more information on a Plugin, run:" + LINE_SEP
                        + "\t easyant -describe <PLUGIN>");
        getProject().log(LINE_SEP + "--- End Of (Plugins Listing) ---");
    }

}
