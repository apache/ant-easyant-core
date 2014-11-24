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

import org.apache.ivy.Ivy;
import org.apache.ivy.ant.AntMessageLogger;
import org.apache.ivy.core.cache.DefaultRepositoryCacheManager;
import org.apache.ivy.core.cache.DefaultResolutionCacheManager;
import org.apache.ivy.core.cache.RepositoryCacheManager;
import org.apache.ivy.core.cache.ResolutionCacheManager;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ConfigurationResolveReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.retrieve.RetrieveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.core.sort.SortOptions;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.ivy.plugins.report.XmlReportParser;
import org.apache.ivy.plugins.repository.url.URLResource;
import org.apache.ivy.plugins.resolver.FileSystemResolver;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.taskdefs.ImportTask;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Path.PathElement;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.*;

/**
 * EXPERIMENTAL, IT IS NOT INTENDED FOR PUBLIC USE
 * <p/>
 * An Ant task which resolve some build scripts and import them
 */
public class ImportAntscripts extends Task {

    private File ivyfile;

    private File ivysettings;

    private Ivy externalIvy;

    public void setIvyfile(File ivyfile) {
        this.ivyfile = ivyfile;
    }

    public void setIvysettings(File ivysettings) {
        this.ivysettings = ivysettings;
    }

    @Override
    public void execute() throws BuildException {
        long startTime = System.currentTimeMillis();

        if (getOwningTarget() == null || !"".equals(getOwningTarget().getName())) {
            throw new BuildException("import only allowed as a top-level task");
        }

        if (ivyfile == null) {
            throw new BuildException("ivyfile is required");
        }
        if (ivysettings == null) {
            throw new BuildException("ivysettings is required");
        }

        boolean refresh = false;
        String refreshValue = getProject().getProperty("easyant.refresh");
        if (refreshValue != null) {
            refresh = Boolean.parseBoolean(refreshValue);
        }

        // configure ivy, which may be the local retrieve repo, depending of the setup
        Ivy ivy = getIvy();
        ivy.pushContext();

        try {
            ModuleDescriptor md = getMd(ivy, ivyfile);

            ArtifactDownloadReport[] artifacts;
            List<ModuleDescriptor> dependencies = new ArrayList<ModuleDescriptor>();

            // first, let's resolve the ant scripts, just the scripts, not their possible jar dependencies, thus only
            // the configuration "default"

            XmlReportParser xmlreport = null;
            if (!refresh) {
                // first try to relad the resolve from the last report
                xmlreport = getResolveReport(ivy, md.getModuleRevisionId().getModuleId(), "default", ivyfile);
            }
            if (xmlreport != null) {
                // collect the ant scripts
                artifacts = xmlreport.getArtifactReports();

                // collect the descriptor associated with each ant script
                ModuleRevisionId[] depIds = xmlreport.getDependencyRevisionIds();
                for (ModuleRevisionId depId : depIds) {
                    File depIvyFile = xmlreport.getMetadataArtifactReport(depId).getLocalFile();
                    dependencies.add(getMd(ivy, depIvyFile));
                }
            } else {
                // do a full resolve

                // if in a retrieved setup, clean the repo and repopulate it
                maybeClearLocalRepo();
                maybeRetrieve(md, "default");

                // launch the actual resolve
                ResolveReport resolveReport = resolve(ivy, md, "default");
                ConfigurationResolveReport confReport = resolveReport.getConfigurationReport("default");

                // collect the ant scripts
                artifacts = confReport.getAllArtifactsReports();

                // collect the descriptor associated with each ant script
                Set<ModuleRevisionId> mrids = confReport.getModuleRevisionIds();
                for (ModuleRevisionId mrid : mrids) {
                    dependencies.add(confReport.getDependency(mrid).getDescriptor());
                }
            }

            int nbPaths = 1;

            // save the collection of ant scripts as a path
            Path antScriptsPath = makePath("easyant.antscripts", sortArtifacts(ivy, artifacts, dependencies));

            // now, for each ant script descriptor, search for an ivy configuration which is used by the ant script
            // itself

            for (ModuleDescriptor depmd : dependencies) {
                log("Searching for external conf for " + depmd.getModuleRevisionId(), Project.MSG_VERBOSE);
                String[] confs = depmd.getConfigurationsNames();
                log("configurations for " + depmd.getModuleRevisionId() + " : " + Arrays.toString(confs),
                        Project.MSG_DEBUG);

                // some trick here: launching a resolve on a module won't resolve the artifacts of the module itself but
                // only of its dependencies. So we'll create a mock one which will depend on the real one
                String mockOrg = "_easyant_mocks_";
                String mockName = depmd.getModuleRevisionId().getOrganisation() + "__"
                        + depmd.getModuleRevisionId().getName();
                ModuleRevisionId mockmrid = ModuleRevisionId.newInstance(mockOrg, mockName, depmd.getModuleRevisionId()
                        .getBranch(), depmd.getRevision(), depmd.getExtraAttributes());
                DefaultModuleDescriptor mock = new DefaultModuleDescriptor(mockmrid, depmd.getStatus(),
                        depmd.getPublicationDate(), depmd.isDefault());
                DefaultDependencyDescriptor mockdd = new DefaultDependencyDescriptor(depmd.getModuleRevisionId(), false);
                for (String conf : confs) {
                    mock.addConfiguration(new Configuration(conf));
                    mockdd.addDependencyConfiguration(conf, conf);
                }
                mock.addDependency(mockdd);

                for (String conf : confs) {
                    if (conf.equals("default")) {
                        continue;
                    }

                    nbPaths++;
                    log("Found configuration " + conf, Project.MSG_VERBOSE);

                    // same process than for the ant script:
                    // * trust the last resolve report
                    // * or launch a full resolve
                    // A full resolve might trigger a retrieve to populate the local repo

                    XmlReportParser xmldepreport = null;
                    if (!refresh) {
                        xmldepreport = getResolveReport(ivy, mock.getModuleRevisionId().getModuleId(), conf, null);
                    }
                    if (xmldepreport != null) {
                        artifacts = xmldepreport.getArtifactReports();
                    } else {
                        maybeRetrieve(mock, conf);
                        ResolveReport resolveReport = resolve(ivy, mock, conf);
                        ConfigurationResolveReport confReport = resolveReport.getConfigurationReport(conf);
                        artifacts = confReport.getAllArtifactsReports();
                    }

                    // finally make the resolved artifact a path which can be used by the ant script itself
                    makePath(depmd.getModuleRevisionId().getModuleId().toString() + "[" + conf + "]",
                            Arrays.asList(artifacts));
                }
            }

            log(nbPaths + " paths resolved in " + (System.currentTimeMillis() - startTime) + "ms.", Project.MSG_VERBOSE);

            log("Importing " + antScriptsPath.size() + " ant scripts", Project.MSG_VERBOSE);
            Iterator<?> itScripts = antScriptsPath.iterator();
            while (itScripts.hasNext()) {
                log("\t" + itScripts.next(), Project.MSG_VERBOSE);
            }

            ImportTask importTask = new ImportTask();
            importTask.setProject(getProject());
            importTask.setOwningTarget(getOwningTarget());
            importTask.setLocation(getLocation());
            importTask.add(antScriptsPath);
            importTask.execute();
        } finally {
            ivy.popContext();
        }

    }

    private List<ArtifactDownloadReport> sortArtifacts(Ivy ivy, ArtifactDownloadReport[] artifacts,
                                                       List<ModuleDescriptor> dependencies) {
        // first lets map the artifacts to their id
        Map<ModuleRevisionId, List<ArtifactDownloadReport>> artifactsById = new HashMap<ModuleRevisionId, List<ArtifactDownloadReport>>();
        for (ArtifactDownloadReport artifact : artifacts) {
            List<ArtifactDownloadReport> artifactsForId = artifactsById.get(artifact.getArtifact()
                    .getModuleRevisionId());
            if (artifactsForId == null) {
                artifactsForId = new ArrayList<ArtifactDownloadReport>();
                artifactsById.put(artifact.getArtifact().getModuleRevisionId(), artifactsForId);
            }
            artifactsForId.add(artifact);
        }

        List<ModuleDescriptor> sorted = ivy.sortModuleDescriptors(dependencies, SortOptions.DEFAULT);

        List<ArtifactDownloadReport> sortedArifacts = new ArrayList<ArtifactDownloadReport>(artifacts.length);
        for (ModuleDescriptor md : sorted) {
            List<ArtifactDownloadReport> artifactsForId = artifactsById.get(md.getModuleRevisionId());
            if (artifactsForId != null) {
                sortedArifacts.addAll(artifactsForId);
            }
        }

        return sortedArifacts;
    }

    private Ivy getIvy() {
        Ivy ivy = getLocalRepoIvy();
        if (ivy != null) {
            return ivy;
        }
        return getExternalIvy();
    }

    /**
     * @return the ivy instance corresponding to the ivysettings.xml provided by the end user
     */
    private Ivy getExternalIvy() {
        if (externalIvy == null) {
            externalIvy = new Ivy();
            try {
                externalIvy.configure(ivysettings);
            } catch (ParseException e) {
                throw new BuildException("Incorrect setup of the ivysettings for easyant (" + e.getMessage() + ")", e);
            } catch (IOException e) {
                throw new BuildException("Incorrect setup of the ivysettings for easyant (" + e.getMessage() + ")", e);
            }
            AntMessageLogger.register(this, externalIvy);
        }
        return externalIvy;
    }

    /**
     * Parse an ivy.xml
     */
    private ModuleDescriptor getMd(Ivy ivy, File file) {
        URL url;
        try {
            url = file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new BuildException("[easyant bug] a file has not a proper url", e);
        }
        URLResource res = new URLResource(url);
        ModuleDescriptorParser mdparser = ModuleDescriptorParserRegistry.getInstance().getParser(res);
        ModuleDescriptor md;
        ivy.pushContext();
        try {
            md = mdparser.parseDescriptor(ivy.getSettings(), url, true);
        } catch (ParseException e) {
            throw new BuildException("The file " + file + " is not a correct ivy file (" + e.getMessage() + ")", e);
        } catch (IOException e) {
            throw new BuildException("The file " + file + " could not be read (" + e.getMessage() + ")", e);
        }
        return md;
    }

    /**
     * Try to load a resolve report. If not found, not available, out of date or contains resolve errors, it returns
     * <code>null</code>.
     */
    private XmlReportParser getResolveReport(Ivy ivy, ModuleId mid, String conf, File ivyfile) {
        File report = ivy.getResolutionCacheManager().getConfigurationResolveReportInCache(
                ResolveOptions.getDefaultResolveId(mid), conf);
        if (!report.exists()) {
            return null;
        }
        if (ivyfile != null && ivyfile.lastModified() > report.lastModified()) {
            return null;
        }
        // found a report, try to parse it.
        try {
            log("Reading resolve report " + report, Project.MSG_DEBUG);
            XmlReportParser reportparser = new XmlReportParser();
            reportparser.parse(report);
            if (reportparser.hasError()) {
                return null;
            }
            log("Loading last resolve report for " + mid + "[" + conf + "]", Project.MSG_VERBOSE);
            return reportparser;
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Launch an actual resolve
     */
    private ResolveReport resolve(Ivy ivy, ModuleDescriptor md, String conf) {
        ResolveOptions options = new ResolveOptions();
        options.setConfs(new String[]{conf});
        ResolveReport report;
        try {
            report = ivy.resolve(md, options);
        } catch (ParseException e) {
            throw new BuildException("Error while resolving " + ivyfile, e);
        } catch (IOException e) {
            throw new BuildException("Error while resolving " + ivyfile, e);
        }

        if (report.hasError()) {
            throw new BuildException("[easyant bug] fail to resolve antlib dependencies");
        }

        return report;
    }

    /**
     * Make an array of artifacts a path and save it in the ant references
     */
    private Path makePath(String pathId, List<ArtifactDownloadReport> artifacts) {
        log("Path '" + pathId + "' computed with " + artifacts.size() + " files", Project.MSG_VERBOSE);
        Path path = new Path(getProject());
        for (ArtifactDownloadReport artifact : artifacts) {
            if (artifact.getLocalFile() != null) {
                PathElement pe = path.createPathElement();
                pe.setLocation(artifact.getLocalFile());
                log("Adding to path '" + pathId + "': " + artifact.getLocalFile(), Project.MSG_DEBUG);
            }
        }

        getProject().addReference(pathId, path);
        return path;
    }

    /**
     * If it is setup to have a retrieved local repo, get the basedir of the repo. Returns <code>null</code> otherwise
     */
    private File getLocalRepoBaseDir() {
        String basedirValue = getProject().getProperty("easyant.localrepo.basedir");
        if (basedirValue == null) {
            return null;
        }
        return getProject().resolveFile(basedirValue);
    }

    /**
     * Build the ivy instance to be used on the local retrieved repo
     */
    private Ivy getLocalRepoIvy() {
        File basedir = getLocalRepoBaseDir();
        if (basedir == null) {
            return null;
        }

        IvySettings settings = new IvySettings();
        settings.setBaseDir(basedir);
        settings.setDefaultUseOrigin(true);

        File cacheDir = new File(basedir, ".cache");

        ResolutionCacheManager resolutionCacheManager = new DefaultResolutionCacheManager(cacheDir);
        settings.setResolutionCacheManager(resolutionCacheManager);

        RepositoryCacheManager repositoryCacheManager = new DefaultRepositoryCacheManager("default-cache", settings,
                cacheDir);
        settings.setDefaultRepositoryCacheManager(repositoryCacheManager);

        FileSystemResolver localResolver = new FileSystemResolver();
        localResolver.setName("local-repo");
        localResolver.addIvyPattern(basedir.getAbsolutePath() + "/[organization]/[module]/[revision]/ivy.xml");
        localResolver.addArtifactPattern(basedir.getAbsolutePath()
                + "/[organization]/[module]/[revision]/[type]s/[artifact].[ext]");

        settings.addResolver(localResolver);

        settings.setDefaultResolver("local-repo");

        Ivy ivy = Ivy.newInstance(settings);
        AntMessageLogger.register(this, ivy);
        return ivy;
    }

    /**
     * If setup with a local repo, clean it
     */
    private void maybeClearLocalRepo() {
        File basedir = getLocalRepoBaseDir();
        if (basedir == null) {
            return;
        }

        log("Deleting the local repository '" + basedir + "'", Project.MSG_VERBOSE);

        Delete delete = new Delete();
        delete.setFailOnError(false);
        delete.setDir(basedir);
    }

    /**
     * If setup with a local repo, resolve the module and retrieve the artifacts into the local repo.
     */
    private void maybeRetrieve(ModuleDescriptor md, String conf) {
        File basedir = getLocalRepoBaseDir();
        if (basedir == null) {
            return;
        }

        log("Populating the local repository '" + basedir + "' with dependencies of " + md, Project.MSG_VERBOSE);

        Ivy ivy = getExternalIvy();
        try {
            ivy.setVariable("easyant.localrepo.basedir", basedir.getCanonicalPath());
        } catch (IOException e) {
            throw new BuildException("Unable to compute the path to the local repository", e);
        }
        ResolveReport resolve = resolve(ivy, md, conf);

        RetrieveOptions options = new RetrieveOptions();
        options.setSync(false);
        options.setResolveId(resolve.getResolveId());
        options.setConfs(new String[]{conf});
        options.setDestIvyPattern("${easyant.localrepo.basedir}/[organization]/[module]/[revision]/ivy.xml");
        try {
            ivy.retrieve(md.getModuleRevisionId(),
                    "${easyant.localrepo.basedir}/[organization]/[module]/[revision]/[type]s/[artifact].[ext]", options);
        } catch (IOException e) {
            throw new BuildException("Unable to build the local repository", e);
        }
    }
}
