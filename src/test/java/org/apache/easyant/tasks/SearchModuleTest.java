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

import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.easyant.core.ant.ProjectUtils;
import org.apache.easyant.core.ivy.IvyInstanceHelper;
import org.apache.ivy.ant.IvyConfigure;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SearchModuleTest {

    private final SearchModule searchModule = new SearchModule();

    private final FakeInputHandler fakeInputHandler = new FakeInputHandler();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() throws URISyntaxException, IOException {
        Project project = new Project();
        ProjectUtils.configureProjectHelper(project);

        File cache = folder.newFolder("build-cache");
        project.setProperty("ivy.cache.dir", cache.getAbsolutePath());

        IvyConfigure configure = new IvyConfigure();
        configure.setSettingsId(EasyAntMagicNames.EASYANT_IVY_INSTANCE);
        configure.setProject(project);
        configure.setFile(new File(this.getClass().getResource("/repositories/easyant-ivysettings-test.xml").toURI()));
        configure.execute();

        project.setBaseDir(new File(this.getClass().getResource("simple").toURI()));
        project.setInputHandler(fakeInputHandler);

        searchModule.setProject(project);
        searchModule.setOwningTarget(ProjectUtils.createTopLevelTarget());
        searchModule.setLocation(new Location(ProjectUtils.emulateMainScript(project).getAbsolutePath()));
        searchModule.setSettingsRef(IvyInstanceHelper.buildEasyAntIvyReference(project));
    }

    @Test
    public void shouldFailIfNoOrganisationIsGiven() {
        expectedException.expectMessage("no organisation provided for SearchModule task");
        searchModule.execute();
    }

    @Test
    public void shouldFailIfNoModuleIsGiven() {
        expectedException.expectMessage("no module name provided for SearchModule task");

        searchModule.setOrganisation("foo");
        searchModule.execute();
    }

    @Test
    public void shouldFailIfNoRevisionIsGiven() {
        expectedException.expectMessage("no revision provided for SearchModule task");

        searchModule.setOrganisation("foo");
        searchModule.setModule("bar");
        searchModule.execute();
    }

    @Test
    public void shouldFailIfNoPropertyPrefixIsGiven() {
        expectedException.expectMessage("no property prefix provided provided for SearchModule task");

        searchModule.setRevision("1.0");
        searchModule.setOrganisation("foo");
        searchModule.setModule("bar");
        searchModule.execute();
    }

    @Test
    public void shouldFailIfModuleNotFound() {
        expectedException.expectMessage("No matching module were found !");

        searchModule.setOrganisation("foo");
        searchModule.setModule("bar");
        searchModule.setRevision("1.0");
        searchModule.setPropertyPrefix("mysearch");
        searchModule.execute();
    }

    @Test
    public void shouldFindExistingModuleWithExactParameter() {
        searchModule.setOrganisation("mycompany");
        searchModule.setModule("simpleplugin");
        searchModule.setRevision("0.1");
        searchModule.setPropertyPrefix("mysearch");
        fakeInputHandler.setInput("0");
        searchModule.execute();
        assertThat(searchModule.getProject().getProperty("mysearch.org"), is("mycompany"));
        assertThat(searchModule.getProject().getProperty("mysearch.module"), is("simpleplugin"));
        assertThat(searchModule.getProject().getProperty("mysearch.rev"), is("0.1"));
    }

    @Test
    public void shouldFindExistingModuleWithRegexpParameter() {
        searchModule.setOrganisation("anothercompany");
        searchModule.setModule("*");
        searchModule.setRevision("*");
        searchModule.setPropertyPrefix("mysearch");
        fakeInputHandler.setInput("0");
        searchModule.execute();
        assertThat(searchModule.getProject().getProperty("mysearch.org"), is("anothercompany"));
        assertThat(searchModule.getProject().getProperty("mysearch.module"), is("simpleplugin"));
        assertThat(searchModule.getProject().getProperty("mysearch.rev"), is("0.1"));
    }

    @Test
    public void shouldFindExistingModuleOnGivenResolver() {
        searchModule.setOrganisation("mycompany");
        searchModule.setModule("simpleplugin");
        searchModule.setRevision("0.1");
        searchModule.setPropertyPrefix("mysearch");
        searchModule.setResolver("test-plugin");
        fakeInputHandler.setInput("0");
        searchModule.execute();
        assertThat(searchModule.getProject().getProperty("mysearch.org"), is("mycompany"));
        assertThat(searchModule.getProject().getProperty("mysearch.module"), is("simpleplugin"));
        assertThat(searchModule.getProject().getProperty("mysearch.rev"), is("0.1"));
    }

}
