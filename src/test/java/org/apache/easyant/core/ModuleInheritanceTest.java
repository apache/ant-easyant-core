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

public class ModuleInheritanceTest extends EasyAntBaseTest {
	
	protected void setUp() throws Exception {
		configureProject(this.getResource("multimodule/myapp-core/module.ivy"),Project.MSG_INFO);
		
		//Configure easyant ivy instance
		conf.setEasyantIvySettingsUrl(this.getClass().getResource("/org/apache/easyant/core/default-easyant-ivysettings.xml"));

		//init project with easyant configuration
		initProject();
	}
	
	public void clean() throws Exception {
		executeTarget("clean:clean");
	}
    
    public void testInheritablePluginWithScopeChild() throws Exception {
    	clean();
    	executeTarget("source-jar:init");
    }
    
    public void testNonInheritableElements() throws Exception {
    	clean();
    	expectBuildException("eadoc:init", "Target \"eadoc:init\" does not exist in the project \"myapp-core\"");
    	expectPropertyUnset("validate", "my.property");
    }
    
    
    
}
