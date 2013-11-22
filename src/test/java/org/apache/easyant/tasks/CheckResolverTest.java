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
import java.net.URISyntaxException;

import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.easyant.core.ant.ProjectUtils;
import org.apache.easyant.core.ivy.IvyInstanceHelper;
import org.apache.ivy.ant.IvyConfigure;
import org.apache.tools.ant.Project;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class CheckResolverTest {

    private CheckResolver checkResolver = new CheckResolver();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() throws IOException, URISyntaxException {
        Project project = new Project();
        ProjectUtils.configureProjectHelper(project);

        File cache = folder.newFolder("build-cache");
        project.setProperty("ivy.cache.dir", cache.getAbsolutePath());

        IvyConfigure configure = new IvyConfigure();
        configure.setProject(project);

        File f = new File(this.getClass().getResource("/repositories/easyant-ivysettings-test.xml").toURI());
        configure.setFile(f);

        configure.setSettingsId(EasyAntMagicNames.EASYANT_IVY_INSTANCE);
        configure.execute();

        checkResolver = new CheckResolver();
        checkResolver.setSettingsRef(IvyInstanceHelper.buildEasyAntIvyReference(project));
        checkResolver.setProject(project);
    }

    @Test
    public void shouldFailIfNoMandatoryAttributesAreSet() {
        expectedException.expectMessage("resolver attribute is mandatory");
        checkResolver.execute();
    }

    @Test
    public void shouldFailIfResolverDoesntExists() {
        expectedException.expectMessage("resolver unknown does not exist in current project");
        checkResolver.getProject().setProperty("resolver.to.check", "unknown");
        checkResolver.setResolver("resolver.to.check");
        checkResolver.execute();
    }

    @Test
    public void shouldNotFailIfResolverExists() {
        checkResolver.getProject().setProperty("resolver.to.check", "test-plugin");
        checkResolver.setResolver("resolver.to.check");
        checkResolver.execute();
    }
}
