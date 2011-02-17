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
package org.apache.easyant.tasks;

import org.apache.easyant.core.ant.Phase;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Property;
import org.apache.tools.ant.types.Path;

/**
 * parameter tasks is used to :
 * 
 * 			document properties / paths / phases 
 * 			check if properties /paths / phases are required 
 * 			set default values if properties are not set
 * 
 * This could be usefull in precondition of each modules, to check if
 * property/phase/path are set. And much more usefull to document our modules.
 * 
 */
public class ParameterTask extends Task {
	private String property;
	private String path;
	private String phase;

	private String description;
	private String defaultValue;
	private boolean required;

	/**
	 * Get a description to the property / path / phase
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * set a description to the property / path / phase
	 * @param description the description
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	
	/**
	 * Appends CDATA text inside the Ant task to description
	 * @see #setDescription(String)
	 */
	public void addText(String descriptionText) {
		if (descriptionText.trim().length() > 0) {
			descriptionText = getProject().replaceProperties(descriptionText);
			if (getDescription() == null) {
				setDescription(descriptionText);
			} else {
				setDescription(getDescription()+descriptionText);
			}
		}
	}

	/**
	 * Get the property name to check
	 * @return a property name
	 */
	public String getProperty() {
		return property;
	}

	/**
	 * Set the property name to check
	 * @param property a property name
	 */
	public void setProperty(String property) {
		this.property = property;
	}

	/**
	 * Get the path to check
	 * @return a pathId 
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Set the path to check
	 * @param path
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * Get a phase to check
	 * @return a phase name
	 */
	public String getPhase() {
		return phase;
	}

	/**
	 * Set the path to check
	 * @param phase a phase name 
	 */
	public void setPhase(String phase) {
		this.phase = phase;
	}

	/**
	 * Get the default value (only available for property)
	 * @return a string that represents the default value
	 */
	public String getDefault() {
		return defaultValue;
	}

	/**
	 * Set the default value (only  available for property)
	 * @param defaultValue a string that represents the default value
	 */
	public void setDefault(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * Is the refererenced property / path required?
	 * @return
	 */
	public boolean isRequired() {
		return required;
	}

	/**
	 * specify if the property / path is mandatory
	 * @param required
	 */
	public void setRequired(boolean required) {
		this.required = required;
	}

	public void execute() throws BuildException {
		if (property != null) {
			if (required && getProject().getProperty(property) == null) {
				throw new BuildException("expected property '" + property
						+ "': " + description);
			}
			if (defaultValue != null
					&& getProject().getProperty(property) == null) {
				Property propTask = new Property();
				propTask.setProject(getProject());
				propTask.setTaskName(getTaskName());
				propTask.setName(property);
				propTask.setValue(defaultValue);
				propTask.execute();
			}
		} else if (phase != null) {
			Target p = (Target) getProject().getTargets().get(phase);
			if (p == null) {
				throw new BuildException("expected phase '" + phase + "': "
						+ description);
			} else if (!(p instanceof Phase)) {
				throw new BuildException("target '" + phase
						+ "' must be a phase rather than a target");
			}
		} else if (path != null) {
			Object p = getProject().getReference(path);
			if (required && p == null) {
				throw new BuildException("expected path '" + path + "': "
						+ description);
			} else if (!(p instanceof Path)) {
				throw new BuildException("reference '" + path
						+ "' must be a path");
			}
		} else {
			throw new BuildException(
					"at least one of these attributes is required: property, path, phase");
		}
	}
}
