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
package org.apache.easyant.man;

import org.apache.easyant.core.descriptor.PropertyDescriptor;
import org.apache.easyant.core.report.ExtensionPointReport;
import org.apache.easyant.core.report.ParameterReport;
import org.apache.easyant.core.report.TargetReport;

/**
 * Implements support for -describe easyant switch.
 * 
 * This command searches for extension point, target, and properties by the supplied search name and returns result for
 * each of these sequentially.
 */
public class Describe extends EasyantOption {

    private static final long serialVersionUID = 1L;

    public Describe() throws IllegalArgumentException {
        super("describe", true, "Describes extension point / target / property / parameters  specified by the argument");
        setStopBuild(true);
    }

    // convenient string constants
    private static final String NONE = "NONE";

    public void execute() {
        String target = getValue();
        if (target == null || target.isEmpty()) {
            throw new IllegalArgumentException("No parameter specified for -describe parameter.");
        }

        getProject().log(
                LINE_SEP + "--- Available references for: " + target + " in current project: " + getProject().getName()
                        + " ---" + LINE_SEP);

        ExtensionPointReport extensionPointRep = getEareport().getExtensionPointReport(target);
        if (extensionPointRep != null) {
            getProject().log("\tExtension Point: " + target);
            getProject().log(
                    "\t\tDescription: "
                            + (extensionPointRep.getDescription() == null ? NONE : extensionPointRep.getDescription()));
            getProject().log(
                    "\t\tDepends: " + (extensionPointRep.getDepends() == null ? NONE : extensionPointRep.getDepends()));
            getProject().log(LINE_SEP + "\t\tFor information on targets attached to this extension-point, run:");
            getProject().log("\t\teasyant -listTargets " + target);
        } else {
            getProject().log("\tNo extension point found for name: " + target);
        }
        TargetReport targetRep = getEareport().getTargetReport(target);
        if (targetRep != null) {
            getProject().log("\tTarget: " + target);
            getProject().log(
                    "\t\tExtension Point: "
                            + (targetRep.getExtensionPoint() == null ? NONE : targetRep.getExtensionPoint()));
            getProject().log(
                    "\t\tDescription: " + (targetRep.getDescription() == null ? NONE : targetRep.getDescription()));
            getProject().log("\t\tDepends: " + (targetRep.getDepends() == null ? NONE : targetRep.getDepends()));
            getProject().log("\t\tIF: " + (targetRep.getIfCase() == null ? NONE : targetRep.getIfCase()));
            getProject().log("\t\tUNLESS: " + (targetRep.getUnlessCase() == null ? NONE : targetRep.getUnlessCase()));
        } else {
            getProject().log("\tNo Target found for name: " + target);
        }
        PropertyDescriptor prop = getEareport().getPropertyDescriptors().get(target);
        if (prop != null) {
            getProject().log("\tProperty: " + target);
            getProject().log("\t\tDescription: " + (prop.getDescription() == null ? NONE : prop.getDescription()));
            String defaultValue = prop.getDefaultValue() == null ? NONE : prop.getDefaultValue();
            getProject().log("\t\tDefault: " + defaultValue);
            getProject().log("\t\tRequired: " + prop.isRequired());
            String currentValue = prop.getValue() == null ? defaultValue : prop.getValue();
            getProject().log("\t\tCurrent value: " + currentValue);
            if (prop.getOwningTarget() != null) {
                getProject().log("\t\tDefined in: " + prop.getOwningTarget());
            }
        } else {
            getProject().log("\tNo Property found for name: " + target);
        }
        ParameterReport parameter = getEareport().getParameterReport(target);
        if (parameter != null) {
            getProject().log("\tParameter name: " + target);
            getProject().log("\t\tType: " + parameter.getType());
            getProject().log(
                    "\t\tDescription: " + (parameter.getDescription() == null ? NONE : parameter.getDescription()));
            getProject().log("\t\tRequired: " + parameter.isRequired());
            if (parameter.getOwningTarget() != null) {
                getProject().log("\t\tDefined in: " + parameter.getOwningTarget());
            }
        } else {
            getProject().log("\tNo Parameter found for name: " + target);
        }

        getProject().log(LINE_SEP + "--- End Of (Describe) ---");
    }

}
