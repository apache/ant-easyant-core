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
package org.apache.easyant.core.configuration;

import java.io.File;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.easyant.core.descriptor.PluginDescriptor;
import org.apache.tools.ant.Project;

/**
 * This class is used to configure easyantEngine.
 */
public class EasyAntConfiguration {

    /** Stream to use for logging. */
    private PrintStream out = System.out;

    /** Stream that we are using for logging error messages. */
    private PrintStream err = System.err;

    /** Our current message output status. Follows Project.MSG_XXX. */
    private int msgOutputLevel = Project.MSG_INFO;

    /** File that we are using for configuration. */
    private File buildFile; /* null */

    private File buildModule; /* null */

    /** The build targets. */
    private List<String> targets = new ArrayList<String>(1);
    /** Set of properties that can be used by tasks. */
    private Properties definedProps = new Properties();

    /** Names of classes to add as listeners to project. */
    private List<String> listeners = new ArrayList<String>(1);

    /** Indicates whether this build is to support interactive input */
    private boolean allowInput = true;

    /** keep going mode */
    private boolean keepGoingMode = false;

    private boolean showMemoryDetails = false;

    /**
     * The Ant logger class. There may be only one logger. It will have the right to use the 'out' PrintStream. The
     * class must implements the BuildLogger interface.
     */
    private String loggerClassname = null;

    /**
     * The Ant InputHandler class. There may be only one input handler.
     */
    private String inputHandlerClassname = null;

    /**
     * Whether or not output to the log is to be unadorned.
     */
    private boolean emacsMode = false;

    /**
     * optional thread priority
     */
    private Integer threadPriority = null;

    /**
     * proxy flag: default is false
     */
    private boolean proxy = false;

    private Set<PluginDescriptor> systemPlugins = new HashSet<PluginDescriptor>();

    private Set<String> activeBuildConfigurations = new HashSet<String>();

    private ClassLoader coreLoader;

    private boolean buildModuleLookupEnabled = false;

    private String easyantIvySettingsFile;
    private String easyantIvySettingsUrl;

    private boolean offline;

    /**
     * Get the ivysettings.xml file used by easyant
     * 
     * @return a string representing the ivysettings.xml used by easyant
     */
    public String getEasyantIvySettingsFile() {
        return easyantIvySettingsFile;
    }

    /**
     * Set the ivysettings.xml file used by easyant
     * 
     * @param easyantIvySettingsFile
     *            a string representing the ivysettings.xml used by easyant
     */
    public void setEasyantIvySettingsFile(String easyantIvySettingsFile) {
        this.easyantIvySettingsFile = easyantIvySettingsFile;
    }

    /**
     * Get the url of a ivysettings.xml used by easyant
     * 
     * @return a string representing the ivysettings.xml url used by easyant
     */
    public String getEasyantIvySettingsUrl() {
        return easyantIvySettingsUrl;
    }

    /**
     * Set the url of a ivysettings.xml used by easyant
     * 
     * @param easyantIvySettingsUrl
     *            a string representing the ivysettings.xml url used by easyant.
     */
    public void setEasyantIvySettingsUrl(String easyantIvySettingsUrl) {
        this.easyantIvySettingsUrl = easyantIvySettingsUrl;
    }

    /**
     * Set the url of a ivysettings.xml used by easyant
     * 
     * @param easyantIvySettingsUrl
     *            a string representing the ivysettings.xml url used by easyant.
     */
    public void setEasyantIvySettingsUrl(URL easyantIvySettingsUrl) {
        this.easyantIvySettingsUrl = easyantIvySettingsUrl.toString();
    }

    /**
     * Get the output level Default is Project.MSG_INFO Follows Project.MSG_XXX.
     * 
     * @return a int representing the output level
     */
    public int getMsgOutputLevel() {
        return msgOutputLevel;
    }

    /**
     * Set the output level Default is Project.MSG_INFO Follows Project.MSG_XXX.
     * 
     * @param msgOutputLevel
     *            a int representing the output level
     */
    public void setMsgOutputLevel(int msgOutputLevel) {
        this.msgOutputLevel = msgOutputLevel;
    }

    /**
     * Get the buildFile that will be included
     * 
     * @return a file representing the buildFile included in this project
     */
    public File getBuildFile() {
        return buildFile;
    }

    /**
     * Set the buildFile that will be included
     * 
     * @param buildFile
     *            a file representing the buildFile included in this project
     */
    public void setBuildFile(File buildFile) {
        this.buildFile = buildFile;
    }

    /**
     * Get the buildModule that will be included
     * 
     * @return a file representing the buildModule included in this project
     */
    public File getBuildModule() {
        return buildModule;
    }

    /**
     * Set the buildModule that will be included
     * 
     * @param buildModule
     *            a file representing the buildModule included in this project
     */
    public void setBuildModule(File buildModule) {
        this.buildModule = buildModule;
    }

    /**
     * Get a Set of properties that can be used by tasks.
     * 
     * @return a Set of properties that can be used by tasks.
     */
    public Properties getDefinedProps() {
        return definedProps;
    }

    /**
     * Set of properties that can be used by tasks.
     * 
     * @param definedProps
     *            a Set of properties that can be used by tasks.
     */
    public void setDefinedProps(Properties definedProps) {
        this.definedProps = definedProps;
    }

    /**
     * Indicates whether this build is to support interactive input
     * 
     * @return true if input is allowed, false if we are in batchmode
     */
    public boolean isAllowInput() {
        return allowInput;
    }

    /**
     * Indicates whether this build is to support interactive input
     * 
     * @param allowInput
     */
    public void setAllowInput(boolean allowInput) {
        this.allowInput = allowInput;
    }

    public boolean isKeepGoingMode() {
        return keepGoingMode;
    }

    public void setKeepGoingMode(boolean keepGoingMode) {
        this.keepGoingMode = keepGoingMode;
    }

    /**
     * Should we print memory details?
     * 
     * @return true if we want to print memory details
     */
    public boolean isShowMemoryDetails() {
        return showMemoryDetails;
    }

    /**
     * Set to true if you want to print memory details Default is false.
     * 
     * @param showMemoryDetails
     */
    public void setShowMemoryDetails(boolean showMemoryDetails) {
        this.showMemoryDetails = showMemoryDetails;
    }

    /**
     * Get the default logger classname The Ant logger class. There may be only one logger. It will have the right to
     * use the 'out' PrintStream. The class must implements the BuildLogger interface.
     * 
     * @return a string representing the logger classname
     */
    public String getLoggerClassname() {
        return loggerClassname;
    }

    /**
     * Set the default logger classname The Ant logger class. There may be only one logger. It will have the right to
     * use the 'out' PrintStream. The class must implements the BuildLogger interface.
     * 
     * @param loggerClassname
     *            a string representing the logger classname
     */
    public void setLoggerClassname(String loggerClassname) {
        this.loggerClassname = loggerClassname;
    }

    /**
     * The Ant InputHandler class. There may be only one input handler.
     * 
     * @return a string representing the input handler classname
     */
    public String getInputHandlerClassname() {
        return inputHandlerClassname;
    }

    /**
     * set The Ant InputHandler class. There may be only one input handler.
     * 
     * @param inputHandlerClassname
     *            a string representing the input handler classname
     */
    public void setInputHandlerClassname(String inputHandlerClassname) {
        this.inputHandlerClassname = inputHandlerClassname;
    }

    /**
     * Whether or not output to the log is to be unadorned.
     * 
     * @return true if the output to the log is to be unadorned, otherwise false
     */
    public boolean isEmacsMode() {
        return emacsMode;
    }

    /**
     * Whether or not output to the log is to be unadorned.
     * 
     * @param emacsMode
     */
    public void setEmacsMode(boolean emacsMode) {
        this.emacsMode = emacsMode;
    }

    /**
     * Get the threadPriority
     * 
     * @return a integer representing the thread priority.
     */
    public Integer getThreadPriority() {
        return threadPriority;
    }

    public void setThreadPriority(Integer threadPriority) {
        this.threadPriority = threadPriority;
    }

    public boolean isProxy() {
        return proxy;
    }

    public void setProxy(boolean proxy) {
        this.proxy = proxy;
    }

    public List<String> getTargets() {
        return targets;
    }

    public void setTargets(List<String> targets) {
        this.targets = targets;
    }

    /**
     * Names of classes to add as listeners to project.
     * 
     * @return a list of listerners
     */
    public List<String> getListeners() {
        return listeners;
    }

    /**
     * Names of classes to add as listeners to project.
     * 
     * @param listeners
     */
    public void setListeners(List<String> listeners) {
        this.listeners = listeners;
    }

    /**
     * Stream to use for logging.
     * 
     * @return a stream used for logging
     */
    public PrintStream getOut() {
        return out;
    }

    /**
     * Stream to use for logging.
     * 
     * @param out
     *            stream to use for logging
     */
    public void setOut(PrintStream out) {
        this.out = out;
    }

    /**
     * Stream that we are using for logging error messages.
     * 
     * @return stream used for error logging
     */
    public PrintStream getErr() {
        return err;
    }

    /**
     * Stream that we are using for logging error messages.
     * 
     * @param err
     *            stream used for error logging
     */
    public void setErr(PrintStream err) {
        this.err = err;
    }

    /**
     * Get a set of system plugins that will be included everytime
     * 
     * @return a set of system plugins that will be included everytime
     */
    public Set<PluginDescriptor> getSystemPlugins() {
        return systemPlugins;
    }

    /**
     * set of system plugins that will be included everytime
     * 
     * @param systemPlugins
     *            a set of system plugins that will be included everytime
     */
    protected void setSystemPlugins(Set<PluginDescriptor> systemPlugins) {
        this.systemPlugins = systemPlugins;
    }

    /**
     * Add a system plugin that will be included everytime
     * 
     * @param pluginDescriptor
     *            representing a plugin
     * @return true if the plugin has been added to the list of system plugins, otherwise false
     */
    public boolean addSystemPlugin(PluginDescriptor pluginDescriptor) {
        if (pluginDescriptor == null) {
            throw new IllegalArgumentException("pluginDescriptor cannot be null");
        }
        return this.systemPlugins.add(pluginDescriptor);
    }

    public ClassLoader getCoreLoader() {
        return coreLoader;
    }

    public void setCoreLoader(ClassLoader coreLoader) {
        this.coreLoader = coreLoader;
    }

    /**
     * Search parent directories for the build file.
     * 
     * @return true if easyantEngine should lookup for buildModule
     */
    public boolean isBuildModuleLookupEnabled() {
        return this.buildModuleLookupEnabled;
    }

    /**
     * Search parent directories for the build file.
     * 
     * @param buildModuleLookupEnabled
     *            true if easyantEngine should lookup for buildModule
     */
    public void setBuildModuleLookupEnabled(boolean buildModuleLookupEnabled) {
        this.buildModuleLookupEnabled = buildModuleLookupEnabled;
    }

    public Set<String> getActiveBuildConfigurations() {
        return activeBuildConfigurations;
    }

    public void setActiveBuildConfigurations(Set<String> buildConfigurations) {
        this.activeBuildConfigurations = buildConfigurations;
    }

    public void setOffline(boolean offline) {
        this.offline = offline;
    }

    public boolean isOffline() {
        return offline;
    }
}
