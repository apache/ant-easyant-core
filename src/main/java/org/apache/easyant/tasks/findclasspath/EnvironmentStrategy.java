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
package org.apache.easyant.tasks.findclasspath;

import java.io.File;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;

public class EnvironmentStrategy extends AbstractFindClassPathStrategy {

    private String env;
    private String layout = "/lib";
    private String filter;

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getLayout() {
        return layout;
    }

    public void setLayout(String layout) {
        this.layout = layout;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    /**
     * check environment variable
     * 
     * @return return true if the environment variable exist
     */
    protected boolean doCheck() {
        log("Checking environment variable ...", Project.MSG_VERBOSE);

        if (getEnv() != null && System.getenv(getEnv()) != null) {
            log(getEnv() + " found !", Project.MSG_VERBOSE);
            File libDir = new File(System.getenv(getEnv()), getLayout());
            FileSet fileSet = new FileSet();
            fileSet.setDir(libDir);
            fileSet.setIncludes(getFilter());
            fileSet.setProject(getProject());
            fileSet.setLocation(getLocation());

            getPath().addFileset(fileSet);
            getProject().addReference(getPathid(), getPath());
            return true;
        }
        return false;

    }

}
