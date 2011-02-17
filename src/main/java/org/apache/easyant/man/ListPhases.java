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
import org.apache.easyant.core.report.PhaseReport;
import org.apache.tools.ant.Project;

/**
 * ManCommand implementation to list all phases associated with specified 
 * build module.
 * 
 * Supports the -listPhases switch.
 */
public class ListPhases implements ManCommand {
	// convenient string constant for formatting
	private static String TAB = "\t";
	
	public void addParam(String param) {
		// this command does not make use of params
	}

	public void execute(EasyAntReport earep, Project project) {
		String lineSep = System.getProperty("line.separator");
		project.log(lineSep + "--- Available Phases for current project: " + project.getName() + " ---" + lineSep);
		
		List<PhaseReport> phases = earep.getAvailablePhases();
		for(int i = 0; i<phases.size(); i++) {
			project.log(TAB + phases.get(i).getName());
		}
		
		project.log(lineSep + lineSep + "For more information on a Phase, run:" + lineSep + "\t easyant -describe <PHASE>");
		project.log(lineSep + "--- End Of (Phases Listing) ---");
	}

}
