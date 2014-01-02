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

public class StandardJavaProjectTest extends EasyAntBaseTest {

    @Before
    public void setUp() {
        configureAndInitProject(this.getResource("standardJavaProject.ivy"), Project.MSG_INFO);
        cleanTargetDirectory();
    }

    @Test
    public void shouldInvokeMyTarget() {
        executeTarget("modulewithtarget:mytarget");
    }

    @Test
    public void shouldInvokePackage() {
        executeTarget("package");
    }

    @Test
    public void shouldImportWithoutAsAttribute() {
        // <ea:plugin organisation="mycompany" module="modulewithtarget:mytarget" revision="0.1"/>
        // no "as" attribute is specified, easyant should prefix all targets with "module" value by default
        executeTarget("modulewithtarget:mytarget");
    }

    @Test
    public void shouldImportWithAsAttribute() {
        // <ea:plugin organisation="mycompany" module="modulewithtarget:mytarget" revision="0.1" as="foobar"/>
        executeTarget("foobarmodulewithtarget:mytarget");
    }

    @Test
    public void shouldOverrideExistingProperty() {
        assertPropertyEquals("myproperty", "newvalue");
    }

}
