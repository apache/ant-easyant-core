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

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.easyant.core.descriptor.PropertyDescriptor;
import org.apache.easyant.core.report.EasyAntReport;
import org.apache.easyant.core.report.ImportedModuleReport;
import org.apache.tools.ant.Project;

/**
 * Lists all properties (deep search - includes all imported modules)
 * available in the specified build module.
 */
public class ListProps implements ManCommand {
    private String plugin = null;
    
    public void addParam(String param) {
        this.plugin = param;
    }

    public void execute(EasyAntReport earep, Project project) {
        String lineSep = System.getProperty("line.separator");

        /*
         * the plugin specified to this class through the addParam method
         * needs to be searched for all properties, and those
         * properties will be displayed by this class.
         */
        
        if(plugin == null || plugin.trim().length() == 0) {
            throw new IllegalArgumentException("No plugin found to list properties for.");
        }

        project.log(lineSep + "--- Available Properties for current project: " + project.getName() + 
                ":: Plugin: " + plugin + " ---" + lineSep);

        ImportedModuleReport moduleRep = earep.getImportedModuleReport(plugin);
        if(moduleRep == null) {
            project.log("\tNo Module / Plugin found by given name: " + plugin);
        } else {
            Map<String, PropertyDescriptor> allprops = moduleRep.getEasyantReport().getPropertyDescriptors();
            
            if(allprops.size() > 0) {
                for(Iterator<Entry<String, PropertyDescriptor>> it = allprops.entrySet().iterator(); it.hasNext(); ) {
                    Entry<String, PropertyDescriptor> entry = it.next();
                    PropertyDescriptor prop = entry.getValue();
                    project.log("\tProperty: " + prop.getName());
                }
        
                project.log(lineSep + lineSep + "For more information on a Property, run:" + lineSep 
                        + "\t easyant -describe <PROPERTY>");
            } else {
                project.log(lineSep + "No property found in the plugin: " + plugin);
            }
        }
        project.log(lineSep + "--- End Of (Properties Listing) ---");
    }

}
