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

import org.apache.easyant.core.descriptor.PropertyDescriptor;
import org.apache.easyant.core.report.ImportedModuleReport;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Lists all properties (deep search - includes all imported modules) available in the specified build module.
 */
public class ListProps extends EasyantOption {

    private static final long serialVersionUID = 1L;

    public ListProps() throws IllegalArgumentException {
        super("listProps", true, "List all properties available or specified in a given plugin as argument");
        setOptionalArg(true);
        setStopBuild(true);
    }

    public void execute() {
        getProject().log(
                LINE_SEP + "--- Available Properties for current project: " + getProject().getName() + " ---"
                        + LINE_SEP);
        /*
         * the plugin specified to this class through the addParam method needs to be searched for all properties, and
         * those properties will be displayed by this class.
         */
        String plugin = getValue();
        if (plugin == null || plugin.trim().isEmpty()) {

            getProject().log(LINE_SEP + "No plugin specified. Listing all properties available in the project.");

            Map<String, PropertyDescriptor> allProjectProps = getEareport().getPropertyDescriptors();
            if (!allProjectProps.isEmpty()) {
                printProperties(allProjectProps);

            } else {
                getProject().log(LINE_SEP + "No property found in current project");
            }

        } else {
            getProject().log(
                    LINE_SEP + "--- Filtering properties declared in the following plugin: " + plugin + " ---"
                            + LINE_SEP);
            ImportedModuleReport moduleRep = getEareport().getImportedModuleReport(plugin);
            if (moduleRep == null) {
                getProject().log("\tNo Module / Plugin found by given name: " + plugin);
            } else {
                Map<String, PropertyDescriptor> allprops = moduleRep.getEasyantReport().getPropertyDescriptors();

                if (!allprops.isEmpty()) {
                    printProperties(allprops);
                } else {
                    getProject().log(LINE_SEP + "No property found in the plugin: " + plugin);
                }
            }
        }
        getProject().log(
                LINE_SEP + LINE_SEP + "For more information on a Property, run:" + LINE_SEP
                        + "\t easyant -describe <PROPERTY>");
        getProject().log(LINE_SEP + "--- End Of (Properties Listing) ---");
    }

    private void printProperties(Map<String, PropertyDescriptor> allProjectProps) {
        for (Entry<String, PropertyDescriptor> entry : allProjectProps.entrySet()) {
            PropertyDescriptor prop = entry.getValue();
            getProject().log("\tProperty: " + prop.getName());

        }
    }

}
