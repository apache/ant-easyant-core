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

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.easyant.core.EasyAntConstants;
import org.apache.ivy.Ivy;
import org.apache.ivy.ant.IvyTask;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.input.InputHandler;
import org.apache.tools.ant.input.InputRequest;
import org.apache.tools.ant.input.MultipleChoiceInputRequest;

public class SearchModule extends IvyTask {

    private String organisation;

    private String module;

    private String branch = PatternMatcher.ANY_EXPRESSION;

    private String revision;

    private String matcher = PatternMatcher.EXACT_OR_REGEXP;

    private String propertyPrefix;
    private String resolver;

    public void doExecute() throws BuildException {
        if (getOrganisation() == null) {
            throw new BuildException("no organisation provided for SearchModule task");
        }
        if (getModule() == null) {
            throw new BuildException("no module name provided for SearchModule task");
        }
        if (getRevision() == null) {
            throw new BuildException("no revision provided for SearchModule task");
        }

        if (getPropertyPrefix() == null) {
            throw new BuildException("no property prefix provided provided for SearchModule task");
        }
        Ivy ivy = getIvyInstance();
        IvySettings settings = ivy.getSettings();

        // search all modules revision matching the requested criteria
        DependencyResolver resolverToCheck;
        if (getResolver() != null) {
            resolverToCheck = settings.getResolver(getResolver());
        } else {
            resolverToCheck = settings.getDefaultResolver();
        }
        ModuleRevisionId mridToSearch = ModuleRevisionId.newInstance(getOrganisation(), getModule(), getBranch(),
                getRevision());
        ModuleRevisionId[] mrids = ivy.getSearchEngine().listModules(resolverToCheck, mridToSearch,
                settings.getMatcher(matcher));

        // diplay the list
        List<String> choices = new ArrayList<String>();
        for (int i = 0; i < mrids.length; i++) {
            ResolvedModuleRevision rmr = ivy.findModule(mrids[i]);
            if (rmr == null) {
                log("Can't retrieve " + mrids[i].toString(), Project.MSG_WARN);
            } else {

                choices.add(String.valueOf(i));
                StringBuilder sb = new StringBuilder();
                sb.append(i).append(": ");
                sb.append(mrids[i].getName());
                sb.append(" v").append(mrids[i].getRevision());
                // hide organization if its the default one
                if (!EasyAntConstants.EASYANT_SKELETONS_ORGANISATION.equals(mrids[i].getOrganisation())) {
                    sb.append(" by ").append(mrids[i].getOrganisation());
                }
                // Get the description
                if (rmr.getDescriptor().getDescription() != null && !rmr.getDescriptor().getDescription().equals("")) {
                    sb.append(" (").append(rmr.getDescriptor().getDescription()).append(")");
                }
                log(sb.toString());
            }
        }
        if (choices.isEmpty()) {
            throw new BuildException("No matching module were found !");
        }

        // ask end user to select a module
        // TODO handle a default value
        Integer value = Integer.valueOf(getInput("Choose a number:", null, choices));
        ModuleRevisionId moduleToRetrieve = mrids[value];

        // set final properties
        getProject().setProperty(getPropertyPrefix() + ".org", moduleToRetrieve.getOrganisation());
        getProject().setProperty(getPropertyPrefix() + ".module", moduleToRetrieve.getName());
        getProject().setProperty(getPropertyPrefix() + ".rev", moduleToRetrieve.getRevision());

    }

    protected String getInput(String message, String defaultvalue, List<String> choices) {
        InputRequest request;
        request = new MultipleChoiceInputRequest(message, new Vector(choices));
        request.setDefaultValue(defaultvalue);

        InputHandler h = getProject().getInputHandler();

        h.handleInput(request);

        String value = request.getInput();
        if ((value == null || value.trim().isEmpty()) && defaultvalue != null) {
            value = defaultvalue;
        }
        return value;

    }

    public String getMatcher() {
        return matcher;
    }

    public void setMatcher(String matcher) {
        this.matcher = matcher;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getOrganisation() {
        return organisation;
    }

    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getPropertyPrefix() {
        return propertyPrefix;
    }

    public void setPropertyPrefix(String propertyPrefix) {
        this.propertyPrefix = propertyPrefix;
    }

    public String getResolver() {
        return resolver;
    }

    public void setResolver(String resolver) {
        this.resolver = resolver;
    }

}
