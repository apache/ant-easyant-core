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

/**
 * ManCommand implementation to list all extension points associated with specified 
 * build module.
 * 
 * Supports the -listExtensionPoints switch.
 */
public class ListExtensionPoints extends EasyantOption{
    
    private static final long serialVersionUID = 1L;
    
    
    public ListExtensionPoints()
            throws IllegalArgumentException {
        super("listExtensionPoints", false, "List all phases available");
        setStopBuild(true);
    }

    public void execute() {
        getProject().log(LINE_SEP+ "--- Available ExtensionPoints for current project: " + getProject().getName() + " ---" + LINE_SEP);
        
        List<ExtensionPointReport> extensionPoints = getEareport().getAvailableExtensionPoints();
        for(int i = 0; i<extensionPoints.size(); i++) {
            getProject().log("\t" + extensionPoints.get(i).getName());
        }
        
        getProject().log(LINE_SEP+ LINE_SEP+ "For more information on an ExtensionPoint, run:" + LINE_SEP + "\t easyant -describe <EXTENSION POINT>");
        getProject().log(LINE_SEP+ "--- End Of (ExtensionPoints Listing) ---");
    }

}
