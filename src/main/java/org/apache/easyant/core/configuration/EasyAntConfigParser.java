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
package org.apache.easyant.core.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.easyant.core.descriptor.PluginDescriptor;
import org.apache.easyant.core.ivy.InheritableScope;
import org.apache.ivy.util.ContextualSAXHandler;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.XMLHelper;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class EasyAntConfigParser {

    private boolean validate = true;

    public boolean isValidate() {
        return validate;
    }

    public void setValidate(boolean validate) {
        this.validate = validate;
    }

    public URL getEasyAntConfigSchema() {
        return getClass().getResource("/org/apache/easyant/core/easyant-config.xsd");
    }

    public EasyAntConfiguration parseAndMerge(final URL configUrl, final EasyAntConfiguration easyAntConfiguration)
            throws Exception {
        ConfigParser parser = new ConfigParser();

        URL schemaURL = null;

        if (isValidate()) {
            schemaURL = getEasyAntConfigSchema();
        }
        try {
            parser.setConfigUrl(configUrl);
            parser.setEasyAntConfiguration(easyAntConfiguration);
            XMLHelper.parse(configUrl, schemaURL, parser, null);
            parser.checkErrors();
        } catch (ParserConfigurationException ex) {
            IllegalStateException ise = new IllegalStateException(ex.getMessage() + " in " + configUrl);
            ise.initCause(ex);
            throw ise;

        } catch (Exception e) {

            throw new Exception("Can't parse " + configUrl, e);
        }
        return easyAntConfiguration;
    }

    public EasyAntConfiguration parse(URL configUrl) throws Exception {
        EasyAntConfiguration easyAntConfiguration = new EasyAntConfiguration();
        return parseAndMerge(configUrl, easyAntConfiguration);
    }

    public static class ConfigParser extends ContextualSAXHandler {
        private List<String> errors = new ArrayList<String>();
        private URL configUrl;
        private EasyAntConfiguration easyAntConfiguration;

        public URL getConfigUrl() {
            return configUrl;
        }

        public void setConfigUrl(URL configUrl) {
            this.configUrl = configUrl;
        }

        public EasyAntConfiguration getEasyAntConfiguration() {
            return easyAntConfiguration;
        }

        public void setEasyAntConfiguration(EasyAntConfiguration easyAntConfiguration) {
            this.easyAntConfiguration = easyAntConfiguration;
        }

        protected void addError(String msg) {
            if (configUrl != null) {
                errors.add(msg + " in " + configUrl + "\n");
            } else {
                errors.add(msg + "\n");
            }
        }

        protected void checkErrors() throws ParseException {
            if (!errors.isEmpty()) {
                throw new ParseException(errors.toString(), 0);
            }
        }

        private String getLocationString(SAXParseException ex) {
            StringBuilder str = new StringBuilder();

            String systemId = ex.getSystemId();
            if (systemId != null) {
                int index = systemId.lastIndexOf('/');
                if (index != -1) {
                    systemId = systemId.substring(index + 1);
                }
                str.append(systemId);
            } else if (configUrl != null) {
                str.append(configUrl.toString());
            }
            str.append(':');
            str.append(ex.getLineNumber());
            str.append(':');
            str.append(ex.getColumnNumber());

            return str.toString();

        }

        public void warning(SAXParseException ex) {
            Message.warn("xml parsing: " + getLocationString(ex) + ": " + ex.getMessage());
        }

        public void error(SAXParseException ex) {
            addError("xml parsing: " + getLocationString(ex) + ": " + ex.getMessage());
        }

        public void fatalError(SAXParseException ex) throws SAXException {
            addError("[Fatal Error] " + getLocationString(ex) + ": " + ex.getMessage());
        }

        public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {

            super.startElement(uri, localName, name, attributes);
            if ("easyant-config/ivysettings".equals(getContext())) {
                if (attributes.getValue("url") != null && !attributes.getValue("url").equals("")) {
                    easyAntConfiguration.setEasyantIvySettingsUrl(attributes.getValue("url"));
                }
                if (attributes.getValue("file") != null && !attributes.getValue("file").equals("")) {
                    easyAntConfiguration.setEasyantIvySettingsFile(attributes.getValue("file"));
                }
            }
            if ("easyant-config/system-plugins/plugin".equals(getContext())) {
                PluginDescriptor pluginDescriptor = new PluginDescriptor();
                String org = attributes.getValue("org") != null ? attributes.getValue("org") : attributes
                        .getValue("organisation");
                pluginDescriptor.setOrganisation(org);
                pluginDescriptor.setModule(attributes.getValue("module"));
                String rev = attributes.getValue("rev") != null ? attributes.getValue("rev") : attributes
                        .getValue("revision");
                pluginDescriptor.setRevision(rev);
                pluginDescriptor.setMrid(attributes.getValue("mrid"));
                pluginDescriptor.setAs(attributes.getValue("as"));
                boolean mandatory = false;
                if (attributes.getValue("mandatory") != null && "true".equals(attributes.getValue("mandatory"))) {
                    mandatory = true;
                }
                pluginDescriptor.setMandatory(mandatory);
                if (attributes.getValue("inherit-scope") != null) {
                    InheritableScope scope = InheritableScope.valueOf(attributes.getValue("inherit-scope")
                            .toUpperCase());
                    pluginDescriptor.setInheritScope(scope);
                }
                if (attributes.getValue("inheritable") != null && "true".equals(attributes.getValue("inheritable"))) {
                    pluginDescriptor.setInheritable(true);
                }
                pluginDescriptor.setMode(attributes.getValue("mode"));
                easyAntConfiguration.addSystemPlugin(pluginDescriptor);
            }
            if ("easyant-config/properties/property".equals(getContext())) {
                if (attributes.getValue("file") != null || attributes.getValue("url") != null) {
                    Properties properties = new Properties();
                    InputStream is = null;

                    try {
                        if (attributes.getValue("file") != null) {
                            File f = new File(attributes.getValue("file"));
                            is = new FileInputStream(f);
                            properties.load(is);
                        } else if (attributes.getValue("url") != null) {
                            URL url = new URL(attributes.getValue("url"));
                            is = url.openStream();
                            properties.load(is);
                        }

                        Enumeration<?> propertiesEnum = properties.propertyNames();
                        while (propertiesEnum.hasMoreElements()) {
                            String key = (String) propertiesEnum.nextElement();
                            easyAntConfiguration.getDefinedProps().put(key, properties.get(key));
                        }
                    } catch (IOException e) {
                        if (attributes.getValue("file") != null) {
                            throw new SAXException("can't read property file at : " + attributes.getValue("file"));
                        } else if (attributes.getValue("url") != null) {
                            throw new SAXException("can't read property file at : " + attributes.getValue("url"));
                        }
                    } finally {
                        if (is != null) {
                            try {
                                is.close();
                            } catch (IOException e) {
                                // do nothing
                            }
                        }
                    }

                } else if (attributes.getValue("name") != null) {
                    easyAntConfiguration.getDefinedProps().put(attributes.getValue("name"),
                            attributes.getValue("value"));
                }

            }
        }
    }
}
