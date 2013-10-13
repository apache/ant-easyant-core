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
package org.apache.easyant.core.descriptor;

import java.util.List;
import java.util.Map;

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;

/**
 * This interface is used to access to all metadata provided in &lt;easyant&gt; tag in a module descriptor
 */
public interface EasyAntModuleDescriptor {

    /**
     * Get the corresponding ivy module descriptor
     * 
     * @return the ivy module Descriptor
     */
    ModuleDescriptor getIvyModuleDescriptor();

    /**
     * Get all the properties loaded inside the &lt;easyant&gt; tag.
     * 
     * @return a map of properties
     */
    Map<String, PropertyDescriptor> getProperties();

    /**
     * Get the build type defined in the module descriptor
     * 
     * @return a {@link PluginDescriptor} of build type module
     */
    PluginDescriptor getBuildType();

    /**
     * Set the build type that should be used
     * 
     * @param buildType
     *            a {@link PluginDescriptor} of build type module
     */
    void setBuildType(PluginDescriptor buildType);

    /**
     * Get all plugins defined in the module descriptor
     * 
     * @return a list of module revision id that references plugins
     */
    List<PluginDescriptor> getPlugins();

    /**
     * Add a plugin to the easyant context
     * 
     * @param pluginDescriptor
     *            a plugindescriptor
     */
    void addPlugin(PluginDescriptor pluginDescriptor);

    /**
     * Add a build configuration to the easyant context
     * 
     * @param value
     *            a string that represents a build configuration name
     */
    void addBuildConfiguration(String value);

    /**
     * Return a list of build configuration defined in this context
     * 
     * @return a list of string that contains build configuration names
     */
    List<String> getBuildConfigurations();

    /**
     * Return the module name
     * 
     * @return a string representing the module name
     */
    String getName();

    /**
     * Return the module description
     * 
     * @return a string representing the module description
     */
    String getDescription();

    /**
     * Get all registered extension points mappings
     * 
     * @return a list of {@link ExtensionPointMappingDescriptor}
     */
    List<ExtensionPointMappingDescriptor> getExtensionPointsMappings();

    /**
     * Add a {@link ExtensionPointMappingDescriptor} to the list of all registered extension points mappings
     * 
     * @param extensionPointMappingDescriptor
     *            a {@link ExtensionPointMappingDescriptor}
     */
    void addExtensionPointMapping(ExtensionPointMappingDescriptor extensionPointMappingDescriptor);

    /**
     * Get configure project descriptor (defining basedir/defaulttarget etc...)
     * 
     * @return a {@link ConfigureProjectDescriptor}
     */
    ConfigureProjectDescriptor getConfigureProjectDescriptor();

    /**
     * Set configure project descriptor (defining basedir/defaulttarget etc...)
     * 
     * @param configureProjectDescriptor
     *            a {@link ConfigureProjectDescriptor}
     */
    void setConfigureProjectDescriptor(ConfigureProjectDescriptor configureProjectDescriptor);
}