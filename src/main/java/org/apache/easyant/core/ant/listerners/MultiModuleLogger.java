/* 
 *  Copyright 2008-2010 the EasyAnt project
 * 
 *  See the NOTICE file distributed with this work for additional information
 *  regarding copyright ownership. 
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software 
 *  distributed under the License is distributed on an "AS IS" BASIS, 
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and 
 *  limitations under the License.
 */
package org.apache.easyant.core.ant.listerners;

import java.io.File;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.listener.TimestampedLogger;
import org.apache.tools.ant.util.StringUtils;

public class MultiModuleLogger extends DefaultEasyAntLogger {

	private volatile boolean subBuildStartedRaised = false;
	private final Object subBuildLock = new Object();

	// CheckStyle:VisibilityModifier OFF - bc
	/**
	 * Name of the current target, if it should be displayed on the next
	 * message. This is set when a target starts building, and reset to
	 * <code>null</code> after the first message for the target is logged.
	 */
	protected String targetName;
	// CheckStyle:VisibilityModifier ON

	/**
	 * Header string for the log. * {@value}
	 */
	public static final String HEADER = "======================================================================";
	/**
	 * Footer string for the log. * {@value}
	 */
	public static final String FOOTER = HEADER;

	/**
	 * This is an override point: the message that indicates whether a build
	 * failed. Subclasses can change/enhance the message.
	 * 
	 * @return The classic "BUILD FAILED" plus a timestamp
	 */
	protected String getBuildFailedMessage() {
		return super.getBuildFailedMessage() + TimestampedLogger.SPACER
				+ getTimestamp();
	}

	/**
	 * This is an override point: the message that indicates that a build
	 * succeeded. Subclasses can change/enhance the message.
	 * 
	 * @return The classic "BUILD SUCCESSFUL" plus a timestamp
	 */
	protected String getBuildSuccessfulMessage() {
		return super.getBuildSuccessfulMessage() + TimestampedLogger.SPACER
				+ getTimestamp();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param event
	 */
	public void targetStarted(BuildEvent event) {
		maybeRaiseSubBuildStarted(event);
		targetName = extractTargetName(event);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param event
	 */
	public void taskStarted(BuildEvent event) {
		maybeRaiseSubBuildStarted(event);
		super.taskStarted(event);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param event
	 */
	public void buildFinished(BuildEvent event) {
		maybeRaiseSubBuildStarted(event);
		subBuildFinished(event);
		super.buildFinished(event);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param event
	 */
	public void messageLogged(BuildEvent event) {
		maybeRaiseSubBuildStarted(event);
		super.messageLogged(event);
		if (event.getPriority() > msgOutputLevel || null == event.getMessage()
				|| "".equals(event.getMessage().trim())) {
			return;
		}

		synchronized (this) {
			if (null != targetName) {
				out.println(StringUtils.LINE_SEP + targetName + ":");
				targetName = null;
			}
		}

	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param event
	 *            An event with any relevant extra information. Must not be
	 *            <code>null</code>.
	 */
	public void subBuildStarted(BuildEvent event) {
		String name = extractNameOrDefault(event);
		Project project = event.getProject();

		File base = project == null ? null : project.getBaseDir();
		String path = (base == null) ? "With no base directory" : "In "
				+ base.getAbsolutePath();
		printMessage(StringUtils.LINE_SEP + getHeader() + StringUtils.LINE_SEP
				+ "Entering project " + name + StringUtils.LINE_SEP + path
				+ StringUtils.LINE_SEP + getFooter(), out, event.getPriority());
	}

	/**
	 * Get the name of an event
	 * 
	 * @param event
	 *            the event name
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

	/** {@inheritDoc} */
	public void subBuildFinished(BuildEvent event) {
		String name = extractNameOrDefault(event);
		String failed = event.getException() != null ? "failing " : "";
		printMessage(StringUtils.LINE_SEP + getHeader() + StringUtils.LINE_SEP
				+ "Exiting " + failed + "project " + name
				+ StringUtils.LINE_SEP + getFooter(), out, event.getPriority());
	}

	/**
	 * Override point: return the header string for the entry/exit message
	 * 
	 * @return the header string
	 */
	protected String getHeader() {
		return HEADER;
	}

	/**
	 * Override point: return the footer string for the entry/exit message
	 * 
	 * @return the footer string
	 */
	protected String getFooter() {
		return FOOTER;
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
	 * @param event
	 *            the event to work on
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

}
