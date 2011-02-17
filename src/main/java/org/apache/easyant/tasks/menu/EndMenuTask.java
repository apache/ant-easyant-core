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
package org.apache.easyant.tasks.menu;

import org.apache.easyant.core.menu.MenuGeneratorRegistry;
import org.apache.tools.ant.BuildException;

import java.io.IOException;

public class EndMenuTask extends AbstractMenuGeneratorTask {
    @Override
    public void execute() throws BuildException {
        String context = getContext();
        if (context == null) {
            throw new BuildException("context attribute is required");
        }
        MenuGeneratorRegistry registry = getMenuGeneratorForContext(context);
        try {
            registry.endMenu();
        } catch (IOException ioe) {
            throw new BuildException("Error ending menu " + context, ioe);
        }
    }
}
