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
 */package org.apache.easyant.tasks;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ParameterTaskTest {

    private ParameterTask parameterTask;

    @Before
    public void setUp() {
        parameterTask = new ParameterTask();
        parameterTask.setProject(new Project());
    }

    @Test(expected = BuildException.class)
    public void shouldFailIfNoMandatoryAttributesAreSet() {
        parameterTask.execute();
    }

    @Test
    public void shouldCreatePropertyWithDefaultValue() {
        parameterTask.setProperty("a-property");
        parameterTask.setDefault("default-value");
        parameterTask.execute();

        String property = parameterTask.getProject().getProperty("a-property");
        Assert.assertNotNull(property);
        Assert.assertEquals("default-value", property);
    }

    @Test
    public void shouldNotChangeExistingPropertyWithDefaultValue() {
        String existingPropertyName = "existing-property";
        parameterTask.getProject().setProperty(existingPropertyName, "a-value");
        parameterTask.setProperty(existingPropertyName);
        parameterTask.setDefault("default-value");
        parameterTask.execute();

        String property = parameterTask.getProject().getProperty(existingPropertyName);
        Assert.assertNotNull(property);
        Assert.assertEquals("a-value", property);
    }

    @Test(expected = BuildException.class)
    public void shouldFailIfPropertyIsRequired() {
        parameterTask.setProperty("a-property");
        parameterTask.setRequired(true);
        parameterTask.execute();

    }

    @Test(expected = BuildException.class)
    public void shouldFailIfPropertyIsRequiredWithDescription() {
        parameterTask.setProperty("a-property");
        parameterTask.setDescription("a property can be documented");
        parameterTask.setRequired(true);
        parameterTask.execute();

    }

    @Test
    public void shouldSetPropertyWithAPossibleValue() {
        parameterTask.getProject().setProperty("a-property", "false");
        parameterTask.setProperty("a-property");
        parameterTask.setPossibleValues("true,false");
        parameterTask.execute();

        String property = parameterTask.getProject().getProperty("a-property");
        Assert.assertNotNull(property);
        Assert.assertEquals("false", property);

    }

    @Test(expected = BuildException.class)
    public void shouldFailToSetPropertyWithANonPossibleValue() {
        parameterTask.getProject().setProperty("a-property", "a-value");
        parameterTask.setProperty("a-property");
        parameterTask.setPossibleValues("true,false");
        parameterTask.execute();
    }

    @Test(expected = BuildException.class)
    public void shouldFailIfRequiredPathIsMissing() {
        parameterTask.setPath("a-path-id");
        parameterTask.setRequired(true);
        parameterTask.execute();
    }

    @Test(expected = BuildException.class)
    public void shouldFailIfGivenPathIdIsNotAPath() {
        parameterTask.getProject().addReference("a-path-id", true);
        parameterTask.setPath("a-path-id");
        parameterTask.execute();
    }

    @Test
    public void shouldNotFailIfGivenPathIdIsAPath() {
        parameterTask.getProject().addReference("a-path-id", new Path(parameterTask.getProject()));
        parameterTask.setPath("a-path-id");
        parameterTask.execute();
    }

    @Test
    @Deprecated
    // phase attribute is maintained for backward compatibility with plugins prior to easyant 0.9
    public void shouldNotFailIfPhaseAttributeIsUsed() {
        parameterTask.setPhase("phase");
        parameterTask.execute();
    }
}