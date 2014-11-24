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

import org.apache.ivy.ant.IvyTask;
import org.apache.tools.ant.BuildException;

/**
 * This task is responsible of checking existance of a resolver in a given ivy instance
 */
public class CheckResolver extends IvyTask {

    private String resolver;

    private String description;

    @Override
    public void doExecute() throws BuildException {
        if (resolver == null || resolver.equals("")) {
            throw new BuildException("resolver attribute is mandatory");
        }
        String resolverProperty = getProject().getProperty(resolver);
        if (resolverProperty == null) {
            throw new BuildException("Can't check resolvers :Unknown property " + resolver);
        }
        if (!getSettings().getResolverNames().contains(resolverProperty)) {
            StringBuilder sb = new StringBuilder();
            sb.append("resolver ").append(resolverProperty);
            sb.append(" does not exist in current project, please check your project ivysettings.xml file.");
            sb.append("\n");
            if (getDescription() != null) {
                sb.append(resolver);
                sb.append(" : ");
                sb.append(getDescription());
                sb.append("\n");
            }
            sb.append("Available resolvers : ");
            sb.append(getSettings().getResolverNames().toString());
            throw new BuildException(sb.toString());
        }

    }

    /**
     * Get property resolver name to check
     *
     * @return the property name representing the resolver
     */
    public String getResolver() {
        return resolver;
    }

    /**
     * Set the property resolver name to check
     *
     * @param resolver a property name representing the resolver
     */
    public void setResolver(String resolver) {
        this.resolver = resolver;
    }

    /**
     * Get a description to the property / path / extension-point
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * set a description to the property / path / extension-point
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

}
