package org.apache.easyant.tasks;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.apache.easyant.core.EasyAntConfiguration;
import org.apache.easyant.core.EasyAntEngine;
import org.apache.easyant.core.factory.EasyantConfigurationFactory;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Configure easyant ivy instance in current project
 *
 */
public class ConfigureEasyAntIvyInstance extends Task {
    private EasyAntConfiguration easyantConfiguration = new EasyAntConfiguration();

    @Override
    public void execute() throws BuildException {
        EasyAntEngine eaEngine = new EasyAntEngine(getEasyantConfiguration());
        eaEngine.configureEasyAntIvyInstance(getProject());
    }

    public void setConfigurationFile(String configurationFile) {
        File f = new File(configurationFile);
        try {
            EasyantConfigurationFactory.getInstance()
                    .createConfigurationFromFile(getEasyantConfiguration(),
                            f.toURL());
        } catch (Exception e) {
            throw new BuildException(
                    "Can't create easyantConfiguration from File "
                            + configurationFile, e);
        }
    }

    public void setConfigurationUrl(String configurationUrl) {
        try {
            URL url = new URL(configurationUrl);
            EasyantConfigurationFactory
                    .getInstance()
                    .createConfigurationFromFile(getEasyantConfiguration(), url);

        } catch (Exception e) {
            throw new BuildException(
                    "Can't create easyantConfiguration from URL "
                            + configurationUrl, e);
        }
    }

    public void setBuildConfiguration(String buildConfiguration) {
        String[] buildConfs = buildConfiguration.split(",");
        Set<String> buildConfigurations = new HashSet<String>();
        for (String conf : buildConfs) {
            buildConfigurations.add(conf);
        }
        getEasyantConfiguration().setActiveBuildConfigurations(
                buildConfigurations);
    }

    public EasyAntConfiguration getEasyantConfiguration() {
        return easyantConfiguration;
    }

    public void setEasyantConfiguration(
            EasyAntConfiguration easyantConfiguration) {
        this.easyantConfiguration = easyantConfiguration;
    }

}
