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
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.tools.ant.BuildException;

/**
 * Provide go offline feature. It retrieves all easyant modules used by the project (plugins, buildtype) and all project
 * dependencies. You can then define target resolver name where all artifacts will get installed. You can combine
 * GoOffline task with ConfigureBuildScopeRepository, to have everything embedded in your project directory
 * 
 */
public class GoOffline extends AbstractEasyAntTask {

    private String projectResolverName;
    private String easyantResolverName;
    private File moduleIvy;

    @Override
    public void execute() throws BuildException {
        if (moduleIvy == null && getProject().getProperty(EasyAntMagicNames.EASYANT_FILE) != null) {
            moduleIvy = new File(getProject().getProperty(EasyAntMagicNames.EASYANT_FILE));
        }
        if (moduleIvy == null || !moduleIvy.exists()) {
            throw new BuildException("Couldn't locate module ivy did you specified moduleivy attribute ?");
        }

        if (projectResolverName == null) {
            throw new BuildException("projectResolverName is mandatory !");
        }

        if (easyantResolverName == null) {
            throw new BuildException("easyantResolverName is mandatory !");
        }

        PluginService pluginService = getProject().getReference(
                EasyAntMagicNames.PLUGIN_SERVICE_INSTANCE);
        EasyAntReport easyAntReport;
        try {
            easyAntReport = pluginService.generateEasyAntReport(moduleIvy);
            installBuildTypeAndPlugins(easyAntReport);
            installProjectDependencies(easyAntReport);
        } catch (Exception e) {
            throw new BuildException("Can't retrieve project report", e);
        }

    }

    /***
     * Install project dependencies
     * 
     * @param easyAntReport
     *            {@link EasyAntReport} where project dependencies are described
     */
    private void installProjectDependencies(EasyAntReport easyAntReport) {
        for (DependencyDescriptor dependencyDescriptor : easyAntReport.getModuleDescriptor().getDependencies()) {
            install(dependencyDescriptor.getDependencyRevisionId(), getProjectResolverName(),
                    IvyInstanceHelper.getProjectIvyInstanceName(getProject()));
        }
    }

    /**
     * Install easyant plugins and buildtypes used by a project
     * 
     * @param easyAntReport
     *            {@link EasyAntReport} where plugin / buildtypes is described
     */
    private void installBuildTypeAndPlugins(EasyAntReport easyAntReport) {
        for (ImportedModuleReport importedModule : easyAntReport.getImportedModuleReports()) {
            install(ModuleRevisionId.parse(importedModule.getModuleMrid()), getEasyantResolverName(),
                    EasyAntMagicNames.EASYANT_IVY_INSTANCE);
            if (importedModule.getEasyantReport() != null) {
                // install module dependencies
                ResolveReport resolveReport = importedModule.getEasyantReport().getResolveReport();
                for (Object o : resolveReport.getDependencies()) {
                    IvyNode dependency = (IvyNode) o;
                    install(dependency.getResolvedId(), getEasyantResolverName(),
                            EasyAntMagicNames.EASYANT_IVY_INSTANCE);
                }
                // install plugins declared inside current module

                installBuildTypeAndPlugins(importedModule.getEasyantReport());
            }

        }
    }

    private void install(ModuleRevisionId moduleRevisionId, String targetResolver, String ivyInstanceRef) {
        IvyInstall install = new IvyInstall();
        install.setSettingsRef(IvyInstanceHelper.buildIvyReference(getProject(), ivyInstanceRef));

        // locate source resolver
        IvyAntSettings ivyAntSettings = IvyInstanceHelper.getIvyAntSettings(getProject(), ivyInstanceRef);
        String from = ivyAntSettings.getConfiguredIvyInstance(this).getSettings().getResolverName(moduleRevisionId);
        install.setFrom(from);

        install.setTo(targetResolver);
        install.setOrganisation(moduleRevisionId.getOrganisation());
        install.setModule(moduleRevisionId.getName());
        install.setRevision(moduleRevisionId.getRevision());
        install.setOverwrite(true);
        install.setHaltonfailure(false);
        install.setTransitive(true);
        initTask(install).execute();

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
