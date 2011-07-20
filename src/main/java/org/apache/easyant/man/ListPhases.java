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

import org.apache.easyant.core.report.PhaseReport;

/**
 * ManCommand implementation to list all phases associated with specified 
 * build module.
 * 
 * Supports the -listPhases switch.
 */
public class ListPhases extends EasyantOption{
    
    public ListPhases()
            throws IllegalArgumentException {
        super("listPhases", false, "List all phases available");
        setOptionalArg(true);
        setStopBuild(true);
    }

    public void execute() {
        getProject().log(LINE_SEP+ "--- Available Phases for current project: " + getProject().getName() + " ---" + LINE_SEP);
        
        List<PhaseReport> phases = getEareport().getAvailablePhases();
        for(int i = 0; i<phases.size(); i++) {
            getProject().log("\t" + phases.get(i).getName());
        }
        
        getProject().log(LINE_SEP+ LINE_SEP+ "For more information on a Phase, run:" + LINE_SEP + "\t easyant -describe <PHASE>");
        getProject().log(LINE_SEP+ "--- End Of (Phases Listing) ---");
    }

}
