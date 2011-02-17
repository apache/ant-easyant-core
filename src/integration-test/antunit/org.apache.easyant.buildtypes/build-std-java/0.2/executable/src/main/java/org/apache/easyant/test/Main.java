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
package org.apache.easyant.test;

public class Main {
	
	/**
	 * Prints out the fully qualified name of jUnit's Assert utility classes.
	 * This tests the presence of Assert on our classpath, which should be defined
	 * in our manifest Class-Path attribute.
	 */
	public static void main(String[] argv) {
		System.out.print("Successfully loaded: " + org.junit.Assert.class.getName());
	}
	
}