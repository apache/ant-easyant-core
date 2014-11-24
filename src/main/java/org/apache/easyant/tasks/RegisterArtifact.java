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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.ivy.ant.IvyPostResolveTask;
import org.apache.ivy.core.cache.ResolutionCacheManager;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.MDArtifact;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;
import org.apache.tools.ant.BuildException;

public class RegisterArtifact extends IvyPostResolveTask {

    private String name;
    private String type;
    private String ext;
    private String confs = "*";
    private String classifier;

    @Override
    public void doExecute() throws BuildException {
        prepareAndCheck();
        DefaultModuleDescriptor md = (DefaultModuleDescriptor) getResolvedReport().getModuleDescriptor();

        // this is a published artifact
        String artName = getSettings().substitute(getName());
        artName = artName == null ? md.getModuleRevisionId().getName() : artName;
        String artType = getSettings().substitute(getType());
        artType = artType == null ? "jar" : artType;
        String artExt = getSettings().substitute(getExt());
        artExt = artExt != null ? artExt : artType;
        Map<String, String> extraAttributes = new HashMap<String, String>();
        if (getClassifier() != null) {
            md.addExtraAttributeNamespace("m", "http://ant.apache.org/ivy/maven");
            extraAttributes.put("m:classifier", getClassifier());
        }
        MDArtifact artifact = new MDArtifact(md, artName, artType, artExt, null, extraAttributes);
        String[] configurations = getConfs().split(",");
        for (String configuration : configurations) {
            if ("*".equals(configuration)) {
                String[] declaredConfs = md.getConfigurationsNames();
                for (String declaredConf : declaredConfs) {
                    artifact.addConfiguration(declaredConf);
                    md.addArtifact(declaredConf, artifact);
                }
            } else {
                // create configuration if it doesn't exist
                if (md.getConfiguration(configuration) == null) {
                    Configuration generatedConfiguration = new Configuration(configuration);
                    md.addConfiguration(generatedConfiguration);
                }
                artifact.addConfiguration(configuration);
                md.addArtifact(configuration, artifact);
            }
        }

        ResolutionCacheManager cacheManager = getSettings().getResolutionCacheManager();
        File ivyFileInCache = cacheManager.getResolvedIvyFileInCache(md.getResolvedModuleRevisionId());
        try {
            XmlModuleDescriptorWriter.write(md, ivyFileInCache);
        } catch (IOException e) {
            throw new BuildException("Can't register specified artifact", e);
        }

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public String getConfs() {
        return confs;
    }

    public void setConfs(String confs) {
        this.confs = confs;
    }

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

}
