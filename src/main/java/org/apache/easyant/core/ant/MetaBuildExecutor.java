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
package org.apache.easyant.core.ant;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.easyant.core.EasyAntConstants;
import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.easyant.core.ivy.IvyInstanceHelper;
import org.apache.easyant.tasks.SubModule;
import org.apache.easyant.tasks.SubModule.TargetList;
import org.apache.ivy.ant.IvyAntSettings;
import org.apache.ivy.ant.IvyBuildList;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.MDArtifact;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.ivy.plugins.repository.url.URLResource;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.helper.DefaultExecutor;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.selectors.FilenameSelector;

/**
 * Recursively executes build targets on sub-modules for a multi-module project.
 */
public class MetaBuildExecutor extends DefaultExecutor {

    @Override
    public void executeTargets(Project project, String[] targets)
            throws BuildException {

        // delegate to the ivy:buildlist task to compute an ordered list of
        // submodules
        FilenameSelector ivySelector = new FilenameSelector();
        ivySelector.setProject(project);
        ivySelector.setName("**/module.ivy");

        FileSet ivyFiles = (FileSet) project.createDataType("fileset");
        ivyFiles.setDir(project.getBaseDir());
        ivyFiles.setExcludes("module.ivy"); // exclude the root module ivy.
        ivyFiles.setIncludes(project.getProperty("submodule.dirs"));
        ivyFiles.addFilename(ivySelector);

        IvyBuildList buildList = new IvyBuildList();
        buildList.setTaskName("meta:buildlist");
        buildList.setProject(project);
        buildList.setReference("build-path");
        buildList.setIvyfilepath("module.ivy");
        buildList.setSettingsRef(IvyInstanceHelper
                .buildProjectIvyReference(project));
        buildList.addFileset(ivyFiles);

        buildList.execute();

        // publish the parent module descriptor into the build-scoped
        // repository, so that it can
        // be used by submodules if needed
        File moduleFile = new File(project.getBaseDir(), "module.ivy");
        String buildRepo = project
                .getProperty(EasyAntMagicNames.EASYANT_BUILD_REPOSITORY);
        if (moduleFile.isFile() && buildRepo != null) {

            project.log("Publishing module descriptor " + moduleFile.getPath()
                    + " to build repository '" + buildRepo + "'");

            IvyAntSettings projectSettings = IvyInstanceHelper
                    .getProjectIvyAntSettings(project);
            IvySettings ivy = projectSettings.getConfiguredIvyInstance(
                    buildList).getSettings();

            try {
                URL ivyUrl = moduleFile.toURL();
                ModuleDescriptorParser mdp = ModuleDescriptorParserRegistry
                        .getInstance().getParser(
                                new URLResource(moduleFile.toURL()));
                ModuleDescriptor descriptor = mdp.parseDescriptor(ivy, ivyUrl,
                        true);
                Artifact artifact = MDArtifact.newIvyArtifact(descriptor);
                ivy.getResolver(buildRepo).publish(artifact, moduleFile, true);
            } catch (Exception ioe) {
                throw new BuildException("Error publishing meta-module file "
                        + moduleFile.getAbsolutePath() + " to " + buildRepo,
                        ioe);
            }

        }

        // hook for pre-module-targets
        String preModuleTargets = project
                .getProperty(EasyAntMagicNames.PRE_MODULE_TARGETS);
        if (preModuleTargets == null) {
            preModuleTargets = EasyAntConstants.DEFAULT_PRE_MODULE_TARGETS;
        }

        List<String> postTargetsToRun = new ArrayList<String>();
        List<String> preTargetsToRun = new ArrayList<String>();
        List<String> preModuleTargetList = Arrays.asList(preModuleTargets
                .split(","));
        for (int i = 0; i < targets.length; i++) {
            if (preModuleTargetList.contains(targets[i])) {
                // fill a list of targets to run BEFORE subproject delegation
                preTargetsToRun.add(targets[i]);
            } else {
                // fill a list of target to run AFTER subproject delagation
                // make sure target exists
                if (project.getTargets().get(targets[i]) != null) {
                    postTargetsToRun.add(targets[i]);
                } else {
                    project.log("Skipping undefined target '" + targets[i] + "'",
                            Project.MSG_VERBOSE);
                }

            }
        }

        // now call the default executor to include any extra
        // targets defined in the root module.ant
        super.executeTargets(project, preTargetsToRun.toArray(new String[] {}));

        // delegate to the ea:submodule task to execute the list of targets on
        // all modules in the build list
        SubModule subModule = new SubModule();
        subModule.setTaskName("meta:submodule");
        subModule.setProject(project);

        Boolean useBuildRepository = project
                .getProperty(EasyAntMagicNames.USE_BUILD_REPOSITORY) != null ? Boolean
                .parseBoolean(project
                        .getProperty(EasyAntMagicNames.USE_BUILD_REPOSITORY))
                : true;
        subModule.setUseBuildRepository(useBuildRepository);

        subModule.setBuildpathRef(new Reference(project, "build-path"));
        subModule.setTargets(new TargetList(targets));
        subModule.execute();

        // now call the default executor to include any extra targets defined in
        // the root module.ant
        super
                .executeTargets(project, postTargetsToRun
                        .toArray(new String[] {}));
    }

}
