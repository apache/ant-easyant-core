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

import org.apache.easyant.core.BuildConfigurationHelper;
import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ConfigurationResolveReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DynamicAttribute;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;

/**
 * Import implementation relying on pre resolved plugins by {@link ResolvePlugins} Organisation / module are used to
 * pick up the right plugin and theirs dependencies from ResolveReport. Example :
 * 
 * <pre>
 * &lt;import-deferred organisation="org.apache.easyant.plugins" module="compile-java" /&gt;
 * </pre>
 * 
 * exploded style with shortcut attributes
 * 
 * <pre>
 * &lt;import-deferred org="org.apache.easyant.plugins" module="compile-java"/&gt;
 * </pre>
 */
public class ImportDeferred extends AbstractImport implements DynamicAttribute {

    private String module;
    private String organisation;

    public void execute() {
        ModuleId moduleId;
        if (organisation != null && module != null) {
            moduleId = ModuleId.newInstance(organisation, module);
        } else {
            throw new BuildException(
                    "The module to import is not properly specified, you must set set organisation / module attributes");
        }
        String moduleName = moduleId.toString();
        if (!BuildConfigurationHelper.isBuildConfigurationActive(getBuildConfigurations(), getProject(), "module"
                + getModule())) {
            log("no matching build configuration for module " + moduleName + " this module will be skipped ",
                    Project.MSG_DEBUG);
            return;
        }

        // if no as attribute was given use module name
        if (getAs() == null && "include".equals(getMode())) {
            if (getModule() != null) {
                setAs(getModule());
            }
        }

        // check if a property skip.${module} or skip.${as} is set
        boolean toBeSkipped = getProject().getProperty("skip." + moduleName) != null
                || getProject().getProperty("skip." + getAs()) != null;

        if (isMandatory() && toBeSkipped) {
            log("Impossible to skip a mandatory module : " + moduleName, Project.MSG_WARN);
        }
        // a module can be skipped *only* if it is not mandatory
        if (!isMandatory() && toBeSkipped) {
            log(moduleName + " skipped !");
        } else {

            ResolveReport importedModuleResolveReport = getProject().getReference(
                    EasyAntMagicNames.IMPORTED_MODULES_RESOLVE_REPORT_REF);
            if (importedModuleResolveReport != null) {
                importModule(moduleId, importedModuleResolveReport);
            }
        }
    }

    protected void importModule(ModuleId moduleId, ResolveReport report) {
        // Check dependency on core
        checkCoreCompliance(report, getProvidedConf());
        ConfigurationResolveReport confReport = report.getConfigurationReport(getMainConf());
        for (Object o : confReport.getModuleRevisionIds()) {
            ModuleRevisionId mrid = (ModuleRevisionId) o;
            if (mrid.getModuleId().equals(moduleId)) {
                ArtifactDownloadReport[] artifactsReports = confReport.getDownloadReports(mrid);

                // Fill classpath with whole set of dependencies
                Path path = createModulePath(moduleId);
                for (int i = 0; i < confReport.getAllArtifactsReports().length; i++) {
                    ArtifactDownloadReport artifactReport = confReport.getAllArtifactsReports()[i];
                    if (shouldBeAddedToClasspath(artifactReport)) {
                        path.createPathElement().setLocation(artifactReport.getLocalFile());
                    }
                }

                File antFile = null;
                for (ArtifactDownloadReport artifact : artifactsReports) {
                    if ("ant".equals(artifact.getType())) {
                        antFile = artifact.getLocalFile();
                    } else {
                        handleOtherResourceFile(artifact.getArtifact().getModuleRevisionId(), artifact.getName(),
                                artifact.getExt(), artifact.getLocalFile());
                    }
                }

                // effective import should be executed AFTER any other resource files has been handled
                if (antFile != null && antFile.exists()) {
                    doEffectiveImport(antFile);
                }

            }
        }
        // loop on dependencies
    }

    /**
     * Get the module name to import
     * 
     * @return the module name
     */
    public String getModule() {
        return module;
    }

    /**
     * Set the module name to import
     * 
     * @param module
     *            the module name
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
     * Set the organisation of the module to import
     *
     */
    public void setOrg(String org) {
        this.organisation = org;
    }

}
