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

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

public class FindParentDirectory extends Task {
    
    private String filename=null;
    private String property=null;
    private String mode="highest";
    private boolean haltonmissing=true;
    private boolean haltonfirstfound=false;

    public void execute() throws BuildException {
        if (getFilename() == null || getProperty() == null) {
            throw new BuildException("filename and property attributes are mandatory");
        }
        File f = new File(getProject().getBaseDir(),getFilename());
        String highest = null;
        boolean canContinue=true;
        while (canContinue && f.getParentFile() !=null) {
            f = f.getParentFile();
            File child = new File(f, getFilename());
            log ("Searching "+ getFilename() + " in "+ child.getAbsolutePath(),Project.MSG_DEBUG);
            if (child.exists()) {
                log (getFilename() + " found !",Project.MSG_DEBUG);
                highest = f.getAbsolutePath();
                if (isHaltonfirstfound()) {
                    canContinue=false;
                }
            //at least halt when first is found
            } else if(highest !=null && isHaltonmissing()) {
                canContinue=false;
            }
        }
        if (highest == null) {
            throw new BuildException("Unable to find " + filename + " on parent directories");
        }
        getProject().setProperty(getProperty(), highest);
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public boolean isHaltonmissing() {
        return haltonmissing;
    }

    public void setHaltonmissing(boolean haltonmissing) {
        this.haltonmissing = haltonmissing;
    }

    public boolean isHaltonfirstfound() {
        return haltonfirstfound;
    }

    public void setHaltonfirstfound(boolean haltonfirstfound) {
        this.haltonfirstfound = haltonfirstfound;
    }
    
}
