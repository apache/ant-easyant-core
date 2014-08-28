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
package org.apache.easyant.core.descriptor;

import org.apache.easyant.core.ivy.InheritableScope;
import org.apache.ivy.core.module.id.ModuleRevisionId;

public class ExtensionPointMappingDescriptor implements AdvancedInheritableItem {

    private String target;
    private String extensionPoint;
    private String buildConfigurations;
    private InheritableScope inheritScope = InheritableScope.BOTH;
    private boolean inheritable = true;
    private final ModuleRevisionId sourceModule;

    public ExtensionPointMappingDescriptor() {
        this.sourceModule = null;
    }

    public ExtensionPointMappingDescriptor(ModuleRevisionId sourceModule) {
        this.sourceModule = sourceModule;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getExtensionPoint() {
        return extensionPoint;
    }

    public void setExtensionPoint(String extensionPoint) {
        this.extensionPoint = extensionPoint;
    }

    /**
     * Return a build configuration name bound to this plugin
     * 
     * @return a build configuration name
     */
    public String getBuildConfigurations() {
        return buildConfigurations;
    }

    /**
     * set a build configuration name bound to this plugin
     *
     */
    public void setBuildConfigurations(String buildConfigurations) {
        this.buildConfigurations = buildConfigurations;
    }

    public InheritableScope getInheritScope() {
        return inheritScope;
    }

    public void setInheritScope(InheritableScope inheritScope) {
        this.inheritScope = inheritScope;
    }

    public boolean isInheritable() {
        return inheritable;
    }

    public void setInheritable(boolean inheritable) {
        this.inheritable = inheritable;
    }

    public ModuleRevisionId getSourceModule() {
        return sourceModule;
    }

}
