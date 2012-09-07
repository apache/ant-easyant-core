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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.easyant.core.EasyAntMagicNames;
import org.apache.easyant.core.report.EasyAntReport;
import org.apache.easyant.core.report.XMLEasyAntReportWriter;
import org.apache.easyant.core.services.PluginService;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.cache.ResolutionCacheManager;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.util.FileUtil;
import org.apache.ivy.util.Message;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.XSLTProcess;
import org.apache.tools.ant.taskdefs.XSLTProcess.Param;
import org.apache.tools.ant.util.JAXPUtils;

public class PluginReport extends AbstractEasyAntTask {

    private File todir;

    private String conf;

    private File xslFile;

    private String xslext = "html";

    private List<XSLTProcess.Param> params = new ArrayList<XSLTProcess.Param>();

    private File moduleIvyFile;

    private File sourceDirectory;

    private String outputpattern;

    public File getModuleIvyFile() {
        return moduleIvyFile;
    }

    public void setModuleIvyFile(File moduleIvyFile) {
        this.moduleIvyFile = moduleIvyFile;
    }

    public File getSourceDirectory() {
        return sourceDirectory;
    }

    public void setSourceDirectory(File sourceDirectory) {
        this.sourceDirectory = sourceDirectory;
    }

    public File getTodir() {
        return todir;
    }

    public void setTodir(File todir) {
        this.todir = todir;
    }

    public String getConf() {
        return conf;
    }

    public void setConf(String conf) {
        this.conf = conf;
    }

    public File getXslfile() {
        return xslFile;
    }

    public void setXslfile(File xslFile) {
        this.xslFile = xslFile;
    }

    public String getOutputpattern() {
        return outputpattern;
    }

    public void setOutputpattern(String outputpattern) {
        this.outputpattern = outputpattern;
    }

    public String getXslext() {
        return xslext;
    }

    public void setXslext(String xslext) {
        this.xslext = xslext;
    }

    public void execute() throws BuildException {
        IvySettings settings = getEasyAntIvyInstance().getSettings();

        conf = getProperty(conf, settings, "ivy.resolved.configurations");
        if ("*".equals(conf)) {
            conf = getProperty(settings, "ivy.resolved.configurations");
        }
        if (conf == null) {
            throw new BuildException("no conf provided for ivy report task: "
                    + "It can either be set explicitely via the attribute 'conf' or "
                    + "via 'ivy.resolved.configurations' property or a prior call to <resolve/>");
        }
        if (todir == null) {
            String t = getProperty(settings, "ivy.report.todir");
            if (t != null) {
                todir = getProject().resolveFile(t);
            } else {
                todir = getProject().getBaseDir();
            }
        }
        if (todir != null && todir.exists()) {
            todir.mkdirs();
        }
        outputpattern = getProperty(outputpattern, settings, "ivy.report.output.pattern");
        if (outputpattern == null) {
            outputpattern = "[organisation]-[module]-[conf].[ext]";
        }

        if (todir != null && todir.exists() && !todir.isDirectory()) {
            throw new BuildException("destination directory should be a directory !");
        }

        PluginService pluginService = (PluginService) getProject().getReference(
                EasyAntMagicNames.PLUGIN_SERVICE_INSTANCE);

        try {
            EasyAntReport easyantReport = pluginService.getPluginInfo(moduleIvyFile, sourceDirectory, conf);
            ModuleRevisionId moduleRevisionId = easyantReport.getModuleDescriptor().getModuleRevisionId();
            File reportFile = new File(todir, getOutputPattern(moduleRevisionId, conf, "xml"));
            todir.mkdirs();
            OutputStream stream = new FileOutputStream(reportFile);
            XMLEasyAntReportWriter writer = new XMLEasyAntReportWriter();
            writer.output(easyantReport, stream);
            stream.close();
            genStyled(reportFile, getReportStylePath(),easyantReport);
        } catch (Exception e) {
            throw new BuildException("impossible to generate report: " + e, e);
        }
    }

    // private void genreport(String[] confs) throws IOException {
    // genStyled(confs, getReportStylePath(), xslext);
    // }

    private File getReportStylePath() throws IOException {
        if (xslFile != null) {
            return xslFile;
        }
        // style should be a file (and not an url)
        // so we have to copy it from classpath to cache
        ResolutionCacheManager cacheMgr = getEasyAntIvyInstance().getResolutionCacheManager();
        File style = new File(cacheMgr.getResolutionCacheRoot(), "ivy-report.xsl");
        if (!style.exists()) {
            Message.debug("copying ivy-report.xsl to " + style.getAbsolutePath());
            FileUtil.copy(XMLEasyAntReportWriter.class.getResourceAsStream("ivy-report.xsl"), style, null);
        }
        return style;
    }

    private String getOutputPattern(ModuleRevisionId moduleRevisionId, String conf, String ext) {
        return IvyPatternHelper.substitute(outputpattern, moduleRevisionId.getOrganisation(), moduleRevisionId.getName(),
                moduleRevisionId.getRevision(), "", "", ext, conf, moduleRevisionId.getQualifiedExtraAttributes(), null);
    }

    private void genStyled(File reportFile, File style, EasyAntReport easyantReport) throws IOException {
        InputStream xsltStream = null;
        try {
            // create stream to stylesheet
            xsltStream = new BufferedInputStream(new FileInputStream(style));
            Source xsltSource = new StreamSource(xsltStream, JAXPUtils.getSystemId(style));

            // create transformer
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer(xsltSource);

            // add the provided XSLT parameters
            for (Iterator<Param> it = params.iterator(); it.hasNext();) {
                XSLTProcess.Param param = (XSLTProcess.Param) it.next();
                transformer.setParameter(param.getName(), param.getExpression());
            }
            ModuleRevisionId moduleRevisionId = easyantReport.getModuleDescriptor().getModuleRevisionId();
            File outFile = new File(todir, getOutputPattern(moduleRevisionId, conf, xslext));

            log("Processing " + reportFile + " to " + outFile);

            // make sure the output directory exist
            File outFileDir = outFile.getParentFile();
            if (!outFileDir.exists()) {
                if (!outFileDir.mkdirs()) {
                    throw new BuildException("Unable to create directory: " + outFileDir.getAbsolutePath());
                }
            }

            InputStream inStream = null;
            OutputStream outStream = null;
            try {
                inStream = new BufferedInputStream(new FileInputStream(reportFile));
                outStream = new BufferedOutputStream(new FileOutputStream(outFile));
                StreamResult res = new StreamResult(outStream);
                Source src = new StreamSource(inStream, JAXPUtils.getSystemId(style));
                transformer.transform(src, res);
            } catch (TransformerException e) {
                throw new BuildException(e);
            } finally {
                if (inStream != null) {
                    try {
                        inStream.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
                if (outStream != null) {
                    try {
                        outStream.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        } catch (TransformerConfigurationException e) {
            throw new BuildException(e);
        } finally {
            if (xsltStream != null) {
                try {
                    xsltStream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    public XSLTProcess.Param createParam() {
        XSLTProcess.Param result = new XSLTProcess.Param();
        params.add(result);
        return result;
    }

    private String getProperty(String value, IvySettings ivy, String name) {
        if (value == null) {
            return getProperty(ivy, name);
        } else {
            value = ivy.substitute(value);
            Message.debug("parameter found as attribute value: " + name + "=" + value);
            return value;
        }
    }

    private String getProperty(IvySettings ivy, String name) {
        String val = ivy.getVariable(name);
        if (val == null) {
            val = ivy.substitute(getProject().getProperty(name));
            if (val != null) {
                Message.debug("parameter found as ant project property: " + name + "=" + val);
            } else {
                Message.debug("parameter not found: " + name);
            }
        } else {
            val = ivy.substitute(val);
            Message.debug("parameter found as ivy variable: " + name + "=" + val);
        }
        return val;
    }

}