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

import org.apache.easyant.core.ant.ProjectUtils;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.NoBannerLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.util.StringUtils;

public class DefaultEasyAntLogger extends NoBannerLogger {

    private static final String WHERE_MSG = "* Where";
    private static final String DIAGNOSTIC_MSG = "* Dr Myrmex diagnostic";
    private static final String RECOMENDATION_MSG = "* Recomendation ...";
    private static final String RECOMENDATION_LONG_DESC = "Dr Myrmex suggest you to run easyant with -verbose or -debug option to get more details.";

    private long startTime = System.currentTimeMillis();

    protected static void throwableMessage(StringBuilder m, Throwable error, boolean verbose) {

        while (error != null) {
            Throwable cause = error.getCause();
            if (cause == null) {
                break;
            }
            String msg1 = error.toString();
            String msg2 = cause.toString();
            if (msg1.endsWith(msg2)) {
                String messageException = msg1.substring(0, msg1.length() - msg2.length());
                if (error instanceof BuildException) {
                    BuildException be = (BuildException) error;

                    // wipe location information
                    if (be.getLocation() != null) {
                        messageException = messageException.substring(be.getLocation().toString().length());

                    }
                }
                m.append("Error : ").append(messageException);
                m.append(lSep);
                m.append("Cause : ");
                error = cause;
            } else {
                break;
            }
        }

        if (verbose) {
            m.append(StringUtils.getStackTrace(error));
        } else {
            m.append(error.getMessage()).append(lSep);
        }
    }

    @Override
    public void buildFinished(BuildEvent event) {
        Throwable error = event.getException();
        StringBuilder message = new StringBuilder();
        if (error == null) {
            message.append(StringUtils.LINE_SEP);
            message.append(getBuildSuccessfulMessage());
        } else {
            message.append(StringUtils.LINE_SEP);
            message.append(getBuildFailedMessage());
            message.append(StringUtils.LINE_SEP);

            message.append("Dr Myrmex found an error when building ");
            message.append(extractProjectName(event));
            message.append(StringUtils.LINE_SEP);
            if (error instanceof BuildException) {
                BuildException be = (BuildException) error;
                if (be.getLocation().getFileName() != null) {
                    message.append(WHERE_MSG);
                    message.append(lSep).append(lSep);
                    message.append("File : ").append(be.getLocation().getFileName()).append(lSep);
                    message.append("Line : ").append(be.getLocation().getLineNumber());
                    message.append(" column : ").append(be.getLocation().getColumnNumber()).append(lSep);
                }
                if (Project.MSG_DEBUG == msgOutputLevel) {
                    message.append(StringUtils.LINE_SEP);
                    message.append("Import stack :");
                    message.append(StringUtils.LINE_SEP);
                    ProjectHelper helper = ProjectUtils.getConfiguredProjectHelper(event.getProject());
                    for (int i = 0; i < helper.getImportStack().size(); i++) {
                        message.append(helper.getImportStack().get(i).toString());
                        message.append(StringUtils.LINE_SEP);

                    }
                }

            }
            message.append(StringUtils.LINE_SEP);
            message.append(DIAGNOSTIC_MSG);
            message.append(StringUtils.LINE_SEP);
            message.append(StringUtils.LINE_SEP);
            throwableMessage(message, error, Project.MSG_VERBOSE <= msgOutputLevel);
            message.append(StringUtils.LINE_SEP);
            if (msgOutputLevel < Project.MSG_VERBOSE) {
                message.append(StringUtils.LINE_SEP);
                message.append(RECOMENDATION_MSG);
                message.append(StringUtils.LINE_SEP);
                message.append(StringUtils.LINE_SEP);
                message.append(RECOMENDATION_LONG_DESC);
            }
        }
        message.append(StringUtils.LINE_SEP);
        message.append(StringUtils.LINE_SEP);
        message.append("Total time: ");
        message.append(formatTime(System.currentTimeMillis() - startTime));

        String msg = message.toString();
        if (error == null) {
            printMessage(msg, out, Project.MSG_VERBOSE);
        } else {
            printMessage(msg, err, Project.MSG_ERR);
        }
        log(msg);

    }

    @Override
    public void buildStarted(BuildEvent event) {
        startTime = System.currentTimeMillis();
    }

}
