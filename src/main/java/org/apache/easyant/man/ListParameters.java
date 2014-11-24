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

import org.apache.easyant.core.report.ImportedModuleReport;
import org.apache.easyant.core.report.ParameterReport;

import java.util.List;

/**
 * Lists all parameters (deep search - includes all imported modules) available in the specified build module.
 */
public class ListParameters extends EasyantOption {

    private static final long serialVersionUID = 1L;

    public ListParameters() throws IllegalArgumentException {
        super("listParameters", true, "List all parameters available or specified in a given plugin as argument");
        setOptionalArg(true);
        setStopBuild(true);
    }

    public void execute() {
        getProject().log(
                LINE_SEP + "--- Available parameters for current project: " + getProject().getName() + " ---"
                        + LINE_SEP);
        /*
         * the plugin specified to this class through the addParam method needs to be searched for all parmeters, and
         * those parameters will be displayed by this class.
         */
        String plugin = getValue();
        if (plugin == null || plugin.trim().isEmpty()) {

            getProject().log(LINE_SEP + "No plugin specified. Listing all parameters available in the project.");
            List<ParameterReport> parameterReports = getEareport().getParameterReports();
            if (!parameterReports.isEmpty()) {
                printParameters(parameterReports);

            } else {
                getProject().log(LINE_SEP + "No parameter found in current project");
            }

        } else {
            getProject().log(
                    LINE_SEP + "--- Filtering parameters declared in the following plugin: " + plugin + " ---"
                            + LINE_SEP);
            ImportedModuleReport moduleRep = getEareport().getImportedModuleReport(plugin);
            if (moduleRep == null) {
                getProject().log("\tNo Module / Plugin found by given name: " + plugin);
            } else {
                List<ParameterReport> parameterReports = moduleRep.getEasyantReport().getParameterReports();
                if (!parameterReports.isEmpty()) {
                    printParameters(parameterReports);
                } else {
                    getProject().log(LINE_SEP + "No parameter found in the plugin: " + plugin);
                }
            }
        }
        getProject().log(
                LINE_SEP + LINE_SEP + "For more information on a parameter, run:" + LINE_SEP
                        + "\t easyant -describe <parameter name>");
        getProject().log(LINE_SEP + "--- End Of (Parameter Listing) ---");
    }

    private void printParameters(List<ParameterReport> parameters) {
        for (ParameterReport parameterReport : parameters) {
            getProject().log("\tParameter (type=" + parameterReport.getType() + "): " + parameterReport.getName());
        }
    }
}
