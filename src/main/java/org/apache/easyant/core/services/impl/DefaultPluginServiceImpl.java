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
package org.apache.easyant.core.services.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.easyant.core.EasyAntConstants;
import org.apache.easyant.core.EasyAntEngine;
import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.easyant.core.ant.ProjectUtils;
import org.apache.easyant.core.descriptor.EasyAntModuleDescriptor;
import org.apache.easyant.core.descriptor.PluginDescriptor;
import org.apache.easyant.core.descriptor.PropertyDescriptor;
import org.apache.easyant.core.parser.DefaultEasyAntXmlModuleDescriptorParser;
import org.apache.easyant.core.parser.EasyAntModuleDescriptorParser;
import org.apache.easyant.core.report.EasyAntReport;
import org.apache.easyant.core.report.ExtensionPointReport;
import org.apache.easyant.core.report.ImportedModuleReport;
import org.apache.easyant.core.report.ParameterReport;
import org.apache.easyant.core.report.ParameterType;
import org.apache.easyant.core.report.TargetReport;
import org.apache.easyant.core.services.PluginService;
import org.apache.easyant.tasks.AbstractImport;
import org.apache.easyant.tasks.Import;
import org.apache.easyant.tasks.ImportTestModule;
import org.apache.easyant.tasks.ParameterTask;
import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.ivy.plugins.repository.url.URLResource;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.util.Message;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ComponentHelper;
import org.apache.tools.ant.ExtensionPoint;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Property;
import org.apache.tools.ant.types.Path;

public class DefaultPluginServiceImpl implements PluginService {

    private final EasyAntModuleDescriptorParser parser;

    private final Ivy ivyInstance;

    /**
     * This is the default constructor, the IvyContext should be the IvyContext configured to the easyant ivy instance
     * 
     * @param ivyInstance
     *            the easyant ivy instance
     */
    public DefaultPluginServiceImpl(final Ivy ivyInstance) {
        this(ivyInstance, new DefaultEasyAntXmlModuleDescriptorParser());
    }

    /**
     * A custom constructor if you want to specify your own parser / configuration service, you should use this
     * constructor the IvyContext should be the IvyContext configured to the easyant ivy instance
     * 
     * @param ivyInstance
     *            the easyant ivy instance
     * @param parser
     *            a valid easyantModuleDescriptor
     */
    public DefaultPluginServiceImpl(final Ivy ivyInstance, EasyAntModuleDescriptorParser parser) {
        this.ivyInstance = ivyInstance;
        if (parser == null) {
            throw new IllegalArgumentException("You must set a valid easyant module descriptor parser");
        }
        this.parser = parser;
        ModuleDescriptorParserRegistry.getInstance().addParser(parser);
    }

    public EasyAntReport getPluginInfo(File pluginIvyFile, File sourceDirectory, String conf) throws Exception {
        IvyContext.pushNewContext().setIvy(ivyInstance);
        EasyAntReport eaReport = null;
        try {

            ResolveOptions resolveOptions = new ResolveOptions();
            resolveOptions.setLog(ResolveOptions.LOG_QUIET);
            resolveOptions.setConfs(conf.split(","));
            ResolveReport report = IvyContext.getContext().getIvy().getResolveEngine()
                    .resolve(pluginIvyFile.toURI().toURL(), resolveOptions);
            eaReport = new EasyAntReport();
            eaReport.setResolveReport(report);
            eaReport.setModuleDescriptor(report.getModuleDescriptor());

            Project project = buildProject(null);
            
            // emulate top level project
            ProjectHelper helper = ProjectHelper.getProjectHelper();
            helper.getImportStack().addElement(ProjectUtils.emulateMainScript(project));
            project.addReference(ProjectHelper.PROJECTHELPER_REFERENCE, helper);

            ImportTestModule importTestModule = new ImportTestModule();
            importTestModule.setModuleIvy(pluginIvyFile);
            importTestModule.setSourceDirectory(sourceDirectory);
            importTestModule.setOwningTarget(ProjectUtils.createTopLevelTarget());
            importTestModule.setLocation(new Location(ProjectUtils.emulateMainScript(project).getAbsolutePath()));
            importTestModule.setProject(project);
            importTestModule.execute();

            analyseProject(project, eaReport, conf);
        } catch (Exception e) {
            throw new Exception("An error occured while fetching plugin informations : " + e.getMessage(), e);
        } finally {
            IvyContext.popContext();
        }
        return eaReport;

    }

    public EasyAntReport getPluginInfo(final ModuleRevisionId moduleRevisionId, String conf) throws Exception {

        IvyContext.pushNewContext().setIvy(ivyInstance);
        EasyAntReport eaReport = null;
        try {

            ResolveOptions resolveOptions = new ResolveOptions();
            resolveOptions.setLog(ResolveOptions.LOG_QUIET);
            resolveOptions.setConfs(conf.split(","));
            final ResolveReport report = IvyContext.getContext().getIvy().getResolveEngine()
                    .resolve(moduleRevisionId, resolveOptions, true);
            eaReport = new EasyAntReport();
            eaReport.setResolveReport(report);
            eaReport.setModuleDescriptor(report.getModuleDescriptor());

            Project project = buildProject(null);

            AbstractImport abstractImport = new AbstractImport() {
                @Override
                public void execute() throws BuildException {
                    Path path = createModulePath(moduleRevisionId);
                    File antFile = null;
                    for (int j = 0; j < report.getConfigurationReport(getMainConf()).getAllArtifactsReports().length; j++) {
                        ArtifactDownloadReport artifact = report.getConfigurationReport(getMainConf())
                                .getAllArtifactsReports()[j];

                        if ("ant".equals(artifact.getType())) {
                            antFile = artifact.getLocalFile();
                        } else if ("jar".equals(artifact.getType())) {
                            path.createPathElement().setLocation(artifact.getLocalFile());
                        } else {
                            handleOtherResourceFile(moduleRevisionId, artifact.getName(), artifact.getExt(),
                                    artifact.getLocalFile());
                        }
                    }
                    if (antFile != null && antFile.exists()) {
                        ProjectHelper.configureProject(getProject(), antFile);
                    }
                }
            };

            abstractImport.setProject(project);
            // location ?
            abstractImport.execute();

            analyseProject(project, eaReport, conf);
        } catch (Exception e) {
            throw new Exception("An error occured while fetching plugin informations : " + e.getMessage(), e);
        } finally {
            IvyContext.popContext();
        }
        return eaReport;

    }

    private Project buildProject(Map<String, String> properties) {
        Project project = new Project();
        project.setNewProperty(EasyAntMagicNames.AUDIT_MODE, "true");
        project.setNewProperty(EasyAntMagicNames.SKIP_CORE_REVISION_CHECKER, "true");
        EasyAntEngine eagAntEngine = new EasyAntEngine();
        eagAntEngine.configureEasyAntIvyInstance(project);
        if (properties != null) {
            for (Entry<String, String> entry : properties.entrySet()) {
                project.setNewProperty(entry.getKey(), entry.getValue());
            }
        }
        project.init();
        return project;
    }

    private void analyseProject(Project project, EasyAntReport eaReport, String conf) throws IOException, Exception {
        for (Iterator iterator = project.getTargets().values().iterator(); iterator.hasNext();) {
            Target target = (Target) iterator.next();
            handleTarget(target, eaReport);
            for (int i = 0; i < target.getTasks().length; i++) {
                Task task = target.getTasks()[i];
                Class taskClass = ComponentHelper.getComponentHelper(project).getComponentClass(task.getTaskType());
                if (taskClass == null) {
                    continue;
                }
                if (ParameterTask.class.getName().equals(taskClass.getName())) {
                    handleParameterTask(task, eaReport);
                }
                if (Property.class.getName().equals(taskClass.getName())) {
                    handleProperty(task, eaReport);
                }
                if (Import.class.getName().equals(taskClass.getName())) {
                    handleImport(task, eaReport, conf);
                }

            }
        }
    }

    private void scanAntFile(File antFile, Map<String, String> properties, EasyAntReport eaReport, String conf)
            throws IOException, Exception {
        Project project = buildProject(properties);
        ProjectHelper.configureProject(project, antFile);

        analyseProject(project, eaReport, conf);
    }

    private void handleImport(Task task, EasyAntReport eaReport, String conf) throws Exception {
        Map<String, String> attributes = task.getRuntimeConfigurableWrapper().getAttributeMap();
        ImportedModuleReport importedModuleReport = new ImportedModuleReport();
        PropertyHelper propertyHelper = PropertyHelper.getPropertyHelper(task.getProject());

        importedModuleReport.setModuleMrid(propertyHelper.replaceProperties(attributes.get("mrid")));
        importedModuleReport.setModule(propertyHelper.replaceProperties(attributes.get("module")));
        String org = attributes.get("org") != null ? attributes.get("org") : attributes.get("organisation");
        importedModuleReport.setOrganisation(propertyHelper.replaceProperties(org));

        String rev = attributes.get("rev") != null ? attributes.get("rev") : attributes.get("revision");
        importedModuleReport.setRevision(propertyHelper.replaceProperties(rev));

        importedModuleReport.setType(propertyHelper.replaceProperties(attributes.get("type")));
        importedModuleReport.setAs(propertyHelper.replaceProperties(attributes.get("as")));
        if (attributes.get("mandatory") != null) {
            importedModuleReport.setMandatory(Boolean.parseBoolean(propertyHelper.replaceProperties(attributes
                    .get("mandatory"))));
        }
        importedModuleReport
                .setEasyantReport(getPluginInfo(ModuleRevisionId.parse(importedModuleReport.getModuleMrid())));

        eaReport.addImportedModuleReport(importedModuleReport);
        Message.debug("Ant file import another module called : " + importedModuleReport.getModuleMrid() + " with mode "
                + importedModuleReport.getType());
    }

    /**
     * @param task
     * @param eaReport
     * @throws IOException
     */
    private void handleProperty(Task task, EasyAntReport eaReport) throws IOException {
        Map<String, String> attributes = task.getRuntimeConfigurableWrapper().getAttributeMap();
        if (attributes.get("file") != null) {
            Properties propToLoad = new Properties();
            File f = new File(PropertyHelper.getPropertyHelper(task.getProject()).replaceProperties(
                    attributes.get("file")));
            if (f.exists()) {
                try {
                    propToLoad.load(new FileInputStream(f));
                    for (Iterator iter = propToLoad.keySet().iterator(); iter.hasNext();) {
                        String key = (String) iter.next();
                        PropertyDescriptor propertyDescriptor = new PropertyDescriptor(key);
                        propertyDescriptor.setValue(propToLoad.getProperty(key));
                        eaReport.addPropertyDescriptor(propertyDescriptor.getName(), propertyDescriptor);
                    }

                } catch (IOException e) {
                    IOException ioe = new IOException("Unable to parse the property file :" + attributes.get("file"));
                    ioe.initCause(e);
                    throw ioe;
                }
            }
        }
    }

    /**
     * @param task
     * @param eaReport
     */
    private void handleParameterTask(Task task, EasyAntReport eaReport) {
        Map<String, String> attributes = task.getRuntimeConfigurableWrapper().getAttributeMap();
        PropertyDescriptor propertyDescriptor = null;

        if (attributes.get("property") != null) {
            propertyDescriptor = new PropertyDescriptor(attributes.get("property"));
            propertyDescriptor.setDefaultValue(attributes.get("default"));
            if (attributes.get("required") == null)
                propertyDescriptor.setRequired(false);
            else
                propertyDescriptor.setRequired(new Boolean(attributes.get("required")));
            if (attributes.get("description") != null) {
                propertyDescriptor.setDescription(attributes.get("description"));
            }
            if (task.getRuntimeConfigurableWrapper().getText() != null
                    && task.getRuntimeConfigurableWrapper().getText().length() > 0) {
                propertyDescriptor.setDescription(task.getRuntimeConfigurableWrapper().getText().toString());
            }
            Message.debug("Ant file has a property called : " + propertyDescriptor.getName());
            eaReport.addPropertyDescriptor(propertyDescriptor.getName(), propertyDescriptor);
        } else if (attributes.get("path") != null) {
            ParameterReport parameterReport = new ParameterReport(ParameterType.PATH);
            parameterReport.setName(attributes.get("path"));
            parameterReport.setDefaultValue(attributes.get("default"));
            parameterReport.setRequired(new Boolean(attributes.get("required")));
            eaReport.addParameterReport(parameterReport);
            Message.debug("Ant file has a path called : " + parameterReport.getName());
        }
    }

    /**
     * @param target
     * @param eaReport
     */
    private void handleTarget(Target target, EasyAntReport eaReport) {
        if (!"".equals(target.getName())) {
            boolean isExtensionPoint = target instanceof ExtensionPoint;
            if (!isExtensionPoint) {
                TargetReport targetReport = new TargetReport();
                targetReport.setName(target.getName());
                StringBuilder sb = new StringBuilder();
                Enumeration targetDeps = target.getDependencies();
                while (targetDeps.hasMoreElements()) {
                    String t = (String) targetDeps.nextElement();
                    sb.append(t);
                    if (targetDeps.hasMoreElements()) {
                        sb.append(",");
                    }
                }
                targetReport.setDepends(sb.toString());
                targetReport.setDescription(target.getDescription());
                targetReport.setIfCase(target.getIf());
                targetReport.setUnlessCase(target.getUnless());
                for (Iterator iterator = target.getProject().getTargets().values().iterator(); iterator.hasNext();) {
                    Target currentTarget = (Target) iterator.next();
                    if (currentTarget instanceof ExtensionPoint) {
                        Enumeration dependencies = currentTarget.getDependencies();
                        while (dependencies.hasMoreElements()) {
                            String dep = (String) dependencies.nextElement();
                            if (dep.equals(target.getName())) {
                                targetReport.setExtensionPoint(currentTarget.getName());
                            }
                        }

                    }
                }

                eaReport.addTargetReport(targetReport);

                Message.debug("Ant file has a target called : " + targetReport.getName());
            } else {
                ExtensionPointReport extensionPoint = new ExtensionPointReport(target.getName());
                StringBuilder sb = new StringBuilder();
                Enumeration targetDeps = target.getDependencies();
                while (targetDeps.hasMoreElements()) {
                    String t = (String) targetDeps.nextElement();
                    sb.append(t);
                    if (targetDeps.hasMoreElements()) {
                        sb.append(",");
                    }
                }
                extensionPoint.setDepends(sb.toString());
                extensionPoint.setDescription(target.getDescription());
                eaReport.addExtensionPointReport(extensionPoint);
                Message.debug("Ant file has an extensionPoint called : " + extensionPoint.getName());
            }
        }
    }

    public EasyAntReport getPluginInfo(ModuleRevisionId moduleRevisionId) throws Exception {
        return getPluginInfo(moduleRevisionId, "default");
    }

    public EasyAntReport getPluginInfo(String moduleRevisionId) throws Exception {
        ModuleRevisionId module = buildModuleRevisionId(moduleRevisionId, PluginType.PLUGIN);
        return getPluginInfo(module);
    }

    public EasyAntReport getBuildTypeInfo(String moduleRevisionId) throws Exception {
        ModuleRevisionId module = buildModuleRevisionId(moduleRevisionId, PluginType.BUILDTYPE);
        return getPluginInfo(module);
    }

    public EasyAntModuleDescriptor getEasyAntModuleDescriptor(File moduleDescriptor) throws Exception {
        if (moduleDescriptor == null)
            throw new Exception("moduleDescriptor cannot be null");
        if (!moduleDescriptor.exists()) {
            throw new Exception("imposible to find the specified module descriptor"
                    + moduleDescriptor.getAbsolutePath());
        }
        IvyContext.pushNewContext().setIvy(ivyInstance);
        // First we need to parse the specified file to retrieve all the easyant
        // stuff
        parser.parseDescriptor(ivyInstance.getSettings(), moduleDescriptor.toURL(),
                new URLResource(moduleDescriptor.toURL()), true);
        EasyAntModuleDescriptor md = parser.getEasyAntModuleDescriptor();
        IvyContext.popContext();
        return md;
    }

    public EasyAntReport generateEasyAntReport(File moduleDescriptor, File optionalAntModule, File overrideAntModule)
            throws Exception {
        EasyAntReport eaReport = new EasyAntReport();

        if (overrideAntModule != null && overrideAntModule.exists()) {
            scanAntFile(overrideAntModule, null, eaReport, "default");
        }
        try {
            EasyAntModuleDescriptor md = getEasyAntModuleDescriptor(moduleDescriptor);
            eaReport.setModuleDescriptor(md.getIvyModuleDescriptor());
            for (Iterator<PropertyDescriptor> iterator = md.getProperties().values().iterator(); iterator.hasNext();) {
                PropertyDescriptor property = iterator.next();
                eaReport.addPropertyDescriptor(property.getName(), property);
            }

            // Store infos on the buildtype
            if (md.getBuildType() != null) {
                ImportedModuleReport buildType = new ImportedModuleReport();
                buildType.setModuleMrid(md.getBuildType());
                buildType.setEasyantReport(getPluginInfo(ModuleRevisionId.parse(md.getBuildType())));
                eaReport.addImportedModuleReport(buildType);
            }
            // Store infos on plugins
            for (Iterator iterator = md.getPlugins().iterator(); iterator.hasNext();) {
                PluginDescriptor plugin = (PluginDescriptor) iterator.next();
                ImportedModuleReport pluginReport = new ImportedModuleReport();
                ModuleRevisionId mrid = ModuleRevisionId.parse(plugin.getMrid());
                pluginReport.setModuleMrid(plugin.getMrid());
                if (plugin.getAs() == null) {
                    pluginReport.setAs(mrid.getName());
                } else {
                    pluginReport.setAs(plugin.getAs());
                }
                pluginReport.setType(plugin.getMode());
                pluginReport.setEasyantReport(getPluginInfo(ModuleRevisionId.parse(plugin.getMrid())));
                eaReport.addImportedModuleReport(pluginReport);
            }
        } catch (Exception e) {
            throw new Exception("problem while parsing Ivy module file: " + e.getMessage(), e);
        }

        if (optionalAntModule != null && optionalAntModule.exists()) {
            scanAntFile(optionalAntModule, null, eaReport, "default");
        }

        return eaReport;
    }

    public ModuleRevisionId[] search(String organisation, String moduleName, String revision, String branch,
            String matcher, String resolver) throws Exception {
        IvySettings settings = ivyInstance.getSettings();

        if (moduleName == null && PatternMatcher.EXACT.equals(matcher)) {
            throw new Exception("no module name provided for ivy repository graph task: "
                    + "It can either be set explicitely via the attribute 'module' or "
                    + "via 'ivy.module' property or a prior call to <resolve/>");
        } else if (moduleName == null && !PatternMatcher.EXACT.equals(matcher)) {
            moduleName = PatternMatcher.ANY_EXPRESSION;
        }
        ModuleRevisionId mrid = ModuleRevisionId.newInstance(organisation, moduleName, revision);

        ModuleRevisionId criteria = null;

        if ((revision == null) || settings.getVersionMatcher().isDynamic(mrid)) {
            criteria = new ModuleRevisionId(new ModuleId(organisation, moduleName), branch, "*");
        } else {
            criteria = new ModuleRevisionId(new ModuleId(organisation, moduleName), branch, revision);
        }

        PatternMatcher patternMatcher = settings.getMatcher(matcher);
        if ("*".equals(resolver)) {
            // search in all resolvers. this can be quite slow for complex
            // repository configurations
            // with ChainResolvers, since resolvers in chains will be searched
            // multiple times.
            return ivyInstance.listModules(criteria, patternMatcher);
        } else {
            // limit search to the specified resolver.
            DependencyResolver dependencyResolver = resolver == null ? settings.getDefaultResolver() : settings
                    .getResolver(resolver);
            if (dependencyResolver == null) {
                throw new IllegalArgumentException("Unknown dependency resolver for search: " + resolver);
            }

            ivyInstance.pushContext();
            try {
                return ivyInstance.getSearchEngine().listModules(dependencyResolver, criteria, patternMatcher);
            } finally {
                ivyInstance.popContext();
            }
        }
    }

    public ModuleRevisionId[] search(String organisation, String moduleName) throws Exception {
        return search(organisation, moduleName, null, null, PatternMatcher.EXACT_OR_REGEXP, null);
    }

    public String[] searchModule(String organisation, String moduleName) throws Exception {
        ModuleRevisionId[] mrids = search(organisation, moduleName);
        String[] result = new String[mrids.length];
        for (int i = 0; i < mrids.length; i++) {
            result[i] = mrids[i].toString();
        }
        return result;
    }

    public String getDescription(ModuleRevisionId mrid) {
        ResolvedModuleRevision rmr = ivyInstance.findModule(mrid);
        return rmr.getDescriptor().getDescription();
    }

    public String getPluginDescription(String moduleRevisionId) {
        ModuleRevisionId module = buildModuleRevisionId(moduleRevisionId, PluginType.PLUGIN);
        return getDescription(module);
    }

    public String getBuildTypeDescription(String moduleRevisionId) {
        ModuleRevisionId module = buildModuleRevisionId(moduleRevisionId, PluginType.BUILDTYPE);

        return getDescription(module);
    }

    private ModuleRevisionId buildModuleRevisionId(String moduleRevisionId, PluginType pluginType) {
        String mrid = moduleRevisionId;
        if (!mrid.matches(".*#.*")) {
            if (pluginType.equals(PluginType.BUILDTYPE)) {
                Message.debug("No organisation specified for buildtype " + mrid + " using the default one");

                mrid = EasyAntConstants.EASYANT_BUILDTYPES_ORGANISATION + "#" + mrid;

            } else {
                Message.debug("No organisation specified for plugin " + mrid + " using the default one");

                mrid = EasyAntConstants.EASYANT_PLUGIN_ORGANISATION + "#" + mrid;
            }
        }
        ModuleRevisionId module = ModuleRevisionId.parse(mrid);
        return module;
    }

    public EasyAntReport generateEasyAntReport(File moduleDescriptor) throws Exception {
        return generateEasyAntReport(moduleDescriptor, null, null);
    }

    private enum PluginType {
        BUILDTYPE, PLUGIN

    }

}
