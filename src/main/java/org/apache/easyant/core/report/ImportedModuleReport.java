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
package org.apache.easyant.core.report;

import org.apache.ivy.core.module.id.ModuleRevisionId;

public class ImportedModuleReport {
    String module = null;
    String organisation = null;
    String revision = null;
    String moduleMrid = null;
    String type = null;
    String as = null;
    boolean mandatory = false;

    EasyAntReport easyantReport = null;

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getOrganisation() {
        return organisation;
    }

    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public String getModuleMrid() {
        return moduleMrid != null ? moduleMrid : ModuleRevisionId.newInstance(
                organisation, module, revision).toString();
    }
    
    public ModuleRevisionId getModuleRevisionId() {
        return ModuleRevisionId.parse(getModuleMrid());
    }

    public void setModuleMrid(String moduleMrid) {
        this.moduleMrid = moduleMrid;
    }

    public String getMode() {
        return type;
    }

    public void setMode(String type) {
        this.type = type;
    }

    public String getAs() {
        return as;
    }

    public void setAs(String as) {
        this.as = as;
    }

    public EasyAntReport getEasyantReport() {
        return easyantReport;
    }

    public void setEasyantReport(EasyAntReport easyantReport) {
        this.easyantReport = easyantReport;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

}
