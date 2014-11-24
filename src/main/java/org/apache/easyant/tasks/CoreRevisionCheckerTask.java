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

import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * <p>
 * The core-version task is used to define the compatibility between core and modules.
 * </p>
 * <p>
 * This tasks support :
 * <ul>
 * <li>static version (Example : 0.5)</li>
 * <li>dynamic version (Example : latest.revision) even if we do not recommend to use it</li>
 * <li>listed version (Example : (0.1,0.3,0.5))</li>
 * <li>range version (Example : [0.5,0.8] means from 0.5 to 0.8. Example2 : [0.5,+] means all version superior to 0.5)</li>
 * </ul>
 * </p>
 */
public class CoreRevisionCheckerTask extends AbstractEasyAntTask {

    private String requiredRevision;
    private String message;

    /**
     * Set the error message to display if the core revision check fails.
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Appends CDATA text inside the Ant task to the error message used if the revision check fails.
     *
     * @see #setMessage(String)
     */
    public void addText(String messageText) {
        if (!messageText.trim().isEmpty()) {
            messageText = getProject().replaceProperties(messageText);
            if (this.message == null) {
                this.message = messageText;
            } else {
                this.message += messageText;
            }
        }
    }

    /**
     * Get the requiredRevision
     *
     * @return a string that represent the required revision
     */
    public String getRequiredRevision() {
        return requiredRevision;
    }

    /**
     * Set the requiredRevision
     *
     * @param requiredRevision a string that represent the required revision
     */
    public void setRequiredRevision(String requiredRevision) {
        this.requiredRevision = requiredRevision;
    }

    static String easyantSpecVersion = null;

    public static synchronized String getEasyAntSpecVersion() throws BuildException {
        if (easyantSpecVersion == null) {
            InputStream in = null;
            try {
                Properties props = new Properties();
                in = CoreRevisionCheckerTask.class.getResourceAsStream("/META-INF/version.properties");
                if (in == null) {
                    throw new BuildException("Could not load the version information.");
                }
                props.load(in);
                easyantSpecVersion = props.getProperty("SPEC-VERSION");
            } catch (IOException ioe) {
                throw new BuildException("Could not load the version information:" + ioe.getMessage());
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        // do nothing
                    }
                }
            }
        }
        return easyantSpecVersion;
    }

    public void execute() throws BuildException {
        if (requiredRevision == null) {
            throw new BuildException("requiredRevision argument is required!");
        }

        if (getProject().getProperty(EasyAntMagicNames.SKIP_CORE_REVISION_CHECKER) != null
                && "true".equals(getProject().getProperty(EasyAntMagicNames.SKIP_CORE_REVISION_CHECKER))) {
            log("core revision checker is disabled, this should not append in production.", Project.MSG_DEBUG);
            return;
        }

        String easyantVersion = getEasyAntSpecVersion();
        if (easyantVersion == null) {
            throw new BuildException("Unable to find easyant version.");
        }
        ModuleRevisionId easyantMrid = ModuleRevisionId.parse("org.apache.ant#easyant;" + easyantVersion);
        ModuleRevisionId requiredMrid = ModuleRevisionId.parse("org.apache.ant#easyant;" + requiredRevision);

        // Should we loop on each VersionMatchers?
        if (!getEasyAntIvyInstance().getSettings().getVersionMatcher().accept(requiredMrid, easyantMrid)) {
            throw new BuildException(getErrorMessage());
        }

    }

    protected String getErrorMessage() {
        if (message != null) {
            return message;
        } else {
            // no custom message provided, return default error message.
            return "This module requires easyant " + requiredRevision;
        }
    }

}
