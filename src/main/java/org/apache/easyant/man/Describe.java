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
import org.apache.easyant.core.report.EasyAntReport;
import org.apache.easyant.core.report.PhaseReport;
import org.apache.easyant.core.report.TargetReport;
import org.apache.tools.ant.Project;

/**
 * Implements support for -describe easyant switch.
 * 
 * This command searches for a phase, target, and properties by the 
 * supplied search name and returns result for each of these sequentially. 
 */
public class Describe implements ManCommand {
    private String target;
    
    // convenient string constants
    private static final String NONE = "NONE";
    
    public void addParam(String param) {
        this.target = param;
    }

    public void execute(EasyAntReport earep, Project project) {
        String lineSep = System.getProperty("line.separator");
        
        if(target == null || target.length() == 0) {
            throw new IllegalArgumentException("No parameter specified for -describe parameter.");
        }
        
        project.log(lineSep + "--- Available references for: " + target +
                " in current project: " + project.getName() + " ---" + lineSep);
        
        PhaseReport phaseRep = earep.getPhaseReport(target, true);
        if(phaseRep != null) {
            project.log("\tPhase: " + target);
            project.log("\t\tDescription: " + (phaseRep.getDescription() == null ? NONE : phaseRep.getDescription()));
            project.log("\t\tDepends: " + (phaseRep.getDepends() == null ? NONE : phaseRep.getDepends()));
            project.log(lineSep + "\t\tFor information on targets attached to this phase, run:");
            project.log("\t\teasyant -listTargets " + target);
        } else {
            project.log("\tNo Phase found for name: " + target);
        }
        TargetReport targetRep = earep.getTargetReport(target, true);
        if(targetRep != null) {
            project.log("\tTarget: " + target);
            project.log("\t\tPhase: " + (targetRep.getPhase() == null ? NONE : targetRep.getPhase()));
            project.log("\t\tDescription: " + (targetRep.getDescription() == null ? NONE : targetRep.getDescription()));
            project.log("\t\tDepends: " + (targetRep.getDepends() == null ? NONE : targetRep.getDepends()));
            project.log("\t\tIF: " + (targetRep.getIfCase() == null ? NONE : targetRep.getIfCase()));
            project.log("\t\tUNLESS: " + (targetRep.getUnlessCase() == null ? NONE : targetRep.getUnlessCase()));
        } else {
            project.log("\tNo Target found for name: " + target);
        }
        PropertyDescriptor prop = earep.getAvailableProperties().get(target);
        if(prop != null) {
            project.log("\tProperty: " + target);
            project.log("\t\tDescription: " + (prop.getDescription() == null ? NONE : prop.getDescription()));
            String defaultValue = prop.getDefaultValue() == null ? NONE : prop.getDefaultValue();
            project.log("\t\tDefault: " + defaultValue);
            project.log("\t\tRequired: " + prop.isRequired());
            String currentValue = prop.getValue() == null ? defaultValue : prop.getValue();
            project.log("\t\tCurrent value: " + currentValue);
        } else {
            project.log("\tNo Property found for name: " + target);
        }
        
        project.log(lineSep + "--- End Of (Describe) ---");
    }

}
