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

import org.apache.ivy.ant.IvyPostResolveTask;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.tools.ant.BuildException;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Backport of IvyDependencyUpdateChecker until a new version of ivy will be released
 */
public class IvyDependencyUpdateChecker extends IvyPostResolveTask {

    private String revisionToCheck = "latest.integration";

    private boolean download = false;

    private boolean checkIfChanged = false;

    private boolean showTransitive = false;

    public void doExecute() throws BuildException {
        prepareAndCheck();

        ModuleDescriptor originalModuleDescriptor = getResolvedReport().getModuleDescriptor();
        // clone module descriptor
        DefaultModuleDescriptor latestModuleDescriptor = new DefaultModuleDescriptor(
                originalModuleDescriptor.getModuleRevisionId(), originalModuleDescriptor.getStatus(),
                originalModuleDescriptor.getPublicationDate());
        // copy configurations
        for (int i = 0; i < originalModuleDescriptor.getConfigurations().length; i++) {
            Configuration configuration = originalModuleDescriptor.getConfigurations()[i];
            latestModuleDescriptor.addConfiguration(configuration);
        }
        // clone dependency and add new one with the requested revisionToCheck
        for (int i = 0; i < originalModuleDescriptor.getDependencies().length; i++) {
            DependencyDescriptor dependencyDescriptor = originalModuleDescriptor.getDependencies()[i];
            ModuleRevisionId upToDateMrid = ModuleRevisionId.newInstance(
                    dependencyDescriptor.getDependencyRevisionId(), revisionToCheck);
            latestModuleDescriptor.addDependency(dependencyDescriptor.clone(upToDateMrid));
        }

        // resolve
        ResolveOptions resolveOptions = new ResolveOptions();
        resolveOptions.setDownload(isDownload());
        resolveOptions.setLog(getLog());
        resolveOptions.setConfs(splitConfs(getConf()));
        resolveOptions.setCheckIfChanged(checkIfChanged);

        ResolveReport latestReport;
        try {
            latestReport = getIvyInstance().getResolveEngine().resolve(latestModuleDescriptor, resolveOptions);

            displayDependencyUpdates(getResolvedReport(), latestReport);
            if (showTransitive) {
                displayNewDependencyOnLatest(getResolvedReport(), latestReport);
                displayMissingDependencyOnLatest(getResolvedReport(), latestReport);
            }

        } catch (ParseException e) {
            throw new BuildException("impossible to resolve dependencies:\n\t" + e, e);
        } catch (IOException e) {
            throw new BuildException("impossible to resolve dependencies:\n\t" + e, e);
        }

    }

    private void displayDependencyUpdates(ResolveReport originalReport, ResolveReport latestReport) {
        log("Dependencies updates available :");
        boolean dependencyUpdateDetected = false;
        for (Object o : latestReport.getDependencies()) {
            IvyNode latest = (IvyNode) o;
            for (Object o1 : originalReport.getDependencies()) {
                IvyNode originalDependency = (IvyNode) o1;
                if (originalDependency.getModuleId().equals(latest.getModuleId())) {
                    if (!originalDependency.getResolvedId().getRevision().equals(latest.getResolvedId().getRevision())) {
                        // is this dependency a transitive dependency ? or direct dependency
                        // (unfortunatly
                        // .isTranstive() methods doesn't have the same meaning)
                        boolean isTransitiveDependency = latest.getDependencyDescriptor(latest.getRoot()) == null;
                        if ((!isTransitiveDependency) || (isTransitiveDependency && showTransitive)) {
                            log("\t" + originalDependency.getResolvedId().getOrganisation() + '#' + originalDependency.getResolvedId().getName() + (isTransitiveDependency ? " (transitive)" : "") + "\t" + originalDependency.getResolvedId().getRevision() + " -> " + latest.getResolvedId().getRevision());
                            dependencyUpdateDetected = true;
                        }
                    }

                }
            }
        }
        if (!dependencyUpdateDetected) {
            log("\tAll dependencies are up to date");
        }
    }

    private void displayMissingDependencyOnLatest(ResolveReport originalReport, ResolveReport latestReport) {
        List/* <ModuleRevisionId> */listOfMissingDependencyOnLatest = new ArrayList/*
                                                                                    * <ModuleRevisionId >
                                                                                    */();
        for (Object o : originalReport.getDependencies()) {
            IvyNode originalDependency = (IvyNode) o;
            boolean dependencyFound = false;
            for (Object o1 : latestReport.getDependencies()) {
                IvyNode latest = (IvyNode) o1;
                if (originalDependency.getModuleId().equals(latest.getModuleId())) {
                    dependencyFound = true;
                }
            }
            if (!dependencyFound) {
                listOfMissingDependencyOnLatest.add(originalDependency.getId());
            }
        }

        if (!listOfMissingDependencyOnLatest.isEmpty()) {
            log("List of missing dependency on latest resolve :");
            for (Object aListOfMissingDependencyOnLatest : listOfMissingDependencyOnLatest) {
                ModuleRevisionId moduleRevisionId = (ModuleRevisionId) aListOfMissingDependencyOnLatest;
                log("\t" + moduleRevisionId.toString());
            }
        }
    }

    private void displayNewDependencyOnLatest(ResolveReport originalReport, ResolveReport latestReport) {
        List/* <ModuleRevisionId> */listOfNewDependencyOnLatest = new ArrayList/* <ModuleRevisionId> */();
        for (Object o : latestReport.getDependencies()) {
            IvyNode latest = (IvyNode) o;

            boolean dependencyFound = false;
            for (Object o1 : originalReport.getDependencies()) {
                IvyNode originalDependency = (IvyNode) o1;
                if (originalDependency.getModuleId().equals(latest.getModuleId())) {
                    dependencyFound = true;
                }
            }
            if (!dependencyFound) {
                listOfNewDependencyOnLatest.add(latest.getId());
            }
        }
        if (!listOfNewDependencyOnLatest.isEmpty()) {
            log("List of new dependency on latest resolve :");
            for (Object aListOfNewDependencyOnLatest : listOfNewDependencyOnLatest) {
                ModuleRevisionId moduleRevisionId = (ModuleRevisionId) aListOfNewDependencyOnLatest;
                log("\t" + moduleRevisionId.toString());
            }
        }
    }

    public String getRevisionToCheck() {
        return revisionToCheck;
    }

    public void setRevisionToCheck(String revisionToCheck) {
        this.revisionToCheck = revisionToCheck;
    }

    public boolean isDownload() {
        return download;
    }

    public void setDownload(boolean download) {
        this.download = download;
    }

    public boolean isShowTransitive() {
        return showTransitive;
    }

    public void setShowTransitive(boolean showTransitive) {
        this.showTransitive = showTransitive;
    }

}
