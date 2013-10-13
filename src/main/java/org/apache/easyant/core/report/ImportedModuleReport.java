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
    private String module = null;
    private String organisation = null;
    private String revision = null;
    private String moduleMrid = null;
    private String type = null;
    private String as = null;
    private boolean mandatory = false;

    private EasyAntReport easyantReport = null;

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
        return moduleMrid != null ? moduleMrid : ModuleRevisionId.newInstance(organisation, module, revision)
                .toString();
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((module == null) ? 0 : module.hashCode());
        result = prime * result + ((moduleMrid == null) ? 0 : moduleMrid.hashCode());
        result = prime * result + ((organisation == null) ? 0 : organisation.hashCode());
        result = prime * result + ((revision == null) ? 0 : revision.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ImportedModuleReport other = (ImportedModuleReport) obj;
        if (module == null) {
            if (other.module != null) {
                return false;
            }
        } else if (!module.equals(other.module)) {
            return false;
        }
        if (moduleMrid == null) {
            if (other.moduleMrid != null) {
                return false;
            }
        } else if (!moduleMrid.equals(other.moduleMrid)) {
            return false;
        }
        if (organisation == null) {
            if (other.organisation != null) {
                return false;
            }
        } else if (!organisation.equals(other.organisation)) {
            return false;
        }
        if (revision == null) {
            if (other.revision != null) {
                return false;
            }
        } else if (!revision.equals(other.revision)) {
            return false;
        }
        return true;
    }

}
