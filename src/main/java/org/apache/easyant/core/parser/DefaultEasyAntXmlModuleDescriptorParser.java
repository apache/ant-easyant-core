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
package org.apache.easyant.core.parser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.easyant.core.EasyAntConstants;
import org.apache.easyant.core.descriptor.AdvancedInheritableItem;
import org.apache.easyant.core.descriptor.DefaultEasyAntDescriptor;
import org.apache.easyant.core.descriptor.EasyAntModuleDescriptor;
import org.apache.easyant.core.descriptor.ExtensionPointMappingDescriptor;
import org.apache.easyant.core.descriptor.PluginDescriptor;
import org.apache.easyant.core.descriptor.PluginType;
import org.apache.easyant.core.descriptor.PropertyDescriptor;
import org.apache.easyant.core.ivy.InheritableScope;
import org.apache.ivy.ant.IvyConflict;
import org.apache.ivy.ant.IvyDependency;
import org.apache.ivy.ant.IvyDependencyArtifact;
import org.apache.ivy.ant.IvyDependencyConf;
import org.apache.ivy.ant.IvyDependencyExclude;
import org.apache.ivy.ant.IvyDependencyInclude;
import org.apache.ivy.ant.IvyExclude;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;
import org.apache.ivy.plugins.parser.ParserSettings;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorParser;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.PropertiesFile;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Parses an easyant module descriptor and output an EasyAntModuleDescriptor
 */
public class DefaultEasyAntXmlModuleDescriptorParser extends XmlModuleDescriptorParser implements
        EasyAntModuleDescriptorParser {

    private static final DefaultEasyAntXmlModuleDescriptorParser INSTANCE = new DefaultEasyAntXmlModuleDescriptorParser();

    public static DefaultEasyAntXmlModuleDescriptorParser getInstance() {
        return INSTANCE;
    }

    static final String[] PLUGIN_REGULAR_ATTRIBUTES = new String[] { "organisation", "org", "module", "revision",
            "rev", "mrid", "conf", "mode", "as", "inheritable", "inherit-scope" };

    public boolean accept(Resource res) {
        return res.getName().endsWith(".ivy") || res.getName().endsWith(".xml");
    }

    protected Parser newParser(ParserSettings ivySettings) {

        return new EasyAntParser(this, ivySettings);
    }

    private EasyAntModuleDescriptor easyAntModuleDescriptor;

    public EasyAntModuleDescriptor getEasyAntModuleDescriptor() {
        return easyAntModuleDescriptor;
    }

    public ModuleDescriptor parseDescriptor(ParserSettings ivySettings, URL xmlURL, Resource res, boolean validate)
            throws ParseException, IOException {

        EasyAntParser parser = (EasyAntParser) newParser(ivySettings);
        parser.setValidate(validate);
        parser.setResource(res);
        parser.setInput(xmlURL);
        parser.parse();
        easyAntModuleDescriptor = parser.getEasyAntModuleDescriptor();
        return parser.getModuleDescriptor();
    }

    private enum EasyAntState {
        NONE, EASYANT, PLUGIN, PLUGIN_DEPENDENCY
    }

    public class EasyAntParser extends Parser {

        private EasyAntState easyAntState;

        private DefaultEasyAntDescriptor easyAntModuleDescriptor;

        private String easyantPrefix;

        private PluginDescriptor currentPluginDescriptor;
        private IvyDependency currentPluginDependency;

        public EasyAntParser(ModuleDescriptorParser parser, ParserSettings ivySettings) {
            super(parser, ivySettings);
            easyAntState = EasyAntState.NONE;
            easyAntModuleDescriptor = new DefaultEasyAntDescriptor();
            easyantPrefix = "";
        }

        public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
            super.startElement(uri, localName, name, attributes);
            if (name.equals(easyantPrefix + ":build") && State.EXTRA_INFO == getState()) {
                eaBuildStarted(attributes);
            }
            if (name.equals(easyantPrefix + ":plugin") && State.EXTRA_INFO == getState()) {
                pluginStarted(attributes);
            }
            if (name.equals(easyantPrefix + ":property") && State.EXTRA_INFO == getState()) {
                easyantPropertyStarted(attributes);
            }
            if (name.equals(easyantPrefix + ":bindtarget") && State.EXTRA_INFO == getState()) {
                bindTargetStarted(attributes);
            }

            if (name.equals(easyantPrefix + ":dependency") && easyAntState == EasyAntState.PLUGIN) {
                pluginDependencyStarted(attributes);
            }
            if (name.equals(easyantPrefix + ":exclude") && easyAntState == EasyAntState.PLUGIN_DEPENDENCY) {
                pluginDependencyExcludeStarted(attributes);
            }
            if (name.equals(easyantPrefix + ":include") && easyAntState == EasyAntState.PLUGIN_DEPENDENCY) {
                pluginDependencyIncludeStarted(attributes);
            }
            if (name.equals(easyantPrefix + ":artifact") && easyAntState == EasyAntState.PLUGIN_DEPENDENCY) {
                pluginDependencyIncludeStarted(attributes);
            }
            if (name.equals(easyantPrefix + ":conf") && easyAntState == EasyAntState.PLUGIN_DEPENDENCY) {
                pluginDependencyConfStarted(attributes);
            }

            if (name.equals(easyantPrefix + ":exclude") && easyAntState == EasyAntState.PLUGIN) {
                pluginExcludeStarted(attributes);
            }
            if (name.equals(easyantPrefix + ":conflict") && easyAntState == EasyAntState.PLUGIN) {
                pluginConflictStarted(attributes);
            }
        }

        @Override
        public void endElement(String uri, String localName, String name) throws SAXException {
            super.endElement(uri, localName, name);
            if (name.equals(easyantPrefix + ":plugin") && easyAntState == EasyAntState.PLUGIN) {
                endPlugin();
            }

            if (name.equals(easyantPrefix + ":dependency") && easyAntState == EasyAntState.PLUGIN_DEPENDENCY) {
                endPluginDependency();
            }
        }

        protected void endPlugin() {
            currentPluginDescriptor = null;
            easyAntState = EasyAntState.NONE;
        }

        public void endPluginDependency() {
            currentPluginDependency = null;
            easyAntState = EasyAntState.PLUGIN;

        }

        @Override
        protected void ivyModuleStarted(Attributes attributes) throws SAXException {
            super.ivyModuleStarted(attributes);
            // lookup easyant namespace
            for (Iterator<?> iterator = getMd().getExtraAttributesNamespaces().entrySet().iterator(); iterator
                    .hasNext();) {
                Entry<?, ?> namespace = (Entry<?, ?>) iterator.next();
                if (EasyAntConstants.EASYANT_MD_NAMESPACE.equals(namespace.getValue())) {
                    easyantPrefix = (String) namespace.getKey();
                }
            }

        }

        @Override
        public void endDocument() throws SAXException {
            super.endDocument();

            for (Configuration conf : getMd().getConfigurations()) {
                if ("profile".equals(conf.getExtraAttribute(easyantPrefix + ":type"))) {
                    Message.debug("Adding build configuration named " + conf.getName());
                    easyAntModuleDescriptor.addBuildConfiguration(conf.getName());
                }
            }
            try {
                easyAntModuleDescriptor.setIvyModuleDescriptor(super.getModuleDescriptor());
            } catch (ParseException e) {
                throw new SAXException(e);
            }
        }

        /**
         * Parsing the plugin tag
         * 
         * @param attributes
         *            reprensents the plugins attributes
         */
        protected void pluginStarted(Attributes attributes) {
            easyAntState = EasyAntState.PLUGIN;
            PluginDescriptor plugin = handleCommonPluginDescriptorAttributes(attributes, PluginType.PLUGIN);
            // plugin specific attribute
            boolean mandatory = false;
            String mandatoryValue = getSettings().substitute(attributes.getValue("mandatory"));
            if (mandatoryValue != null && "true".equals(mandatoryValue)) {
                mandatory = true;
            }
            plugin.setMandatory(mandatory);
            handleInheritedScopeAttribute(attributes, plugin);

            currentPluginDescriptor = plugin;
            easyAntModuleDescriptor.addPlugin(plugin);

        }

        /**
         * Parsing the easyant tag
         * 
         * @param attributes
         *            reprensents the easyant attributes
         */
        protected void eaBuildStarted(Attributes attributes) {
            easyAntState = EasyAntState.EASYANT;
            PluginDescriptor buildtype = handleCommonPluginDescriptorAttributes(attributes, PluginType.BUILDTYPE);
            // a build type cannot be skipped
            buildtype.setMandatory(true);
            easyAntModuleDescriptor.setBuildType(buildtype);
        }

        private PluginDescriptor handleCommonPluginDescriptorAttributes(Attributes attributes, PluginType pluginType) {
            PluginDescriptor pluginDescriptor = new PluginDescriptor();
            String mrid = getSettings().substitute(attributes.getValue("mrid"));
            if (mrid != null) {
                if (!mrid.matches(".*#.*")) {
                    if (pluginType == PluginType.BUILDTYPE) {
                        Message.debug("No organisation specified for buildtype " + mrid + " using the default one");
                        mrid = EasyAntConstants.EASYANT_BUILDTYPES_ORGANISATION + "#" + mrid;
                    } else {
                        Message.debug("No organisation specified for plugin " + mrid + " using the default one");
                        mrid = EasyAntConstants.EASYANT_PLUGIN_ORGANISATION + "#" + mrid;
                    }
                }
                pluginDescriptor.setMrid(mrid);
            } else {
                String org = attributes.getValue("org") != null ? attributes.getValue("org") : attributes
                        .getValue("organisation");
                org = getSettings().substitute(org);
                if (org == null) {
                    if (pluginType == PluginType.BUILDTYPE) {
                        Message.debug("No organisation specified for buildtype " + mrid + " using the default one");
                        org = EasyAntConstants.EASYANT_BUILDTYPES_ORGANISATION;
                    } else {
                        Message.debug("No organisation specified for plugin " + mrid + " using the default one");
                        org = EasyAntConstants.EASYANT_PLUGIN_ORGANISATION;
                    }
                }
                String module = getSettings().substitute(attributes.getValue("module"));
                String revision = attributes.getValue("rev") != null ? attributes.getValue("rev") : attributes
                        .getValue("revision");
                revision = getSettings().substitute(revision);
                pluginDescriptor.setOrganisation(org);
                pluginDescriptor.setModule(module);
                pluginDescriptor.setRevision(revision);
            }

            String conf = getSettings().substitute(attributes.getValue("conf"));
            pluginDescriptor.setBuildConfigurations(conf);

            pluginDescriptor.setMode(getSettings().substitute(attributes.getValue("mode")));
            pluginDescriptor.setAs(getSettings().substitute(attributes.getValue("as")));
            handlePropertyAsAttribute(attributes, conf);
            return pluginDescriptor;
        }

        public void pluginDependencyStarted(Attributes attributes) {
            easyAntState = EasyAntState.PLUGIN_DEPENDENCY;
            IvyDependency ivyDependency = new IvyDependency();
            ivyDependency.setOrg(getSettings().substitute(attributes.getValue("org")));
            ivyDependency.setForce(Boolean.valueOf(getSettings().substitute(attributes.getValue("force")))
                    .booleanValue());
            ivyDependency.setChanging(Boolean.valueOf(getSettings().substitute(attributes.getValue("changing")))
                    .booleanValue());
            String transitiveValue = getSettings().substitute(attributes.getValue("transitive"));
            boolean transitive = (transitiveValue == null) ? true : Boolean.valueOf(attributes.getValue("transitive"))
                    .booleanValue();
            ivyDependency.setTransitive(transitive);

            ivyDependency.setName(getSettings().substitute(attributes.getValue("name")));
            ivyDependency.setBranch(getSettings().substitute(attributes.getValue("branch")));

            ivyDependency.setRev(getSettings().substitute(attributes.getValue("rev")));
            currentPluginDescriptor.addDependency(ivyDependency);
            currentPluginDependency = ivyDependency;
        }

        public void pluginDependencyExcludeStarted(Attributes attributes) {
            IvyDependencyExclude ivyDependencyExclude = currentPluginDependency.createExclude();
            ivyDependencyExclude.setOrg(getSettings().substitute(attributes.getValue("org")));
            ivyDependencyExclude.setModule(getSettings().substitute(attributes.getValue("module")));
            ivyDependencyExclude.setName(getSettings().substitute(attributes.getValue("name")));
            ivyDependencyExclude.setExt(getSettings().substitute(attributes.getValue("ext")));
            ivyDependencyExclude.setType(getSettings().substitute(attributes.getValue("type")));
            ivyDependencyExclude.setMatcher(getSettings().substitute(attributes.getValue("matcher")));

        }

        public void pluginDependencyIncludeStarted(Attributes attributes) {
            IvyDependencyInclude ivyDependencyInclude = currentPluginDependency.createInclude();
            ivyDependencyInclude.setName(getSettings().substitute(attributes.getValue("name")));
            ivyDependencyInclude.setExt(getSettings().substitute(attributes.getValue("ext")));
            ivyDependencyInclude.setType(getSettings().substitute(attributes.getValue("type")));
            ivyDependencyInclude.setMatcher(getSettings().substitute(attributes.getValue("matcher")));
        }

        public void pluginDependencyArtifactStarted(Attributes attributes) {
            IvyDependencyArtifact ivyDependencyArtifact = currentPluginDependency.createArtifact();
            ivyDependencyArtifact.setName(getSettings().substitute(attributes.getValue("name")));
            ivyDependencyArtifact.setExt(getSettings().substitute(attributes.getValue("ext")));
            ivyDependencyArtifact.setType(getSettings().substitute(attributes.getValue("type")));
            ivyDependencyArtifact.setUrl(getSettings().substitute(attributes.getValue("url")));
        }

        public void pluginDependencyConfStarted(Attributes attributes) {
            IvyDependencyConf ivyDependencyConf = currentPluginDependency.createConf();
            ivyDependencyConf.setMapped(getSettings().substitute(attributes.getValue("mapped")));
        }

        public void pluginConflictStarted(Attributes attributes) {
            IvyConflict ivyConflict = new IvyConflict();
            ivyConflict.setOrg(getSettings().substitute(attributes.getValue("org")));
            ivyConflict.setModule(getSettings().substitute(attributes.getValue("module")));
            ivyConflict.setRev(getSettings().substitute(attributes.getValue("rev")));
            ivyConflict.setManager(getSettings().substitute(attributes.getValue("manager")));
            ivyConflict.setMatcher(getSettings().substitute(attributes.getValue("matcher")));
            currentPluginDescriptor.addConflict(ivyConflict);
        }

        public void pluginExcludeStarted(Attributes attributes) {
            IvyExclude ivyExclude = new IvyExclude();
            ivyExclude.setOrg(getSettings().substitute(attributes.getValue("org")));
            ivyExclude.setModule(getSettings().substitute(attributes.getValue("module")));
            ivyExclude.setArtifact(getSettings().substitute(attributes.getValue("artifact")));
            ivyExclude.setType(getSettings().substitute(attributes.getValue("type")));
            ivyExclude.setExt(getSettings().substitute(attributes.getValue("ext")));
            ivyExclude.setMatcher(getSettings().substitute(attributes.getValue("matcher")));
            currentPluginDescriptor.addExcludes(ivyExclude);
        }

        /**
         * handle properties as attribute
         * 
         * @param attributes
         *            a set of attributes
         * @param conf
         *            build configurations where this property should be applied (can be null)
         */
        protected void handlePropertyAsAttribute(Attributes attributes, String conf) {
            List<String> ignored = Arrays.asList(PLUGIN_REGULAR_ATTRIBUTES);
            for (int i = 0; i < attributes.getLength(); i++) {
                if (!ignored.contains(attributes.getQName(i))) {
                    String propertyName = attributes.getQName(i);
                    String value = IvyContext.getContext().getSettings().substitute(attributes.getValue(i));
                    PropertyDescriptor property = new PropertyDescriptor(propertyName);
                    property.setValue(value);
                    property.setBuildConfigurations(conf);
                    applyInheritableItemAttributesFromPlugin(property);
                    easyAntModuleDescriptor.getProperties().put(propertyName, property);
                }
            }
        }

        /**
         * Handle inherited scope specific attributes
         * 
         * @param attributes
         *            a set of attributes
         * @param inheritScopeElement
         *            an element supporting inherit scope attributes
         */
        private void handleInheritedScopeAttribute(Attributes attributes, AdvancedInheritableItem inheritScopeElement) {
            String inheritScopeValue = getSettings().substitute(attributes.getValue("inherit-scope"));
            if (inheritScopeValue != null) {
                InheritableScope scope = InheritableScope.valueOf(inheritScopeValue.toUpperCase());
                inheritScopeElement.setInheritScope(scope);
            }

            String inheritableValue = getSettings().substitute(attributes.getValue("inheritable"));
            if (inheritableValue != null) {
                inheritScopeElement.setInheritable("true".equalsIgnoreCase(inheritableValue));
            }

        }

        /**
         * Parsing the bindtarget tag
         * 
         * @param attributes
         *            reprensents the bindtarget attributes
         */
        protected void bindTargetStarted(Attributes attributes) {
            String target = getSettings().substitute(attributes.getValue("target"));
            String toExtensionPoint = getSettings().substitute(attributes.getValue("extensionOf"));
            String conf = getSettings().substitute(attributes.getValue("conf"));
            if (EasyAntState.PLUGIN == easyAntState && conf == null) {
                conf = currentPluginDescriptor.getBuildConfigurations();
            }

            // if bindtarget tag is a subelement and look if an alias was
            // defined on the plugin
            if (EasyAntState.PLUGIN == easyAntState && currentPluginDescriptor.getAs() != null) {
                // check if the fully qualified name was defined, if not get
                // the alias defined on the plugin
                if (!target.startsWith(currentPluginDescriptor.getAs())) {
                    target = currentPluginDescriptor.getAs() + target;
                }

            }
            ExtensionPointMappingDescriptor extensionPointMappingDescriptor = new ExtensionPointMappingDescriptor();
            extensionPointMappingDescriptor.setBuildConfigurations(conf);
            extensionPointMappingDescriptor.setTarget(target);
            extensionPointMappingDescriptor.setExtensionPoint(toExtensionPoint);

            handleInheritedScopeAttribute(attributes, extensionPointMappingDescriptor);

            easyAntModuleDescriptor.addExtensionPointMapping(extensionPointMappingDescriptor);

        }

        public EasyAntModuleDescriptor getEasyAntModuleDescriptor() {
            return easyAntModuleDescriptor;
        }

        /**
         * Parse the property tag
         * 
         * @param attributes
         *            reprensents the plugins attributes
         * @throws SAXException
         */
        protected void easyantPropertyStarted(Attributes attributes) throws SAXException {
            String conf = getSettings().substitute(attributes.getValue("conf"));
            // if property tag is a subelement and
            // no build configuration was defined looked at plugin build
            // configurations
            if (EasyAntState.PLUGIN == easyAntState && conf == null) {
                conf = currentPluginDescriptor.getBuildConfigurations();
            }

            if (attributes.getValue("file") != null) {
                String fileName = getSettings().substitute(attributes.getValue("file"));
                File file = new File(fileName);
                if (!file.exists()) {
                    throw new SAXException(fileName + " doesn't exists !");
                }
                PropertiesFile props = new PropertiesFile(file, "project properties");
                Enumeration<?> enumeration = props.propertyNames();
                while (enumeration.hasMoreElements()) {
                    String key = (String) enumeration.nextElement();
                    String value = getSettings().substitute(props.getProperty(key));

                    // put this variable on the context
                    IvyContext.getContext().getSettings().getVariableContainer().setVariable(key, value, true);
                    PropertyDescriptor property = new PropertyDescriptor(key);
                    property.setValue(value);
                    property.setBuildConfigurations(conf);

                    applyInheritableItemAttributesFromPlugin(property);
                    // override with explicit inherited scope attributes
                    handleInheritedScopeAttribute(attributes, property);

                    easyAntModuleDescriptor.getProperties().put(key, property);
                }
            } else {
                if (attributes.getValue("name") == null) {
                    throw new SAXException("Can't set a null property!");
                }
                String propertyName = getSettings().substitute(attributes.getValue("name"));
                String value = getSettings().substitute(attributes.getValue("value"));

                // put this variable on the context
                IvyContext.getContext().getSettings().getVariableContainer().setVariable(propertyName, value, true);
                PropertyDescriptor property = new PropertyDescriptor(propertyName, getMd().getModuleRevisionId());
                property.setValue(value);
                property.setBuildConfigurations(conf);

                applyInheritableItemAttributesFromPlugin(property);
                // override with explicit inherited scope attributes
                handleInheritedScopeAttribute(attributes, property);

                easyAntModuleDescriptor.getProperties().put(propertyName, property);
            }
        }

        /**
         * Apply {@link AdvancedInheritableItem} attributes from current plugin
         * 
         * @param property
         */
        private void applyInheritableItemAttributesFromPlugin(PropertyDescriptor property) {
            if (EasyAntState.PLUGIN == easyAntState) {
                property.setInheritable(currentPluginDescriptor.isInheritable());
                property.setInheritScope(currentPluginDescriptor.getInheritScope());
            }
        }

        /**
         * Merge all metadata with an other module descriptor
         * 
         * @param parent
         *            an other module descriptor
         */
        protected void mergeAll(ModuleDescriptor parent) {
            super.mergeAll(parent);
            if (parent.getParser() instanceof DefaultEasyAntXmlModuleDescriptorParser) {
                DefaultEasyAntXmlModuleDescriptorParser parser = (DefaultEasyAntXmlModuleDescriptorParser) parent
                        .getParser();
                mergeEasyantProperties(parser.getEasyAntModuleDescriptor().getProperties());
                mergeEasyantPlugins(parser.getEasyAntModuleDescriptor().getPlugins());
                mergeBindTargets(parser.getEasyAntModuleDescriptor().getExtensionPointsMappings());
            }
        }

        /**
         * Merge with other module descriptor
         * 
         * @param extendTypes
         *            a string that represents what we should extends
         * @param parent
         *            an other module descriptor
         * @throws ParseException
         */
        protected void mergeWithOtherModuleDescriptor(@SuppressWarnings("rawtypes") List extendTypes,
                ModuleDescriptor parent) throws ParseException {
            super.mergeWithOtherModuleDescriptor(extendTypes, parent);
            if (parent.getParser() instanceof DefaultEasyAntXmlModuleDescriptorParser) {
                DefaultEasyAntXmlModuleDescriptorParser parser = (DefaultEasyAntXmlModuleDescriptorParser) parent
                        .getParser();

                if (extendTypes.contains("properties")) {
                    mergeEasyantProperties(parser.getEasyAntModuleDescriptor().getProperties());
                }
                if (extendTypes.contains("plugins")) {
                    mergeEasyantPlugins(parser.getEasyAntModuleDescriptor().getPlugins());
                }
                if (extendTypes.contains("bindtarget")) {
                    mergeBindTargets(parser.getEasyAntModuleDescriptor().getExtensionPointsMappings());
                }
            }

        }

        /**
         * Merge easyant plugins
         * 
         * @param plugins
         *            a list of plugins that will be merged with current one
         */
        protected void mergeEasyantPlugins(List<PluginDescriptor> plugins) {
            for (PluginDescriptor plugin : plugins) {
                if (plugin.isInheritable()) {
                    StringBuilder sb = new StringBuilder("Merging plugin : ");
                    sb.append(plugin.toString());
                    if (plugin.getSourceModule() != null) {
                        sb.append(" from ").append(plugin.getSourceModule().toString());
                    }
                    Message.debug(sb.toString());

                    easyAntModuleDescriptor.addPlugin(plugin);
                }
            }
        }

        /**
         * Merge easyant properties
         * 
         * @param properties
         *            a map of properties that will be merged with current one
         */
        protected void mergeEasyantProperties(Map<String, PropertyDescriptor> properties) {
            for (Iterator<PropertyDescriptor> iterator = properties.values().iterator(); iterator.hasNext();) {
                PropertyDescriptor prop = iterator.next();
                if (prop.isInheritable()) {
                    IvyContext.getContext().getSettings().getVariableContainer()
                            .setVariable(prop.getName(), prop.getValue(), true);
                    StringBuilder sb = new StringBuilder("Merging property");
                    sb.append(prop.getName());
                    if (prop.getSourceModule() != null) {
                        sb.append(" from ").append(prop.getSourceModule().toString());
                    }
                    Message.debug(sb.toString());
                    easyAntModuleDescriptor.getProperties().put(prop.getName(), prop);
                }
            }
        }

        protected void mergeBindTargets(List<ExtensionPointMappingDescriptor> extensionPointsMappings) {
            for (ExtensionPointMappingDescriptor extensionPointMappingDescriptor : extensionPointsMappings) {
                if (extensionPointMappingDescriptor.isInheritable()) {
                    StringBuilder sb = new StringBuilder("Merging extension-point binding : ");
                    sb.append(extensionPointMappingDescriptor.toString());
                    if (extensionPointMappingDescriptor.getSourceModule() != null) {
                        sb.append(" from ").append(extensionPointMappingDescriptor.getSourceModule().toString());
                    }
                    Message.debug(sb.toString());
                    easyAntModuleDescriptor.addExtensionPointMapping(extensionPointMappingDescriptor);
                }
            }
        }

        /**
         * Get the default parent location
         * 
         * @return a string that represents the default parent location
         */
        protected String getDefaultParentLocation() {
            return "../parent.ivy";
        }
    }
}
