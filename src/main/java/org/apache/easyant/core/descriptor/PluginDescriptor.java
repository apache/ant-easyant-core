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
import java.util.List;

import org.apache.easyant.core.ivy.InheritableScope;
import org.apache.ivy.ant.IvyConflict;
import org.apache.ivy.ant.IvyDependency;
import org.apache.ivy.ant.IvyExclude;
import org.apache.ivy.core.module.id.ModuleRevisionId;

/**
 * This class is a simple POJO used to store informations on a plugin.
 * 
 */
public class PluginDescriptor implements AdvancedInheritableItem {
    private String organisation;
    private String module;
    private String revision;

    private String mrid;

    private String mode;
    private String as;

    private boolean mandatory;
    private String buildConfigurations;
    private InheritableScope inheritScope = InheritableScope.BOTH;

    private final ModuleRevisionId sourceModule;
    private boolean inheritable = true;

    private List<IvyDependency> dependencies = new ArrayList<IvyDependency>();

    private List<IvyExclude> excludes = new ArrayList<IvyExclude>();

    private List<IvyConflict> conflicts = new ArrayList<IvyConflict>();

    /**
     * Default constructor
     */
    public PluginDescriptor() {
        sourceModule = null;
    }

    /**
     * Constructor specifying the source module which was defining the plugin
     * 
     * @param sourceModule
     *            a source module
     */
    public PluginDescriptor(ModuleRevisionId sourceModule) {
        this.sourceModule = sourceModule;
    }

    /**
     * Get the plugin name
     * 
     * @return the plugin name
     */
    public String getModule() {
        return module;
    }

    /**
     * Set the plugin name
     * 
     * @param module
     *            the plugin name to set
     */
    public void setModule(String module) {
        this.module = module;
    }

    /**
     * Get the organisation of the module to import
     * 
     * @return the organisation name
     */
    public String getOrganisation() {
        return organisation;
    }

    /**
     * Set the organisation of the module to import
     * 
     * @param organisation
     *            the organisation name
     */
    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }

    /**
     * Get the revision of the module to import
     * 
     * @return the revision
     */
    public String getRevision() {
        return revision;
    }

    /**
     * Set th revision of the module to import
     * 
     * @param revision
     *            the revision
     */
    public void setRevision(String revision) {
        this.revision = revision;
    }

    /**
     * Get the full mrid of the module to import
     * 
     * @return the mrid to import
     */
    public String getMrid() {
        return mrid != null ? mrid : ModuleRevisionId.newInstance(organisation, module, revision).toString();
    }

    /**
     * Set the full mrid of the module to import
     * 
     * @param mrid
     *            the mrdi to import
     */
    public void setMrid(String mrid) {
        this.mrid = mrid;
    }

    public ModuleRevisionId getModuleRevisionId() {
        return ModuleRevisionId.parse(getMrid());
    }

    /**
     * Get the import mode of a plugin
     * 
     * @return a string that represent the import mode (import / include)
     */
    public String getMode() {
        return mode;
    }

    /**
     * Set the import mode of a plugin
     * 
     * @param mode
     *            a string that represent the import mode (import / include)
     */
    public void setMode(String mode) {
        this.mode = mode;
    }

    /**
     * Get the alias name
     * 
     * @return the alias name
     */
    public String getAs() {
        return as;
    }

    /**
     * Set the alias name
     * 
     * @param as
     *            the alias name
     */
    public void setAs(String as) {
        this.as = as;
    }

    /**
     * is this plugin mandatory?
     * 
     * @return true if the plugin is mandatory, false if the plugin can be skipped
     */
    public boolean isMandatory() {
        return mandatory;
    }

    /**
     * is this plugin mandatory?
     * 
     * @param mandatory
     *            true if the plugin is mandatory, false if the plugin can be skipped
     */
    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    /**
     * set a build configuration name bound to this plugin
     * 
     * @param buildConfigurationName
     *            a build configuration name
     */
    public void setBuildConfigurations(String buildConfigurationName) {
        this.buildConfigurations = buildConfigurationName;
    }

    /**
     * Return a build configuration name bound to this plugin
     * 
     * @return a build configuration name
     */
    public String getBuildConfigurations() {
        return buildConfigurations;
    }

    /**
     * {@inheritDoc}
     */
    public ModuleRevisionId getSourceModule() {
        return sourceModule;
    }

    public InheritableScope getInheritScope() {
        return inheritScope;
    }

    public void setInheritScope(InheritableScope inheritScope) {
        this.inheritScope = inheritScope;
    }

    public boolean isInheritable() {
        return inheritable;
    }

    public void setInheritable(boolean isIneritable) {
        this.inheritable = isIneritable;

    }

    public List<IvyDependency> getDependencies() {
        return dependencies;
    }

    public List<IvyExclude> getExcludes() {
        return excludes;
    }

    public List<IvyConflict> getConflicts() {
        return conflicts;
    }

    public boolean addDependency(IvyDependency dependency) {
        return dependencies.add(dependency);
    }

    public boolean addExcludes(IvyExclude exclude) {
        return excludes.add(exclude);
    }

    public boolean addConflict(IvyConflict conflict) {
        return conflicts.add(conflict);
    }

}
