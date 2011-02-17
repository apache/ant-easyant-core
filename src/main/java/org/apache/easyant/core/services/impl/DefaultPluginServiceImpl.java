/* 
 *  Copyright 2008-2010 the EasyAnt project
 * 
 *  See the NOTICE file distributed with this work for additional information
 *  regarding copyright ownership. 
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software 
 *  distributed under the License is distributed on an "AS IS" BASIS, 
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and 
 *  limitations under the License.
 */

package org.apache.easyant.core.services.impl;

import java.io.File;
import java.util.Iterator;

import org.apache.easyant.core.EasyAntConstants;
import org.apache.easyant.core.descriptor.EasyAntModuleDescriptor;
import org.apache.easyant.core.descriptor.PluginDescriptor;
import org.apache.easyant.core.descriptor.PropertyDescriptor;
import org.apache.easyant.core.parser.DefaultEasyAntXmlModuleDescriptorParser;
import org.apache.easyant.core.parser.EasyAntModuleDescriptorParser;
import org.apache.easyant.core.report.EasyAntReport;
import org.apache.easyant.core.report.EasyAntReportModuleParser;
import org.apache.easyant.core.report.ImportedModuleReport;
import org.apache.easyant.core.services.PluginService;
import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.ivy.plugins.repository.url.URLResource;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.util.Message;
import org.xml.sax.SAXException;

public class DefaultPluginServiceImpl implements PluginService {

	private final EasyAntModuleDescriptorParser parser;

	private final Ivy ivyInstance;

	/**
	 * This is the default constructor, the IvyContext should be the IvyContext
	 * configured to the easyant ivy instance
	 * @param ivyInstance the easyant ivy instance
	 */
	public DefaultPluginServiceImpl(final Ivy ivyInstance) {
		this(ivyInstance, new DefaultEasyAntXmlModuleDescriptorParser());
	}

	/**
	 * A custom constructor if you want to specify your own parser /
	 * configuration service, you should use this constructor
	 * the IvyContext should be the IvyContext
	 * configured to the easyant ivy instance
	 * @param ivyInstance the easyant ivy instance
	 * @param parser
	 *            a valid easyantModuleDescriptor
	 */
	public DefaultPluginServiceImpl(final Ivy ivyInstance,
			EasyAntModuleDescriptorParser parser) {
		this.ivyInstance = ivyInstance;
		if (parser == null) {
			throw new IllegalArgumentException(
					"You must set a valid easyant module descriptor parser");
		}
		this.parser = parser;
		ModuleDescriptorParserRegistry.getInstance().addParser(parser);
	}

	public EasyAntReport getPluginInfo(ModuleRevisionId moduleRevisionId,
			String conf) throws Exception {
		try {
			
			IvyContext.pushNewContext().setIvy(ivyInstance);
			EasyAntReport eaReport = EasyAntReportModuleParser.parseEasyAntModule(
					moduleRevisionId, conf);
			
			IvyContext.popContext();
			return eaReport;
		} catch (SAXException e) {
			throw new Exception("Impossible to parse " + moduleRevisionId, e);
		}
	}

	public EasyAntReport getPluginInfo(ModuleRevisionId moduleRevisionId)
			throws Exception {
		return getPluginInfo(moduleRevisionId, "default");
	}

	public EasyAntReport getPluginInfo(String moduleRevisionId)
			throws Exception {
		String mrid = moduleRevisionId;
		if (!mrid.matches(".*#.*")) {
			Message.debug("No organisation specified for plugin " + mrid
					+ " using the default one");
			mrid = EasyAntConstants.EASYANT_PLUGIN_ORGANISATION + "#" + mrid;
		}
		ModuleRevisionId module = ModuleRevisionId.parse(mrid);
		return getPluginInfo(module);
	}

	public EasyAntReport getBuildTypeInfo(String moduleRevisionId)
			throws Exception {
		String mrid = moduleRevisionId;
		if (!mrid.matches(".*#.*")) {
			Message.debug("No organisation specified for buildtype " + mrid
					+ " using the default one");
			mrid = EasyAntConstants.EASYANT_BUILDTYPES_ORGANISATION + "#"
					+ mrid;
		}
		ModuleRevisionId module = ModuleRevisionId.parse(mrid);
		return getPluginInfo(module);
	}

	public EasyAntModuleDescriptor getEasyAntModuleDescriptor(
			File moduleDescriptor) throws Exception {
		if (moduleDescriptor == null)
			throw new Exception("moduleDescriptor cannot be null");
		if (!moduleDescriptor.exists()) {
			throw new Exception(
					"imposible to find the specified module descriptor"
							+ moduleDescriptor.getAbsolutePath());
		}
		IvyContext.pushNewContext().setIvy(ivyInstance);
		// First we need to parse the specified file to retrieve all the easyant
		// stuff
		parser.parseDescriptor(ivyInstance.getSettings(),
				moduleDescriptor.toURL(), new URLResource(moduleDescriptor
						.toURL()), true);
		EasyAntModuleDescriptor md = parser.getEasyAntModuleDescriptor();
		IvyContext.popContext();
		return md;
	}

	public EasyAntReport generateEasyAntReport(File moduleDescriptor)
			throws Exception {
		EasyAntReport eaReport = new EasyAntReport();
		try {
			EasyAntModuleDescriptor md = getEasyAntModuleDescriptor(moduleDescriptor);

			// Then we can Store properties
			for (Iterator<PropertyDescriptor> iterator = md.getProperties()
					.values().iterator(); iterator.hasNext();) {
				PropertyDescriptor property = iterator.next();
				eaReport.addPropertyDescriptor(property.getName(), property);
			}

			// Store infos on the buildtype
			if (md.getBuildType() != null) {
				ImportedModuleReport buildType = new ImportedModuleReport();
				buildType.setModuleMrid(md.getBuildType());
				buildType.setEasyantReport(getPluginInfo(ModuleRevisionId
						.parse(md.getBuildType())));
				eaReport.addImportedModuleReport(buildType);
				// Store infos on plugins
				for (Iterator iterator = md.getPlugins().iterator(); iterator
						.hasNext();) {
					PluginDescriptor plugin = (PluginDescriptor) iterator
							.next();
					ImportedModuleReport pluginReport = new ImportedModuleReport();
					pluginReport.setModuleMrid(plugin.getMrid());
					pluginReport.setAs(plugin.getAs());
					pluginReport.setType(plugin.getMode());
					pluginReport
							.setEasyantReport(getPluginInfo(ModuleRevisionId
									.parse(plugin.getMrid())));
					eaReport.addImportedModuleReport(pluginReport);
				}
			}

		} catch (Exception e) {
			throw new Exception("problem while parsing Ivy module file: "
					+ e.getMessage(), e);
		}
		return eaReport;
	}

	public ModuleRevisionId[] search(String organisation, String moduleName,
			String revision, String branch, String matcher, String resolver) throws Exception {
		IvySettings settings = ivyInstance.getSettings();

		if (moduleName == null && PatternMatcher.EXACT.equals(matcher)) {
			throw new Exception(
					"no module name provided for ivy repository graph task: "
							+ "It can either be set explicitely via the attribute 'module' or "
							+ "via 'ivy.module' property or a prior call to <resolve/>");
		} else if (moduleName == null && !PatternMatcher.EXACT.equals(matcher)) {
			moduleName = PatternMatcher.ANY_EXPRESSION;
		}
		ModuleRevisionId mrid = ModuleRevisionId.newInstance(organisation,
				moduleName, revision);

		ModuleRevisionId criteria = null;

		if ((revision == null) || settings.getVersionMatcher().isDynamic(mrid)) {
			criteria = new ModuleRevisionId(new ModuleId(organisation,
					moduleName), branch, "*");
		} else {
			criteria = new ModuleRevisionId(new ModuleId(organisation,
					moduleName), branch, revision);
		}

		PatternMatcher patternMatcher = settings.getMatcher(matcher);
		if ("*".equals(resolver)) {
			//search in all resolvers.  this can be quite slow for complex repository configurations
			//with ChainResolvers, since resolvers in chains will be searched multiple times.
			return ivyInstance.listModules(criteria, patternMatcher);
		} else {
			//limit search to the specified resolver.
			DependencyResolver dependencyResolver =
					resolver == null ? settings.getDefaultResolver()
									 : settings.getResolver(resolver);
			if (dependencyResolver == null) {
				throw new IllegalArgumentException("Unknown dependency resolver for search: " + resolver);
			}

			ivyInstance.pushContext();
			try {
				return ivyInstance.getSearchEngine().listModules(dependencyResolver, criteria, patternMatcher);
			} finally {
				ivyInstance.popContext();
			}
		}
	}

	public ModuleRevisionId[] search(String organisation, String moduleName)
			throws Exception {
		return search(organisation, moduleName, null, null,
				PatternMatcher.EXACT_OR_REGEXP, null);
	}

	public String[] searchModule(String organisation, String moduleName)
			throws Exception {
		ModuleRevisionId[] mrids = search(organisation, moduleName);
		String[] result = new String[mrids.length];
		for (int i = 0; i < mrids.length; i++) {
			result[i] = mrids[i].toString();
		}
		return result;
	}
	
	public String getDescription(ModuleRevisionId mrid) {
		ResolvedModuleRevision rmr = ivyInstance.findModule(mrid);
		return rmr.getDescriptor().getDescription();
	}
	
	
	public String getPluginDescription(String moduleRevisionId) {
		String mrid = moduleRevisionId;
		if (!mrid.matches(".*#.*")) {
			Message.debug("No organisation specified for plugin " + mrid
					+ " using the default one");
			mrid = EasyAntConstants.EASYANT_PLUGIN_ORGANISATION + "#" + mrid;
		}
		ModuleRevisionId module = ModuleRevisionId.parse(mrid);

		return getDescription(module);
	}
	
	public String getBuildTypeDescription(String moduleRevisionId) {
		String mrid = moduleRevisionId;
		if (!mrid.matches(".*#.*")) {
			Message.debug("No organisation specified for buildtype " + mrid
					+ " using the default one");
			mrid = EasyAntConstants.EASYANT_BUILDTYPES_ORGANISATION + "#" + mrid;
		}
		ModuleRevisionId module = ModuleRevisionId.parse(mrid);

		return getDescription(module);
	}

}
