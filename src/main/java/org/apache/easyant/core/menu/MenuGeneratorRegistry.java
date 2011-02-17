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
package org.apache.easyant.core.menu;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A registry holding a list of {@link MenuGenerator} related to a given context
 *
 */
public class MenuGeneratorRegistry  {
	private List<MenuGenerator> menuGenerators = new ArrayList<MenuGenerator>();
	private final String context;
	
	/**
	 * Default constructor 
	 * @param context the related context
	 */
	public MenuGeneratorRegistry(String context) {
		this.context = context;
	}
	
	
	/**
	 * Get a list of registred {@link MenuGenerator}s
	 * @return a list of registred {@link MenuGenerator}
	 */
	public List<MenuGenerator> getMenuGenerators() {
		return menuGenerators;
	}

	/**
	 * Add a given {@link MenuGenerator}
	 * @param menuGenerator a given {@link MenuGenerator}
	 * @return true the element was added
	 */
	public boolean addMenuGenerator(MenuGenerator menuGenerator) {
		return menuGenerators.add(menuGenerator);
	}
	
	/**
	 * Remove a given {@link MenuGenerator}
	 * @param menuGenerator a given {@link MenuGenerator}
	 * @return true if the element was removed
	 */
	public boolean removeMenuGenerator(MenuGenerator menuGenerator) {
		return menuGenerators.remove(menuGenerator);
	}

	/**
	 * Propagate the generation to all registered {@link MenuGenerator}
	 * {@inheritDoc}
	 */
	public void addEntry(String title, String targetLink) throws IOException {
		for (MenuGenerator menuGenerator : menuGenerators) {
			menuGenerator.addEntry(title, targetLink);
		}
	}

    /**
     * Call {@link org.apache.easyant.core.menu.MenuGenerator#endMenu()} on all registered
     * menu generators.
     */
    public void endMenu() throws IOException {
        for (MenuGenerator menuGenerator : menuGenerators) {
            menuGenerator.endMenu();
        }
    }

	/**
	 * Get the context related to this registry
	 * @return a string representing the context
	 */
	public String getContext() {
		return context;
	}

}
