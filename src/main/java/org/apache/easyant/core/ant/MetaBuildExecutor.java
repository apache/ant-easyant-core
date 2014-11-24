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

import org.apache.easyant.core.EasyAntConstants;
import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.easyant.tasks.SubModule;
import org.apache.easyant.tasks.SubModule.TargetList;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.helper.DefaultExecutor;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Recursively executes build targets on sub-modules for a multi-module project.
 */
public class MetaBuildExecutor extends DefaultExecutor {

    private static final String DEMARKER = "======================================================================";

    @Override
    public void executeTargets(Project project, String[] targets) throws BuildException {

        ProjectUtils.installMultiModuleLogger(project);

        if (project.getReference("build-path") == null) {
            throw new BuildException("build-path is required to use MetaBuildExecutor");
        }

        // hook for pre-module-targets
        String preModuleTargets = project.getProperty(EasyAntMagicNames.PRE_MODULE_TARGETS);
        if (preModuleTargets == null) {
            preModuleTargets = EasyAntConstants.DEFAULT_PRE_MODULE_TARGETS;
        }

        List<String> postTargetsToRun = new ArrayList<String>();
        List<String> preTargetsToRun = new ArrayList<String>();
        List<String> preModuleTargetList = Arrays.asList(preModuleTargets.split(","));
        for (String target : targets) {
            if (preModuleTargetList.contains(target)) {
                // fill a list of targets to run BEFORE subproject delegation
                preTargetsToRun.add(target);
            } else {
                // fill a list of target to run AFTER subproject delagation
                // make sure target exists
                if (project.getTargets().get(target) != null) {
                    postTargetsToRun.add(target);
                } else {
                    project.log("Skipping undefined target '" + target + "'", Project.MSG_VERBOSE);
                }

            }
        }

        // now call the default executor to include any extra
        // targets defined in the root module.ant
        super.executeTargets(project, preTargetsToRun.toArray(new String[preTargetsToRun.size()]));

        printSubBuildsInOrder(project);
        // delegate to the ea:submodule task to execute the list of targets on
        // all modules in the build list
        SubModule subModule = new SubModule();
        subModule.setTaskName("meta:submodule");
        subModule.setProject(project);

        Boolean useBuildRepository = project.getProperty(EasyAntMagicNames.USE_BUILD_REPOSITORY) != null ? Boolean
                .parseBoolean(project.getProperty(EasyAntMagicNames.USE_BUILD_REPOSITORY)) : true;
        subModule.setUseBuildRepository(useBuildRepository);

        subModule.setBuildpathRef(new Reference(project, "build-path"));
        subModule.setTargets(new TargetList(targets));
        subModule.execute();

        // now call the default executor to include any extra targets defined in
        // the root module.ant
        super.executeTargets(project, postTargetsToRun.toArray(new String[postTargetsToRun.size()]));
    }

    /*
     * informs all the sub-modules that will be built, in the order they will be built
     */
    private void printSubBuildsInOrder(Project project) {
        // print all sub modules and order in which they will
        // be executed
        String sortedModules = project.getProperty("ivy.sorted.modules");
        if (sortedModules != null && !sortedModules.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(DEMARKER).append(StringUtils.LINE_SEP);
            sb.append("Build Order for Sub Modules").append(StringUtils.LINE_SEP);
            sb.append(DEMARKER).append(StringUtils.LINE_SEP);
            String[] subModules = sortedModules.split("\\,");
            for (String subModule : subModules) {
                sb.append(" * ").append(subModule.trim()).append(StringUtils.LINE_SEP);
            }
            sb.append(DEMARKER);
            project.log(sb.toString());
        }
    }

}
