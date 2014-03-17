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
package org.apache.easyant.core.services;

import java.io.File;

import org.apache.easyant.core.descriptor.EasyAntModuleDescriptor;
import org.apache.easyant.core.report.EasyAntReport;
import org.apache.ivy.core.module.id.ModuleRevisionId;

public interface PluginService {

    /**
     * Generate an easyantReport for a given moduleRevisionID
     * 
     * @param moduleRevisionId
     *            a given moduleRevisionID
     * @return an easyantReport an easyantReport
     * @throws Exception
     */
    EasyAntReport getPluginInfo(ModuleRevisionId moduleRevisionId) throws Exception;

    /**
     * Generate an easyantReport for a given string representing a plugin moduleRevisionID mrid should be with the
     * following format : organisation#moduleName;revision If no organisation is specified, this method will use the
     * default one, then you could use the shorter form like myPlugin;0.1
     * 
     * @param moduleRevisionId
     *            a given moduleRevisionID
     * @return an easyantReport an easyantReport
     * @throws Exception
     */
    EasyAntReport getPluginInfo(String moduleRevisionId) throws Exception;

    /**
     * Generate an easyantReport for a given string representing a buildtype moduleRevisionID mrid should be with the
     * following format : organisation#moduleName;revision If no organisation is specified, this method will use the
     * default one, then you could use the shorter form like myPlugin;0.1
     * 
     * @param moduleRevisionId
     *            a given moduleRevisionID
     * @return an easyantReport an easyantReport
     * @throws Exception
     */
    EasyAntReport getBuildTypeInfo(String moduleRevisionId) throws Exception;

    /**
     * Generate an easyantReport for a given moduleRevisionID
     * 
     * @param moduleRevisionId
     *            a given moduleRevisionID
     * @param conf
     *            a configuration name
     * @return an easyantReport
     * @throws Exception
     */
    EasyAntReport getPluginInfo(ModuleRevisionId moduleRevisionId, String conf) throws Exception;

    /**
     * Generate an {@link EasyAntReport} for a given pluginIvyFile
     * 
     * @param pluginIvyFile
     *            plugin ivy file
     * @param sourceDirectory
     *            source directory
     * @param conf
     *            a configuration name
     * @return an {@link EasyAntReport}
     * @throws Exception
     */
    EasyAntReport getPluginInfo(File pluginIvyFile, File sourceDirectory, String conf) throws Exception;

    /**
     * Return an array of moduleRevisionId that match with given criteria. Equivalent to
     * {@link #search(String, String, String, String, String, String) search(organisation, moduleName, null, null,
     * PatternMatcher.EXACT_OR_REGEXP, null)}
     * 
     * @param organisation
     *            the organisation name
     * @param moduleName
     *            the module name
     * @return an array of moduleRevisionId
     * @throws Exception
     * @see org.apache.ivy.plugins.matcher.PatternMatcher
     */
    ModuleRevisionId[] search(String organisation, String moduleName) throws Exception;

    /**
     * Return an array of moduleRevisionId that match with given criteria. Null values are unconstrained (any value is
     * matched).
     * 
     * @param organisation
     *            the organisation name as it appears in the module descriptor
     * @param moduleName
     *            the module name as it appears in the module descriptor
     * @param revision
     *            the revision as it appears in the module descriptor
     * @param branch
     *            the branch as it appears in the module descriptor
     * @param matcher
     *            an Ivy pattern matcher types, see {@link org.apache.ivy.plugins.matcher.PatternMatcher}
     * @param resolver
     *            the name of the Ivy resolver. null matches the default resolver; "*" searches all resolvers.
     * @return an array of matching moduleRevisionId
     * @throws Exception
     */
    ModuleRevisionId[] search(String organisation, String moduleName, String revision, String branch, String matcher,
            String resolver) throws Exception;

    /**
     * Generate an easyantReport for a given moduleDescriptor. Using this report you should have all properties /
     * plugins / targets loaded in your module descriptor
     * 
     * @param moduleDescriptor
     *            a file that represent the module descriptor
     * @param optionalAntModule
     *            the optional build file
     * @param overrideAntModule
     *            an optional override buildfile
     * @return an easyantReport for a given moduleDescriptor
     * @throws Exception
     */
    EasyAntReport generateEasyAntReport(File moduleDescriptor, File optionalAntModule, File overrideAntModule)
            throws Exception;

    /**
     * Generate an easyantReport for a given moduleDescriptor. Using this report you should have all properties /
     * plugins / targets loaded in your module descriptor
     * 
     * @param moduleDescriptor
     *            a file that represent the module descriptor
     * @return an easyantReport for a given moduleDescriptor
     * @throws Exception
     */
    EasyAntReport generateEasyAntReport(File moduleDescriptor) throws Exception;

    /**
     * Return the EasyAnt model containing all data of the module described in given file.
     * 
     * @param moduleDescriptor
     *            a file that represent the module descriptor
     * @return an EasyAnt module descriptor
     * @throws Exception
     */
    EasyAntModuleDescriptor getEasyAntModuleDescriptor(File moduleDescriptor) throws Exception;

    /**
     * Return an array of string representing the fully qualified named matching with given criterias
     * 
     * @param organisation
     *            the organisation name
     * @param moduleName
     *            the module name
     * @return an array of moduleRevisionId
     * @throws Exception
     */
    String[] searchModule(String organisation, String moduleName) throws Exception;

    /**
     * Return the description of a module descriptor Useful for IDE's integration
     * 
     * @param mrid
     *            the module revision id to check
     * @return a string representing the description of the module descriptor
     */
    String getDescription(ModuleRevisionId mrid);

    /**
     * Return the description of a module descriptor Useful for IDE's integration The module revision id parameter
     * should be with the following format organisation#moduleName;revision If no organisation is specified, this method
     * will use the default one, then you could use the shorter form like myPlugin;0.1
     * 
     * @param moduleRevisionId
     *            a string representing a buildtype
     * @return a string representing the description of the module descriptor
     */
    String getPluginDescription(String moduleRevisionId);

    /**
     * Return the description of a module descriptor Useful for IDE's integration The module revision id parameter
     * should be with the following format organisation#moduleName;revision If no organisation is specified, this method
     * will use the default one, then you could use the shorter form like myBuildType;0.1
     * 
     * @param moduleRevisionId
     *            a string representing a buildtype
     * @return a string representing the description of the module descriptor
     */
    String getBuildTypeDescription(String moduleRevisionId);

    /**
     * Specify if plugin service should be used offline
     * @param offlineMode
     */
    void setOfflineMode(boolean offlineMode);
}
