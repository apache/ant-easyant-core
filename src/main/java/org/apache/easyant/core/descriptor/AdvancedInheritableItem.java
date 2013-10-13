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
package org.apache.easyant.core.descriptor;

import org.apache.easyant.core.ivy.InheritableScope;
import org.apache.ivy.core.module.descriptor.InheritableItem;

/**
 * Interface for elements that can be inherited from a parent descriptor by a child descriptor This interface provides
 * some useful methods to have fine grain control on inheritable elements
 */
public interface AdvancedInheritableItem extends InheritableItem {

    /**
     * Get the current inherit scope
     * 
     * @return the inherit scope
     */
    InheritableScope getInheritScope();

    /**
     * Set inherit scope
     * 
     * @param inheritScope
     *            an inherit scope
     */
    void setInheritScope(InheritableScope inheritScope);

    /**
     * Check if element can be inherited
     * 
     * @return true if element can be inherited
     */
    boolean isInheritable();

    /**
     * Specify if an element can be inherited or not
     * 
     * @param isIneritable
     *            true if element can be inherited
     */
    void setInheritable(boolean isIneritable);

}
