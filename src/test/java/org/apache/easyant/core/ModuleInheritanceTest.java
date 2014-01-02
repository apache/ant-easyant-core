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

public class ModuleInheritanceTest extends EasyAntBaseTest {

    @Before
    public void setUp() {
        configureAndInitProject(this.getResource("multimodule/myapp-core/module.ivy"), Project.MSG_INFO);
        cleanTargetDirectory();
    }

    @Test
    public void shouldInheritProperty() {
        assertPropertyEquals("test.property", "myvalue");
    }

    @Test
    public void shouldInheritPluginWithScopeChild() {
        executeTarget("modulewithtarget:mytarget");
    }

    @Test
    public void shouldNotInheritElements() {
        expectPropertyUnset("package", "aproperty");
    }

    @Test
    public void shouldInheritBindTarget() {
        // this property is loaded by modulewithtarget:mytarget and should be bind to validate extensionPoint only if
        // bindtarget
        // has been inherited
        expectPropertySet("package", "apropertyinmytarget", "foobar");
    }
}
