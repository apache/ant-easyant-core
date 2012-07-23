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
import java.util.Iterator;

import org.apache.easyant.core.BuildConfigurationHelper;
import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.ivy.Ivy;
import org.apache.ivy.core.LogOptions;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DynamicAttribute;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.ImportTask;
import org.apache.tools.ant.types.Path;

/**
 * This task is used to include / import scripts.
 * 
 * The include mechanism is similar to the current import task, excepts that it
 * automatically prefix all targets of the used build module (=ant script). The
 * prefix used by default is the name of the imported project, but it can be
 * overriden when calling "include".
 * 
 * This is useful to use features provided by a build module, while preserving a
 * namespace isolation to avoid names collisions.
 * 
 * While possible, overriding a target defined in a included module is not
 * recommended. To do so, the import mechanism is preferred.
 * 
 * The import mechanism is equivalent to the current import mechanism.
 */
public class Import extends AbstractEasyAntTask implements DynamicAttribute {

    private String module;
    private String organisation;
    private String revision;

    private String mrid;

    private String as;
    private String mode;
    private boolean mandatory;
    private String buildConfigurations;

    private String mainConf = "default";
    private String providedConf = "provided";

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
     * @param organisation
     *            the organisation name
     */
    public void setOrg(String org) {
        this.organisation = org;
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
     * Set th revision of the module to import
     * 
     * @param revision
     *            the revision
     */
    public void setRev(String rev) {
        this.revision = rev;
    }

    /**
     * Get the full mrid of the module to import
     * 
     * @return the mrid to import
     */
    public String getMrid() {
        return mrid;
    }

    /**
     * Set the full mrid of the module to import
     * 
     * @param mrid
     *            the mrid to import
     */
    public void setMrid(String mrid) {
        this.mrid = mrid;
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
     *            a string that represents the import mode (e.g. import /
     *            include)
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
     * Get the configuration that may contain dependency on easyant-core. This
     * configuration is used to check core compliance at resolve time. It should
     * not affect the plugin classpath
     * 
     * @return provided configuration
     */
    public String getProvidedConf() {
        return providedConf;
    }

    /**
     * Set the configuration that may contain dependency on easyant-core. This
     * configuration is used to check core compliance at resolve time. It should
     * not affect the plugin classpath
     * 
     * @return provided configuration
     */
    public void setProvidedConf(String providedConf) {
        this.providedConf = providedConf;
    }

    /**
     * Get resolve log settings
     * 
     * @return a string representing the log strategy
     */
    public String getResolveLog() {
        String downloadLog = getProject().getProperty(
                EasyAntMagicNames.MODULE_DOWNLOAD_LOG);
        return downloadLog != null ? downloadLog : LogOptions.LOG_DOWNLOAD_ONLY;
    }

    public void execute() throws BuildException {
    	ModuleRevisionId moduleRevisionId = null;
        if (mrid != null) {
        	moduleRevisionId = ModuleRevisionId.parse(mrid);
        } else if (organisation != null && module != null && revision != null) {
        	moduleRevisionId = ModuleRevisionId.newInstance(organisation,
        								module, revision);
        } else {
            throw new BuildException(
            		"The module to import is not properly specified, you must set the mrid attribute or set organisation / module / revision attributes");
        }
        String moduleName = moduleRevisionId.toString();
        if (!BuildConfigurationHelper.isBuildConfigurationActive(
                getBuildConfigurations(), getProject(), "module" + getModule())) {
            log("no matching build configuration for module " + moduleName
                    + " this module will be skipped ", Project.MSG_DEBUG);
            return;
        }
        
        //if no as attribute was given use module name
        if (as==null && "include".equals(getMode())) {
            //when using mrid style
            if (mrid!=null) {
                as = moduleRevisionId.getName();
            //when using exploded style
            } else if (getModule() != null) {
                as=getModule();
            }
        }
        
        // check if a property skip.${module} or skip.${as} is set
        boolean toBeSkipped = getProject().getProperty("skip." + moduleName) != null
        						|| getProject().getProperty("skip." + getAs()) != null;

        if (mandatory && toBeSkipped) {
            log("Impossible to skip a mandatory module : " + moduleName,
                    Project.MSG_WARN);
        }

        // a module can be skipped *only* if it is not mandatory
        if (!mandatory && toBeSkipped) {
            log(moduleName + " skipped !");
        } else {
            try {
            	Ivy ivy = getEasyAntIvyInstance();
                ResolveOptions resolveOptions = new ResolveOptions();
                // TODO: This should be configurable

                resolveOptions.setLog(getResolveLog());

                Boolean offline = Boolean.valueOf(getProject().getProperty(EasyAntMagicNames.EASYANT_OFFLINE));
                resolveOptions.setUseCacheOnly(offline);

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
                ResolveReport report = ivy.getResolveEngine().resolve(
                								moduleRevisionId, resolveOptions, true);
                // Check dependency on core
                checkCoreCompliance(report, providedConf);

                Path path = new Path(getProject());
                getProject().addReference(
                        moduleRevisionId.getModuleId().toString()
                                + ".classpath", path);
                File antFile = null;
                for (int j = 0; j < report.getConfigurationReport(mainConf)
                        .getAllArtifactsReports().length; j++) {
                    ArtifactDownloadReport artifact = report
                            .getConfigurationReport(mainConf)
                            .getAllArtifactsReports()[j];

					if ("ant".equals(artifact.getType())
							&& "ant".equals(artifact.getExt())) {
						antFile = artifact.getLocalFile();
					} else if ("jar".equals(artifact.getType())) {
                        path.createPathElement().setLocation(
                                artifact.getLocalFile());
                    } else {
                        StringBuilder sb = new StringBuilder();
                        sb.append(moduleRevisionId.getOrganisation());
                        sb.append("#");
                        sb.append(moduleRevisionId.getName());
                        sb.append(".");
                        if (!moduleRevisionId.getName().equals(
                                artifact.getName())) {
                            sb.append(artifact.getName());
                            sb.append(".");
                        }
                        sb.append(artifact.getExt());
                        sb.append(".file");
                        getProject().setNewProperty(sb.toString(),
                                artifact.getLocalFile().getAbsolutePath());
                    }
                }
                if (antFile != null && antFile.exists()) {
                    ImportTask importTask = new ImportTask();
                    importTask.setProject(getProject());
                    importTask.setTaskName(getTaskName());
                    importTask.setOwningTarget(getOwningTarget());
                    importTask.setLocation(getLocation());
                    importTask.setFile(antFile.getAbsolutePath());
                    if (as != null) {
                        importTask.setAs(as);
                        importTask.setPrefixSeparator("");
                    }
                    if (mode != null && "include".equals(mode)) {
                        importTask.setTaskType(getMode());
                    }
                    importTask.execute();
                }

            } catch (Exception e) {
                throw new BuildException(e);
            }

        }
    }

    /**
     * Check dependency on easyant core with a given configuration. If
     * dependency is found we'll check compliance with current core version. It
     * uses {@link CoreRevisionCheckerTask} internally.
     * 
     * @param report
     *            a {@link ResolveReport}
     * @param confToCheck
     *            configuration to check
     */
    private void checkCoreCompliance(ResolveReport report, String confToCheck) {
        if (report.getConfigurationReport(confToCheck) != null) {
            log("checking module's provided dependencies ...",
                    Project.MSG_DEBUG);
            for (Iterator iterator = report.getConfigurationReport(confToCheck)
                    .getModuleRevisionIds().iterator(); iterator.hasNext();) {
                ModuleRevisionId currentmrid = (ModuleRevisionId) iterator
                        .next();
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

    /**
     * Can we skip the load of this module?
     * 
     * @param mandatory
     *            true if the module can't be skipped
     */
    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;

    }

    public void setDynamicAttribute(String attributeName, String value)
            throws BuildException {
        PropertyTask property = new PropertyTask();
        property.setName(attributeName);
        property.setValue(value);
        initTask(property).execute();
    }
}
