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

import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.easyant.core.services.PluginService;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Property;
import org.apache.tools.ant.types.Path;

import java.util.ArrayList;
import java.util.List;

/**
 * parameter tasks is used to :
 * <p/>
 * document properties / paths check if properties /paths are required set default values if properties are not set
 * <p/>
 * This could be usefull in precondition of each modules, to check if property/path are set. And much more usefull to
 * document our modules.
 */
public class ParameterTask extends Task {
    private String property;
    private String path;

    private String description;
    private String defaultValue;
    private boolean required;
    private String phase;
    private List<String> possibleValues = new ArrayList<String>();

    /**
     * Get a description to the property / path
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * set a description to the property / path
     *
     * @param description the description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Appends CDATA text inside the Ant task to description
     *
     * @see #setDescription(String)
     */
    public void addText(String descriptionText) {
        if (!descriptionText.trim().isEmpty()) {
            descriptionText = getProject().replaceProperties(descriptionText);
            if (getDescription() == null) {
                setDescription(descriptionText);
            } else {
                setDescription(getDescription() + descriptionText);
            }
        }
    }

    /**
     * Get the property name to check
     *
     * @return a property name
     */
    public String getProperty() {
        return property;
    }

    /**
     * Set the property name to check
     *
     * @param property a property name
     */
    public void setProperty(String property) {
        this.property = property;
    }

    /**
     * Get the path to check
     *
     * @return a pathId
     */
    public String getPath() {
        return path;
    }

    /**
     * Set the path to check
     *
     * @param path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Get the default value (only available for property)
     *
     * @return a string that represents the default value
     */
    public String getDefault() {
        return defaultValue;
    }

    /**
     * Set the default value (only available for property)
     *
     * @param defaultValue a string that represents the default value
     */
    public void setDefault(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * Is the refererenced property / path required?
     *
     * @return
     */
    public boolean isRequired() {
        // don't break the build in audit mode
        if (Project.toBoolean(getProject().getProperty(EasyAntMagicNames.AUDIT_MODE))) {
            return false;
        } else {
            return required;
        }
    }
    
    /**
     * Gets if this parameter is required or not.
     *
     * <p>
     * This is the unsafe version of {@link #isRequired()}, this method does not check if project is in audit mode. This
     * method should not be use in EasyAnt engine, but only to output the required value in {@link PluginService}.
     * 
     * @return {@code true} if this parameter is required, {@code false} otherwise.
     * 
     * @see EasyAntMagicNames#AUDIT_MODE
     */
    public boolean isRequiredUnsafe() {
        return required;
    }

    /**
     * specify if the property / path is mandatory
     *
     * @param required
     */
    public void setRequired(boolean required) {
        this.required = required;
    }

    /**
     * Get list of possible values of a property
     *
     * @return a list of values
     */
    public List<String> getPossibleValues() {
        return possibleValues;
    }

    /**
     * Set list of possible values of a property
     *
     * @param possibleValuesAsString a comma separated list of values
     */
    public void setPossibleValues(String possibleValuesAsString) {
        if (possibleValuesAsString != null) {
            String[] split = possibleValuesAsString.split(",");
            for (String possibleValue : split) {
                possibleValues.add(possibleValue.trim());
            }
        }
    }

    public void execute() throws BuildException {
        if (property != null) {
            handlePropertyParameter();
        } else if (path != null) {
            handlePathParameter();
        } else if (phase != null) {
            // to be removed
        } else {
            throw new BuildException("at least one of these attributes is required: property, path");
        }
    }

    private void handlePathParameter() {
        Object p = getProject().getReference(path);
        if (isRequired() && p == null) {
            throw new BuildException("expected path '" + path + "': " + description);
        } else if (p != null && !(p instanceof Path)) {
            throw new BuildException("reference '" + path + "' must be a path");
        }
    }

    private void handlePropertyParameter() {
        if (isRequired() && getProject().getProperty(property) == null) {
            throw new BuildException("expected property '" + property + "': " + description);
        }
        if (!possibleValues.isEmpty()) {
            String currentValue = getProject().getProperty(property);
            if (!possibleValues.contains(currentValue)) {
                throw new BuildException("current value of property '" + property
                        + "' doesn't match with possible values : " + possibleValues.toString());
            }
        }
        if (defaultValue != null && getProject().getProperty(property) == null) {
            Property propTask = new Property();
            propTask.setProject(getProject());
            propTask.setTaskName(getTaskName());
            propTask.setName(property);
            propTask.setValue(defaultValue);
            propTask.execute();
        }
    }

    @Deprecated
    // FIXME : remove this method after 0.9 release
    public void setPhase(String phase) {
        this.phase = phase;
    }
}
