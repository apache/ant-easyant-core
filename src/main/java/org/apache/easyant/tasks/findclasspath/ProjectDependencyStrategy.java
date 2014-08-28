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

import java.io.File;

import org.apache.easyant.core.ivy.IvyInstanceHelper;
import org.apache.ivy.ant.IvyPostResolveTask;
import org.apache.ivy.core.cache.ResolutionCacheManager;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.plugins.report.XmlReportParser;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

public class ProjectDependencyStrategy extends BasicConfigurationStrategy {

    /**
     * check if project dependencies matches with expected module
     * 
     * @return return true if project dependencies matches with expected module
     */
    @Override
    protected boolean doCheck() {
        log("Checking project dependencies ...", Project.MSG_VERBOSE);
        CheckProjectDependencies checkProjectDependencies = new CheckProjectDependencies();
        checkProjectDependencies.setSettingsRef(getSettingsReference());
        checkProjectDependencies.setOrganisationToFind(getOrganisation());
        checkProjectDependencies.setModuleToFind(getModule());
        checkProjectDependencies.setRevisionToFind(getRevision());
        checkProjectDependencies.setConfToFind(getConf());
        initTask(checkProjectDependencies).execute();
        return checkProjectDependencies.checkProjectDependencies();
    }

    @Override
    public String getSettingsRef() {
        if (settingsRef == null) {
            setSettingsRef(IvyInstanceHelper.getProjectIvyInstanceName(getProject()));
        }
        return settingsRef;
    }

    private class CheckProjectDependencies extends IvyPostResolveTask {

        private String organisationToFind;
        private String moduleToFind;
        private String revisionToFind;
        private String confToFind;

        public String getOrganisationToFind() {
            return organisationToFind;
        }

        public void setOrganisationToFind(String organisationToFind) {
            this.organisationToFind = organisationToFind;
        }

        public String getModuleToFind() {
            return moduleToFind;
        }

        public void setModuleToFind(String moduleToFind) {
            this.moduleToFind = moduleToFind;
        }

        public String getRevisionToFind() {
            return revisionToFind;
        }

        public void setRevisionToFind(String revisionToFind) {
            this.revisionToFind = revisionToFind;
        }

        public String getConfToFind() {
            return confToFind;
        }

        public void setConfToFind(String confToFind) {
            this.confToFind = confToFind;
        }

        @Override
        public void doExecute() throws BuildException {
            prepareAndCheck();
        }

        public boolean checkProjectDependencies() {
            execute();
            try {
                ResolutionCacheManager cacheMgr = getIvyInstance().getResolutionCacheManager();
                String[] confs = splitConfs(getConf());
                String resolveId = getResolveId();
                if (resolveId == null) {
                    resolveId = ResolveOptions.getDefaultResolveId(getResolvedModuleId());
                }
                XmlReportParser parser = new XmlReportParser();
                for (String conf : confs) {
                    File report = cacheMgr.getConfigurationResolveReportInCache(resolveId, conf);
                    parser.parse(report);

                    Artifact[] artifacts = parser.getArtifacts();
                    for (Artifact artifact : artifacts) {
                        ModuleRevisionId mrid = artifact.getModuleRevisionId();
                        if (mrid.getOrganisation().equals(getOrganisationToFind())) {
                            if (mrid.getName().equals(getModuleToFind())) {
                                log(mrid.getOrganisation() + "#" + mrid.getName() + " found in project dependencies !",
                                        Project.MSG_DEBUG);
                                // use this version
                                loadCachePath(getOrganisationToFind(), getModuleToFind(), mrid.getRevision(),
                                        getConfToFind(), getSettingsReference());
                                return true;
                            } else {
                                // if only organization is found in project
                                // dependencies use the same version with the
                                // required module
                                log("Only organisation : " + mrid.getOrganisation()
                                        + " was found in project dependencies !", Project.MSG_DEBUG);
                                loadCachePath(mrid.getOrganisation(), getModuleToFind(), mrid.getRevision(),
                                        getConfToFind(), getSettingsReference());
                                return true;

                            }
                        }

                    }
                }
            } catch (Exception ex) {
                throw new BuildException("impossible to check project dependencies: " + ex, ex);
            }
            return false;
        }

    }

}
