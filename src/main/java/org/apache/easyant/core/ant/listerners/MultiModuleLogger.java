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

import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.easyant.core.ant.ExecutionStatus;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.SubBuildListener;
import org.apache.tools.ant.listener.TimestampedLogger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.apache.tools.ant.util.StringUtils.LINE_SEP;

public class MultiModuleLogger extends DefaultEasyAntLogger implements SubBuildListener {


    /**
     * Reference key against which build execution results will be stored
     */
    public static final String EXECUTION_TIMER_BUILD_RESULTS = "execution.timer.build.results";

    private static final String DEMARKER = "======================================================================";
    private volatile boolean subBuildStartedRaised = false;
    private final Object subBuildLock = new Object();
    private long buildStartTime;

    /**
     * This is an override point: the message that indicates whether a build failed. Subclasses can change/enhance the
     * message.
     *
     * @return The classic "BUILD FAILED" plus a timestamp
     */
    protected String getBuildFailedMessage() {
        return super.getBuildFailedMessage() + TimestampedLogger.SPACER + getTimestamp();
    }

    /**
     * This is an override point: the message that indicates that a build succeeded. Subclasses can change/enhance the
     * message.
     *
     * @return The classic "BUILD SUCCESSFUL" plus a timestamp
     */
    protected String getBuildSuccessfulMessage() {
        return super.getBuildSuccessfulMessage() + TimestampedLogger.SPACER + getTimestamp();
    }

    public void targetStarted(BuildEvent event) {
        maybeRaiseSubBuildStarted(event);
        targetName = extractTargetName(event);
    }

    public void taskStarted(BuildEvent event) {
        maybeRaiseSubBuildStarted(event);
        super.taskStarted(event);
    }

    public void buildFinished(BuildEvent event) {
        stopTimer(event);
        printExecutionSubBuildsExecutionTimes(event.getProject());
        maybeRaiseSubBuildStarted(event);
        subBuildFinished(event);
        super.buildFinished(event);
    }

    public void messageLogged(BuildEvent event) {
        maybeRaiseSubBuildStarted(event);
        super.messageLogged(event);
    }

    public void subBuildStarted(BuildEvent event) {
        initTimer(event.getProject());
        String name = extractNameOrDefault(event);
        Project project = event.getProject();

        File base = project == null ? null : project.getBaseDir();
        String path = (base == null) ? "With no base directory" : "In " + base.getAbsolutePath();
        printMessage(LINE_SEP + DEMARKER + LINE_SEP + "Entering project " + name
                + LINE_SEP + path + LINE_SEP + DEMARKER, out, event.getPriority());
    }


    @Override
    public void buildStarted(BuildEvent event) {
        initTimer(event.getProject());
        super.buildStarted(event);
    }

    /**
     * Get the name of an event
     *
     * @param event the event name
     * @return the name or a default string
     */
    protected String extractNameOrDefault(BuildEvent event) {
        String name = extractProjectName(event);
        if (name == null) {
            name = "";
        } else {
            name = '"' + name + '"';
        }
        return name;
    }

    public void subBuildFinished(BuildEvent event) {
        stopTimer(event);
        String name = extractNameOrDefault(event);
        String failed = event.getException() != null ? "failing " : "";
        printMessage(LINE_SEP + DEMARKER + LINE_SEP + "Exiting " + failed + "project "
                + name + LINE_SEP + DEMARKER, out, event.getPriority());
    }


    private void maybeRaiseSubBuildStarted(BuildEvent event) {
        // double checked locking should be OK since the flag is write-once
        if (!subBuildStartedRaised) {
            synchronized (subBuildLock) {
                if (!subBuildStartedRaised) {
                    subBuildStartedRaised = true;
                    subBuildStarted(event);
                }
            }
        }
    }

    /**
     * Override point, extract the target name
     *
     * @param event the event to work on
     * @return the target name -including the owning project name (if non-null)
     */
    protected String extractTargetName(BuildEvent event) {
        String targetName = event.getTarget().getName();
        String projectName = extractProjectName(event);
        if (projectName != null && targetName != null) {
            return projectName + '.' + targetName;
        } else {
            return targetName;
        }
    }

    private void initTimer(Project project) {
        buildStartTime = System.currentTimeMillis();
        project.addReference(EXECUTION_TIMER_BUILD_RESULTS, new ArrayList<ExecutionResult>());
    }

    /**
     * stops the timer and stores the result as a project reference by the key 'referenceName'
     */
    private void stopTimer(BuildEvent event) {
        List<ExecutionResult> results = event.getProject().getReference(EXECUTION_TIMER_BUILD_RESULTS);
        ExecutionStatus status = ExecutionStatus.SUCCESS;
        if (event.getException() != null) {
            status = ExecutionStatus.FAILED;
        } else if (event.getProject().getProperty(EasyAntMagicNames.PROJECT_EXECUTED_TARGETS) == null) {
            status = ExecutionStatus.SKIPPED;
        }

        ExecutionResult execResult = new ExecutionResult(event.getProject().getName(), System.currentTimeMillis()
                - buildStartTime, status);

        results.add(execResult);

    }

    private void printExecutionSubBuildsExecutionTimes(Project project) {
        List<ExecutionResult> allSubBuildResults = project.getReference(EXECUTION_TIMER_BUILD_RESULTS);
        if (allSubBuildResults != null && !allSubBuildResults.isEmpty()) {
            project.log(LINE_SEP + "Project Sub-modules Summary: " + LINE_SEP + formatExecutionResults(allSubBuildResults));
        }
    }

    private String formatExecutionResults(List<ExecutionResult> results) {
        int maxUnitNameLength = 0;
        int maxExecTimeLength = 0;
        for (ExecutionResult result : results) {
            maxUnitNameLength = result.getUnitName().length() > maxUnitNameLength ? result.getUnitName().length()
                    : maxUnitNameLength;
            maxExecTimeLength = result.getFormattedElapsedTime().length() > maxExecTimeLength ? result
                    .getFormattedElapsedTime().length() : maxExecTimeLength;
        }
        StringBuilder sb = new StringBuilder(LINE_SEP);
        for (ExecutionResult result : results) {
            String moduleName = padRight(result.getUnitName(), maxUnitNameLength + 10);
            sb.append(" * ").append(moduleName);
            // keeping both success and failed strings of equal length
            String execResult = padRight(result.getStatus().toString(), 7);
            sb.append(execResult)//
                    .append(" [ took ")//
                    .append(padRight(result.getFormattedElapsedTime(), maxExecTimeLength + 1))//
                    .append("]")
                    .append(LINE_SEP);
        }

        return sb.toString();
    }

    private String padRight(String string, int nbSpace) {
        return String.format("%1$-" + nbSpace + "s", string);
    }


}
