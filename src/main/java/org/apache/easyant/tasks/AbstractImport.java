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
import org.apache.ivy.core.LogOptions;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.ImportTask;
import org.apache.tools.ant.types.Path;

/**
 * This abstract task is used to include / import modules.
 * 
 * The include mechanism is similar to the current import task, excepts that it automatically prefix all targets of the
 * used build module (=ant script). The prefix used by default is the name of the imported project, but it can be
 * overriden when calling "include".
 * 
 * Include is useful to use features provided by a build module, while preserving a namespace isolation to avoid names
 * collisions.
 * 
 * While possible, overriding a target defined in a included module is not recommended. To do so, the import mechanism
 * is preferred.
 * 
 */
public abstract class AbstractImport extends AbstractEasyAntTask {

    private String as;
    private String mode;
    private boolean mandatory;
    private String buildConfigurations;
    private String mainConf = "default";
    private String providedConf = "provided";
    private boolean changing = false;

    public AbstractImport() {
        super();
    }

    /**
     * Import a module
     * 
     * @param moduleRevisionId
     *            {@link ModuleRevisionId} of main artifact
     * @param report
     *            a resolved report of the module to import
     */
    protected void importModule(ModuleRevisionId moduleRevisionId, ResolveReport report) {
        // Check dependency on core
        checkCoreCompliance(report, providedConf);

        Path path = createModulePath(moduleRevisionId.getModuleId());
        File antFile = null;
        for (int j = 0; j < report.getConfigurationReport(mainConf).getAllArtifactsReports().length; j++) {
            ArtifactDownloadReport artifact = report.getConfigurationReport(mainConf).getAllArtifactsReports()[j];
            if ("ant".equals(artifact.getType())) {
                antFile = artifact.getLocalFile();
            } else if (shouldBeAddedToClasspath(artifact)) {
                path.createPathElement().setLocation(artifact.getLocalFile());
            } else {
                handleOtherResourceFile(moduleRevisionId, artifact.getName(), artifact.getExt(),
                        artifact.getLocalFile());
            }
        }
        // effective import should be executed AFTER any other resource files has been handled
        if (antFile != null && antFile.exists()) {
            doEffectiveImport(antFile);
        }
    }

    public boolean shouldBeAddedToClasspath(ArtifactDownloadReport artifact) {
        String[] types;
        if (getProject().getProperty(EasyAntMagicNames.IMPORT_CLASSPATH_TYPES) != null) {
            types = getProject().getProperty(EasyAntMagicNames.IMPORT_CLASSPATH_TYPES).split(",");
        } else {
            types = new String[] { "jar", "bundle" };
        }
        for (String type : types) {
            if (artifact.getType().equals(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Do effective import of a given ant file
     * 
     * @param antFile
     *            a given ant file
     */
    protected void doEffectiveImport(File antFile) {
        ImportTask importTask = new ImportTask();
        importTask.setFile(antFile.getAbsolutePath());
        if (as != null) {
            importTask.setAs(as);
            importTask.setPrefixSeparator("");
        }
        if (mode != null && "include".equals(mode)) {
            importTask.setTaskType(getMode());
        }
        initTask(importTask).execute();
    }

    /**
     * <p>
     * Register all location of other resource file in properties.
     * </p>
     * <p>
     * Properties are composed with the following syntax : [organisation].[module].[artifact].[type].file
     * </p>
     * <p>
     * The '.artifact' is optional when module name and artifact name are the same. [organisation].[module].[type].file
     * </p>
     * 
     * @param moduleRevisionId
     *            a {@link ModuleRevisionId} of the main artifact
     * @param artifactName
     *            artifact name
     * @param artifactExtension
     *            artifact extension name
     * @param localResourceFile
     */
    protected void handleOtherResourceFile(ModuleRevisionId moduleRevisionId, String artifactName,
            String artifactExtension, File localResourceFile) {
        if (localResourceFile == null) {
            getProject().log(
                    "Can't find artifact " + artifactName + " ext" + artifactExtension
                            + ". See resolution report to have details", Project.MSG_WARN);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(moduleRevisionId.getOrganisation());
            sb.append("#");
            sb.append(moduleRevisionId.getName());
            sb.append(".");
            if (!moduleRevisionId.getName().equals(artifactName)) {
                sb.append(artifactName);
                sb.append(".");
            }
            sb.append(artifactExtension);
            sb.append(".file");

            getProject().log(
                    "registering location of artifact " + artifactName + " ext" + artifactExtension + " on "
                            + sb.toString(), Project.MSG_DEBUG);
            getProject().setNewProperty(sb.toString(), localResourceFile.getAbsolutePath());
        }
    }

    /**
     * creates a classpath specific for each module, this classpath will contains all the required dependency .jars. The
     * classpath is named [organisation]#[module].classpath
     * 
     * @param moduleId
     * @return
     */
    protected Path createModulePath(ModuleId moduleId) {
        Path path = new Path(getProject());
        getProject().addReference(moduleId.toString() + ".classpath", path);
        return path;
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
     * Check dependency on easyant core with a given configuration. If dependency is found we'll check compliance with
     * current core version. It uses {@link CoreRevisionCheckerTask} internally.
     * 
     * @param report
     *            a {@link ResolveReport}
     * @param confToCheck
     *            configuration to check
     */
    protected void checkCoreCompliance(ResolveReport report, String confToCheck) {
        if (report.getConfigurationReport(confToCheck) != null) {
            log("checking module's provided dependencies ...", Project.MSG_DEBUG);
            for (Object o : report.getConfigurationReport(confToCheck).getModuleRevisionIds()) {
                ModuleRevisionId currentmrid = (ModuleRevisionId) o;
                log("checking " + currentmrid.toString(), Project.MSG_DEBUG);
                if (currentmrid.getOrganisation().equals("org.apache.easyant")
                        && currentmrid.getName().equals("easyant-core")) {
                    CoreRevisionCheckerTask checker = new CoreRevisionCheckerTask();
                    checker.setRequiredRevision(currentmrid.getRevision());
                    initTask(checker).execute();
                }
            }
        }
    }

    public void setDynamicAttribute(String attributeName, String value) throws BuildException {
        PropertyTask property = new PropertyTask();
        property.setName(attributeName);
        property.setValue(value);
        initTask(property).execute();
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

    /**
     * Get the alias name
     * 
     * @return a string that represents the alias name
     */
    public String getAs() {
        return as;
    }

    /**
     * Set the alias name
     * 
     * @param as
     *            a string that represents the alias name
     */
    public void setAs(String as) {
        this.as = as;
    }

    /**
     * Get the import mode
     * 
     * @return a string that represents the import mode (e.g. import / include)
     */
    public String getMode() {
        return mode;
    }

    /**
     * Set the import mode
     * 
     * @param mode
     *            a string that represents the import mode (e.g. import / include)
     */
    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getBuildConfigurations() {
        return buildConfigurations;
    }

    public void setBuildConfigurations(String conf) {
        this.buildConfigurations = conf;
    }

    public void setConf(String conf) {
        this.buildConfigurations = conf;
    }

    /**
     * Get the main configuration where plugin are resolved
     * 
     * @return a string representing the main configuration
     */
    public String getMainConf() {
        return mainConf;
    }

    /**
     * Set the main configuration where plugin are resolved
     * 
     * @param mainConf
     *            a string representing the main configuration
     */
    public void setMainConf(String mainConf) {
        this.mainConf = mainConf;
    }

    /**
     * Get the configuration that may contain dependency on easyant-core. This configuration is used to check core
     * compliance at resolve time. It should not affect the plugin classpath
     * 
     * @return provided configuration
     */
    public String getProvidedConf() {
        return providedConf;
    }

    /**
     * Set the configuration that may contain dependency on easyant-core. This configuration is used to check core
     * compliance at resolve time. It should not affect the plugin classpath
     * 
     * @return provided configuration
     */
    public void setProvidedConf(String providedConf) {
        this.providedConf = providedConf;
    }

    /**
     * Can we skip the load of this module?
     * 
     * return true if the module can't be skipped
     */
    public boolean isMandatory() {
        return mandatory;
    }

    /**
     * Can we skip the load of this module?
     * 
     * @param mandatory
     *            true if the module can't be skipped
     */
    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;

    }

    public boolean isChanging() {
        return changing;
    }

    public void setChanging(boolean changing) {
        this.changing = changing;
    }

}