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
package org.apache.easyant.tasks.menu;

import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.easyant.core.menu.MenuGeneratorRegistry;
import org.apache.tools.ant.Task;

/**
 * Abstract class for MenuGenerator. 
 * This task provides a few methods to retrieve the right MenuGeneratorRegistry
 * for a given context. 
 * If you're planning to write your own MenuGeneratorTask we strongly recommend you to extend this task.
 */
public abstract class AbstractMenuGeneratorTask extends Task{
	
	private String context="default";

	/**
	 * Get or create if necessary a MenuGeneratorRegistry for a given context;
	 * @param context a string representing a given context
	 * @return a MenuGeneratorRegistry
	 */
	public MenuGeneratorRegistry getMenuGeneratorForContext(String context) {
		return MenuGeneratorUtils.getRegistryForContext(getProject(), context, true);
	}

	/**
	 * Get the context
	 * @return a context
	 */
	public String getContext() {
		return context;
	}

	/**
	 * Set the context
	 * @param context a context
	 */
	public void setContext(String context) {
		this.context = context;
	}

}
