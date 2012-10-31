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

/**
 * This class is a simple POJO used to store informations on configureProject.
 * 
 */
public class ConfigureProjectDescriptor implements AdvancedInheritableItem {

    private String defaultTarget;
    private String basedir;

    private InheritableScope inheritScope = InheritableScope.BOTH;
    private final ModuleRevisionId sourceModule;
    private boolean inheritable = true;

    /**
     * Default constructor
     */
    public ConfigureProjectDescriptor() {
        sourceModule = null;
    }

    /**
     * Constructor specifying the source module which was defining the configureproject
     * 
     * @param sourceModule
     *            a source module
     */
    public ConfigureProjectDescriptor(ModuleRevisionId sourceModule) {
        this.sourceModule = sourceModule;
    }

    public String getDefaultTarget() {
        return defaultTarget;
    }

    public void setDefaultTarget(String defaultTarget) {
        this.defaultTarget = defaultTarget;
    }

    public String getBasedir() {
        return basedir;
    }

    public void setBasedir(String basedir) {
        this.basedir = basedir;
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