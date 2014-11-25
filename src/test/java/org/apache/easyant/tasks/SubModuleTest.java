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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.easyant.core.ant.listerners.MultiModuleLogger;
import org.apache.easyant.tasks.SubModule.TargetList;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class SubModuleTest extends AntTaskBaseTest {

    private SubModule submodule;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setUp() throws URISyntaxException, IOException {
        Project project = new Project();

        File cache = folder.newFolder("build-cache");
        project.setUserProperty("ivy.cache.dir", cache.getAbsolutePath());

        File f = new File(this.getClass().getResource("/repositories/easyant-ivysettings-test.xml").toURI());
        // FIXME: property are not yet inherited
        project.setUserProperty(EasyAntMagicNames.USER_EASYANT_IVYSETTINGS, f.getAbsolutePath());

        submodule = new SubModule();
        submodule.setProject(project);
    }

    @Test
    public void shouldFailIfNoMandatoryAttributesAreSet() {
        expectedException.expectMessage("No buildpath specified");
        submodule.execute();
    }

    @Test
    public void shouldNotFailIfBuildpathAttributeIsSet() {
        configureBuildLogger(submodule.getProject(), Project.MSG_WARN);

        Path path = new Path(submodule.getProject());
        path.createPath();

        submodule.setBuildpath(path);
        submodule.execute();

        assertLogContaining("No sub-builds to iterate on");
    }

    @Test
    public void shouldFailIfPathContainsInvalidFile() {
        expectedException.expectMessage("Invalid file:");

        configureBuildLogger(submodule.getProject(), Project.MSG_WARN);

        Path path = new Path(submodule.getProject());
        File file2 = new File("anotherfile");
        path.createPathElement().setLocation(file2);
        path.createPath();

        submodule.setBuildpath(path);
        submodule.execute();

    }

    @Test
    public void shouldRunEvenIfNoTargetsAreSet() throws URISyntaxException {
        configureBuildLogger(submodule.getProject(), Project.MSG_DEBUG);

        Path path = new Path(submodule.getProject());
        FileSet fs = new FileSet();
        File multimodule = new File(this.getClass().getResource("multimodule").toURI());
        fs.setDir(multimodule);
        path.addFileset(fs);
        path.createPath();

        submodule.setBuildpath(path);
        submodule.execute();

        assertLogContaining("Executing [] on module1");
        assertLogContaining("Executing [] on module2");
        assertLogContaining("Skipping sub-project build because no matching targets were found");

    }

    @Test
    public void shouldRunEvenIfTargetDoesntExistsInSubModules() throws URISyntaxException {
        configureBuildLogger(submodule.getProject(), Project.MSG_DEBUG);

        Path path = new Path(submodule.getProject());
        FileSet fs = new FileSet();
        File multimodule = new File(this.getClass().getResource("multimodule").toURI());
        fs.setDir(multimodule);
        path.addFileset(fs);
        path.createPath();

        submodule.setBuildpath(path);
        submodule.setTarget("a-missing-target");
        submodule.execute();

        assertLogContaining("Executing [a-missing-target] on module1");
        assertLogContaining("Skipping undefined target 'a-missing-target' on module1");
        assertLogContaining("Executing [a-missing-target] on module2");
        assertLogContaining("Skipping undefined target 'a-missing-target' on module2");
        assertLogContaining("Skipping sub-project build because no matching targets were found");

    }

    @Test
    public void shouldRunMyTargetOnBothModule() throws URISyntaxException {
        configureBuildLogger(submodule.getProject(), Project.MSG_DEBUG);

        Path path = new Path(submodule.getProject());
        FileSet fs = new FileSet();
        File multimodule = new File(this.getClass().getResource("multimodule").toURI());
        fs.setDir(multimodule);
        path.addFileset(fs);
        path.createPath();

        submodule.setBuildpath(path);
        submodule.setTarget("modulewithtarget:mytarget");
        submodule.execute();

        assertLogContaining("Executing [modulewithtarget:mytarget] on module1");
        assertLogContaining("Executing [modulewithtarget:mytarget] on module2");

        assertThat(submodule.getProject().getReference(MultiModuleLogger.EXECUTION_TIMER_BUILD_RESULTS),
                notNullValue());
    }
        
    @Test
    public void shouldRunTargetInRightOrder() throws URISyntaxException {
        configureBuildLogger(submodule.getProject(), Project.MSG_DEBUG);
        
        Path path = new Path(submodule.getProject());
        FileSet fs = new FileSet();
        File multimodule = new File(this.getClass().getResource("multimodule").toURI());
        fs.setDir(multimodule);
        path.addFileset(fs);
        path.createPath();
        
        submodule.setBuildpath(path);
        submodule.setTargets(new TargetList("modulewithtarget:firstTarget", "modulewithtarget:secondTarget", "modulewithtarget:thirdTarget"));
        submodule.execute();
                
        assertLogContaining("Executing [modulewithtarget:firstTarget, modulewithtarget:secondTarget, modulewithtarget:thirdTarget] on module1");
        assertLogContaining("Executing [modulewithtarget:firstTarget, modulewithtarget:secondTarget, modulewithtarget:thirdTarget] on module2");
        assertLogContaining("ant.project.invoked-targets -> modulewithtarget:firstTarget,modulewithtarget:secondTarget,modulewithtarget:thirdTarget");
        assertLogContaining("project.executed.targets -> modulewithtarget:firstTarget,modulewithtarget:secondTarget,modulewithtarget:thirdTarget");
        assertLogContaining("firstProperty=first secondProperty=second thirdProperty=third");
        
        assertThat(submodule.getProject().getReference(MultiModuleLogger.EXECUTION_TIMER_BUILD_RESULTS),
                notNullValue());
    }
}
