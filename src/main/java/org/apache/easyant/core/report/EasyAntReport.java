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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.easyant.core.descriptor.PropertyDescriptor;
import org.apache.ivy.core.report.ResolveReport;

/**
 * This class represent a report of an easyant module It contains all
 * informations about properties / parameters / plugins / etc..
 */
public class EasyAntReport {

	private List<TargetReport> targetReports;
	private List<PhaseReport> phaseReports;
	private List<ParameterReport> parameterReports;
	private List<ImportedModuleReport> importedModuleReports;
	private Map<String, PropertyDescriptor> propertyReports;
	private ResolveReport resolveReport;

	/**
	 * Default Constructor
	 */
	public EasyAntReport() {
		super();
		targetReports = new ArrayList<TargetReport>();
		phaseReports = new ArrayList<PhaseReport>();
		parameterReports = new ArrayList<ParameterReport>();
		importedModuleReports = new ArrayList<ImportedModuleReport>();
		propertyReports = new HashMap<String, PropertyDescriptor>();
	}

	/**
	 * Get a list of targets available in this module
	 * 
	 * @return
	 */
	public List<TargetReport> getTargetReports() {
		return Collections.unmodifiableList(targetReports);
	}

	public TargetReport getTargetReport(String name) {
		if (name == null || name.equals("")) {
			throw new IllegalArgumentException("target name cannot be null");
		}
		for (TargetReport target : targetReports) {
			if (target.getName().equals(name)) {
				return target;
			}
		}
		return null;
	}
	
	public TargetReport getTargetReport(String name, boolean includeImports) {
		TargetReport retVal = getTargetReport(name);
		if(retVal == null && includeImports) {
			List<TargetReport> targets = getAvailableTargets();
			for(TargetReport target : targets) {
				if (target.getName().equals(name)) {
					retVal = target;
				}
			}
		}
		return retVal;
	}
	
	/**
	 * Add a given targetReport to the list of know target
	 * 
	 * @param targetReport
	 *            a given targeReport
	 */
	public void addTargetReport(TargetReport targetReport) {
		if (targetReport == null) {
			throw new IllegalArgumentException("targetReport cannot be null");
		}
		targetReports.add(targetReport);
	}

	/**
	 * Get a phase by its name Return null if no phaseReport where found with
	 * this name
	 * 
	 * @param name
	 *            represent the phase name
	 * @return a phase report
	 */
	public PhaseReport getPhaseReport(String name) {
		if (name == null || name.equals("")) {
			throw new IllegalArgumentException("phase name cannot be null");
		}
		for (PhaseReport phase : phaseReports) {
			if (phase.getName().equals(name)) {
				return phase;
			}
		}
		return null;
	}

	/**
	 * Get a phase by its name Return null if no phaseReport where found with
	 * this name. The includeImports parameters can be additionally used to
	 * extend the search to include imports by the current module.
	 * 
	 * @param name
	 *            represent the phase name
	 * @param includeImports
	 * 			  should the method search included modules
	 * @return a phase report
	 */
	public PhaseReport getPhaseReport(String name, boolean includeImports) {
		PhaseReport retVal = getPhaseReport(name);
		if(includeImports && retVal == null) {
			List<PhaseReport> phases = getAvailablePhases();
			for(PhaseReport phase : phases) {
				if (phase.getName().equals(name)) {
					retVal = phase;
				}
			}
		}
		return retVal;
	}
	
	/**
	 * Get a list of phases available in this module
	 * 
	 * @return a list of phases
	 */
	public List<PhaseReport> getPhaseReports() {
		return Collections.unmodifiableList(phaseReports);
	}

	/**
	 * Add a given phaseReport
	 * 
	 * @param phaseReport
	 *            a given phaseReport
	 */
	public void addPhaseReport(PhaseReport phaseReport) {
		if (phaseReport == null) {
			throw new IllegalArgumentException("phaseReport cannot be null");
		}
		phaseReports.add(phaseReport);
	}

	/**
	 * Get a list of all parameters defined in this module
	 * 
	 * @return a list of parameters
	 */
	public List<ParameterReport> getParameterReports() {
		return Collections.unmodifiableList(parameterReports);
	}

	/**
	 * Add a parameterReport
	 * 
	 * @param parameterReport
	 *            a parameterReport
	 */
	public void addParameterReport(ParameterReport parameterReport) {
		if (parameterReport == null) {
			throw new IllegalArgumentException("parameterReport cannot be null");
		}
		parameterReports.add(parameterReport);
	}

	/**
	 * Get a list of all imported modules
	 * 
	 * @return a list of imported modules
	 */
	public List<ImportedModuleReport> getImportedModuleReports() {
		return Collections.unmodifiableList(importedModuleReports);
	}

	/**
	 * Returns the imported module indicated by the passed parameter.
	 * 
	 * The method attempts to match either the complete module id
	 * or the module alias as specified in the build module.
	 * 
	 * @param module
	 * 		name of the module - either module id or the module alias.
	 * 
	 * @return
	 * 		instance of the exact module report queried for, if such a 
	 * 		module exists. it returns null otherwise.
	 */
	public ImportedModuleReport getImportedModuleReport(String module) {
		if(module.indexOf(';') > 0) {
			module = module.substring(0, module.indexOf(';'));
		}
		ImportedModuleReport retVal = null;
		for(ImportedModuleReport moduleRep : importedModuleReports) {
			if(moduleRep.moduleMrid.startsWith(module)) {
				retVal = moduleRep;
				break;
			} else if(moduleRep.as != null && moduleRep.as.equals(module)) {
				retVal = moduleRep;
				break;
			}
		}
		return retVal;
	}
	
	/**
	 * Add an imported module
	 * 
	 * @param importedModuleReport
	 *            a report that represent the importedModule
	 */
	public void addImportedModuleReport(
			ImportedModuleReport importedModuleReport) {
		if (importedModuleReport == null) {
			throw new IllegalArgumentException(
					"importedModuleReport cannot be null");
		}
		importedModuleReports.add(importedModuleReport);
	}

	/**
	 * Get the related ivyReport
	 * 
	 * @return the resolveReport
	 */
	public ResolveReport getResolveReport() {
		return resolveReport;
	}

	/**
	 * Set the related ivyReport
	 * 
	 * @param resolveReport
	 *            a resolveReport
	 */
	public void setResolveReport(ResolveReport resolveReport) {
		this.resolveReport = resolveReport;
	}

	/**
	 * Add a property
	 * 
	 * @param propertyName
	 *            the property name
	 * @param propertyDescriptor
	 *            a property descriptor that contains several info on the
	 *            propery (description / required or not etc...)
	 */
	public void addPropertyDescriptor(String propertyName,
			PropertyDescriptor propertyDescriptor) {
		if (propertyName == null || propertyDescriptor == null) {
			throw new IllegalArgumentException(
					"propertyName and propertyDescriptor cannot be null");
		}
		this.propertyReports.put(propertyName, propertyDescriptor);
	}

	/**
	 * Add all properties contained in an other propertyDescriptor
	 * 
	 * @param properties
	 *            a map of propertyDescriptor to inject
	 */
	public void addAllPropertyDescriptor(
			Map<String, PropertyDescriptor> properties) {
		if (properties == null) {
			throw new IllegalArgumentException("properties cannot be null");
		}
		this.propertyReports.putAll(properties);
	}

	/**
	 * Return a map of the properties
	 * 
	 * @return a map of the properties
	 */
	public Map<String, PropertyDescriptor> getPropertyDescriptors() {
		return Collections.unmodifiableMap(propertyReports);
	}

	/**
	 * Get a list of all the properties available in this module or in all imported modules
	 * @return a list of all the properties available in this module or in all imported modules
	 */
	public Map<String, PropertyDescriptor> getAvailableProperties() {
		Map<String, PropertyDescriptor> availableProperties = new HashMap<String, PropertyDescriptor>();

		if (propertyReports != null) {
			availableProperties.putAll(propertyReports);
		}
		if (importedModuleReports != null) {
			for (ImportedModuleReport importedModuleReport : importedModuleReports) {
				if (importedModuleReport.getEasyantReport() != null) {
					Map<String, PropertyDescriptor> subproperties = importedModuleReport
							.getEasyantReport().getAvailableProperties();
					for (Iterator iterator = subproperties.keySet().iterator(); iterator
							.hasNext();) {
						String propName = (String) iterator.next();
						PropertyDescriptor propertyToInsert = (PropertyDescriptor) subproperties
								.get(propName);
						if (availableProperties.containsKey(propName)) {
							PropertyDescriptor propertyDescriptor = (PropertyDescriptor) availableProperties
									.get(propName);

							if (propertyDescriptor.getDescription() == null
									&& propertyToInsert.getDescription() != null) {
								propertyDescriptor
										.setDescription(propertyToInsert
												.getDescription());
								propertyDescriptor.setRequired(propertyToInsert
										.isRequired());
								propertyDescriptor
										.setDefaultValue(propertyToInsert
												.getDefaultValue());
								availableProperties.put(propName,
										propertyDescriptor);
							}

						} else
							availableProperties.put(propName, propertyToInsert);

					}

				}
			}
		}
		return availableProperties;
	}

	/**
	 * This utilitary function allow us to retrieve a list of all targetReport
	 * available in this module and in all imported subModules
	 * 
	 * @return a list of all TargetReport available in the module or in its
	 *         submodules
	 */
	public List<TargetReport> getAvailableTargets() {
		List<TargetReport> targets = new ArrayList<TargetReport>();
		targets.addAll(targetReports);
		for (ImportedModuleReport importedModuleReport : importedModuleReports) {
			if (importedModuleReport.getEasyantReport() != null)
				for (TargetReport targetReport : importedModuleReport
						.getEasyantReport().getAvailableTargets()) {
					TargetReport target = new TargetReport();
					if (importedModuleReport.getAs() == null)
						targets.add(targetReport);
					else {
						target.setName(importedModuleReport.getAs()
								+ targetReport.getName());
						target.setDepends(targetReport.getDepends());
						target.setIfCase(targetReport.getIfCase());
						target.setUnlessCase(targetReport.getUnlessCase());
						target.setPhase(targetReport.getPhase());
						targets.add(target);
					}
				}

		}

		return targets;

	}
	
	/**
	 * Return a list of target that are not bound to any phases
	 */
	public List<TargetReport> getUnboundTargets() {
		List<TargetReport> targets = new ArrayList<TargetReport>();
		for (TargetReport targetReport : getAvailableTargets()) {
			if (targetReport.getPhase() == null) {
				targets.add(targetReport);
			}
		}
		return targets;
	}

	private List<PhaseReport> getAvailablePhasesWithoutTarget() {
		List<PhaseReport> phases = new ArrayList<PhaseReport>();

		phases.addAll(phaseReports);
		for (ImportedModuleReport importedModuleReport : importedModuleReports) {
			if (importedModuleReport.getEasyantReport() != null) {
				phases.addAll(importedModuleReport.getEasyantReport()
						.getAvailablePhasesWithoutTarget());
			}
		}
		return phases;
	}
	
	/**
	 * This utilitary function allow us to retrieve a list of all phaseReport
	 * available in this module and in all imported subModules
	 * 
	 * @return a list of all PhaseReport available in the module or in its
	 *         submodules
	 */
	public List<PhaseReport> getAvailablePhases() {
		List<PhaseReport> phases = getAvailablePhasesWithoutTarget();

		// associate target to the phase
		List<TargetReport> targets = getAvailableTargets();
		for (int i = 0; i < phases.size(); i++) {
			PhaseReport phase = phases.get(i);
			for (TargetReport target : targets) {
				if (target.getPhase() != null
						&& target.getPhase().equals(phase.getName())) {
					phase.addTargetReport(target);
					phases.set(i, phase);
				}
			}
		}

		return phases;
	}

}
