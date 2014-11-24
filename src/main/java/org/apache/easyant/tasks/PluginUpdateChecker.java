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

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import org.apache.easyant.core.EasyAntConstants;
import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.easyant.core.report.EasyAntReport;
import org.apache.easyant.core.report.ImportedModuleReport;
import org.apache.easyant.core.services.PluginService;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.MagicNames;

public class PluginUpdateChecker extends AbstractEasyAntTask {

    private File moduleIvy;
    private File moduleAnt;
    private File overrideModuleAnt;
    private String revisionToCheck = "latest.release";
    private boolean pluginUpdateDetected=false;

    public void execute() throws BuildException {
        PluginService pluginService = getProject().getReference(
                EasyAntMagicNames.PLUGIN_SERVICE_INSTANCE);
        if (moduleIvy == null) {
            String moduleIvyProperty = getProject().getProperty(EasyAntMagicNames.EASYANT_FILE);
            if (moduleIvyProperty != null) {
                moduleIvy = new File(moduleIvyProperty);
            } else {
                moduleIvy = new File(getProject().getBaseDir(), EasyAntConstants.DEFAULT_BUILD_MODULE);
            }
            if (moduleIvy.exists() && overrideModuleAnt == null) {
                overrideModuleAnt = new File(moduleIvy.getParent(), EasyAntConstants.DEFAULT_OVERRIDE_BUILD_FILE);
            }
        }
        if (moduleAnt == null) {
            String moduleAntProperty = getProject().getProperty(MagicNames.ANT_FILE);
            if (moduleAntProperty != null) {
                moduleAnt = new File(moduleAntProperty);
            } else {
                moduleAnt = new File(getProject().getBaseDir(), EasyAntConstants.DEFAULT_BUILD_FILE);
            }
        }
        try {
            EasyAntReport easyantReport = pluginService.generateEasyAntReport(moduleIvy, moduleAnt, overrideModuleAnt);
            log("Plugin updates available :");
            for (ImportedModuleReport importedModuleReport : easyantReport.getImportedModuleReports()) {
                checkNewRevision(importedModuleReport.getModuleRevisionId());
            }
            if (!pluginUpdateDetected) {
                log("\tAll plugins are up to date");
            }
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    public void checkNewRevision(ModuleRevisionId moduleRevisionId) throws ParseException, IOException {
        ModuleRevisionId latest = ModuleRevisionId.newInstance(moduleRevisionId, revisionToCheck);

        ResolveOptions resolveOptions = new ResolveOptions();
        resolveOptions.setLog(ResolveOptions.LOG_QUIET);
        resolveOptions.setDownload(false);
        ResolveReport report = getEasyAntIvyInstance().getResolveEngine().resolve(latest, resolveOptions, true);
        String resolvedRevision = report.getModuleDescriptor().getDependencies()[0].getDependencyRevisionId()
                .getRevision();
        if (!resolvedRevision.equals(moduleRevisionId.getRevision())) {
            log("\t" + moduleRevisionId.getOrganisation() + '#' + moduleRevisionId.getName() + "\t" + moduleRevisionId.getRevision() + " -> " + resolvedRevision);
            pluginUpdateDetected=true;
        }

    }

}
