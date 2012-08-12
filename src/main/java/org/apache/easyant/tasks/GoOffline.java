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

import java.io.File;

import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.easyant.core.ivy.IvyInstanceHelper;
import org.apache.easyant.core.report.EasyAntReport;
import org.apache.easyant.core.report.ImportedModuleReport;
import org.apache.easyant.core.services.PluginService;
import org.apache.ivy.ant.IvyAntSettings;
import org.apache.ivy.ant.IvyInstall;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.tools.ant.BuildException;

public class GoOffline extends AbstractEasyAntTask {

    private String projectResolverName;
    private String easyantResolverName;
    private File moduleIvy;

    @Override
    public void execute() throws BuildException {
        if (moduleIvy == null) {
            moduleIvy = new File(getProject().getProperty(EasyAntMagicNames.EASYANT_FILE));
        }

        // 1 ask plugin service about current project
        PluginService pluginService = (PluginService) getProject().getReference(
                EasyAntMagicNames.PLUGIN_SERVICE_INSTANCE);
        EasyAntReport easyAntReport =null;
        try {
            easyAntReport = pluginService.generateEasyAntReport(moduleIvy);
            installBuildTypeAndPlugins(easyAntReport);
            installProjectDependencies(easyAntReport);
        } catch (Exception e) {
            throw new BuildException("Can't retrieve project report", e);
        }

    }

    private void installProjectDependencies(EasyAntReport easyAntReport) {
        for (DependencyDescriptor dependencyDescriptor : easyAntReport.getModuleDescriptor().getDependencies()) {

            IvyInstall install = new IvyInstall();
            install.setSettingsRef(IvyInstanceHelper.buildProjectIvyReference(getProject()));
            ModuleRevisionId mrid = dependencyDescriptor.getDependencyRevisionId();
            // locate resolver
            IvyAntSettings ivyAntSettings = IvyInstanceHelper.getProjectIvyAntSettings(getProject());
            String from = ivyAntSettings.getConfiguredIvyInstance(this).getSettings().getResolverName(mrid);
            install.setFrom(from);
            install.setTo(getProjectResolverName());
            install.setOrganisation(mrid.getOrganisation());
            install.setModule(mrid.getName());
            install.setRevision(mrid.getRevision());
            install.setOverwrite(true);
            install.setHaltonfailure(false);
            initTask(install).execute();
        }
    }

    private void installBuildTypeAndPlugins(EasyAntReport easyAntReport) {
        for (ImportedModuleReport importedModule : easyAntReport.getImportedModuleReports()) {
            // make this recursive
            IvyInstall install = new IvyInstall();
            install.setSettingsRef(IvyInstanceHelper.buildEasyAntIvyReference(getProject()));
            ModuleRevisionId mrid = ModuleRevisionId.parse(importedModule.getModuleMrid());
            // locate resolver
            IvyAntSettings ivyAntSettings = IvyInstanceHelper.getEasyAntIvyAntSettings(getProject());
            String from = ivyAntSettings.getConfiguredIvyInstance(this).getSettings().getResolverName(mrid);
            install.setFrom(from);

            install.setTo(getEasyantResolverName());
            install.setOrganisation(mrid.getOrganisation());
            install.setModule(mrid.getName());
            install.setRevision(mrid.getRevision());
            install.setOverwrite(true);
            install.setHaltonfailure(false);
            initTask(install).execute();
            // install plugins declared inside current module
            if (importedModule.getEasyantReport() != null) {
                installBuildTypeAndPlugins(importedModule.getEasyantReport());
            }

        }
    }

    public String getProjectResolverName() {
        return projectResolverName;
    }

    public void setProjectResolverName(String projectResolverName) {
        this.projectResolverName = projectResolverName;
    }

    public String getEasyantResolverName() {
        return easyantResolverName;
    }

    public void setEasyantResolverName(String easyantResolverName) {
        this.easyantResolverName = easyantResolverName;
    }

    public File getModuleIvy() {
        return moduleIvy;
    }

    public void setModuleIvy(File moduleIvy) {
        this.moduleIvy = moduleIvy;
    }

}
