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
package org.apache.easyant.tasks.menu;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.easyant.core.menu.MenuGeneratorRegistry;
import org.apache.tools.ant.BuildException;

/**
 * This task provides capabilities to add menu entries.
 * This class is also responsible of finding the right {@link MenuGeneratorRegistry}
 * and to propagate the generation to all registered implementations. 
 *
 */
public class AddMenuEntryTask extends AbstractMenuGeneratorTask {

	private String title;
	private String targetLink;

	private List<MenuEntry> menuEntries = new ArrayList<MenuEntry>();

	@Override
	public void execute() throws BuildException {
		if (getTitle() == null && getTargetLink() == null && menuEntries.size() == 0) {
			throw new BuildException("You must specfify a title and a target link as argument or as a nested element using a menuentry datatype !");
		}
		
        try {
            //handle argument
            if (getTitle() != null && getTargetLink() != null) {
                getMenuGeneratorForContext(getContext()).addEntry(getTitle(),
                        getTargetLink());
            }
            //handle nested menuentry
            for (MenuEntry menuItem : getMenuEntries()) {
                getMenuGeneratorForContext(getContext()).addEntry(
                        menuItem.getTitle(), menuItem.getTargetLink());
            }
        } catch (IOException ioe) {
            throw new BuildException("Error writing menu entry: " + ioe.getMessage(), ioe);
        }
	}

	/**
	 * Get the title of the {@link MenuEntry}
	 * @return a title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Set the title of the {@link MenuEntry}
	 * @param title a title
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Get the target link of the {@link MenuEntry}
	 * @return the target url
	 */
	public String getTargetLink() {
		return targetLink;
	}

	/**
	 * Set the target link of the {@link MenuEntry}
	 * @param targetLink the target url
	 */
	public void setTargetLink(String targetLink) {
		this.targetLink = targetLink;
	}

	/**
	 * Get a list of nested {@link MenuEntry}
	 * @return a list of {@link MenuEntry}
	 */
	public List<MenuEntry> getMenuEntries() {
		return menuEntries;
	}

	/**
	 * Set a list of nested {@link MenuEntry}
	 * @param menuEntries a list of {@link MenuEntry}
	 */
	public void setMenuEntries(List<MenuEntry> menuEntries) {
		this.menuEntries = menuEntries;
	}

	/**
	 * Add a given {@link MenuEntry} 
	 * @param menuEntry a given {@link MenuEntry}
	 */
	public void add(MenuEntry menuEntry) {
		getMenuEntries().add(menuEntry);
	}

}
