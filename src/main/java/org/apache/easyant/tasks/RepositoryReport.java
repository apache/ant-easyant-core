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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.easyant.core.report.EasyAntReport;
import org.apache.easyant.core.report.XmlEasyAntReportOutputter;
import org.apache.easyant.core.services.PluginService;
import org.apache.ivy.Ivy;
import org.apache.ivy.core.cache.ResolutionCacheManager;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.report.XmlReportOutputter;
import org.apache.ivy.util.FileUtil;
import org.apache.ivy.util.Message;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.XSLTProcess;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.util.FileNameMapper;

/**
 * Generates a report of dependencies of a set of modules in the repository. The
 * set of modules is specified using organisation/module and matcher.
 */
public class RepositoryReport extends AbstractEasyAntTask {
    private String organisation = "*";

    private String module;

    private String branch;

    private String revision = "latest.integration";

    private String matcher = PatternMatcher.EXACT_OR_REGEXP;

    private File todir;

    private boolean graph = false;

    private boolean dot = false;

    private boolean xml = false;

    private boolean xsl = true;

    private boolean displaySubProperties = false;

    private String xslFile;

    private String outputname = "ivy-repository-report";

    private String xslext = "html";

    private String resolver;

    private List<XSLTProcess.Param> params = new ArrayList<XSLTProcess.Param>();

    private List<ReportMenu> menus = new ArrayList<ReportMenu>();

    public void execute() throws BuildException {
        Ivy ivy = getEasyAntIvyInstance();
        IvySettings settings = ivy.getSettings();

        ModuleRevisionId mrid = ModuleRevisionId.newInstance(organisation,
                module, revision);

        PluginService pluginService = (PluginService) getProject()
                .getReference(EasyAntMagicNames.PLUGIN_SERVICE_INSTANCE);
        try {
            ModuleRevisionId[] mrids = pluginService.search(organisation,
                    module, revision, branch, matcher, resolver);
            // replace all found revisions with the original requested revision
            Set<ModuleRevisionId> modules = new HashSet<ModuleRevisionId>();
            for (int i = 0; i < mrids.length; i++) {
                modules.add(ModuleRevisionId.newInstance(mrids[i], revision));
            }

            String conf = "default";
            XslReport xslReport = new XslReport();

            ResolutionCacheManager cacheMgr = ivy.getResolutionCacheManager();
            final String dotXsl = dot ? getDotStylePath(cacheMgr.getResolutionCacheRoot()) : null;
            final String graphXsl = graph ? getGraphStylePath(cacheMgr.getResolutionCacheRoot()) : null;
            final String reportXsl = xsl ? getXslPath() : null;

            for (Iterator iterator = modules.iterator(); iterator.hasNext();) {
                ModuleRevisionId moduleRevisionId = (ModuleRevisionId) iterator
                        .next();
                ModuleDescriptor md = DefaultModuleDescriptor
                        .newCallerInstance(moduleRevisionId, conf.split(","),
                                true, false);
                outputname = moduleRevisionId.getName();
                String resolveId = ResolveOptions.getDefaultResolveId(md);
                EasyAntReport easyAntReport = null;
                try {
                    easyAntReport = pluginService.getPluginInfo(
                        moduleRevisionId, conf);
                } catch (Exception e) {
                    throw new Exception("can't parse " + moduleRevisionId.toString(),e);
                }
                if (easyAntReport == null
                        || easyAntReport.getResolveReport() == null) {
                    throw new Exception("impossible to generate graph for "
                            + moduleRevisionId.toString()
                            + ": can't find easyant report");
                }

                new XmlEasyAntReportOutputter().output(easyAntReport
                        .getResolveReport(), cacheMgr, new ResolveOptions(),
                        easyAntReport, displaySubProperties);
                
                File xmlSource = cacheMgr.getConfigurationResolveReportInCache(
                        resolveId, "default");

                if (graph) {
                    xslReport.add(graphXsl, xmlSource, outputname, "graphml");
                }
                if (dot) {
                    xslReport.add(dotXsl, xmlSource, outputname, "dot");
                }
                if (xsl) {
                    xslReport.add(reportXsl, xmlSource, outputname, getXslext());
                }
                if (xml) {
                    FileUtil.copy(cacheMgr
                            .getConfigurationResolveReportInCache(resolveId,
                                    "default"), new File(getTodir(), outputname
                            + ".xml"), null);
                }
            }

            //run XSL transform on accumulated source files
            xslReport.generateAll();

            if (xsl && xslFile == null) {
                // Copy css for default report if required
                File css;
                if (todir != null) {
                    css = new File(todir, "easyant-report.css");
                } else {
                    css = getProject().resolveFile("easyant-report.css");
                }

                if (!css.exists()) {
                    Message.debug("copying report css to " + css.getAbsolutePath());
                    FileUtil.copy(XmlEasyAntReportOutputter.class
                            .getResourceAsStream("easyant-report.css"), css, null);
                }
            }

        } catch (Exception e) {
            throw new BuildException("impossible to generate graph for " + mrid
                    + ": " + e, e);
        }
    }

    private String getGraphStylePath(File cache) throws IOException {
        // style should be a file (and not an url)
        // so we have to copy it from classpath to cache
        File style = new File(cache, "easyant-report-graph.xsl");
        FileUtil.copy(XmlReportOutputter.class
                .getResourceAsStream("easyant-report-graph.xsl"), style, null);
        return style.getAbsolutePath();
    }

    private String getXslPath() throws IOException {
        if (xslFile != null) {
            return xslFile;
        }
        // style should be a file (and not an url)
        // so we have to copy it from classpath to cache
        ResolutionCacheManager cacheMgr = getEasyAntIvyInstance()
                .getResolutionCacheManager();
        File style = new File(cacheMgr.getResolutionCacheRoot(),
                "easyant-report.xsl");
        FileUtil.copy(XmlEasyAntReportOutputter.class
                .getResourceAsStream("easyant-report.xsl"), style, null);
        return style.getAbsolutePath();
    }

    private String getDotStylePath(File cache) throws IOException {
        // style should be a file (and not an url)
        // so we have to copy it from classpath to cache
        File style = new File(cache, "easyant-report-dot.xsl");
        FileUtil.copy(XmlEasyAntReportOutputter.class
                .getResourceAsStream("easyant-report-dot.xsl"), style, null);
        return style.getAbsolutePath();
    }

    public File getTodir() {
        if (todir == null && getProject() != null) {
            return getProject().getBaseDir();
        }
        return todir;
    }

    public void setTodir(File todir) {
        this.todir = todir;
    }

    public boolean isGraph() {
        return graph;
    }

    public void setGraph(boolean graph) {
        this.graph = graph;
    }

    public String getXslfile() {
        return xslFile;
    }

    public void setXslfile(String xslFile) {
        this.xslFile = xslFile;
    }

    public boolean isXml() {
        return xml;
    }

    public void setXml(boolean xml) {
        this.xml = xml;
    }

    public boolean isXsl() {
        return xsl;
    }

    public void setXsl(boolean xsl) {
        this.xsl = xsl;
    }

    public String getXslext() {
        return xslext;
    }

    public void setXslext(String xslext) {
        this.xslext = xslext;
    }

    public XSLTProcess.Param createParam() {
        XSLTProcess.Param result = new XSLTProcess.Param();
        params.add(result);
        return result;
    }

    public String getOutputname() {
        return outputname;
    }

    public void setOutputname(String outputpattern) {
        outputname = outputpattern;
    }

    public String getMatcher() {
        return matcher;
    }

    public void setMatcher(String matcher) {
        this.matcher = matcher;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getOrganisation() {
        return organisation;
    }

    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public boolean isDot() {
        return dot;
    }

    public void setDot(boolean dot) {
        this.dot = dot;
    }

    public boolean isDisplaySubProperties() {
        return displaySubProperties;
    }

    public void setDisplaySubProperties(boolean displaySubProperties) {
        this.displaySubProperties = displaySubProperties;
    }

    public String getResolver() {
        return resolver;
    }

    /**
     * Specify which resolver to report against.  The default EasyAnt resolver will be used if unspecified.
     * "*" means search all resolvers.
     */
    public void setResolver(String resolver) {
        this.resolver = resolver;
    }

    /**
     * Add a menu generator context, to which menu entries will be published for each report file created.
     */
    public ReportMenu createMenuGenerator() {
        ReportMenu menu = new ReportMenu();
        menus.add(menu);
        return menu;
    }

    /**
     * Represents a single menu generator context, to which entries will be added as repository report files
     * are created.  The attribute {@link #setContext context} is required.  Additionally supports three optional nested
     * elements:
     * <ol>
     * <li><code>mapper</code> maps report file to menu item title and link path (both title and link path
     * will have the same value).  The mapped title and link path are arguments to {@link MenuGenerator#addEntry(String, String)}.</li>
     * <li><code>linkMapper</code> maps report file to menu item link.  <code>linkMapper</code> and <code>titleMapper</code>
     * can be used together as an alternative to <code>mapper</code>, in cases were the link and title for the menu entry
     * have different values.</li>
     * <li><code>titleMapper</code> maps report file to menu item title.  <code>linkMapper</code> and <code>titleMapper</code>
     * can be used together as an alternative to <code>mapper</code>, in cases were the link and title for the menu entry
     * have different values.</li>
     * </ol>
     * If none of the above elements are provided, 
     */
    public class ReportMenu {

        private Mapper titleMapper;
        private Mapper linkMapper;

        private String context;

        //TreeMap to sort menu entries in alphabetical order.  should this behavior be configurable somehow?
        private Map<String,String> entries = new TreeMap<String,String>();

        /**
         * Set the menu generator context name.  Entries will be added to this context for each generated report file.
         * @see MenuGenerateRegistry
         */
        public void setContext(String context) {
            this.context = context;
        }

        public Mapper createMapper() {
            return createLinkMapper();
        }

        public void setMapper(String typeName, String from, String to) {
            setLinkMapper(typeName, from, to);
        }

        public Mapper createTitleMapper() {
            return titleMapper = new Mapper(getProject());
        }

        public void setTitleMapper(String typeName, String from, String to) {
            configureMapper(createTitleMapper(), typeName, from, to);
        }

        public Mapper createLinkMapper() {
            return linkMapper = new Mapper(getProject());
        }

        public void setLinkMapper(String typeName, String from, String to) {
            configureMapper(createLinkMapper(), typeName, from, to);
        }

        public void addEntry(String reportPath) {
            String link = map(reportPath, linkMapper, reportPath);
            String title = map(reportPath, titleMapper, link);

            entries.put(title, link);
        }

        public void generate() throws IOException {
            for (Map.Entry<String,String> entry : entries.entrySet()) {
            	// TODO
            }
        }

        private void configureMapper(Mapper mapper, String typeName, String from, String to) {
            Mapper.MapperType type = new Mapper.MapperType();
            type.setValue(typeName);

            mapper.setType(type);
            mapper.setFrom(from);
            mapper.setTo(to);
        }

        private String map(String reportPath, Mapper mapper, String defaultValue) {
            if (mapper != null) {
                String[] mappedTitle = mapper.getImplementation().mapFileName(reportPath);
                if (mappedTitle != null && mappedTitle.length > 0) {
                    return mappedTitle[0];
                }
            }
            return defaultValue;
        }

    }

    /** accumulates a set of input XML files and their transformed destination files */
    private class XslReport {

        private HashMap<String,XslMapper> reports = new HashMap<String,XslMapper>();

        /**
         * Add a rule to transform <code>source</code> using stylesheet <code>xslPath</code>,
         * storing the result at location <code>destination</code>.<code>extension</code>
         */
        public void add(String xslPath, File source, String destination, String extension) throws IOException {
            XslMapper mapper = reports.get(xslPath);
            if (mapper == null) {
                reports.put(xslPath, mapper = new XslMapper(xslPath, extension));
            }
            mapper.add(source, destination);
        }

        /**
         * Run all transforms previously configured by calls to {@link #add(String, java.io.File, String, String)}
         */
        public void generateAll() throws IOException {
            for (XslMapper report : reports.values())
                report.generate();
        }

        /** transforms a set of input files using a single XSL stylesheet */
        private class XslMapper extends Path implements FileNameMapper {

            //maps input file to destination file name
            private HashMap<String,String> map = new HashMap<String,String>();
            private String xslPath;
            private String extension;

            public XslMapper(String xslPath, String extension) {
                super(RepositoryReport.this.getProject());
                this.xslPath = xslPath;
                this.extension = extension;
            }

            public void generate() throws IOException {
                XSLTProcess xslt = new XSLTProcess();
                xslt.setTaskName(getTaskName());
                xslt.setProject(getProject());
                xslt.init();

                xslt.setDestdir(getTodir());
                xslt.setUseImplicitFileset(false);
                xslt.add((ResourceCollection)this);
                xslt.add((FileNameMapper)this);
                xslt.setStyle(xslPath);

                XSLTProcess.Param param = xslt.createParam();
                param.setName("extension");
                param.setExpression(extension);

                // add the provided XSLT parameters
                for (Iterator it = params.iterator(); it.hasNext();) {
                    param = (XSLTProcess.Param) it.next();
                    XSLTProcess.Param realParam = xslt.createParam();
                    realParam.setName(param.getName());
                    realParam.setExpression(param.getExpression());
                }

                xslt.execute();

                for (ReportMenu menu : menus) {
                    menu.generate();
                }
            }

            /**
             * add the XML file <code>source</code> to this path, mapping to the output file
             * <code>destionation</code> (without base directory or extension)
             */
            public void add(File source, String destination) {
                map.put(source.getPath(), destination + "." + extension);
                createPathElement().setLocation(source);
                for (ReportMenu menu : menus) {
                    menu.addEntry(destination);
                }
            }

            public void setFrom(String s) {}
            public void setTo(String s) {}

            public String[] mapFileName(String s) {
                s = map.get(new File(s).getPath());
                return s == null ? null : new String[]{ s };
            }
        }

    }

}
