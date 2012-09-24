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

public class StandardJavaProjectTest extends EasyAntBaseTest {

    protected void setUp() throws Exception {
        configureProject(this.getResource("standardJavaProject.ivy"), Project.MSG_INFO);

        // Configure easyant ivy instance
        conf.setEasyantIvySettingsUrl(this.getClass().getResource("/ivysettings-test.xml"));

        // init project with easyant configuration
        initProject();
    }

    public void testClean() throws Exception {
        executeTarget("clean");
    }

    public void testValidate() throws Exception {
        expectPropertySet("validate", "default.build.number", "10");
    }

    public void testPackage() throws Exception {
        testClean();
        executeTarget("package");
    }

    public void testImportWithoutAsAttribute() throws Exception {
        testClean();
        // <ea:plugin module="javadoc" revision="0.1"/>
        // no "as" attribute is specified, easyant should prefix all targets with "module" value by default
        executeTarget("javadoc:javadoc");
    }

    public void testImportWithAsAttribute() throws Exception {
        testClean();
        // <ea:plugin module="javadoc" revision="0.1" as="foobar"/>
        executeTarget("foobarjavadoc:javadoc");
    }

    public void testVerify() throws Exception {
        testClean();
        executeTarget("verify");
    }

}
