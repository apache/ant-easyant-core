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
package org.apache.easyant.core.menu;

import java.io.IOException;


/**
 * Common interface for menu generators.  The recursive parameter <code>M</code>
 * is used to support building nested menus.  For example, the typical class
 * declaration for a MenuGenerator will look like
 * <pre>
 * public class MyMenuGenerator implements MenuGenerator&lt;MyMenuGenerator&gt; { ... }
 * </pre>
 * @param <M> the concrete menu type implemented by this generator, to be
 *  passed when creating nested menus as in {@link #addSubMenu}.
 */
public interface MenuGenerator<M extends MenuGenerator<M>> {

    /**
     * Create a new menu with the given title and location on disk.
     * @param title the text that should be displayed for the menu title.
     * @param location the location on disk where the menu text should be stored.  may be ignored by some implementations for submenus.
     * @throws IOException if there is an error creating the menu
     */
    public void startMenu(String title, String location) throws IOException;

    /**
     * Add an entry to the menu.
     * @param title the title to appear on the menu entry
     * @param targetLink the target for the menu entry, e.g. a URL or file path
     * @throws IOException if there are errors adding a new entry to the menu.
     */
    public void addEntry(String title, String targetLink) throws IOException;

    /**
     * Add a submenu entry to the menu.  It is <strong>not</strong> the responsibility
     * of the parent menu to call {@link #startMenu(String, String)} on the provided child
     * menu.
     *
     * @param title the title in the parent menu.
     * @param subMenu the submenu
     * @throws IOException if there are errors adding a link to the submenu
     */
    public void addSubMenu(String title, M subMenu) throws IOException;

    /**
     * Finish writing the menu
     */
    public void endMenu() throws IOException;
}
