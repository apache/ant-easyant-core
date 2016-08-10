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
package org.apache.easyant.core.services;

import org.apache.easyant.core.EasyAntConstants;
import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.easyant.core.ant.ProjectUtils;
import org.apache.easyant.core.ant.listerners.TaskCollectorFromImplicitTargetListener;
import org.apache.easyant.core.descriptor.EasyAntModuleDescriptor;
import org.apache.easyant.core.descriptor.PluginType;
import org.apache.easyant.core.descriptor.PropertyDescriptor;
import org.apache.easyant.core.parser.DefaultEasyAntXmlModuleDescriptorParser;
import org.apache.easyant.core.parser.EasyAntModuleDescriptorParser;
import org.apache.easyant.core.report.*;
import org.apache.easyant.tasks.*;
import org.apache.ivy.Ivy;
import org.apache.ivy.ant.IvyAntSettings;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.ivy.plugins.repository.url.URLResource;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.util.Message;
import org.apache.tools.ant.*;
import org.apache.tools.ant.property.ParseNextProperty;
import org.apache.tools.ant.property.PropertyExpander;
import org.apache.tools.ant.taskdefs.Property;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParsePosition;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

public class DefaultPluginService implements PluginService {

    private final EasyAntModuleDescriptorParser parser;

    private final Ivy ivyInstance;
    private final IvyAntSettings easyantIvySettings;

    private boolean offlineMode;

    /**
     * This is the default constructor, the IvyContext should be the IvyContext configured to the easyant ivy instance
     *
     * @param easyantIvySettings the easyant ivy instance
     */
    public DefaultPluginService(final IvyAntSettings easyantIvySettings) {
        this(easyantIvySettings, new DefaultEasyAntXmlModuleDescriptorParser());
    }

    /**
     * A custom constructor if you want to specify your own parser / configuration service, you should use this
     * constructor the IvyContext should be the IvyContext configured to the easyant ivy instance
     *
     * @param easyantIvySetings the easyant ivy instance
     * @param parser            a valid easyantModuleDescriptor
     */
    public DefaultPluginService(final IvyAntSettings easyantIvySetings, EasyAntModuleDescriptorParser parser) {
        this.easyantIvySettings = easyantIvySetings;
        this.ivyInstance = easyantIvySetings.getConfiguredIvyInstance(easyantIvySetings);
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

            ResolveOptions resolveOptions = buildResolveOptions(conf);
            ResolveReport report = IvyContext.getContext().getIvy().getResolveEngine()
                    .resolve(pluginIvyFile.toURI().toURL(), resolveOptions);
            eaReport = new EasyAntReport();
            eaReport.setResolveReport(report);
            eaReport.setModuleDescriptor(report.getModuleDescriptor());

            Project project = buildProject();

            // expose resolve report for import deferred
            project.addReference(EasyAntMagicNames.IMPORTED_MODULES_RESOLVE_REPORT_REF, report);

            // emulate top level project
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

    private ResolveOptions buildResolveOptions(String conf) {
        ResolveOptions resolveOptions = new ResolveOptions();
        resolveOptions.setLog(ResolveOptions.LOG_QUIET);
        resolveOptions.setConfs(conf.split(","));
        resolveOptions.setUseCacheOnly(offlineMode);
        return resolveOptions;
    }

    public EasyAntReport getPluginInfo(final ModuleRevisionId moduleRevisionId, String conf) throws Exception {
        IvyContext.pushNewContext().setIvy(ivyInstance);
        EasyAntReport eaReport = null;
        try {

            ResolveOptions resolveOptions = buildResolveOptions(conf);
            final ResolveReport report = IvyContext.getContext().getIvy().getResolveEngine()
                    .resolve(moduleRevisionId, resolveOptions, false);
            eaReport = new EasyAntReport();
            eaReport.setResolveReport(report);
            eaReport.setModuleDescriptor(report.getModuleDescriptor());

            Project project = buildProject();
            // expose resolve report for import deferred
            project.addReference(EasyAntMagicNames.IMPORTED_MODULES_RESOLVE_REPORT_REF, report);

            AbstractImport abstractImport = new AbstractImport() {
                @Override
                public void execute() throws BuildException {
                    Path path = createModulePath(moduleRevisionId.getModuleId());
                    File antFile = null;
                    for (ArtifactDownloadReport artifact : report.getConfigurationReport(getMainConf()).getAllArtifactsReports()) {
                        if ("ant".equals(artifact.getType())) {
                            antFile = artifact.getLocalFile();
                        } else if (shouldBeAddedToClasspath(artifact)) {
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

    private Project buildProject() {
        Project project = new Project();
        project.setNewProperty(EasyAntMagicNames.AUDIT_MODE, "true");
        project.setNewProperty(EasyAntMagicNames.SKIP_CORE_REVISION_CHECKER, "true");
        project.addReference(EasyAntMagicNames.EASYANT_IVY_INSTANCE, easyantIvySettings);

        TaskCollectorFromImplicitTargetListener listener = new TaskCollectorFromImplicitTargetListener();
        listener.addClassToCollect(ParameterTask.class);
        listener.addClassToCollect(Property.class);
        listener.addClassToCollect(Import.class);
        listener.addClassToCollect(ImportDeferred.class);
        listener.addClassToCollect(Path.class);
        listener.addClassToCollect(PathTask.class);
        listener.addClassToCollect(FileSet.class);
        project.addBuildListener(listener);

        // add a property helper to ignore basedir property on reports
        PropertyHelper propertyHelper = PropertyHelper.getPropertyHelper(project);
        propertyHelper.add(new BypassDefaultPropertyExpander());

        project.init();
        ProjectUtils.configureProjectHelper(project);
        return project;
    }

    private void analyseProject(Project project, EasyAntReport eaReport, String conf) throws Exception {

        // handle tasks from implicit target
        // When using import/include, ant create a "implicit target" to process root tasks. When tasks are declared
        // outside of a target in root project we are able to parse them "normally" as this implicit target is added to
        // the project. However this is not the case for tasks declared outside any target in imported build script.
        // So we use a listener to collect required informations
        for (BuildListener buildListener : project.getBuildListeners()) {
            if (buildListener instanceof TaskCollectorFromImplicitTargetListener) {
                TaskCollectorFromImplicitTargetListener taskCollectorFromImplicitTargetListener = (TaskCollectorFromImplicitTargetListener) buildListener;
                for (Task task : taskCollectorFromImplicitTargetListener.getTasksCollected()) {
                    handleTask(project, eaReport, conf, task);
                }
            }
        }

        // handle tasks declared in targets
        Map<String, Target> targets = ProjectUtils.removeDuplicateTargets(project.getTargets());
        for (Target target : targets.values()) {
            if (!"".equals(target.getName())) {
                // gather root module location from target if no task at root level was defined
                TaskCollectorFromImplicitTargetListener.gatherRootModuleLocation(target.getProject(),
                        target.getLocation());
                handleTarget(target, eaReport);
                for (int i = 0; i < target.getTasks().length; i++) {
                    Task task = target.getTasks()[i];
                    handleTask(project, eaReport, conf, task);
                }
            }
        }
    }

    private Object maybeConfigureTask(Task task) {
        if (task.getRuntimeConfigurableWrapper().getProxy() instanceof UnknownElement) {
            UnknownElement ue = (UnknownElement) task.getRuntimeConfigurableWrapper().getProxy();
            ue.maybeConfigure();
            return ue.getRealThing();
        } else if (task instanceof UnknownElement) {
            UnknownElement ue = (UnknownElement) task;
            ue.maybeConfigure();
            return ue.getRealThing();
        } else {
            return task;
        }
    }

    private void handleTask(Project project, EasyAntReport eaReport, String conf, Task task) throws Exception {
        Class<?> taskClass = ComponentHelper.getComponentHelper(project).getComponentClass(task.getTaskType());
        if (taskClass != null) {
            if (ParameterTask.class.isAssignableFrom(taskClass)) {
                ParameterTask parameterTask = (ParameterTask) maybeConfigureTask(task);
                handleParameterTask(parameterTask, eaReport);
            }
            if (Property.class.isAssignableFrom(taskClass)) {
                Property propertyTask = (Property) maybeConfigureTask(task);
                handleProperty(propertyTask, eaReport);
            }
            if (Import.class.isAssignableFrom(taskClass)) {
                Import importTask = (Import) maybeConfigureTask(task);
                handleImport(importTask, eaReport, conf);
            }

            if (ImportDeferred.class.isAssignableFrom(taskClass)) {
                ImportDeferred importTask = (ImportDeferred) maybeConfigureTask(task);
                handleImportDeferred(importTask, eaReport, conf);
            }
            if (Path.class.isAssignableFrom(taskClass)) {
                Path path = (Path) maybeConfigureTask(task);
                handlePathParameter(task.getRuntimeConfigurableWrapper().getId(), path, task.getOwningTarget(),
                        eaReport);
            }
            if (PathTask.class.isAssignableFrom(taskClass)) {
                PathTask pathTask = (PathTask) maybeConfigureTask(task);
                handlePathParameter(pathTask, eaReport);
            }
            if (FileSet.class.isAssignableFrom(taskClass)) {
                FileSet fileSet = (FileSet) maybeConfigureTask(task);
                handleFilesetParameter(task.getRuntimeConfigurableWrapper().getId(), fileSet, task.getOwningTarget(),
                        eaReport);
            }
        }
    }

    private boolean isCurrentModule(Project project, Location location) {
        String rootModuleLocation = project.getProperty(TaskCollectorFromImplicitTargetListener.ROOT_MODULE_LOCATION);
        if (rootModuleLocation == null) {
            throw new IllegalStateException(
                    "rootModuleLocation not found, looks like TaskCollectorFromImplicitTargetListener is not properly configured");
        }
        return location != null && location.getFileName().equals(rootModuleLocation);
    }

    private void handleImport(Import importTask, EasyAntReport eaReport, String conf) throws Exception {
        ImportedModuleReport importedModuleReport = new ImportedModuleReport();

        importedModuleReport.setModuleMrid(importTask.getMrid());
        importedModuleReport.setOrganisation(importTask.getOrganisation());
        importedModuleReport.setModule(importTask.getModule());
        importedModuleReport.setRevision(importTask.getRevision());
        importedModuleReport.setMandatory(importTask.isMandatory());
        importedModuleReport.setMode(importTask.getMode());
        importedModuleReport.setAs(importTask.getAs());

        EasyAntReport pluginInfo = getPluginInfo(ModuleRevisionId.parse(importedModuleReport.getModuleMrid()), conf);
        importedModuleReport.setEasyantReport(pluginInfo);
        eaReport.addImportedModuleReport(importedModuleReport,
                isCurrentModule(importTask.getProject(), importTask.getLocation()));

        Message.debug("Ant file import another module called : " + importedModuleReport.getModuleMrid() + " with mode "
                + importedModuleReport.getMode());
    }

    private void handleImportDeferred(ImportDeferred importTask, EasyAntReport eaReport, String conf) throws Exception {
        ImportedModuleReport importedModuleReport = new ImportedModuleReport();

        importedModuleReport.setOrganisation(importTask.getOrganisation());
        importedModuleReport.setModule(importTask.getModule());
        ResolveReport resolveReport = importTask.getProject().getReference(
                EasyAntMagicNames.IMPORTED_MODULES_RESOLVE_REPORT_REF);
        if (resolveReport != null) {
            for (Object o : resolveReport.getDependencies()) {
                IvyNode dependency = (IvyNode) o;
                if (dependency.getResolvedId().getOrganisation().equals(importTask.getOrganisation()) //
                        && dependency.getResolvedId().getName().equals(importTask.getModule())) {
                    importedModuleReport.setRevision(dependency.getResolvedId().getRevision());
                }

            }
        }

        importedModuleReport.setMandatory(importTask.isMandatory());
        importedModuleReport.setMode(importTask.getMode());
        importedModuleReport.setAs(importTask.getAs());

        EasyAntReport pluginInfo = getPluginInfo(ModuleRevisionId.parse(importedModuleReport.getModuleMrid()), conf);
        importedModuleReport.setEasyantReport(pluginInfo);
        eaReport.addImportedModuleReport(importedModuleReport,
                isCurrentModule(importTask.getProject(), importTask.getLocation()));

        Message.debug("Ant file import another module called : " + importedModuleReport.getModuleMrid() + " with mode "
                + importedModuleReport.getMode());
    }

    private void handleProperty(Property property, EasyAntReport eaReport) throws IOException {
        boolean isCurrentModule = isCurrentModule(property.getProject(), property.getLocation());
        if (property.getFile() != null) {
            Properties propToLoad = new Properties();
            File f = property.getFile();
            if (f.exists()) {
                FileInputStream fis = null;

                try {
                    fis = new FileInputStream(f);
                    propToLoad.load(fis);
                    for (Object o : propToLoad.keySet()) {
                        String key = (String) o;
                        PropertyDescriptor propertyDescriptor = new PropertyDescriptor(key);
                        propertyDescriptor.setValue(propToLoad.getProperty(key));
                        if (property.getOwningTarget() != null) {
                            propertyDescriptor.setOwningTarget(property.getOwningTarget().getName());
                        }
                        eaReport.addPropertyDescriptor(propertyDescriptor.getName(), propertyDescriptor,
                                isCurrentModule);
                    }

                } catch (IOException e) {
                    IOException ioe = new IOException("Unable to parse the property file :" + property.getFile());
                    ioe.initCause(e);
                    throw ioe;
                } finally {
                    if (fis != null) {
                        fis.close();
                    }
                }
            }

        }
        if (property.getName() != null) {
            PropertyDescriptor propertyDescriptor = new PropertyDescriptor(property.getName());
            propertyDescriptor.setValue(property.getValue());
            if (property.getOwningTarget() != null) {
                propertyDescriptor.setOwningTarget(property.getOwningTarget().getName());
            }
            eaReport.addPropertyDescriptor(property.getName(), propertyDescriptor, isCurrentModule);
        }
    }

    private void handleParameterTask(ParameterTask parameterTask, EasyAntReport eaReport) {
        boolean isCurrentModule = isCurrentModule(parameterTask.getProject(), parameterTask.getLocation());

        if (parameterTask.getProperty() != null) {
            PropertyDescriptor propertyDescriptor = new PropertyDescriptor(parameterTask.getProperty());
            propertyDescriptor.setDefaultValue(parameterTask.getDefault());
            // Use unsafe version since we are in audit mode and we want the real value of the required field 
            // (#isRequired method will always return false in audit mode)
            propertyDescriptor.setRequired(parameterTask.isRequiredUnsafe());
            propertyDescriptor.setDescription(parameterTask.getDescription());
            if (parameterTask.getOwningTarget() != null) {
                propertyDescriptor.setOwningTarget(parameterTask.getOwningTarget().getName());
            }
            Message.debug("Ant file has a property called : " + propertyDescriptor.getName());
            eaReport.addPropertyDescriptor(propertyDescriptor.getName(), propertyDescriptor, isCurrentModule);
        } else if (parameterTask.getPath() != null) {
            ParameterReport parameterReport = new ParameterReport(ParameterType.PATH);
            parameterReport.setName(parameterTask.getPath());
            // Use unsafe version since we are in audit mode and we want the real value of the required field 
            // (#isRequired method will always return false in audit mode)
            parameterReport.setRequired(parameterTask.isRequiredUnsafe());
            parameterReport.setDescription(parameterTask.getDescription());
            if (parameterTask.getOwningTarget() != null) {
                parameterReport.setOwningTarget(parameterTask.getOwningTarget().getName());
            }
            eaReport.addParameterReport(parameterReport, isCurrentModule);
            Message.debug("Ant file has a path called : " + parameterReport.getName());
        }
    }

    private void handleFilesetParameter(String id, FileSet fileSet, Target target, EasyAntReport eaReport) {
        ParameterReport parameterReport = new ParameterReport(ParameterType.FILESET);
        if (id != null) {
            parameterReport.setName(id);
            parameterReport.setRequired(false);
            parameterReport.setDescription(fileSet.getDescription());
            if (target != null) {
                parameterReport.setOwningTarget(target.getName());
            }
            eaReport.addParameterReport(parameterReport, isCurrentModule(fileSet.getProject(), fileSet.getLocation()));
            Message.debug("Ant file has a fileset called : " + parameterReport.getName());
        }

    }

    private void handlePathParameter(String pathid, Path path, Target target, EasyAntReport eaReport) {
        ParameterReport parameterReport = new ParameterReport(ParameterType.PATH);
        if (pathid != null) {
            parameterReport.setName(pathid);
            parameterReport.setRequired(false);
            parameterReport.setDescription(path.getDescription());
            if (target != null) {
                parameterReport.setOwningTarget(target.getName());
            }
            eaReport.addParameterReport(parameterReport, isCurrentModule(path.getProject(), path.getLocation()));
            Message.debug("Ant file has a path called : " + parameterReport.getName());
        }
    }

    private void handlePathParameter(PathTask pathTask, EasyAntReport eaReport) {
        ParameterReport parameterReport = new ParameterReport(ParameterType.PATH);
        if (pathTask.getPathid() != null) {
            parameterReport.setName(pathTask.getPathid());
            parameterReport.setRequired(false);
            parameterReport.setDescription(pathTask.getDescription());
            if (pathTask.getOwningTarget() != null) {
                parameterReport.setOwningTarget(pathTask.getOwningTarget().getName());
            }
            eaReport.addParameterReport(parameterReport, isCurrentModule(pathTask.getProject(), pathTask.getLocation()));
            Message.debug("Ant file has a path called : " + parameterReport.getName());
        }
    }

    private void handleTarget(Target target, EasyAntReport eaReport) {
        boolean isCurrentModule = isCurrentModule(target.getProject(), target.getLocation());
        boolean isExtensionPoint = target instanceof ExtensionPoint;
        if (!isExtensionPoint) {
            TargetReport targetReport = new TargetReport();
            targetReport.setName(target.getName());
            StringBuilder sb = new StringBuilder();
            Enumeration<?> targetDeps = target.getDependencies();
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
            for (Target currentTarget : target.getProject().getTargets().values()) {
                if (currentTarget instanceof ExtensionPoint) {
                    Enumeration<?> dependencies = currentTarget.getDependencies();
                    while (dependencies.hasMoreElements()) {
                        String dep = (String) dependencies.nextElement();
                        if (dep.equals(target.getName())) {
                            targetReport.setExtensionPoint(currentTarget.getName());
                        }
                    }

                }
            }

            eaReport.addTargetReport(targetReport, isCurrentModule);

            Message.debug("Ant file has a target called : " + targetReport.getName());
        } else {
            ExtensionPointReport extensionPoint = new ExtensionPointReport(target.getName());
            StringBuilder sb = new StringBuilder();
            Enumeration<?> targetDeps = target.getDependencies();
            while (targetDeps.hasMoreElements()) {
                String t = (String) targetDeps.nextElement();
                sb.append(t);
                if (targetDeps.hasMoreElements()) {
                    sb.append(",");
                }
            }
            extensionPoint.setDepends(sb.toString());
            extensionPoint.setDescription(target.getDescription());
            eaReport.addExtensionPointReport(extensionPoint, isCurrentModule);
            Message.debug("Ant file has an extensionPoint called : " + extensionPoint.getName());
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
        if (moduleDescriptor == null) {
            throw new Exception("moduleDescriptor cannot be null");
        }
        if (!moduleDescriptor.exists()) {
            throw new Exception("imposible to find the specified module descriptor"
                    + moduleDescriptor.getAbsolutePath());
        }
        IvyContext.pushNewContext().setIvy(ivyInstance);
        // First we need to parse the specified file to retrieve all the easyant
        // stuff
        parser.parseDescriptor(ivyInstance.getSettings(), moduleDescriptor.toURI().toURL(), new URLResource(
                moduleDescriptor.toURI().toURL()), true);
        EasyAntModuleDescriptor md = parser.getEasyAntModuleDescriptor();
        IvyContext.popContext();
        return md;
    }

    public EasyAntReport generateEasyAntReport(File moduleDescriptor, File optionalAntModule, File overrideAntModule)
            throws Exception {
        EasyAntReport eaReport = new EasyAntReport();
        EasyAntModuleDescriptor md = getEasyAntModuleDescriptor(moduleDescriptor);
        eaReport.setModuleDescriptor(md.getIvyModuleDescriptor());

        Project p = buildProject();
        Target implicitTarget = ProjectUtils.createTopLevelTarget();
        p.addTarget(implicitTarget);

        // calculate basedir
        if (moduleDescriptor != null) {
            p.setBaseDir(moduleDescriptor.getParentFile());
        } else if (optionalAntModule != null) {
            p.setBaseDir(optionalAntModule.getParentFile());
        } else if (overrideAntModule != null) {
            p.setBaseDir(overrideAntModule.getParentFile());
        }

        LoadModule loadModule = new LoadModule();
        loadModule.setBuildModule(moduleDescriptor);
        loadModule.setBuildFile(optionalAntModule);
        loadModule.setOwningTarget(implicitTarget);
        loadModule.setLocation(new Location(ProjectUtils.emulateMainScript(p).getAbsolutePath()));
        loadModule.setProject(p);
        loadModule.setTaskName("load-module");
        loadModule.execute();
        ProjectHelper projectHelper = ProjectUtils.getConfiguredProjectHelper(p);
        projectHelper.resolveExtensionOfAttributes(p);
        analyseProject(p, eaReport, "default");

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

        ModuleRevisionId criteria;

        if ((revision == null) || settings.getVersionMatcher().isDynamic(mrid)) {
            criteria = new ModuleRevisionId(new ModuleId(organisation, moduleName), branch, "*");
        } else {
            criteria = new ModuleRevisionId(new ModuleId(organisation, moduleName), branch, revision);
        }

        PatternMatcher patternMatcher = settings.getMatcher(matcher);
        if (PatternMatcher.ANY_EXPRESSION.equals(resolver)) {
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
        return ModuleRevisionId.parse(mrid);
    }

    public EasyAntReport generateEasyAntReport(File moduleDescriptor) throws Exception {
        return generateEasyAntReport(moduleDescriptor, null, null);
    }

    /**
     * Don't try to expand property on reports. Bypassing default property expander allow us to show real static value
     * of properties on reports.
     */
    private class BypassDefaultPropertyExpander implements PropertyExpander {

        public String parsePropertyName(String s, ParsePosition pos, ParseNextProperty notUsed) {
            int index = pos.getIndex();
            // directly check near, triggering characters:
            if (s.length() - index >= 3 && '$' == s.charAt(index) && '{' == s.charAt(index + 1)) {
                int start = index;

                // defer to String.indexOf() for protracted check:
                int end = s.indexOf('}', start);
                if (end < 0) {
                    throw new BuildException("Syntax error in property: " + s.substring(index));
                }

                // set marker after "}"
                pos.setIndex(end + 1);

                // allow to resolve path to property files
                // this is mandatory if we want report to contains properties loaded by
                // property file like done on buildtypes
                // in that case property needs to be really expanded so we need to strips "${" and "}" characters
                String strippedPropertyName = s.substring(start + 2, end);
                if (strippedPropertyName.endsWith("properties.file")) {

                    return strippedPropertyName;
                }

                // in other cases return the whole property with "${" and "}"
                return start == end ? "" : s.substring(start, end + 1);
            }
            return null;
        }
    }

    public void setOfflineMode(boolean offlineMode) {
        this.offlineMode = offlineMode;
    }
}
