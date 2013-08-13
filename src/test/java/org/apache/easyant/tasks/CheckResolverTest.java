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
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.easyant.core.ant.ProjectUtils;
import org.apache.easyant.core.ivy.IvyInstanceHelper;
import org.apache.ivy.ant.IvyConfigure;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CheckResolverTest {

    private File cache;
    private CheckResolver checkResolver = new CheckResolver();

    @Before
    public void setUp() throws MalformedURLException, URISyntaxException {
        createCache();
        Project project = new Project();
        ProjectUtils.configureProjectHelper(project);
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

    private void createCache() {
        cache = new File("build/cache");
        cache.mkdirs();
    }

    @After
    public void tearDown() throws Exception {
        cleanCache();
    }

    private void cleanCache() {
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(cache);
        del.execute();
    }

    @Test(expected = BuildException.class)
    public void shouldFailIfNoMandatoryAttributesAreSet() {
        checkResolver.execute();
    }

    @Test(expected = BuildException.class)
    public void shouldFailIfResolverDoesntExists() {
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
