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
package org.apache.easyant.core.ant.listerners;

import java.util.ArrayList;
import java.util.List;

import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.easyant.core.ant.ExecutionStatus;
import org.apache.ivy.util.StringUtils;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.SubBuildListener;
import org.apache.tools.ant.util.DateUtils;

/*
 * This listener is created so that it may be used to provide all statistic
 * collection for easyant builds.
 */

/**
 * Build listener to time execution of builds.
 */
public class BuildExecutionTimer implements BuildListener, SubBuildListener {

    /**
     * Reference key against which build execution results will be stored
     */
    public static final String EXECUTION_TIMER_BUILD_RESULTS = "execution.timer.build.results";

    /**
     * Reference key against which sub-build execution times will be stored
     */
    public static final String EXECUTION_TIMER_SUBBUILD_RESULTS = "execution.timer.subbuild.results";

    private static final String DEMARKER = "======================================================================";

    // build start time
    // to be initialized in buildStarted method
    private long buildStartTime;

    public static class ExecutionResult {
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

    /**
     * stops the timer and stores the result as a project reference by the key 'referenceName'
     */
    protected void stopTimer(BuildEvent event, String referenceName, long startTime) {
        List<ExecutionResult> results = event.getProject().getReference(referenceName);
        if (results == null) {
            results = new ArrayList<ExecutionResult>();
            event.getProject().addReference(referenceName, results);
        }
        ExecutionStatus status = ExecutionStatus.SUCCESS;
        if (event.getException() != null) {
            status = ExecutionStatus.FAILED;
        } else if (event.getProject().getProperty(EasyAntMagicNames.PROJECT_EXECUTED_TARGETS) == null) {
            status = ExecutionStatus.SKIPPED;
        }

        ExecutionResult execResult = new ExecutionResult(event.getProject().getName(), System.currentTimeMillis()
                - startTime, status);

        results.add(execResult);

    }

    public void buildFinished(BuildEvent event) {
        stopTimer(event, EXECUTION_TIMER_BUILD_RESULTS, buildStartTime);
        printExecutionSubBuildsExecutionTimes(event.getProject());
    }

    private void printExecutionSubBuildsExecutionTimes(Project project) {
        String lineSep = org.apache.tools.ant.util.StringUtils.LINE_SEP;
        List<ExecutionResult> allSubBuildResults = project.getReference(EXECUTION_TIMER_SUBBUILD_RESULTS);
        if (allSubBuildResults != null && allSubBuildResults.size() > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(lineSep).append(DEMARKER).append(lineSep);
            sb.append("Project Sub-modules Summary: ").append(lineSep).append(DEMARKER);
            sb.append(lineSep).append(formatExecutionResults(allSubBuildResults));
            sb.append(lineSep).append(DEMARKER);
            project.log(sb.toString());
        }
    }

    public void buildStarted(BuildEvent arg0) {
        buildStartTime = System.currentTimeMillis();
    }

    public void messageLogged(BuildEvent arg0) {
        // leaving empty till better use is found
    }

    public void targetFinished(BuildEvent arg0) {
        // leaving empty till better use is found
    }

    /* following methods may be implemented for more comprehensive stats */
    public void targetStarted(BuildEvent arg0) {
        // leaving empty till better use is found
    }

    public void taskFinished(BuildEvent arg0) {
        // leaving empty till better use is found
    }

    public void taskStarted(BuildEvent arg0) {
        // leaving empty till better use is found
    }

    /**
     * Returns a string containing results of execution timing for display on console in a tabular fashion
     * 
     * @param results
     * @return
     */
    public String formatExecutionResults(List<ExecutionResult> results) {
        String formattedResults = "";
        int constantSpaces = 10;
        int maxUnitNameLength = 0;
        int maxExecTimeLength = 0;
        for (ExecutionResult result : results) {
            maxUnitNameLength = result.getUnitName().length() > maxUnitNameLength ? result.getUnitName().length()
                    : maxUnitNameLength;
            maxExecTimeLength = result.getFormattedElapsedTime().length() > maxExecTimeLength ? result
                    .getFormattedElapsedTime().length() : maxExecTimeLength;
        }
        StringBuilder sb = new StringBuilder(org.apache.tools.ant.util.StringUtils.LINE_SEP);
        for (ExecutionResult result : results) {
            String moduleName = result.getUnitName();
            int variableSpaces = maxUnitNameLength - moduleName.length() + constantSpaces;
            sb.append(" * ").append(result.getUnitName()).append(StringUtils.repeat(" ", variableSpaces));
            // keeping both success and failed strings of equal length
            String execResult = result.getStatus().toString();
            if (execResult.length() < 7) {
                execResult += StringUtils.repeat(" ", 7 - execResult.length());
            }
            sb.append(execResult).append(" [took ").append(result.getFormattedElapsedTime()).append("]")
                    .append(org.apache.tools.ant.util.StringUtils.LINE_SEP);
        }

        formattedResults = sb.toString();
        return formattedResults;
    }

    public void subBuildFinished(BuildEvent arg0) {
        stopTimer(arg0, EXECUTION_TIMER_SUBBUILD_RESULTS, buildStartTime);
    }

    public void subBuildStarted(BuildEvent arg0) {
        buildStartTime = System.currentTimeMillis();
    }

}
