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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.tools.ant.Project;
import org.junit.Before;
import org.junit.Test;

public class PropertyTaskTest extends AntTaskBaseTest {

    private PropertyTask propertyTask;

    @Before
    public void setUp() {
        Project project = new Project();

        propertyTask = new PropertyTask();
        propertyTask.setProject(project);
    }

    @Test
    public void shouldCreateProperty() {
        propertyTask.setName("a-property");
        propertyTask.setValue("a-value");
        propertyTask.execute();

        assertThat(propertyTask.getProject().getProperty("a-property"), equalTo("a-value"));
    }

    @Test
    public void shouldNotCreatePropertyWhenConfigurationDoesntMatch() {
        configureBuildLogger(propertyTask.getProject(), Project.MSG_DEBUG);

        propertyTask.getProject().setProperty(EasyAntMagicNames.AVAILABLE_BUILD_CONFIGURATIONS, "aBuildConfNotActive");

        propertyTask.setName("a-property");
        propertyTask.setValue("a-value");
        propertyTask.setConf("aBuildConfNotActive");
        propertyTask.execute();

        assertThat(propertyTask.getProject().getProperty("a-property"), is(nullValue()));
        assertLogContaining("this property will be skipped ");
    }

    @Test
    public void shouldCreatePropertyWhenConfigurationMatch() {
        configureBuildLogger(propertyTask.getProject(), Project.MSG_DEBUG);

        propertyTask.getProject().setProperty(EasyAntMagicNames.AVAILABLE_BUILD_CONFIGURATIONS, "aBuildConfActive");
        propertyTask.getProject().setProperty(EasyAntMagicNames.MAIN_CONFS, "aBuildConfActive");

        propertyTask.setName("a-property");
        propertyTask.setValue("a-value");
        propertyTask.setConf("aBuildConfActive");
        propertyTask.execute();

        assertThat(propertyTask.getProject().getProperty("a-property"), equalTo("a-value"));
    }

}
