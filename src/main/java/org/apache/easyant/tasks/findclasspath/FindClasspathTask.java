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
package org.apache.easyant.tasks.findclasspath;

import java.util.ArrayList;
import java.util.List;

import org.apache.easyant.tasks.AbstractEasyAntTask;
import org.apache.tools.ant.BuildException;

/**
 * This task provides a mechanism to build a plugin classpath depending on rules.
 */
public class FindClasspathTask extends AbstractEasyAntTask {
    private String pathid;
    private String organisation;
    private String module;
    private String revision;
    private String conf = "default";

    private AbstractFindClassPathStrategy currentStrategy;
    private List<AbstractFindClassPathStrategy> strategies = new ArrayList<AbstractFindClassPathStrategy>();

    public void execute() throws BuildException {
        // define the default strategy if there is no nested strategy
        if (getStrategies().isEmpty()) {
            add(createStrategy(new ProjectDependencyStrategy()));
            add(createStrategy(new BasicConfigurationStrategy()));
        }
        // Process the chain
        getStrategies().get(0).check();
    }

    public String getPathid() {
        return pathid;
    }

    public void setPathid(String pathid) {
        this.pathid = pathid;
    }

    public String getOrganisation() {
        return organisation;
    }

    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public String getConf() {
        return conf;
    }

    public void setConf(String conf) {
        this.conf = conf;
    }

    public List<AbstractFindClassPathStrategy> getStrategies() {
        return strategies;
    }

    public void setStrategies(List<AbstractFindClassPathStrategy> strategies) {
        this.strategies = strategies;
    }

    public void add(AbstractFindClassPathStrategy strategy) {
        if (currentStrategy != null) {
            currentStrategy.setNextStrategy(strategy);
        }
        currentStrategy = strategy;
        preconfigureStrategy(strategy);
        getStrategies().add(strategy);
    }

    protected void preconfigureStrategy(AbstractFindClassPathStrategy strategy) {
        strategy.setPathid(getPathid());
        if (strategy instanceof BasicConfigurationStrategy) {
            BasicConfigurationStrategy basicStrategy = (BasicConfigurationStrategy) strategy;
            if (basicStrategy.getOrganisation() == null) {
                basicStrategy.setOrganisation(getOrganisation());
            }
            if (basicStrategy.getModule() == null) {
                basicStrategy.setModule(getModule());

            }
            if (basicStrategy.getRevision() == null) {
                basicStrategy.setRevision(getRevision());
            }
            if (basicStrategy.getConf() == null) {
                basicStrategy.setConf(getConf());
            }
        }
    }

    protected AbstractFindClassPathStrategy createStrategy(AbstractFindClassPathStrategy strategy) {
        strategy.setProject(getProject());
        return strategy;
    }

}
