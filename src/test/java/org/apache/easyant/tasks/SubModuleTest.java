package org.apache.easyant.tasks;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.easyant.core.ant.listerners.BuildExecutionTimer;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class SubModuleTest extends AntTaskBaseTest {
    private File cache;

    private SubModule submodule;
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() throws MalformedURLException, URISyntaxException {
        createCache();
        Project project = new Project();

        // FIXME: property are not yet inherited
        project.setUserProperty("ivy.cache.dir", cache.getAbsolutePath());
        File f = new File(this.getClass().getResource("/repositories/easyant-ivysettings-test.xml").toURI());
        // FIXME: property are not yet inherited
        project.setUserProperty(EasyAntMagicNames.USER_EASYANT_IVYSETTINGS, f.getAbsolutePath());

        submodule = new SubModule();
        submodule.setProject(project);
    }

    private void createCache() {
        cache = new File("build/cache");
        cache.mkdirs();
    }

    @After
    public void tearDown() throws Exception {
        cleanCache();
    }

    private void cleanCache() {
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(cache);
        del.execute();
    }

    @Test(expected = BuildException.class)
    public void shouldFailIfNoMandatoryAttributesAreSet() {
        submodule.execute();
    }

    @Test
    public void shouldNotFailIfBuildpathAttributeIsSet() {
        configureProject(submodule.getProject(), Project.MSG_WARN);

        Path path = new Path(submodule.getProject());
        path.createPath();

        submodule.setBuildpath(path);
        submodule.execute();

        assertLogContaining("No sub-builds to iterate on");
    }

    @Test
    public void shouldFailIfPathContainsInvalidFile() {
        expectedException.expect(BuildException.class);
        expectedException.expectMessage("Invalid file:");

        configureProject(submodule.getProject(), Project.MSG_WARN);

        Path path = new Path(submodule.getProject());
        File file2 = new File("anotherfile");
        path.createPathElement().setLocation(file2);
        path.createPath();

        submodule.setBuildpath(path);
        submodule.execute();

    }

    @Test
    public void shouldRunEvenIfNoTargetsAreSet() throws URISyntaxException {
        configureProject(submodule.getProject(), Project.MSG_DEBUG);

        Path path = new Path(submodule.getProject());
        FileSet fs = new FileSet();
        File multimodule = new File(this.getClass().getResource("multimodule").toURI());
        fs.setDir(multimodule);
        path.addFileset(fs);
        path.createPath();

        submodule.setBuildpath(path);
        submodule.execute();

        assertLogContaining("Executing [] on module1");
        assertLogContaining("Executing [] on module2");
        assertLogContaining("Skipping sub-project build because no matching targets were found");

    }

    @Test
    public void shouldRunEvenIfTargetDoesntExistsInSubModules() throws URISyntaxException {
        configureProject(submodule.getProject(), Project.MSG_DEBUG);

        Path path = new Path(submodule.getProject());
        FileSet fs = new FileSet();
        File multimodule = new File(this.getClass().getResource("multimodule").toURI());
        fs.setDir(multimodule);
        path.addFileset(fs);
        path.createPath();

        submodule.setBuildpath(path);
        submodule.setTarget("a-missing-target");
        submodule.execute();

        assertLogContaining("Executing [a-missing-target] on module1");
        assertLogContaining("Skipping undefined target 'a-missing-target' on module1");
        assertLogContaining("Executing [a-missing-target] on module2");
        assertLogContaining("Skipping undefined target 'a-missing-target' on module2");
        assertLogContaining("Skipping sub-project build because no matching targets were found");

    }

    @Test
    public void shouldRunMyTargetOnBothModule() throws URISyntaxException {
        configureProject(submodule.getProject(), Project.MSG_DEBUG);

        Path path = new Path(submodule.getProject());
        FileSet fs = new FileSet();
        File multimodule = new File(this.getClass().getResource("multimodule").toURI());
        fs.setDir(multimodule);
        path.addFileset(fs);
        path.createPath();

        submodule.setBuildpath(path);
        submodule.setTarget("modulewithtarget:mytarget");
        submodule.execute();

        assertLogContaining("Executing [modulewithtarget:mytarget] on module1");
        assertLogContaining("Executing [modulewithtarget:mytarget] on module2");

        assertThat(submodule.getProject().getReference(BuildExecutionTimer.EXECUTION_TIMER_SUBBUILD_RESULTS),
                notNullValue());
    }
}
