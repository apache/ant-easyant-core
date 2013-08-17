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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.apache.tools.ant.Project;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class BuildConfigurationHelperTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void shouldCreateBuildListWithTwoElements() {
        List<String> buildList = BuildConfigurationHelper.buildList("a-conf,another-conf");
        assertThat(buildList.size(), is(2));
    }

    @Test
    public void shouldReturnFalseIfBuildContainerIsNull() {
        assertThat(BuildConfigurationHelper.contains("a-conf", null), is(false));
    }

    @Test
    public void shouldReturnFalseIfBuildConfsIsNull() {
        assertThat(BuildConfigurationHelper.contains(null, "a-conf"), is(true));
        assertThat(BuildConfigurationHelper.contains(null, null), is(true));
    }

    @Test
    public void shouldReturnTrueIfContainsBuildConfs() {
        assertThat(BuildConfigurationHelper.contains("a-conf", "a-conf"), is(true));
    }

    @Test
    public void shouldReturnFalseIfDoesntContainsBuildConfs() {
        assertThat(BuildConfigurationHelper.contains("a-rmissing-conf", "a-conf"), is(false));
    }

    @Test
    public void shouldReturnNullIfBuildConfOrBuildContainerIsNull() {
        assertThat(BuildConfigurationHelper.getFirstBuildConfigurationMatching(null, null), nullValue());
        assertThat(BuildConfigurationHelper.getFirstBuildConfigurationMatching("a-conf", null), nullValue());
        assertThat(BuildConfigurationHelper.getFirstBuildConfigurationMatching(null, "a-conf"), nullValue());
    }

    @Test
    public void shouldReturnBuildConfigurationIfPresentInBuildContainer() {
        assertThat(BuildConfigurationHelper.getFirstBuildConfigurationMatching("aconf", "aconf"), is("aconf"));
        assertThat(BuildConfigurationHelper.getFirstBuildConfigurationMatching("aconf", "aconf,anotherconf"),
                is("aconf"));
    }

    @Test
    public void shouldReturnNullIfNotPresentInBuildContainer() {
        assertThat(BuildConfigurationHelper.getFirstBuildConfigurationMatching("a-missing-conf", "aconf"), nullValue());
    }

    @Test
    public void shouldReturnTrueIfNotBoundToABuildConfiguration() {
        Project p = new Project();
        boolean buildConfigurationActive = BuildConfigurationHelper.isBuildConfigurationActive(null, p, "a-message");

        assertThat(buildConfigurationActive, is(true));
    }

    @Test
    public void shouldFailIfNoBuildConfigurationAreAvailable() {
        expectedException.expectMessage(is("there is no available build configuration"));
        Project p = new Project();
        BuildConfigurationHelper.isBuildConfigurationActive("a-missing-conf", p, "a-message");
    }

    @Test
    public void shouldFailIfBuildConfigurationIsUnknown() {
        expectedException.expectMessage(is("unknown build configuration named a-missing-conf"));
        Project p = new Project();
        p.setNewProperty(EasyAntMagicNames.AVAILABLE_BUILD_CONFIGURATIONS, "a-conf");
        BuildConfigurationHelper.isBuildConfigurationActive("a-missing-conf", p, "a-message");
    }

    @Test
    public void shouldReturnFalseIfBuildConfigurationIsNotActive() {
        Project p = new Project();
        p.setNewProperty(EasyAntMagicNames.AVAILABLE_BUILD_CONFIGURATIONS, "a-conf,another-conf");
        p.setNewProperty(EasyAntMagicNames.MAIN_CONFS, "a-conf");
        boolean buildConfigurationActive = BuildConfigurationHelper.isBuildConfigurationActive("another-conf", p,
                "a-message");

        assertThat(buildConfigurationActive, is(false));
    }

    @Test
    public void shouldReturnFalseIfNoActiveBuildConfiguration() {
        Project p = new Project();
        p.setNewProperty(EasyAntMagicNames.AVAILABLE_BUILD_CONFIGURATIONS, "a-conf,another-conf");
        boolean buildConfigurationActive = BuildConfigurationHelper.isBuildConfigurationActive("another-conf", p,
                "a-message");

        assertThat(buildConfigurationActive, is(false));
    }

    @Test
    public void shouldReturnTrueIfBuildConfigurationIsActive() {
        Project p = new Project();
        p.setNewProperty(EasyAntMagicNames.AVAILABLE_BUILD_CONFIGURATIONS, "a-conf,another-conf");
        p.setNewProperty(EasyAntMagicNames.MAIN_CONFS, "a-conf");
        boolean buildConfigurationActive = BuildConfigurationHelper
                .isBuildConfigurationActive("a-conf", p, "a-message");

        assertThat(buildConfigurationActive, is(true));
    }

    @Test
    public void shouldRemoveSpaces() {
        assertThat(BuildConfigurationHelper.removeSpaces("aconf, another-conf"), is("aconf,another-conf"));
        assertThat(BuildConfigurationHelper.removeSpaces("aconf,another conf"), is("aconf,anotherconf"));
    }
}
