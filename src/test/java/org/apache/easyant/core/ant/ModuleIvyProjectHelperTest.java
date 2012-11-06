package org.apache.easyant.core.ant;

import java.io.File;
import java.net.URISyntaxException;

import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.ProjectHelperRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ModuleIvyProjectHelperTest {
    @Before
    public void setUp() {
        // ProjectHelperRepository.getInstance().registerProjectHelper("org.apache.easyant.core.ant.EasyAntProjectHelper");

        ProjectHelperRepository.getInstance().registerProjectHelper(
                "org.apache.easyant.core.ant.ModuleIvyProjectHelper");
    }

    @Test
    public void shouldHandleModuleIvyFile() throws URISyntaxException {
        File f = new File(this.getClass().getResource("../standardJavaProject.ivy").toURI());
        Project p = new Project();
        p.setNewProperty(EasyAntMagicNames.IGNORE_USER_IVYSETTINGS, "true");
        p.setNewProperty(EasyAntMagicNames.GLOBAL_EASYANT_IVYSETTINGS,
                this.getClass().getResource("/ivysettings-test.xml").toString());
        ProjectHelper.configureProject(p, f);
        Assert.assertNotNull(p.getTargets().get("clean"));
    }
}
