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
package org.apache.easyant.core;

import java.util.Arrays;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * This class provides a few helper methods to deal with build configurations
 */
public class BuildConfigurationHelper {

    private BuildConfigurationHelper() {
    }

    /**
     * This methods verify if at least one build configuration given in arguments is contained in buildConfContainer.
     * 
     * @param buildConfs
     *            a string that represent build configurations names (can contains multiple configuration if they are
     *            comma separated)
     * @param buildConfContainer
     *            a string that represent build configurations container (can contains multiple configuration if they
     *            are comma separated)
     * @return true if the string contains at least one matching build configurations otherwise return false. If
     *         buildConfs is null this methods return true.
     */
    public static boolean contains(String buildConfs, String buildConfContainer) {
        if (buildConfs == null) {
            return true;
        }
        return (null != getFirstBuildConfigurationMatching(buildConfs, buildConfContainer));
    }

    /**
     * This methods return the first build configuration matching with the criteria given in buildConfs argument.
     * 
     * @param buildConfs
     *            a string that represent build configurations names (can contains multiple configuration if they are
     *            comma separated)
     * @param buildConfContainer
     *            a string that represent build configurations container (can contains multiple configuration if they
     *            are comma separated)
     * @return a string that represent the first build configuration found. Return null if no build configuration
     *         matched.
     */
    public static String getFirstBuildConfigurationMatching(String buildConfs, String buildConfContainer) {
        if (buildConfs == null || buildConfContainer == null) {
            return null;
        }
        List<String> buildConfsToCheck = buildList(buildConfs);
        List<String> availableBuildConfs = buildList(buildConfContainer);
        for (String conf : buildConfsToCheck) {
            if (availableBuildConfs.contains(conf)) {
                return conf;
            }
        }

        return null;
    }

    /**
     * This methods removes all the spaces in a list of confs separated by comma
     * 
     * @param confs
     *            a string that represent a list of conf separated by comma
     * @return a string that represent a list of conf separated by comma without spaces.
     */
    public static String removeSpaces(String confs) {
        return confs.replaceAll("\\s", "");
    }

    public static List<String> buildList(String conf) {
        return Arrays.asList(removeSpaces(conf).split(","));

    }

    /**
     * Check if build configuration is active
     * 
     * @param requestedConfigurations
     *            build configuration to check
     * @param p
     *            the project (used to retrieve available and active build configuration)
     * @param message
     *            (prefix message used for log)
     * @return return true if buildconfiguration is active. This method also returns true if requestedConfigurations is
     *         null
     */
    public static boolean isBuildConfigurationActive(String requestedConfigurations, Project p, String message) {
        if (requestedConfigurations == null) {
            p.log(message + " not bound to any build configuration", Project.MSG_DEBUG);
            return true;
        }
        List<String> buildConfigurationsList = buildList(requestedConfigurations);

        // check consistency, here we consider that a build configuration must
        // be explicitly declared
        if (p.getProperty(EasyAntMagicNames.AVAILABLE_BUILD_CONFIGURATIONS) == null) {
            throw new BuildException("there is no available build configuration");
        }

        List<String> availableBuildConfigurations = Arrays.asList(p.getProperty(
                EasyAntMagicNames.AVAILABLE_BUILD_CONFIGURATIONS).split(","));

        for (String conf : buildConfigurationsList) {
            if (!availableBuildConfigurations.contains(conf)) {
                throw new BuildException("unknown build configuration named " + conf);
            }
        }

        // is there any activated build configuration matching with the build
        // configurations defined?
        if (p.getProperty(EasyAntMagicNames.MAIN_CONFS) != null) {
            String buildConf = getFirstBuildConfigurationMatching(requestedConfigurations,
                    p.getProperty(EasyAntMagicNames.MAIN_CONFS));
            if (buildConf != null) {
                p.log(message + " bound to build configuration " + buildConf, Project.MSG_DEBUG);
                return true;
            } else {
                // if no activated build configuration match with required build
                // configuration, it means
                // that related element should not be loaded
                p.log(message + " not bound to any active build configuration. Requested build configuration was "
                        + requestedConfigurations, Project.MSG_DEBUG);
                return false;
            }
        } else {
            p.log("there is no activated build configuration", Project.MSG_DEBUG);
            return false;
        }
    }
}
