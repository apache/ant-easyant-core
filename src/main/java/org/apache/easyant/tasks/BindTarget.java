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
package org.apache.easyant.tasks;

import java.util.Enumeration;
import java.util.Iterator;

import org.apache.easyant.core.BuildConfigurationHelper;
import org.apache.easyant.core.ant.Phase;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;

public class BindTarget extends Task {

	private String target;
	private String toPhase;

	private String buildConfigurations;

	public void execute() throws BuildException {
		StringBuilder message = new StringBuilder();
		message.append("Phase mapping for target ").append(getTarget()).append(
				" ");
		if (!BuildConfigurationHelper.isBuildConfigurationActive(
				getBuildConfigurations(), getProject(), message.toString())) {
			log(
					"no matching build configuration for this phase mapping, this mapping will be ignored",
					Project.MSG_DEBUG);
			return;
		}
		Target t = (Target) getProject().getTargets().get(getTarget());
		if (t == null) {
			throw new BuildException("unable to find target " + getTarget());
		}

		// unbind current mapping
		for (Iterator iterator = getProject().getTargets().values().iterator(); iterator
				.hasNext();) {
			Target current = (Target) iterator.next();
			if (current instanceof Phase) {
				Enumeration dependencies = current.getDependencies();
				StringBuilder dependsOn = new StringBuilder();
				boolean requiresUpdates = false;
				while (dependencies.hasMoreElements()) {
					String dep = (String) dependencies.nextElement();
					if (dep.equals(getTarget())) {
						log("target" + getTarget() + " is registred in phase"
								+ current.getName(), Project.MSG_VERBOSE);
						requiresUpdates = true;
					} else {
						dependsOn.append(dep);
						dependsOn.append(",");
					}
				}
				if (requiresUpdates) {
					log("removing target" + getTarget() + " from phase"
							+ current.getName(), Project.MSG_VERBOSE);

					Phase p = new Phase();
					p.setDescription(current.getDescription());
					p.setIf(current.getIf());
					p.setLocation(current.getLocation());
					p.setName(current.getName());
					p.setProject(current.getProject());
					p.setUnless(current.getUnless());
					String depends = dependsOn.toString();
					if (depends.endsWith(",")) {
						depends = depends.substring(0, depends.length() - 1);
					}
					p.setDepends(depends);
					getProject().addOrReplaceTarget(p);
				}

			}
		}

		if (getToPhase() != null && !getToPhase().equals("")) {
			if (!getProject().getTargets().containsKey(getToPhase())) {
				throw new BuildException("can't add target " + getTarget()
						+ " to phase " + getToPhase() + " because the phase"
						+ " is unknown.");
			}
			Target p = (Target) getProject().getTargets().get(getToPhase());

			if (!(p instanceof Phase)) {
				throw new BuildException("referenced target " + getToPhase()
						+ " is not a phase");
			}
			p.addDependency(getTarget());
		}

	}

	public String getToPhase() {
		return toPhase;
	}

	public void setToPhase(String toPhase) {
		this.toPhase = toPhase;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public String getBuildConfigurations() {
		return buildConfigurations;
	}

	public void setBuildConfigurations(String buildConfigurations) {
		this.buildConfigurations = buildConfigurations;
	}

	public void setConf(String conf) {
		this.buildConfigurations = conf;
	}

}
