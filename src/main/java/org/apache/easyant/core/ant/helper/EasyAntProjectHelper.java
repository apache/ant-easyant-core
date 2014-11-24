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
package org.apache.easyant.core.ant.helper;

import org.apache.easyant.core.ant.Phase;
import org.apache.easyant.core.ant.ProjectUtils;
import org.apache.tools.ant.*;
import org.apache.tools.ant.helper.AntXMLContext;
import org.apache.tools.ant.helper.ProjectHelper2;
import org.apache.tools.ant.types.Resource;
import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;

import java.util.Map;

/**
 * This class is the custom project helper used by easyant introducing support for phase concept
 */
public class EasyAntProjectHelper extends ProjectHelper2 {

    public EasyAntProjectHelper() {
        super();
        setProjectHandler(new EasyAntProjectHandler());
        setTargetHandler(new EasyAntTargetHandler());
    }

    @Override
    public boolean canParseBuildFile(Resource buildFile) {
        return buildFile.getName().endsWith(".ant") || buildFile.getName().endsWith(".xml");
    }

    /**
     * Handler for the top level "project" element.
     */
    public static class EasyAntProjectHandler extends ProjectHandler {

        /**
         * Handles the start of a top-level element within the project. An appropriate handler is created and
         * initialised with the details of the element.
         *
         * @param uri     The namespace URI for this element.
         * @param name    The name of the element being started. Will not be <code>null</code>.
         * @param qname   The qualified name for this element.
         * @param attrs   Attributes of the element being started. Will not be <code>null</code>.
         * @param context The context for this element.
         * @return a target or an element handler.
         * @throws org.xml.sax.SAXParseException if the tag given is not <code>"taskdef"</code>, <code>"typedef"</code>,
         *                                       <code>"property"</code>, <code>"target"</code>, <code>"phase"</code> or a data type definition
         */
        public AntHandler onStartChild(String uri, String name, String qname, Attributes attrs, AntXMLContext context)
                throws SAXParseException {

            return (name.equals("target") || name.equals("phase") || name.equals("extension-point"))
                    && (uri.equals("") || uri.equals(ANT_CORE_URI)) ? getTargetHandler() : getElementHandler();
        }
    }

    /**
     * Handler for "target" and "phase" elements.
     */
    public static class EasyAntTargetHandler extends TargetHandler {

        /**
         * Initialisation routine called after handler creation with the element name and attributes. The attributes
         * which this handler can deal with are: <code>"name"</code>, <code>"depends"</code>, <code>"if"</code>,
         * <code>"unless"</code>, <code>"id"</code> and <code>"description"</code>.
         *
         * @param uri     The namespace URI for this element.
         * @param tag     Name of the element which caused this handler to be created. Should not be <code>null</code>.
         *                Ignored in this implementation.
         * @param qname   The qualified name for this element.
         * @param attrs   Attributes of the element which caused this handler to be created. Must not be <code>null</code>.
         * @param context The current context.
         * @throws SAXParseException if an unexpected attribute is encountered or if the <code>"name"</code> attribute is missing.
         */
        public void onStartElement(String uri, String tag, String qname, Attributes attrs, AntXMLContext context)
                throws SAXParseException {

            String name = null;
            String depends = "";
            String extensionPoint = null;
            String phase = null;
            OnMissingExtensionPoint extensionPointMissing = null;

            Project project = context.getProject();

            Target target;
            if ("extension-point".equals(tag)) {
                target = new ExtensionPoint();
            } else if ("phase".equals(tag)) {
                target = new Phase();
            } else {
                target = new Target();
            }

            target.setProject(project);
            target.setLocation(new Location(context.getLocator()));
            context.addTarget(target);

            for (int i = 0; i < attrs.getLength(); i++) {
                String attrUri = attrs.getURI(i);
                if (attrUri != null && !attrUri.equals("") && !attrUri.equals(uri)) {
                    continue; // Ignore attributes from unknown uris
                }
                String key = attrs.getLocalName(i);
                String value = attrs.getValue(i);

                if (key.equals("name")) {
                    name = value;
                    if ("".equals(name)) {
                        throw new BuildException("name attribute must " + "not be empty");
                    }
                } else if (key.equals("depends")) {
                    depends = value;
                } else if (key.equals("if")) {
                    target.setIf(value);
                } else if (key.equals("unless")) {
                    target.setUnless(value);
                } else if (key.equals("id")) {
                    if (value != null && !value.equals("")) {
                        context.getProject().addReference(value, target);
                    }
                } else if (key.equals("description")) {
                    target.setDescription(value);
                } else if (key.equals("extensionOf")) {
                    extensionPoint = value;
                } else if (key.equals("onMissingExtensionPoint")) {
                    try {
                        extensionPointMissing = OnMissingExtensionPoint.valueOf(value);
                    } catch (IllegalArgumentException e) {
                        throw new BuildException("Invalid onMissingExtensionPoint " + value);
                    }
                } else if (key.equals("phase")) {
                    phase = value;
                } else {
                    throw new SAXParseException("Unexpected attribute \"" + key + "\"", context.getLocator());
                }
            }

            if (name == null) {
                throw new SAXParseException("target element appears without a name attribute", context.getLocator());
            }

            boolean isPhase = target instanceof Phase;

            String prefix = null;
            boolean isInIncludeMode = context.isIgnoringProjectTag() && isInIncludeMode();
            String sep = getCurrentPrefixSeparator();

            if (isInIncludeMode && !isPhase) {
                prefix = getTargetPrefix(context);
                if (prefix == null) {
                    throw new BuildException("can't include build file " + context.getBuildFile()
                            + ", no as attribute has been given" + " and the project tag doesn't"
                            + " specify a name attribute");
                }
                name = prefix + sep + name;
            }

            // Check if this target is in the current build file
            if (context.getCurrentTargets().get(name) != null) {
                throw new BuildException("Duplicate target '" + name + "'", target.getLocation());
            }
            Map<String, Target> projectTargets = project.getTargets();
            boolean usedTarget = false;
            // If the name has not already been defined define it
            if (projectTargets.containsKey(name)) {
                project.log("Already defined in main or a previous import, ignore " + name, Project.MSG_VERBOSE);
            } else {
                target.setName(name);
                context.getCurrentTargets().put(name, target);
                project.addOrReplaceTarget(name, target);
                usedTarget = true;
            }

            if (!depends.isEmpty()) {
                if (!isInIncludeMode) {
                    target.setDepends(depends);
                } else {
                    for (String curTarget : Target.parseDepends(depends, name, "depends")) {
                        if (projectTargets.containsKey(curTarget) && (projectTargets.get(curTarget) instanceof Phase)) {

                            target.addDependency(curTarget);
                        } else {
                            target.addDependency(prefix + sep + curTarget);
                        }
                    }
                }
            }
            if (!isInIncludeMode && context.isIgnoringProjectTag() && (prefix = getTargetPrefix(context)) != null) {
                // In an imported file (and not completely
                // ignoring the project tag or having a preconfigured prefix)
                String newName = prefix + sep + name;
                Target newTarget = usedTarget ? new Target(target) : target;
                newTarget.setName(newName);
                context.getCurrentTargets().put(newName, newTarget);
                project.addOrReplaceTarget(newName, newTarget);
            }
            if (extensionPointMissing != null && extensionPoint == null) {
                throw new BuildException("onMissingExtensionPoint attribute cannot "
                        + "be specified unless extensionOf is specified", target.getLocation());
            }
            if (extensionPoint != null) {
                ProjectHelper helper = ProjectUtils.getConfiguredProjectHelper(context.getProject());
                for (String tgName : Target.parseDepends(extensionPoint, name, "extensionOf")) {
                    if (isInIncludeMode()) {
                        tgName = prefix + sep + tgName;
                    }
                    if (extensionPointMissing == null) {
                        extensionPointMissing = OnMissingExtensionPoint.FAIL;
                    }

                    // defer extensionpoint resolution until the full
                    // import stack has been processed
                    helper.getExtensionStack().add(new String[]{tgName, name, extensionPointMissing.name()});
                }
            }
            if (phase != null) {
                if (!projectTargets.containsKey(phase)) {
                    if (!Project.toBoolean(project.getProperty("audit.mode"))) {
                        throw new BuildException("can't add target " + name + " to phase " + phase
                                + " because the phase" + " is unknown.");
                    } else {
                        Phase p = new Phase();
                        p.setName(phase);
                        project.addTarget(p);
                    }
                }

                Target t = projectTargets.get(phase);
                if (t != null) {
                    if (!(t instanceof Phase)) {
                        throw new BuildException("referenced target " + phase + " is not a phase");
                    }
                    t.addDependency(name);
                }
            }

        }

        private String getTargetPrefix(AntXMLContext context) {
            String configuredValue = getCurrentTargetPrefix();
            if (configuredValue != null && configuredValue.isEmpty()) {
                configuredValue = null;
            }
            if (configuredValue != null) {
                return configuredValue;
            }

            String projectName = context.getCurrentProjectName();
            if ("".equals(projectName)) {
                projectName = null;
            }

            return projectName;
        }

    }

}
