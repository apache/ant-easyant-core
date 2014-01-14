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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.DirSet;
import org.apache.tools.ant.types.FileList;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Path.PathElement;

/**
 * This task defines or contributes to an existing path. This task is similar to path task provided by ant but add an
 * override attribute with these values: true: new definition will take precedence over preceding one if any false: new
 * definition will be discarded if any definition already exists append: new definition will be added to the existing
 * one if any prepend: new definition will be added at the beginning of the existing one if any
 */
public class PathTask extends Task {
    public static final String OVERWRITE_TRUE = "true";
    public static final String OVERWRITE_FALSE = "false";
    public static final String OVERWRITE_PREPEND = "prepend";
    public static final String OVERWRITE_APPEND = "append";

    private String pathid;

    private String overwrite;

    private Path path;

    public void setProject(Project project) {
        super.setProject(project);
        path = new Path(project);
    }

    public void execute() throws BuildException {
        if (pathid == null) {
            throw new BuildException("pathid is mandatory");
        }
        Object element = getProject().getReference(pathid);
        if (element == null) {
            if (OVERWRITE_PREPEND.equals(overwrite) || OVERWRITE_APPEND.equals(overwrite)) {
                throw new BuildException("destination path not found: " + pathid);
            }
            getProject().addReference(pathid, path);
        } else {
            if (OVERWRITE_FALSE.equals(overwrite)) {
                return;
            }
            if (!(element instanceof Path)) {
                throw new BuildException("destination path is not a path: " + element.getClass());
            }
            if (OVERWRITE_TRUE.equals(overwrite)) {
                getProject().addReference(pathid, path);
            } else {
                Path dest = (Path) element;
                if (OVERWRITE_PREPEND.equals(overwrite)) {
                    // no way to add path elements at te beginning of the
                    // existing path: we do the opposite
                    // and replace the reference
                    path.append(dest);
                    getProject().addReference(pathid, path);
                } else { // OVERWRITE_APPEND
                    dest.append(path);
                }
            }
        }
    }

    public void add(Path path) throws BuildException {
        this.path.add(path);
    }

    public void addDirset(DirSet dset) throws BuildException {
        path.addDirset(dset);
    }

    public void addFilelist(FileList fl) throws BuildException {
        path.addFilelist(fl);
    }

    public void addFileset(FileSet fs) throws BuildException {
        path.addFileset(fs);
    }

    public Path createPath() throws BuildException {
        return path.createPath();
    }

    public PathElement createPathElement() throws BuildException {
        return path.createPathElement();
    }

    /**
     * Get a path id
     * 
     * @return a pathId
     */
    public String getPathid() {
        return pathid;
    }

    /**
     * @param pathid
     *            a pathId
     */
    public void setPathid(String pathid) {
        this.pathid = pathid;
    }

    /**
     * return a string which define if a path is overwritable (Possible values are true/false/append/prepend)
     * 
     * @return Possible values are true/false/append/prepend
     */
    public String getOverwrite() {
        return overwrite;
    }

    /**
     * specify if easyant should overwrite the path (Possible values are true/false/append/prepend)
     * 
     * @param overwrite
     *            Possible values are true/false/append/prepend
     */
    public void setOverwrite(String overwrite) {
        this.overwrite = overwrite;
    }
}
