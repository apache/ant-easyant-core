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

import java.net.URL;

public class EasyantConfigurationFactory {

    private static EasyantConfigurationFactory instance;
    private EasyAntConfigParser parser;

    protected EasyantConfigurationFactory() {
        parser = new EasyAntConfigParser();
    }

    public static EasyantConfigurationFactory getInstance() {
        if (instance == null) {
            instance = new EasyantConfigurationFactory();
        }
        return instance;
    }

    public EasyAntConfiguration createDefaultConfiguration() {
        return new EasyAntConfiguration();
    }

    public EasyAntConfiguration createConfigurationFromFile(final EasyAntConfiguration easyAntConfiguration,
            URL configUrl) throws Exception {

        return parser.parseAndMerge(configUrl, easyAntConfiguration);
    }

    public EasyAntConfiguration createConfigurationFromFile(URL configurationFile) throws Exception {
        EasyAntConfiguration easyAntConfiguration = createDefaultConfiguration();
        return createConfigurationFromFile(easyAntConfiguration, configurationFile);
    }

}
