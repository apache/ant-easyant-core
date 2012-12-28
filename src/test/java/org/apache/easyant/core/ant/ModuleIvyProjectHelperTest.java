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
import java.net.URISyntaxException;

import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.ProjectHelperRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ModuleIvyProjectHelperTest {
    @Before
    public void setUp() {
        // ProjectHelperRepository.getInstance().registerProjectHelper("org.apache.easyant.core.ant.EasyAntProjectHelper");

        ProjectHelperRepository.getInstance().registerProjectHelper(
                "org.apache.easyant.core.ant.ModuleIvyProjectHelper");
    }

    @Test
    public void shouldHandleModuleIvyFile() throws URISyntaxException {
        File f = new File(this.getClass().getResource("../standardJavaProject.ivy").toURI());
        Project p = new Project();
        p.setNewProperty(EasyAntMagicNames.IGNORE_USER_IVYSETTINGS, "true");
        p.setNewProperty(EasyAntMagicNames.GLOBAL_EASYANT_IVYSETTINGS,
                this.getClass().getResource("/ivysettings-test.xml").toString());
        ProjectHelper.configureProject(p, f);
        Assert.assertNotNull(p.getTargets().get("clean"));
    }
}
