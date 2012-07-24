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

import java.io.IOException;
import java.text.ParseException;

import org.apache.easyant.core.BuildConfigurationHelper;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DynamicAttribute;
import org.apache.tools.ant.Project;

/**
 * Default import implementation, resolution is done through give arguments. Example :
 * 
 * <pre>
 * &lt;import mrid="org.apache.easyant.plugins#compile-java;0.9"/&gt;
 * </pre>
 * 
 * exploded style
 * 
 * <pre>
 * &lt;import organisation="org.apache.easyant.plugins" module="compile-java" revision="0.9"/&gt;
 * </pre>
 * 
 * exploded style with shortcut attributes
 * 
 * <pre>
 * &lt;import org="org.apache.easyant.plugins" module="compile-java" rev="0.9"/&gt;
 * </pre>
 */
public class Import extends AbstractImport implements DynamicAttribute {

    private String module;
    private String organisation;
    private String revision;

    private String mrid;

    public void execute() {
        ModuleRevisionId moduleRevisionId = null;
        if (mrid != null) {
            moduleRevisionId = ModuleRevisionId.parse(mrid);
        } else if (organisation != null && module != null && revision != null) {
            moduleRevisionId = ModuleRevisionId.newInstance(organisation, module, revision);
        } else {
            throw new BuildException(
                    "The module to import is not properly specified, you must set the mrid attribute or set organisation / module / revision attributes");
        }
        String moduleName = moduleRevisionId.toString();
        if (!BuildConfigurationHelper.isBuildConfigurationActive(getBuildConfigurations(), getProject(), "module"
                + getModule())) {
            log("no matching build configuration for module " + moduleName + " this module will be skipped ",
                    Project.MSG_DEBUG);
            return;
        }

        // if no as attribute was given use module name
        if (getAs() == null && "include".equals(getMode())) {
            // when using mrid style
            if (mrid != null) {
                setAs(moduleRevisionId.getName());
                // when using exploded style
            } else if (getModule() != null) {
                setAs(getModule());
            }
        }

        // check if a property skip.${module} or skip.${as} is set
        boolean toBeSkipped = getProject().getProperty("skip." + moduleName) != null
                || getProject().getProperty("skip." + getAs()) != null;

        if (isMandatory() && toBeSkipped) {
            log("Impossible to skip a mandatory module : " + moduleName, Project.MSG_WARN);
        }

        // a module can be skipped *only* if it is not mandatory
        if (!isMandatory() && toBeSkipped) {
            log(moduleName + " skipped !");
        } else {
            try {
                ResolveReport report = getEasyAntIvyInstance().getResolveEngine().resolve(moduleRevisionId,
                        configureResolveOptions(), true);
                importModule(moduleRevisionId,report);
            } catch (ParseException e) {
                throw new BuildException("Can't parse module descriptor", e);
            } catch (IOException e) {
                throw new BuildException("Can't parse module descriptor", e);
            }

        }
    }

    /**
     * Get the module name to import
     * 
     * @return the module name
     */
    public String getModule() {
        return module;
    }

    /**
     * Set the module name to import
     * 
     * @param module
     *            the module name
     */
    public void setModule(String module) {
        this.module = module;
    }

    /**
     * Get the organisation of the module to import
     * 
     * @return the organisation name
     */
    public String getOrganisation() {
        return organisation;
    }

    /**
     * Set the organisation of the module to import
     * 
     * @param organisation
     *            the organisation name
     */
    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }

    /**
     * Set the organisation of the module to import
     * 
     * @param organisation
     *            the organisation name
     */
    public void setOrg(String org) {
        this.organisation = org;
    }

    /**
     * Get the revision of the module to import
     * 
     * @return the revision
     */
    public String getRevision() {
        return revision;
    }

    /**
     * Set th revision of the module to import
     * 
     * @param revision
     *            the revision
     */
    public void setRevision(String revision) {
        this.revision = revision;
    }

    /**
     * Set th revision of the module to import
     * 
     * @param revision
     *            the revision
     */
    public void setRev(String rev) {
        this.revision = rev;
    }

    /**
     * Get the full mrid of the module to import
     * 
     * @return the mrid to import
     */
    public String getMrid() {
        return mrid;
    }

    /**
     * Set the full mrid of the module to import
     * 
     * @param mrid
     *            the mrid to import
     */
    public void setMrid(String mrid) {
        this.mrid = mrid;
    }
}
