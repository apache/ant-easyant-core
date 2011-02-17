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
package org.apache.easyant.tasks.menu;

import org.apache.easyant.core.menu.MenuGenerator;
import org.apache.tools.ant.BuildException;

import java.io.IOException;
import java.util.List;

/**
 * Create a new menu using any registered generators.
 */
public class StartMenuTask extends AbstractMenuGeneratorTask {

	private String file;

	@Override
	public void execute() throws BuildException {
		if (getFile() == null) {
			throw new BuildException("file argument is required !");
		}

		//TODO: this isn't quite right.  we shouldn't be passing the same file argument to every generator.
		List<MenuGenerator> generators = getMenuGeneratorForContext(getContext()).getMenuGenerators();
		for (MenuGenerator generator : generators) {
			try {
			    generator.startMenu(getContext(), getFile());
			} catch (IOException ioe) {
			    throw new BuildException("Error writing menu file " + getFile() + ": " + ioe.getMessage(), ioe);
			}
		}
	}

	/**
	 * Get the file associated to this generator
	 * @return a file
	 */
	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

}
