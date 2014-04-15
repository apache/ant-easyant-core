package org.apache.easyant.man;

import java.util.Iterator;
import java.util.Map;

import org.apache.easyant.core.ant.ProjectUtils;
import org.apache.tools.ant.Target;

public class ListTargetsAndExtensionPoints extends EasyantOption {

    public ListTargetsAndExtensionPoints() {
        super("listAllTargets", "list all targets and extension points");
        setStopBuild(true);
    }
    
    @Override
    public void execute() {
        StringBuilder sb = new StringBuilder();
        Map<String, Target> targets = ProjectUtils.removeDuplicateTargets(getProject().getTargets());
        for (Iterator iterator = targets.keySet().iterator(); iterator.hasNext();) {
            String targetName = (String) iterator.next();
            sb.append(targetName).append(" " );
            
        }
        getProject().log("All targets :" + sb.toString());
        getProject().getTargets().toString();
    }

}
