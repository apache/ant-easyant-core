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
package org.apache.easyant.core;

public interface EasyAntConstants {

    /**
     * Name of the task name used for internal easyant jobs
     */
    String EASYANT_TASK_NAME = "easyant";

    /**
     * Name of the default build module
     */
    String DEFAULT_BUILD_MODULE = "module.ivy";

    /**
     * Name of the default build file
     */
    String DEFAULT_BUILD_FILE = "module.ant";

    /**
     * Name of the default override build file
     */
    String DEFAULT_OVERRIDE_BUILD_FILE = "override.module.ant";

    /**
     * URI of easyant module descriptor namespace
     */
    String EASYANT_MD_NAMESPACE = "http://www.easyant.org";

    /**
     * Name of the default target
     */
    String DEFAULT_TARGET = "doit";

    /**
     * Name of the default easyant plugins organisation
     */
    String EASYANT_PLUGIN_ORGANISATION = "org.apache.easyant.plugins";

    /**
     * Name of the default easyant build types organisation
     */
    String EASYANT_BUILDTYPES_ORGANISATION = "org.apache.easyant.buildtypes";

    /**
     * Name of the default easyant skeletons organisation
     */
    String EASYANT_SKELETONS_ORGANISATION = "org.apache.easyant.skeletons";

    String DEFAULT_USER_PROJECT_IVYSETTINGS = "${user.home}/.easyant/project-ivysettings.xml";

    /**
     * Default location of user's easyant ivysettings file
     */
    String DEFAULT_USER_EASYANT_IVYSETTINGS = "${user.home}/.easyant/easyant-ivysettings.xml";

    /**
     * Default location of global easyant ivysettings file
     */
    String DEFAULT_GLOBAL_EASYANT_IVYSETTINGS = "${easyant.home}/easyant-ivysettings.xml";

    /**
     * Default location of global easyant config file
     */
    String DEFAULT_GLOBAL_EASYANT_CONF_FILE = "easyant-conf.xml";

    /**
     * Default value for pre module targets
     */
    String DEFAULT_PRE_MODULE_TARGETS = "clean";

    /**
     * Default build scope repository name
     */
    String BUILD_SCOPE_REPOSITORY = "project-build";

    /**
     * Default offline project repository name
     */
    String DEFAULT_OFFLINE_PROJECT_RESOLVER = "project.buildscope.repository";

    /**
     * Default offline easyant repository name
     */
    String DEFAULT_OFFLINE_EASYANT_RESOLVER = "easyant.buildscope.repository";

}
