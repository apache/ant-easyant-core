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
package org.apache.easyant.core.ivy;

import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.ivy.ant.IvyAntSettings;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Reference;

/**
 * Helper providing methods to play with both project and easyant ivy instances
 * 
 */
public class IvyInstanceHelper {
    private IvyInstanceHelper() {
    }

    /**
     * Get the project ivy instance name. This methods takes in consideration that project ivy instance name can be set
     * through "project.ivy.instance" property
     * 
     * @param project
     *            a project instance
     * @return the project ivy instance name
     */
    public static String getProjectIvyInstanceName(Project project) {
        String projectIvyInstanceProp = project.getProperty(EasyAntMagicNames.PROJECT_IVY_INSTANCE);
        if (projectIvyInstanceProp == null) {
            projectIvyInstanceProp = EasyAntMagicNames.PROJECT_IVY_INSTANCE;
        }
        return projectIvyInstanceProp;
    }

    /**
     * Build a project ivy reference.
     * 
     * @param project
     *            a project instance
     * @return a project ivy refrence
     */
    public static Reference buildProjectIvyReference(Project project) {
        return buildIvyReference(project, getProjectIvyInstanceName(project));
    }

    /**
     * Build an easyant ivy reference
     * 
     * @param project
     *            a project instance
     * @return an easyant ivy reference
     */
    public static Reference buildEasyAntIvyReference(Project project) {
        return buildIvyReference(project, EasyAntMagicNames.EASYANT_IVY_INSTANCE);
    }

    /**
     * Build an ivy instance reference based on a given instance name
     * 
     * @param project
     *            a project instance
     * @param instanceName
     *            an instance name
     * @return an ivy instance reference
     */
    public static Reference buildIvyReference(Project project, String instanceName) {
        return new Reference(project, instanceName);
    }

    /**
     * Get project {@link IvyAntSettings}
     * 
     * @param project
     *            a project instance
     * @return the project {@link IvyAntSettings}
     */
    public static IvyAntSettings getProjectIvyAntSettings(Project project) {
        return getIvyAntSettings(project, getProjectIvyInstanceName(project));
    }

    /**
     * Get easyant {@link IvyAntSettings}
     * 
     * @param project
     *            a project instance
     * @return the easyant {@link IvyAntSettings}
     */
    public static IvyAntSettings getEasyAntIvyAntSettings(Project project) {
        return getIvyAntSettings(project, EasyAntMagicNames.EASYANT_IVY_INSTANCE);
    }

    /**
     * Get an {@link IvyAntSettings} based on instance name
     * 
     * @param project
     *            a project instance
     * @param instanceName
     *            an {@link IvyAntSettings} name
     * @return the requested {@link IvyAntSettings}
     */
    public static IvyAntSettings getIvyAntSettings(Project project, String instanceName) {
        Object o = project.getReference(instanceName);
        if (o instanceof IvyAntSettings) {
            return (IvyAntSettings) o;
        } else {
            throw new IllegalStateException(instanceName + " is not a valid ivy instance");
        }
    }

}
