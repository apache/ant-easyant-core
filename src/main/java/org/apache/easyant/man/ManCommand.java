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

import org.apache.easyant.core.report.EasyAntReport;
import org.apache.tools.ant.Project;

/**
 * Generic interface for all classes implementing functionality
 * for all project manual switches that are accepted on command
 * line when invoking easyant.
 * 
 * For example,
 * <br>
 * 		easyant -listTargets
 * 
 * <p />
 * The -listTargets and similar switches (like -describe etc.) are
 * all implemented by classes implementing this interface.
 * 
 * For each manual switch that is intended to be supported by easyant,
 * a new implementing class for this interface must be added that
 * implements the switch functionality.
 * 
 * Additionally, for the addition of any new switch, EasyAntMain needs
 * to be modified to add the new switch for the new manual functionality.
 */
public interface ManCommand {
	/**
	 * Add a parameter to the ManCommand instance. The implementing
	 * class may choose to process or ignore the parameter as need
	 * may be.
	 * @param param
	 */
	public void addParam(String param);
	
	/**
	 * Provides the ManCommand instance with a context to execute the
	 * command in.
	 * 
	 * The command is provided with an instance of generated EasyAntReport
	 * with which relevant manual information may be retrieved for implementing
	 * the switch functionality.
	 * @param earep
	 * @param project
	 */
	public void execute(EasyAntReport earep, Project project);
}
