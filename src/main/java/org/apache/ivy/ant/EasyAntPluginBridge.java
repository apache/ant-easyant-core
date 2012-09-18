package org.apache.ivy.ant;

import java.util.List;

import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.settings.IvySettings;

public class EasyAntPluginBridge {

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
