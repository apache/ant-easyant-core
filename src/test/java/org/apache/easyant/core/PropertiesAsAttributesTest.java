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
package org.apache.easyant.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.apache.tools.ant.Project;
import org.junit.Before;
import org.junit.Test;

public class PropertiesAsAttributesTest extends EasyAntBaseTest {

    @Before
    public void setUp() {
        configureAndInitProject(this.getResource("propertiesAsAttributes.ivy"), Project.MSG_INFO);
    }

    @Test
    public void shouldHandlePropertiesInBuildType() {
        assertPropertyEquals("my.property.inbuildtype", "true");
        // properties loaded by build configuration
        assertPropertyUnset("my.property.inconf");
    }

    @Test
    public void shouldHandlePropertiesInPlugin() {
        assertPropertyEquals("my.property.inplugin", "true");

        // properties loaded by build configuration
        assertPropertyUnset("my.property.inconf");
    }

    @Test
    public void shouldHandlePropertiesInConfigureProject() {
        assertThat(project.getDefaultTarget(), is("package"));
        assertPropertyEquals("my.property.inconfigureproject", "true");
    }

    @Test
    public void shouldHandlePropertiesInBuildConfiguration() {
        conf.getActiveBuildConfigurations().add("myBuild");

        // re-init project with easyant configuration including build types
        initProject();

        assertPropertyEquals("my.property.inplugin", "true");

        // properties loaded by build configuration
        assertPropertyEquals("my.property.inconf", "true");
    }
}
