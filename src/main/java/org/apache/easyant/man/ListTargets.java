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

import java.util.List;

import org.apache.easyant.core.report.EasyAntReport;
import org.apache.easyant.core.report.ImportedModuleReport;
import org.apache.easyant.core.report.PhaseReport;
import org.apache.easyant.core.report.TargetReport;
import org.apache.tools.ant.Project;

/**
 * Implements the '-listTargets' switch for project Manual.
 * 
 * This command lists all targets belonging to the specified 
 * phase / plugin. 
 * 
 * If no phase / plugin name is specified, then this command
 * lists all targets available in the project (module.ivy)
 */
public class ListTargets implements ManCommand {
	/*
	 * name of the phase or plugin for which the targets 
	 * have been requested for.
	 */
	private String container = null;
	
	/*
	 * defining some convenient string constants
	 */
	private static final String TAB = "\t";
	private static final String NONE = "NONE";
	
	public void addParam(String param) {
		this.container = param;
	}

	/*
	 * simply look up for all targets belonging to a phase named <container>, is such
	 * a phase exists. then list all targets listed in a module named <container>, is
	 * such a module exists.
	 * 
	 * however, if the this.container variable has not been initialized then simply list
	 * down all targets in the current module and all imported sub-modules.
	 */
	public void execute(EasyAntReport earep, Project project) {
		String lineSep = System.getProperty("line.separator");
		
		project.log(lineSep + "--- Available Targets for current project: " + project.getName() + " ---" + lineSep);
		
		if(this.container == null || this.container.trim().length() == 0) {
			project.log(lineSep + "No Phase / Plugin specified. Listing all targets available in the project.");
			
			List<TargetReport> targets = earep.getAvailableTargets();
			printTargets(targets, project);
		} else {
			PhaseReport phase = earep.getPhaseReport(this.container, true);
			
			if(phase != null) {
				project.log("Targets for Phase: " + this.container);
				List<TargetReport> targets = phase.getTargetReports();
				printTargets(targets, project);
			} else {
				project.log(TAB + "No Phase found by name: " + this.container);
			}
			
			List<ImportedModuleReport> modules = earep.getImportedModuleReports();
			ImportedModuleReport selected = null;
			for(int i = 0; i<modules.size(); i++) {
				selected = modules.get(i);
				if(container.equals(selected.getModuleMrid())) {
					break;
				}
			}
			if(selected != null) {
				project.log(lineSep + "Targets for Module: " + this.container);
				List<TargetReport> targets = selected.getEasyantReport().getTargetReports();
				printTargets(targets, project);
			} else {
				project.log(lineSep + TAB + "No Module / Plugin found by name: " + this.container);
			}
				
			project.log(lineSep + lineSep + "For more information on a Phase, run:" + lineSep + "\t easyant -describe <PHASE>");
		}
		project.log(lineSep + "--- End Of (Phases Listing) ---");
	}

	/*
	 * common method to output a list of targets.
	 * 
	 * re-used multiple times in this class.
	 */
	private void printTargets(List<TargetReport> targets, Project project) {
		if(targets.size() == 0) {
			project.log(TAB + "No targets found.");
			return;
		}
		for(int i = 0; i<targets.size(); i++) {
			TargetReport targetRep = targets.get(i);
			project.log(TAB + "Target: " + targetRep.getName());
			project.log(TAB + TAB + "Phase: " + (targetRep.getPhase() == null ? NONE : targetRep.getPhase()));
			project.log(TAB + TAB + "Description: " + (targetRep.getDescription() == null ? NONE : targetRep.getDescription()));
			project.log(TAB + TAB + "Depends: " + (targetRep.getDepends() == null ? NONE : targetRep.getDepends()));
			project.log(TAB + TAB + "IF: " + (targetRep.getIfCase() == null ? NONE : targetRep.getIfCase()));
			project.log(TAB + TAB + "UNLESS: " + (targetRep.getUnlessCase() == null ? NONE : targetRep.getUnlessCase()));
		}
	}
}
