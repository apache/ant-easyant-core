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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.ProjectHelperRepository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ModuleIvyProjectHelperTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setUp() {
        // ProjectHelperRepository.getInstance().registerProjectHelper("org.apache.easyant.core.ant.EasyAntProjectHelper");

        ProjectHelperRepository.getInstance().registerProjectHelper(
                "org.apache.easyant.core.ant.helper.ModuleIvyProjectHelper");
    }

    @Test
    public void shouldHandleModuleIvyFile() throws URISyntaxException, IOException {
        File f = new File(this.getClass().getResource("../../simpleproject.ivy").toURI());
        Project p = new Project();
        // disable project ivy instance
        p.setNewProperty(EasyAntMagicNames.PROJECT_IVY_INSTANCE, EasyAntMagicNames.EASYANT_IVY_INSTANCE);
        p.setNewProperty(EasyAntMagicNames.IGNORE_USER_IVYSETTINGS, "true");
        p.setNewProperty(EasyAntMagicNames.GLOBAL_EASYANT_IVYSETTINGS,
                this.getClass().getResource("/repositories/easyant-ivysettings-test.xml").toString());
        p.setProperty("ivy.cache.dir", folder.newFolder("build-cache").getAbsolutePath());
        ProjectHelper.configureProject(p, f);
        assertThat(p.getTargets().get("complexplugin:mytarget"), is(notNullValue()));
        assertThat(p.getProperty("myproperty"), is("foobar"));

    }
}
