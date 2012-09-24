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

import org.apache.tools.ant.Project;
import org.junit.Before;
import org.junit.Test;

public class PropertiesAsAttributesTest extends EasyAntBaseTest {

    @Before
    public void setUp() throws Exception {

        configureProject(this.getResource("propertiesAsAttributes.ivy"), Project.MSG_INFO);

        // Configure easyant ivy instance
        conf.setEasyantIvySettingsUrl(this.getClass().getResource("/ivysettings-test.xml"));

        // init project with easyant configuration
        initProject();
    }

    @Test
    public void testClean() throws Exception {
        executeTarget("clean");
    }

    @Test
    public void testPropertiesInBuildType() throws Exception {
        expectPropertySet("validate", "my.property.inbuildtype", "true");

        // properties loaded by build configuration
        expectPropertyUnset("validate", "my.property.inconf");
    }

    @Test
    public void testPropertiesInPlugin() throws Exception {
        expectPropertySet("validate", "my.property.inplugin", "true");

        // properties loaded by build configuration
        expectPropertyUnset("validate", "my.property.inconf");
    }

    @Test
    public void testPropertiesInBuildConfiguration() throws Exception {
        conf.getActiveBuildConfigurations().add("myBuild");

        // re-init project with easyant configuration including build types
        initProject();

        expectPropertySet("validate", "my.property.inplugin", "true");

        // properties loaded by build configuration
        expectPropertySet("validate", "my.property.inconf", "true");
    }

    @Test
    public void testVerify() throws Exception {
        testClean();
        executeTarget("verify");
    }

}
