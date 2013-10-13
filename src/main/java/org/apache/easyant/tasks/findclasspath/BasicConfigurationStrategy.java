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

import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Reference;

public class BasicConfigurationStrategy extends AbstractFindClassPathStrategy {
    private String organisation;
    private String module;
    private String revision;
    private String conf = "default";
    protected String settingsRef;

    protected boolean doCheck() {
        log("Checking plugin configuration ...", Project.MSG_VERBOSE);
        loadCachePath(getOrganisation(), getModule(), getRevision(), getConf(), getSettingsReference());
        return true;
    }

    public String getOrganisation() {
        return organisation;
    }

    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public String getConf() {
        return conf;
    }

    public void setConf(String conf) {
        this.conf = conf;
    }

    public String getSettingsRef() {
        if (settingsRef == null) {
            settingsRef = EasyAntMagicNames.EASYANT_IVY_INSTANCE;
        }
        return settingsRef;
    }

    public void setSettingsRef(String settingsRef) {
        this.settingsRef = settingsRef;
    }

    public Reference getSettingsReference() {
        return new Reference(getProject(), getSettingsRef());
    }

}
