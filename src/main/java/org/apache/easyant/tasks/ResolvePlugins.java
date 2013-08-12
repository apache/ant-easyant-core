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

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.easyant.core.EasyAntEngine;
import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.ivy.ant.EasyAntPluginBridge;
import org.apache.ivy.ant.IvyConflict;
import org.apache.ivy.ant.IvyDependency;
import org.apache.ivy.ant.IvyExclude;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.LogOptions;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.tools.ant.BuildException;

/**
 * Resolve plugins given as child elements and store resolve report in a ant reference. Example :
 * 
 * <pre>
 * &lt;resolve-plugins&gt;
 * &lt;dependency org="org.apache.easyant.plugins" name="run-java" revision="0.9"/&gt;
 * &lt;/resolve-plugins&gt;
 * </pre>
 */
public class ResolvePlugins extends AbstractEasyAntTask {

    private List<IvyDependency> dependencies = new ArrayList<IvyDependency>();

    private List<IvyExclude> excludes = new ArrayList<IvyExclude>();

    private List<IvyConflict> conflicts = new ArrayList<IvyConflict>();
    private String mainConf = "default";
    private boolean changing = false;

    public void execute() {
        try {
            ModuleRevisionId builderMRID = new ModuleRevisionId(new ModuleId(getProject().getName(), getProject()
                    .getName() + "-builder"), EasyAntEngine.getEasyAntVersion());
            DefaultModuleDescriptor md = DefaultModuleDescriptor.newDefaultInstance(builderMRID);

            IvySettings settings = getEasyAntIvyInstance().getSettings();
            IvyContext.pushNewContext();
            IvyContext.getContext().setIvy(getEasyAntIvyInstance());
            // FIXME: If additionnal dependency are loaded or a superior version of a dependency is defined it works
            // as expected
            // But it doesn't work if you specify a revision lower to original one
            md = EasyAntPluginBridge.computeModuleDescriptor(md, settings, dependencies, conflicts, excludes);
            ResolveReport report = getEasyAntIvyInstance().getResolveEngine().resolve(md, configureResolveOptions());
            getProject().addReference(EasyAntMagicNames.IMPORTED_MODULES_RESOLVE_REPORT_REF, report);

            IvyContext.popContext();
        } catch (ParseException e) {
            throw new BuildException("Can't parse module descriptor", e);
        } catch (IOException e) {
            throw new BuildException("Can't parse module descriptor", e);
        }
    }

    /**
     * Configures resolve options
     * 
     * @return configured resolveOptions
     */
    protected ResolveOptions configureResolveOptions() {
        // Here we do not specify explicit configuration to resolve as
        // we want to check multiple configurations.
        // If we make specify explicitly configurations to resolve, the
        // resolution could through exceptions when configuration does
        // not exist in resolved modules.
        // resolveOptions.setConfs(new String[] { mainConf,providedConf });

        // By default we consider that main conf is default.
        // To verify core compliance we can have a dependency on
        // easyant-core in a specific configuration.
        // By default this configuration is provided.

        // An error can be thrown if module contains non-public configurations.
        ResolveOptions resolveOptions = new ResolveOptions();
        resolveOptions.setLog(getResolveLog());

        Boolean offline = Boolean.valueOf(getProject().getProperty(EasyAntMagicNames.EASYANT_OFFLINE));
        resolveOptions.setUseCacheOnly(offline);
        return resolveOptions;
    }

    /**
     * Get resolve log settings
     * 
     * @return a string representing the log strategy
     */
    protected String getResolveLog() {
        String downloadLog = getProject().getProperty(EasyAntMagicNames.MODULE_DOWNLOAD_LOG);
        return downloadLog != null ? downloadLog : LogOptions.LOG_DOWNLOAD_ONLY;
    }

    public IvyDependency createDependency() {
        IvyDependency dep = new IvyDependency();
        dependencies.add(dep);
        return dep;
    }

    public IvyExclude createExclude() {
        IvyExclude ex = new IvyExclude();
        excludes.add(ex);
        return ex;
    }

    public IvyConflict createConflict() {
        IvyConflict c = new IvyConflict();
        conflicts.add(c);
        return c;
    }

    public List<IvyDependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<IvyDependency> dependencies) {
        this.dependencies = dependencies;
    }

    public List<IvyExclude> getExcludes() {
        return excludes;
    }

    public void setExcludes(List<IvyExclude> excludes) {
        this.excludes = excludes;
    }

    public List<IvyConflict> getConflicts() {
        return conflicts;
    }

    public void setConflicts(List<IvyConflict> conflicts) {
        this.conflicts = conflicts;
    }

    public String getMainConf() {
        return mainConf;
    }

    public void setMainConf(String mainConf) {
        this.mainConf = mainConf;
    }

    public boolean isChanging() {
        return changing;
    }

    public void setChanging(boolean changing) {
        this.changing = changing;
    }

}
