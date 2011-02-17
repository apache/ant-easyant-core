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

import org.apache.easyant.core.report.EasyAntReport;
import org.apache.easyant.core.report.ImportedModuleReport;
import org.apache.tools.ant.Project;

/**
 * Lists all available plugins (imported modules) for the specified 
 * build module.
 */
public class ListPlugins implements ManCommand {
	private static String TAB = "\t";

	public void addParam(String param) {
		// DO NOTHING - NOT REQUIRED, SINCE THIS IS A 'LIST-ALL' COMMAND
	}

	public void execute(EasyAntReport earep, Project project) {
		String lineSep = System.getProperty("line.separator");
		project.log(lineSep + "--- Available Plugins for current project: " + project.getName() + " ---" + lineSep);
		
		List<ImportedModuleReport> moduleReps = earep.getImportedModuleReports();
		for(int i = 0; i<moduleReps.size(); i++) {
			project.log(TAB + moduleReps.get(i).getModuleMrid() + (moduleReps.get(i).getAs() == null ? 
					"" : ": Known as " + moduleReps.get(i).getAs()));
		}
		
		project.log(lineSep + lineSep + "For more information on a Plugin, run:" + lineSep + "\t easyant -describe <PLUGIN>");
		project.log(lineSep + "--- End Of (Plugins Listing) ---");
	}

}
