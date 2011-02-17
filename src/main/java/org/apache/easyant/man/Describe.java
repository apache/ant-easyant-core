/* 
 *  Copyright 2008-2010 the EasyAnt project
 * 
 *  See the NOTICE file distributed with this work for additional information
 *  regarding copyright ownership. 
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software 
 *  distributed under the License is distributed on an "AS IS" BASIS, 
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and 
 *  limitations under the License.
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
	private static final String TAB = "\t";
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
			project.log(TAB + "Phase: " + target);
			project.log(TAB + TAB + "Description: " + (phaseRep.getDescription() == null ? NONE : phaseRep.getDescription()));
			project.log(TAB + TAB + "Depends: " + (phaseRep.getDepends() == null ? NONE : phaseRep.getDepends()));
			project.log(lineSep + TAB + TAB + "For information on targets attached to this phase, run:");
			project.log(TAB + TAB + "easyant -listTargets " + target);
		} else {
			project.log(TAB + "No Phase found for name: " + target);
		}
		TargetReport targetRep = earep.getTargetReport(target, true);
		if(targetRep != null) {
			project.log(TAB + "Target: " + target);
			project.log(TAB + TAB + "Phase: " + (targetRep.getPhase() == null ? NONE : targetRep.getPhase()));
			project.log(TAB + TAB + "Description: " + (targetRep.getDescription() == null ? NONE : targetRep.getDescription()));
			project.log(TAB + TAB + "Depends: " + (targetRep.getDepends() == null ? NONE : targetRep.getDepends()));
			project.log(TAB + TAB + "IF: " + (targetRep.getIfCase() == null ? NONE : targetRep.getIfCase()));
			project.log(TAB + TAB + "UNLESS: " + (targetRep.getUnlessCase() == null ? NONE : targetRep.getUnlessCase()));
		} else {
			project.log(TAB + "No Target found for name: " + target);
		}
		PropertyDescriptor prop = earep.getAvailableProperties().get(target);
		if(prop != null) {
			project.log(TAB + "Property: " + target);
			project.log(TAB + TAB + "Description: " + (prop.getDescription() == null ? NONE : prop.getDescription()));
			String defaultValue = prop.getDefaultValue() == null ? NONE : prop.getDefaultValue();
			project.log(TAB + TAB + "Default: " + defaultValue);
			project.log(TAB + TAB + "Required: " + prop.isRequired());
			String currentValue = prop.getValue() == null ? defaultValue : prop.getValue();
			project.log(TAB + TAB + "Current value: " + currentValue);
		} else {
			project.log(TAB + "No Property found for name: " + target);
		}
		
		project.log(lineSep + "--- End Of (Describe) ---");
	}

}
