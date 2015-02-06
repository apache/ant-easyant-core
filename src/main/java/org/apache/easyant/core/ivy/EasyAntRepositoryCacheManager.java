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
package org.apache.easyant.core.ivy;

import org.apache.ivy.core.cache.DefaultRepositoryCacheManager;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorParser;
import org.apache.ivy.plugins.repository.url.URLResource;

import java.io.File;
import java.net.MalformedURLException;

/**
 * Fork default CacheManager as default cache resolver use {@link XmlModuleDescriptorParser} to resolve files from cache
 * If parent module is resolved from cache with {@link XmlModuleDescriptorParser} then easyant is not able to handle
 * inherit properties or plugins
 */
public class EasyAntRepositoryCacheManager extends DefaultRepositoryCacheManager {
    public EasyAntRepositoryCacheManager() {
        super();
    }

    public EasyAntRepositoryCacheManager(String name, IvySettings settings, File basedir) {
        super(name, settings, basedir);
    }


    @Override
    protected ModuleDescriptorParser getModuleDescriptorParser(File moduleDescriptorFile) {
        try {
            return ModuleDescriptorParserRegistry.getInstance().getParser(
                    new URLResource(moduleDescriptorFile.toURI().toURL()));
        } catch (MalformedURLException e) {
            throw new RuntimeException("Can't access to " + moduleDescriptorFile.getAbsolutePath(), e);
        }
    }
}
