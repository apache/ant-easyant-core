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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.apache.tools.ant.Project;

public class AntTaskBaseTest {

    protected AntTestListener antTestListener;

    public Project configureProject(Project project) {
        return configureBuildLogger(project, Project.MSG_INFO);
    }

    public Project configureBuildLogger(Project project, int logLevel) {
        antTestListener = new AntTestListener(logLevel);
        project.addBuildListener(antTestListener);
        return project;
    }

    public void assertLogContaining(String substring) {
        checkAntListener();
        String realLog = antTestListener.getLog();
        assertThat(realLog, containsString(substring));
    }

    public void assertLogNotContaining(String substring) {
        checkAntListener();
        String realLog = antTestListener.getLog();
        assertThat(realLog, not(containsString(substring)));
    }
    
    public void assertFileExists(File root, String relativeFilename){
        File file = new File(root.getAbsolutePath() + relativeFilename);
        assertThat(file.exists(), is(true));
    }

    private void checkAntListener() {
        if (antTestListener == null) {
            throw new IllegalStateException(
                    "Project is not properly configure, please invoke configureProject method first");
        }
    }

}
