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

import org.apache.easyant.core.ant.ProjectUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class PathTaskTest {

    private PathTask pathTask = new PathTask();
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() throws MalformedURLException, URISyntaxException {
        Project project = new Project();
        ProjectUtils.configureProjectHelper(project);

        pathTask = new PathTask();
        pathTask.setProject(project);
    }

    @Test(expected = BuildException.class)
    public void shouldFailIfNoMandatoryAttributesAreSet() {
        pathTask.execute();
    }

    @Test
    public void shouldCreatePath() {
        String classpathName = "myclasspath";
        pathTask.setPathid(classpathName);
        pathTask.execute();
        Path classpath = pathTask.getProject().getReference(classpathName);
        Assert.assertNotNull(classpath);
        Assert.assertEquals(0, classpath.size());
    }

    @Test
    public void shouldOverridePathAndPrependElements() {
        String classpathName = "myclasspath";

        Path originalClasspath = new Path(pathTask.getProject());
        File file1 = new File("afile");
        originalClasspath.createPathElement().setLocation(file1);
        pathTask.getProject().addReference(classpathName, originalClasspath);

        Path classpath = pathTask.getProject().getReference(classpathName);
        Assert.assertNotNull(classpath);
        Assert.assertEquals(1, classpath.list().length);

        pathTask.setPathid(classpathName);
        File file2 = new File("anotherfile");
        pathTask.createPathElement().setLocation(file2);
        pathTask.setOverwrite("prepend");
        pathTask.execute();

        classpath = pathTask.getProject().getReference(classpathName);
        Assert.assertNotNull(classpath);
        Assert.assertEquals(2, classpath.list().length);
        Assert.assertEquals(file2.getAbsolutePath(), classpath.list()[0]);
        Assert.assertEquals(file1.getAbsolutePath(), classpath.list()[1]);

    }

    @Test
    public void shouldOverridePathAndAppendElements() {
        String classpathName = "myclasspath";

        Path originalClasspath = new Path(pathTask.getProject());
        File file1 = new File("afile");
        originalClasspath.createPathElement().setLocation(file1);
        pathTask.getProject().addReference(classpathName, originalClasspath);

        Path classpath = pathTask.getProject().getReference(classpathName);
        Assert.assertNotNull(classpath);
        Assert.assertEquals(1, classpath.list().length);

        pathTask.setPathid(classpathName);
        File file2 = new File("anotherfile");
        pathTask.createPathElement().setLocation(file2);
        pathTask.execute();

        classpath = pathTask.getProject().getReference(classpathName);
        Assert.assertNotNull(classpath);
        Assert.assertEquals(2, classpath.list().length);
        Assert.assertEquals(file1.getAbsolutePath(), classpath.list()[0]);
        Assert.assertEquals(file2.getAbsolutePath(), classpath.list()[1]);

    }

    @Test
    public void shouldOverridePath() {
        String classpathName = "myclasspath";

        Path originalClasspath = new Path(pathTask.getProject());
        File file1 = new File("afile");
        originalClasspath.createPathElement().setLocation(file1);
        pathTask.getProject().addReference(classpathName, originalClasspath);

        Path classpath = pathTask.getProject().getReference(classpathName);
        Assert.assertNotNull(classpath);
        Assert.assertEquals(1, classpath.list().length);
        Assert.assertEquals(file1.getAbsolutePath(), classpath.list()[0]);

        pathTask.setPathid(classpathName);
        File file2 = new File("anotherfile");
        pathTask.setOverwrite("true");
        pathTask.createPathElement().setLocation(file2);
        pathTask.execute();

        classpath = pathTask.getProject().getReference(classpathName);
        Assert.assertNotNull(classpath);
        Assert.assertEquals(1, classpath.list().length);
        Assert.assertEquals(file2.getAbsolutePath(), classpath.list()[0]);

    }

    @Test
    public void shouldNotOverridePathIfItAlreadyExists() {
        String classpathName = "myclasspath";

        Path originalClasspath = new Path(pathTask.getProject());
        File file1 = new File("afile");
        originalClasspath.createPathElement().setLocation(file1);
        pathTask.getProject().addReference(classpathName, originalClasspath);

        Path classpath = pathTask.getProject().getReference(classpathName);
        Assert.assertNotNull(classpath);
        Assert.assertEquals(1, classpath.list().length);
        Assert.assertEquals(file1.getAbsolutePath(), classpath.list()[0]);

        pathTask.setPathid(classpathName);
        File file2 = new File("anotherfile");
        pathTask.setOverwrite("false");
        pathTask.createPathElement().setLocation(file2);
        pathTask.execute();

        classpath = pathTask.getProject().getReference(classpathName);
        Assert.assertNotNull(classpath);
        Assert.assertEquals(1, classpath.list().length);
        Assert.assertEquals(file1.getAbsolutePath(), classpath.list()[0]);
    }

    @Test
    public void shouldFailIfPrependDoesntMatchWithExistingPath() {
        expectedException.expectMessage("destination path not found: aMissingClasspath");
        pathTask.setPathid("aMissingClasspath");
        pathTask.setOverwrite("prepend");
        pathTask.execute();
    }

    @Test
    public void shouldFailIfAppendDoesntMatchWithExistingPath() {
        expectedException.expectMessage("destination path not found: aMissingClasspath");
        pathTask.setPathid("aMissingClasspath");
        pathTask.setOverwrite("append");
        pathTask.execute();

    }

    @Test
    public void shouldFailIfPathIdExistsButIsNotAPath() {
        expectedException.expectMessage("destination path is not a path: class java.lang.String");
        pathTask.getProject().addReference("aReference", "plop");
        pathTask.setPathid("aReference");
        pathTask.execute();

    }

}
