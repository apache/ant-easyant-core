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

import org.apache.easyant.core.BuildConfigurationHelper;
import org.apache.tools.ant.*;

import java.util.Enumeration;

public class BindTarget extends Task {

    private String target;
    private String extensionOf;

    private String buildConfigurations;

    public void execute() throws BuildException {
        String message = "extension-point mapping for target " + getTarget();
        if (!BuildConfigurationHelper.isBuildConfigurationActive(getBuildConfigurations(), getProject(),
                message)) {
            log("no matching build configuration for this extension-point mapping, this mapping will be ignored",
                    Project.MSG_DEBUG);
            return;
        }
        Target t = getProject().getTargets().get(getTarget());
        if (t == null) {
            throw new BuildException("unable to find target " + getTarget());
        }

        // unbind current mapping
        for (Target current : getProject().getTargets().values()) {
            if (current instanceof ExtensionPoint) {
                Enumeration<?> dependencies = current.getDependencies();
                StringBuilder dependsOn = new StringBuilder();
                boolean requiresUpdates = false;
                while (dependencies.hasMoreElements()) {
                    String dep = (String) dependencies.nextElement();
                    if (dep.equals(getTarget())) {
                        log("target" + getTarget() + " is registred in extensionPoint" + current.getName(),
                                Project.MSG_VERBOSE);
                        requiresUpdates = true;
                    } else {
                        dependsOn.append(dep);
                        dependsOn.append(",");
                    }
                }
                if (requiresUpdates) {
                    log("removing target" + getTarget() + " from extension-point" + current.getName(),
                            Project.MSG_VERBOSE);

                    ExtensionPoint ep = new ExtensionPoint();
                    ep.setDescription(current.getDescription());
                    ep.setIf(current.getIf());
                    ep.setLocation(current.getLocation());
                    ep.setName(current.getName());
                    ep.setProject(current.getProject());
                    ep.setUnless(current.getUnless());
                    String depends = dependsOn.toString();
                    if (depends.endsWith(",")) {
                        depends = depends.substring(0, depends.length() - 1);
                    }
                    ep.setDepends(depends);
                    getProject().addOrReplaceTarget(ep);
                }

            }
        }

        if (getExtensionOf() != null && !getExtensionOf().equals("")) {
            if (!getProject().getTargets().containsKey(getExtensionOf())) {
                throw new BuildException("can't add target " + getTarget() + " to extension-point " + getExtensionOf()
                        + " because the extension-point" + " is unknown.");
            }
            Target p = getProject().getTargets().get(getExtensionOf());

            if (!(p instanceof ExtensionPoint)) {
                throw new BuildException("referenced target " + getExtensionOf() + " is not a extension-point");
            }
            p.addDependency(getTarget());
        }

    }

    public String getExtensionOf() {
        return extensionOf;
    }

    public void setExtensionOf(String toExtensionPoint) {
        this.extensionOf = toExtensionPoint;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getBuildConfigurations() {
        return buildConfigurations;
    }

    public void setBuildConfigurations(String buildConfigurations) {
        this.buildConfigurations = buildConfigurations;
    }

    public void setConf(String conf) {
        this.buildConfigurations = conf;
    }

}
