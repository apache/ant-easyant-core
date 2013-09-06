package org.apache.easyant.core;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.net.URISyntaxException;

import org.apache.easyant.core.ant.listerners.MultiModuleLogger;
import org.apache.easyant.core.configuration.EasyAntConfiguration;
import org.apache.easyant.core.services.impl.DefaultPluginServiceImpl;
import org.apache.ivy.ant.IvyAntSettings;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.input.PropertyFileInputHandler;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.util.ProxySetup;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class EasyAntEngineTest {
    private EasyAntConfiguration easyAntConfiguration = new EasyAntConfiguration();
    private EasyAntEngine easyantEngine = new EasyAntEngine(easyAntConfiguration);
    private Project project = new Project();
    private File cache;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() throws URISyntaxException {
        createCache();
        project.setProperty("ivy.cache.dir", cache.getAbsolutePath());
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

    @Test
    public void shouldAddBuildListener() {
        easyAntConfiguration.getListeners().add(MultiModuleLogger.class.getCanonicalName());
        easyantEngine.addBuildListeners(project);
        assertThat(project.getBuildListeners(), hasItem(isA(MultiModuleLogger.class)));
    }

    @Test
    public void shouldSetInputHandler() {
        easyAntConfiguration.setInputHandlerClassname(PropertyFileInputHandler.class.getCanonicalName());
        easyantEngine.addInputHandler(project);
        assertThat(project.getInputHandler(), instanceOf(PropertyFileInputHandler.class));
    }

    @Test
    public void shouldSetDefaultInputHandler() {
        easyantEngine.addInputHandler(project);
        assertThat(project.getInputHandler(), instanceOf(DefaultInputHandler.class));
    }

    @Test
    public void shouldCreateLogger() {
        easyAntConfiguration.setLoggerClassname(MultiModuleLogger.class.getCanonicalName());
        BuildLogger logger = easyantEngine.createLogger();
        assertThat(logger, instanceOf(MultiModuleLogger.class));
    }

    @Test
    public void shouldFailIfLoggerDoesntExists() {
        expectedException
                .expectMessage("The specified logger class a-missing-logger could not be used because Class not found");
        easyAntConfiguration.setLoggerClassname("a-missing-logger");
        easyantEngine.createLogger();
    }

    @Test
    public void shouldFindFileInCurrentDirectory() throws URISyntaxException {
        File startFile = new File(this.getClass().getResource("multimodule/myapp-hello-world").toURI());
        File foundFile = easyantEngine.findBuildModule(startFile.getAbsolutePath(), "module.ivy");
        assertThat(foundFile.getName(), is("module.ivy"));
        assertThat(foundFile.getParentFile().getName(), is("myapp-hello-world"));
    }

    @Test
    public void shouldFindFileInParentDirectory() throws URISyntaxException {
        File startFile = new File(this.getClass().getResource("multimodule/myapp-hello-world").toURI());
        File foundFile = easyantEngine.findBuildModule(startFile.getAbsolutePath(), "parent.ivy");
        assertThat(foundFile.getName(), is("parent.ivy"));
        assertThat(foundFile.getParentFile().getName(), is("multimodule"));
    }

    @Test
    public void shouldFailIfNotFound() throws URISyntaxException {
        expectedException.expectMessage("Could not locate a build file!");
        File startFile = new File(this.getClass().getResource("multimodule/myapp-hello-world").toURI());
        easyantEngine.findBuildModule(startFile.getAbsolutePath(), "a-missing-file");
    }

    @Test
    public void shouldConfigurePluginService() throws URISyntaxException {
        IvyAntSettings ivyAntSettings = new IvyAntSettings();
        ivyAntSettings.setProject(project);
        ivyAntSettings.setFile(new File(this.getClass().getResource("/repositories/easyant-ivysettings-test.xml")
                .toURI()));

        easyantEngine.configurePluginService(project, ivyAntSettings);
        assertThat(project.getReference(EasyAntMagicNames.PLUGIN_SERVICE_INSTANCE),
                instanceOf(DefaultPluginServiceImpl.class));
        assertThat(easyantEngine.getPluginService(), instanceOf(DefaultPluginServiceImpl.class));
        assertThat(easyantEngine.getPluginService(),
                is(project.getReference(EasyAntMagicNames.PLUGIN_SERVICE_INSTANCE)));
    }

    @Test
    public void shouldConfigureProject() {
        easyantEngine.configureProject(project);
        assertThat(Thread.currentThread().getPriority(), is(Thread.NORM_PRIORITY));
        assertThat(easyAntConfiguration.getCoreLoader(), nullValue());
        assertThat(easyAntConfiguration.isProxy(), is(false));
        assertProjectIsConfigured();
    }

    @Test
    public void shouldConfigureProjectWithCustomPriority() {
        easyAntConfiguration.setThreadPriority(10);
        easyantEngine.configureProject(project);
        assertThat(Thread.currentThread().getPriority(), is(easyAntConfiguration.getThreadPriority()));
        assertProjectIsConfigured();
    }

    @Test
    public void shouldConfigureProjectWithCustomCoreLoader() {
        easyAntConfiguration.setCoreLoader(this.getClass().getClassLoader());
        easyantEngine.configureProject(project);
        assertProjectIsConfigured();
        assertThat(project.getCoreLoader(), is(this.getClass().getClassLoader()));
    }

    @Test
    public void shouldConfigureProjectWhenKeepGoingModeIsTrue() {
        easyAntConfiguration.setKeepGoingMode(true);
        easyantEngine.configureProject(project);
        assertProjectIsConfigured();
    }

    @Test
    public void shouldConfigureProjectWhenProxyIsTrue() {
        String oldValue = System.getProperty(ProxySetup.USE_SYSTEM_PROXIES);
        System.getProperties().remove(ProxySetup.USE_SYSTEM_PROXIES);

        easyAntConfiguration.setProxy(true);
        easyantEngine.configureProject(project);

        assertProjectIsConfigured();

        if (oldValue != null) {
            System.setProperty(ProxySetup.USE_SYSTEM_PROXIES, oldValue);
        } else {
            System.getProperties().remove(ProxySetup.USE_SYSTEM_PROXIES);
        }
    }

    private void assertProjectIsConfigured() {
        assertThat(project.getCoreLoader(), is(easyAntConfiguration.getCoreLoader()));
        assertThat(project.isKeepGoingMode(), is(easyAntConfiguration.isKeepGoingMode()));
        assertThat(Boolean.parseBoolean(System.getProperty(ProxySetup.USE_SYSTEM_PROXIES)),
                is(easyAntConfiguration.isProxy()));
        assertThat(project.getName(), is("EasyAnt"));
    }

}
