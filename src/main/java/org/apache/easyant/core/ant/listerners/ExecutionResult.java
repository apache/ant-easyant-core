package org.apache.easyant.core.ant.listerners;

import org.apache.easyant.core.ant.ExecutionStatus;
import org.apache.tools.ant.util.DateUtils;

/**
* Created by neoverflow on 30/08/14.
*/
public class ExecutionResult {
    /**
     * Name of the unit whose execution is timed
     */
    private String unitName;

    /**
     * Time taken to execute the unit
     */
    private long elapsedTime;

    /**
     * Formatted representation of the execution time
     */
    private String formattedElapsedTime;

    private ExecutionStatus buildStatus;

    public ExecutionResult(String unitName, long elapsedTime, ExecutionStatus buildStatus) {
        this.unitName = unitName;
        this.elapsedTime = elapsedTime;
        this.formattedElapsedTime = DateUtils.formatElapsedTime(elapsedTime);
        this.buildStatus = buildStatus;
    }

    public String getUnitName() {
        return this.unitName;
    }

    public long getElapsedTime() {
        return this.elapsedTime;
    }

    public String getFormattedElapsedTime() {
        return this.formattedElapsedTime;
    }

    public ExecutionStatus getStatus() {
        return this.buildStatus;
    }
}
