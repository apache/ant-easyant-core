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
import java.io.IOException;
import java.text.ParseException;
import java.util.Iterator;

import org.apache.easyant.core.BuildConfigurationHelper;
import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.easyant.core.ivy.IvyInstanceHelper;
import org.apache.ivy.Ivy;
import org.apache.ivy.ant.IvyAntSettings;
import org.apache.ivy.core.LogOptions;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
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
 * This task is used to include / import an EasyAnt module, such as a buildtype
 * or plugin. The module is located using one of three methods:
 * <ol>
 * <li>A full Ivy Module Revision ID is specified with {@link #setMrid(String)
 * mrid}. The Ivy descriptor and other artifacts for the module are then located
 * in Ivy repositories.</li>
 * <li>Module Revision ID is specified with the {@link #setOrganisation(String)
 * org}, {@link #setModule(String) module}, and {@link #setRevision(String) rev}
 * attributes.</li>
 * <li>a filesystem path to the module Ivy descriptor is given with
 * {@link #setFile(String) file}. Other module artifacts (Ant scripts, etc) are
 * assumed to be in the same parent directory as this file.</li>
 * </ol>
 * 
 * <p>
 * The include mechanism is similar to the Ant import task, excepts that it
 * automatically prefix all targets of the module ant script with the value of
 * {@link #setAs(String) as} attribute. The prefix used by default is the name
 * of the imported project, but it can be overridden when calling "include".
 * This is useful to use features provided by many build modules, while
 * preserving a namespace isolation to avoid collisions.
 * </p>
 * 
 * <p>
 * While possible, overriding a target defined in an included module is not
 * recommended. To do so, the normal Ant import mechanism is preferred.
 * </p>
 */
public class Import extends AbstractEasyAntTask implements DynamicAttribute {

    private String module;
    private String organisation;
    private String revision;

    private String mrid;

    private String file;
    private String settingsRef;

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

    public String getFile() {
        return file;
    }

    /**
     * Set the filesystem location of the module descriptor to load. This is an
     * alternative to specifying a repository location using
     * {@link #setMrid(String)} or {@link #setModule(String)}
     */
    public void setFile(String file) {
        this.file = file;
    }

    public String getSettingsRef() {
        return settingsRef;
    }

    /**
     * Optionally specify the Ivy settings instance used to resolve module
     * dependencies and publications. If unspecified, the EasyAnt Ivy settings
     * are used.
     */
    public void setSettingsRef(String settingsRef) {
        this.settingsRef = settingsRef;
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

        ImportStrategy strategy = null;
        if (mrid != null) {
            strategy = new RepositoryImportStrategy(ModuleRevisionId
                    .parse(mrid));
        } else if (organisation != null && module != null && revision != null) {
            strategy = new RepositoryImportStrategy(ModuleRevisionId
                    .newInstance(organisation, module, revision));
        } else if (file != null) {
            strategy = new FileImportStrategy(new File(file));
        } else {

            throw new BuildException(
                    "You must specify exactly ONE of:  mrid, organisation + module + revision, or file");
        }
        String moduleName = strategy.getModuleName();
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
                ModuleRevisionId moduleRevisionId=ModuleRevisionId.parse(mrid);
                as = moduleRevisionId.getName();
            //when using exploded style
            } else if (getModule() != null) {
                as=getModule();
            }
        }
        
        // check if a property skip.${module} or skip.${as} is set
        boolean toBeSkipped = strategy.isSkipped()
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
                ResolveReport report = strategy.resolveModule(resolveOptions);
                // Check dependency on core
                checkCoreCompliance(report, providedConf);

                ModuleRevisionId moduleRevisionId = strategy
                        .getModuleRevisionId(report);

                Path path = new Path(getProject());
                getProject().addReference(
                        moduleRevisionId.getModuleId().toString()
                                + ".classpath", path);
                File antFile = strategy.findAntScript(mainConf, report);
                for (int j = 0; j < report.getConfigurationReport(mainConf)
                        .getAllArtifactsReports().length; j++) {
                    ArtifactDownloadReport artifact = report
                            .getConfigurationReport(mainConf)
                            .getAllArtifactsReports()[j];

                    if ("jar".equals(artifact.getType())) {
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

    /**
     * Get the Ivy instance used to load the module artifacts.
     * 
     * @see #setSettingsRef(String)
     */
    private Ivy getIvyInstance() {
        if (settingsRef != null) {
            IvyAntSettings settings = IvyInstanceHelper.getIvyAntSettings(getProject(),settingsRef);
            if (settings == null) {
                throw new BuildException("Unable to find Ivy settings named '"
                        + settingsRef + "'");
            }
            return settings.getConfiguredIvyInstance(this);
        } else {
            return getEasyAntIvyInstance();
        }
    }

    /**
     * Abstracts the mechanism used to locate a single module Ivy descriptor and
     * its artifacts.
     */
    private static interface ImportStrategy {

        /**
         * Resolve the module using the given resolution options.
         */
        public ResolveReport resolveModule(ResolveOptions options)
                throws ParseException, IOException;

        public ModuleRevisionId getModuleRevisionId(ResolveReport report);

        public File findAntScript(String conf, ResolveReport report);

        /**
         * Get this module's name, for logging purposes.
         */
        public String getModuleName();

        /**
         * @return true if this module should not be imported.
         */
        public boolean isSkipped();

    }

    /**
     * Load the requested module and its artifacts from an Ivy repository.
     */
    private class RepositoryImportStrategy implements ImportStrategy {
        private ModuleRevisionId mrid;

        protected RepositoryImportStrategy(ModuleRevisionId mrid) {
            this.mrid = mrid;
        }

        public ModuleRevisionId getModuleRevisionId(ResolveReport report) {
            return mrid;
        }

        public ResolveReport resolveModule(ResolveOptions options)
                throws ParseException, IOException {
            Ivy ivy = getIvyInstance();
            ivy.pushContext();
            try {
                return ivy.getResolveEngine().resolve(mrid, options, !options.isUseCacheOnly());
            } finally {
                ivy.popContext();
            }
        }

        public String getModuleName() {
            return mrid.toString();
        }

        public boolean isSkipped() {
            return getProject().getProperty("skip." + getModuleName()) != null;
        }

        public File findAntScript(String conf, ResolveReport report) {
            for (int j = 0; j < report.getConfigurationReport(conf)
                    .getAllArtifactsReports().length; j++) {
                ArtifactDownloadReport artifact = report
                        .getConfigurationReport(conf).getAllArtifactsReports()[j];

                if ("ant".equals(artifact.getType())
                        && "ant".equals(artifact.getExt())) {
                    return artifact.getLocalFile();
                }
            }
            return null;
        }
    }

    /**
     * Load the requested module and its artifacts directly from the filesystem.
     */
    private class FileImportStrategy implements ImportStrategy {
        private File descriptorFile;

        private FileImportStrategy(File descriptorFile) {
            this.descriptorFile = descriptorFile;
        }

        public ModuleRevisionId getModuleRevisionId(ResolveReport report) {
            return report.getModuleDescriptor().getModuleRevisionId();
        }

        public ResolveReport resolveModule(ResolveOptions options)
                throws ParseException, IOException {
            return getIvyInstance().getResolveEngine().resolve(
                    descriptorFile.toURL(), options);
        }

        public String getModuleName() {
            return descriptorFile.getAbsolutePath();
        }

        public boolean isSkipped() {
            return false;
        }

        public File findAntScript(String conf, ResolveReport report) {
            ModuleDescriptor descriptor = report.getModuleDescriptor();
            Artifact[] artifacts = descriptor.getArtifacts(conf);
            for (int i = 0; i < artifacts.length; ++i) {
                Artifact artifact = artifacts[i];
                if ("ant".equals(artifact.getType())
                        && "ant".equals(artifact.getExt())) {
                    File file = getLocalArtifact(artifact.getName(), artifact
                            .getExt());
                    if (file.isFile()) {
                        return file;
                    }
                }
            }
            return null;
        }

        private File getLocalArtifact(String name, String ext) {
            return new File(descriptorFile.getParentFile(), name + "." + ext);
        }
    }

    public void setDynamicAttribute(String attributeName, String value)
            throws BuildException {
        PropertyTask property = new PropertyTask();
        property.setName(attributeName);
        property.setValue(value);
        initTask(property).execute();
    }
}
