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

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.SubBuildListener;

/**
 * Sub build listener to time execution for sub-module builds
 */
public class SubBuildExecutionTimer extends BuildExecutionTimer implements
        SubBuildListener {

    public void subBuildFinished(BuildEvent arg0) {
        stopTimer(arg0, EXECUTION_TIMER_SUBBUILD_RESULTS, buildStartTime);
    }

    public void subBuildStarted(BuildEvent arg0) {
        buildStartTime = System.currentTimeMillis();
    }

    /**
     * Reference key against which sub-build execution times will be stored
     */
    public static final String EXECUTION_TIMER_SUBBUILD_RESULTS = "execution.timer.subbuild.results";
}
