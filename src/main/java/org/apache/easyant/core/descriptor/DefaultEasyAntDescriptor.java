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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;

/**
 * This class is the default implementation of EasyAnt ModuleDescriptor
 */
public class DefaultEasyAntDescriptor implements EasyAntModuleDescriptor {

    public DefaultEasyAntDescriptor() {
        properties = new HashMap<String, PropertyDescriptor>();
        plugins = new ArrayList<PluginDescriptor>();
        buildConfigurations = new ArrayList<String>();
        extensionPointsMappings = new ArrayList<ExtensionPointMappingDescriptor>();
    }

    private ModuleDescriptor ivyModuleDescriptor;
    private PluginDescriptor buildType;
    private Map<String, PropertyDescriptor> properties;
    private List<PluginDescriptor> plugins;
    private List<String> buildConfigurations;

    private List<ExtensionPointMappingDescriptor> extensionPointsMappings;

    private ConfigureProjectDescriptor configureProjectDescriptor;

    public PluginDescriptor getBuildType() {
        return buildType;
    }

    public Map<String, PropertyDescriptor> getProperties() {
        return properties;
    }

    public void setBuildType(PluginDescriptor buildType) {
        this.buildType = buildType;

    }

    public void setProperties(Map<String, PropertyDescriptor> properties) {
        this.properties = properties;

    }

    public void addPlugin(PluginDescriptor pluginDescriptor) {
        if (pluginDescriptor == null) {
            throw new IllegalArgumentException("pluginDescriptor argument can't be null");
        }
        this.plugins.add(pluginDescriptor);
    }

    public List<PluginDescriptor> getPlugins() {
        return plugins;
    }

    public void addBuildConfiguration(String value) {
        this.buildConfigurations.add(value);
    }

    public List<String> getBuildConfigurations() {
        return buildConfigurations;
    }

    public void setIvyModuleDescriptor(ModuleDescriptor ivyModuleDescriptor) {
        this.ivyModuleDescriptor = ivyModuleDescriptor;
    }

    public ModuleDescriptor getIvyModuleDescriptor() {
        return this.ivyModuleDescriptor;
    }

    public String getName() {
        return getIvyModuleDescriptor().getModuleRevisionId().getName();
    }

    public String getDescription() {
        return getIvyModuleDescriptor().getDescription();
    }

    public List<ExtensionPointMappingDescriptor> getExtensionPointsMappings() {
        return extensionPointsMappings;
    }

    public void addExtensionPointMapping(ExtensionPointMappingDescriptor extensionPointMappingDescriptor) {
        this.extensionPointsMappings.add(extensionPointMappingDescriptor);
    }

    public ConfigureProjectDescriptor getConfigureProjectDescriptor() {
        return configureProjectDescriptor;
    }

    public void setConfigureProjectDescriptor(ConfigureProjectDescriptor configureProjectDescriptor) {
        this.configureProjectDescriptor = configureProjectDescriptor;
    }
}
