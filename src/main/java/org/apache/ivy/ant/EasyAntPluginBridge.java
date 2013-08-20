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
package org.apache.ivy.ant;

import java.util.List;

import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.settings.IvySettings;

/**
 * Give access to protected fields in {@link IvyDependency} {@link IvyExclude} {@link IvyConflict}
 */
public class EasyAntPluginBridge {

    private EasyAntPluginBridge() {
    }

    public static DefaultModuleDescriptor computeModuleDescriptor(DefaultModuleDescriptor md, IvySettings settings,
            List<IvyDependency> dependencies, List<IvyConflict> conflicts, List<IvyExclude> excludes) {
        for (IvyDependency dependency : dependencies) {
            DependencyDescriptor dd = dependency.asDependencyDescriptor(md, "default", settings);
            md.addDependency(dd);
        }

        for (IvyExclude exclude : excludes) {
            org.apache.ivy.core.module.descriptor.DefaultExcludeRule rule = exclude.asRule(settings);
            rule.addConfiguration("default");
            md.addExcludeRule(rule);
        }

        for (IvyConflict conflict : conflicts) {
            conflict.addConflict(md, settings);
        }
        return md;
    }

}
