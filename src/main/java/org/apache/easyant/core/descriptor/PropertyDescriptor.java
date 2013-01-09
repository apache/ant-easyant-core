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

public class PropertyDescriptor implements AdvancedInheritableItem {
    private final String name;
    private String description;
    private String defaultValue;
    private String value;
    private boolean required;
    private String buildConfigurations;
    private final ModuleRevisionId sourceModule;
    private InheritableScope inheritScope;
    private boolean inheritable = true;

    private String owningTarget;

    public PropertyDescriptor(String propertyName) {
        this.name = propertyName;
        this.sourceModule = null;
    }

    public PropertyDescriptor(String propertyName, ModuleRevisionId sourceModule) {
        this.name = propertyName;
        this.sourceModule = sourceModule;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getBuildConfigurations() {
        return buildConfigurations;
    }

    public void setBuildConfigurations(String buildConfiguration) {
        this.buildConfigurations = buildConfiguration;
    }

    public ModuleRevisionId getSourceModule() {
        return sourceModule;
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

    public void setInheritable(boolean isIneritable) {
        this.inheritable = isIneritable;

    }

    public String getOwningTarget() {
        return owningTarget;
    }

    public void setOwningTarget(String owningTarget) {
        this.owningTarget = owningTarget;
    }
}