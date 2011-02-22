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
package org.apache.easyant.core.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.easyant.core.descriptor.PropertyDescriptor;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.util.ContextualSAXHandler;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.XMLHelper;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class EasyAntReportModuleParser extends ContextualSAXHandler {

    private EasyAntReport eaReport;

    private Map<String, String> properties = new HashMap<String, String>();

    private String configuration;
    private PropertyDescriptor propertyDescriptor;

    public EasyAntReportModuleParser(String configuration) {
        super();
        this.configuration = configuration;
    }

    public EasyAntReport getEaReport() {
        return eaReport;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public void startDocument() throws SAXException {
        super.startDocument();
        eaReport = new EasyAntReport();
    }

    @Override
    public void startElement(String uri, String localName, String name,
            Attributes attributes) throws SAXException {
        super.startElement(uri, localName, name, attributes);

        if ("project/target".equals(getContext())) {
            TargetReport targetReport = new TargetReport();
            targetReport.setName(attributes.getValue("name"));
            targetReport.setDepends(attributes.getValue("depends"));
            targetReport.setDescription(attributes.getValue("description"));
            targetReport.setIfCase(attributes.getValue("if"));
            targetReport.setUnlessCase(attributes.getValue("unless"));
            targetReport.setPhase(attributes.getValue("phase"));
            eaReport.addTargetReport(targetReport);

            Message.debug("Ant file has a target called : "
                    + targetReport.getName());
        } else if ("ea:parameter".equals(name)) {
            if (attributes.getValue("property") != null) {
                propertyDescriptor = new PropertyDescriptor(
                        attributes.getValue("property"));
                propertyDescriptor.setDefaultValue(attributes
                        .getValue("default"));
                if (attributes.getValue("required") == null)
                    propertyDescriptor.setRequired(false);
                else
                    propertyDescriptor.setRequired(new Boolean(attributes
                            .getValue("required")));
                if (attributes.getValue("description") != null) {
                    propertyDescriptor.setDescription(attributes
                            .getValue("description"));
                }
            } else if (attributes.getValue("phase") != null) {
                ParameterReport parameterReport = new ParameterReport(
                        ParameterType.PHASE);
                parameterReport.setName(attributes.getValue("phase"));
                eaReport.addParameterReport(parameterReport);
                Message.debug("Ant file has a phase called : "
                        + parameterReport.getName());
            } else if (attributes.getValue("path") != null) {
                ParameterReport parameterReport = new ParameterReport(
                        ParameterType.PATH);
                parameterReport.setName(attributes.getValue("path"));
                parameterReport.setDefaultValue(attributes.getValue("default"));
                parameterReport.setRequired(new Boolean(attributes
                        .getValue("required")));
                eaReport.addParameterReport(parameterReport);
                Message.debug("Ant file has a path called : "
                        + parameterReport.getName());
            }

        } else if ("project/phase".equals(getContext())) {
            PhaseReport phaseReport = new PhaseReport(attributes
                    .getValue("name"));
            phaseReport.setDepends(attributes.getValue("depends"));
            phaseReport.setDescription(attributes.getValue("description"));
            eaReport.addPhaseReport(phaseReport);
            Message.debug("Ant file has a phase called : "
                    + phaseReport.getName());
        } else if ("project/ea:include".equals(getContext())) {
            ImportedModuleReport importedModuleReport = new ImportedModuleReport();
            importedModuleReport.setModuleMrid(attributes.getValue("mrid"));
            importedModuleReport.setModule(attributes.getValue("module"));

            String org = attributes.getValue("org") != null ? attributes
                    .getValue("org") : attributes.getValue("organisation");
            importedModuleReport.setOrganisation(org);

            String rev = attributes.getValue("rev") != null ? attributes
                    .getValue("rev") : attributes.getValue("revision");
            importedModuleReport.setRevision(rev);

            importedModuleReport.setType("include");
            importedModuleReport.setAs(attributes.getValue("as"));
            if (attributes.getValue("mandatory") != null) {
                importedModuleReport.setMandatory(Boolean
                        .parseBoolean(attributes.getValue("mandatory")));
            }

            importedModuleReport.setEasyantReport(EasyAntReportModuleParser
                    .parseEasyAntModule(importedModuleReport.getModuleMrid(),
                            configuration));
            ;
            eaReport.addImportedModuleReport(importedModuleReport);
            Message.debug("Ant file import another module called : "
                    + importedModuleReport.getModuleMrid() + " with mode "
                    + importedModuleReport.getType());
        } else if ("project/ea:import".equals(getContext())) {
            ImportedModuleReport importedModuleReport = new ImportedModuleReport();
            importedModuleReport.setModuleMrid(attributes.getValue("mrid"));
            importedModuleReport.setModule(attributes.getValue("module"));

            String org = attributes.getValue("org") != null ? attributes
                    .getValue("org") : attributes.getValue("organisation");
            importedModuleReport.setOrganisation(org);

            String rev = attributes.getValue("rev") != null ? attributes
                    .getValue("rev") : attributes.getValue("revision");
            importedModuleReport.setRevision(rev);

            importedModuleReport.setType("import");
            importedModuleReport.setAs(attributes.getValue("as"));
            if (attributes.getValue("mandatory") != null) {
                importedModuleReport.setMandatory(Boolean
                        .parseBoolean(attributes.getValue("mandatory")));
            }

            importedModuleReport.setEasyantReport(EasyAntReportModuleParser
                    .parseEasyAntModule(importedModuleReport.getModuleMrid(),
                            configuration));
            eaReport.addImportedModuleReport(importedModuleReport);
            Message.debug("Ant file import another module called : "
                    + importedModuleReport.getModuleMrid() + " with mode "
                    + importedModuleReport.getType());
        } else if ("project/property".equals(getContext())
                && properties.size() > 0) {
            if (attributes.getValue("file") != null) {
                Properties propToLoad = new Properties();
                File f = new File(
                        replaceProperties(attributes.getValue("file")));
                try {
                    propToLoad.load(new FileInputStream(f));
                    for (Iterator iterator = propToLoad.keySet().iterator(); iterator
                            .hasNext();) {
                        String key = (String) iterator.next();
                        PropertyDescriptor propertyDescriptor = new PropertyDescriptor(
                                key);
                        propertyDescriptor
                                .setValue(propToLoad.getProperty(key));
                        eaReport.addPropertyDescriptor(propertyDescriptor
                                .getName(), propertyDescriptor);
                    }

                } catch (Exception e) {
                    throw new SAXException(
                            "Unable to parse the property file :"
                                    + attributes.getValue("file"), e);
                }
            }
        }
    }
    
    

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        
        if ("ea:parameter".equals(qName)) {
            //handle nested description on properties   
            if (propertyDescriptor!=null) {
                if (getText()!=null && getText().length() > 0) {
                    propertyDescriptor.setDescription(getText());
                }
                Message.debug("Ant file has a property called : "
                        + propertyDescriptor.getName());
                eaReport.addPropertyDescriptor(propertyDescriptor.getName(),
                        propertyDescriptor);
            }
        }
        super.endElement(uri, localName, qName);
        //reset the propertyDescriptor
        propertyDescriptor=null;
    }

    protected String replaceProperties(String property) {
        return IvyPatternHelper.substituteVariables(property, properties);
    }

    public static final EasyAntReport parseEasyAntModule(String mrid,
            String conf) throws SAXException {
        ModuleRevisionId moduleRevisionId = null;
        try {
            moduleRevisionId = ModuleRevisionId.parse(mrid);
        } catch (IllegalArgumentException e) {
            Message.debug("Imposible to fetch data from " + mrid);
            Message.debug(e.getMessage());
            return null;
        }
        return parseEasyAntModule(moduleRevisionId, conf);
    }

    public static final EasyAntReport parseEasyAntModule(ModuleRevisionId mrid,
            String conf) throws SAXException {
        try {
            ResolveOptions resolveOptions = new ResolveOptions();
            resolveOptions.setLog(ResolveOptions.LOG_QUIET);
            resolveOptions.setConfs(conf.split(","));
            ResolveReport report = IvyContext.getContext().getIvy()
                    .getResolveEngine().resolve(mrid, resolveOptions, true);
            // Try to parse easyAnt infos in ant files
            EasyAntReport easyAntReport = null;

            EasyAntReportModuleParser parser = new EasyAntReportModuleParser(
                    conf);

            File antFile = null;

            for (int j = 0; j < report.getConfigurationReport(conf)
                    .getAllArtifactsReports().length; j++) {
                ArtifactDownloadReport artifact = report
                        .getConfigurationReport(conf).getAllArtifactsReports()[j];

                if ("ant".equals(artifact.getType())
                        && "ant".equals(artifact.getExt())) {
                    antFile = artifact.getLocalFile();
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append(mrid.getOrganisation());
                    sb.append("#");
                    sb.append(mrid.getName());
                    sb.append(".");
                    if (!mrid.getName().equals(artifact.getName())) {
                        sb.append(artifact.getName());
                        sb.append(".");
                    }
                    sb.append(artifact.getExt());
                    sb.append(".file");
                    parser.properties.put(sb.toString(), artifact
                            .getLocalFile().getAbsolutePath());
                }
            }

            if (antFile != null) {

                XMLHelper.parse(antFile.toURI().toURL(), null, parser, null);
                easyAntReport = parser.getEaReport();
                easyAntReport.setResolveReport(report);
                return easyAntReport;
            }

        } catch (ParseException e) {
            Message.debug("Imposible to fetch data from " + mrid.toString());
            Message.debug(e.getMessage());
        } catch (IOException e) {
            Message.debug("Imposible to fetch data from " + mrid.toString());
            Message.debug(e.getMessage());
        } catch (ParserConfigurationException e) {
            Message.debug("Imposible to fetch data from " + mrid.toString());
            Message.debug(e.getMessage());
        }
        return null;
    }

}
