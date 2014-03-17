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
package org.apache.easyant.core.ant.helper;

import java.io.File;

import org.apache.easyant.core.EasyAntEngine;
import org.apache.easyant.core.configuration.EasyAntConfiguration;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.taskdefs.Ant;
import org.apache.tools.ant.types.Resource;

/**
 * This project helper can parse module.ivy files. It relies on {@link EasyAntEngine} under the hood It is designed to
 * support tasks invoking {@link ProjectHelper}.configureProject(Project project, File buildFile) such as {@link Ant}.
 * 
 */
public class ModuleIvyProjectHelper extends ProjectHelper {

    @Override
    public void parse(Project project, Object source) throws BuildException {
        File buildFile = null;

        if (source instanceof File) {
            buildFile = (File) source;
        } else {
            throw new BuildException("Resource type not yet supported: " + source.getClass().getCanonicalName());
        }

        EasyAntConfiguration eaConfiguration = new EasyAntConfiguration();
        eaConfiguration.setBuildModule(buildFile);

        EasyAntEngine.configureAndLoadProject(project, eaConfiguration);
    }

    @Override
    public boolean canParseBuildFile(Resource buildFile) {
        return buildFile.getName().endsWith(".ivy");
    }

    @Override
    public boolean canParseAntlibDescriptor(Resource r) {
        return false;
    }

}
