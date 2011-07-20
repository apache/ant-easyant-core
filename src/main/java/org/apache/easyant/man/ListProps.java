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
import org.apache.easyant.core.report.ImportedModuleReport;

/**
 * Lists all properties (deep search - includes all imported modules)
 * available in the specified build module.
 */
public class ListProps extends EasyantOption {
    
    public ListProps()
            throws IllegalArgumentException {
        super("listProps", false, "List all properties available or specified in a given plugin as argument");
        setOptionalArg(true);
        setStopBuild(true);
    }

    public void execute() {
        /*
         * the plugin specified to this class through the addParam method
         * needs to be searched for all properties, and those
         * properties will be displayed by this class.
         */
        String plugin = getValue();
        if(plugin == null || plugin.trim().length() == 0) {
            throw new IllegalArgumentException("No plugin found to list properties for.");
        }

        getProject().log(LINE_SEP+ "--- Available Properties for current project: " + getProject().getName() + 
                ":: Plugin: " + plugin + " ---" + LINE_SEP);

        ImportedModuleReport moduleRep = getEareport().getImportedModuleReport(plugin);
        if(moduleRep == null) {
            getProject().log("\tNo Module / Plugin found by given name: " + plugin);
        } else {
            Map<String, PropertyDescriptor> allprops = moduleRep.getEasyantReport().getPropertyDescriptors();
            
            if(allprops.size() > 0) {
                for(Iterator<Entry<String, PropertyDescriptor>> it = allprops.entrySet().iterator(); it.hasNext(); ) {
                    Entry<String, PropertyDescriptor> entry = it.next();
                    PropertyDescriptor prop = entry.getValue();
                    getProject().log("\tProperty: " + prop.getName());
                }
        
                getProject().log(LINE_SEP+LINE_SEP+ "For more information on a Property, run:" + LINE_SEP 
                        + "\t easyant -describe <PROPERTY>");
            } else {
                getProject().log(LINE_SEP + "No property found in the plugin: " + plugin);
            }
        }
        getProject().log(LINE_SEP + "--- End Of (Properties Listing) ---");
    }

}
