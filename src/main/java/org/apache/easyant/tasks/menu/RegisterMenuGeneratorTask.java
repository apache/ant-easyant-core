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
package org.apache.easyant.tasks.menu;

import org.apache.easyant.core.menu.MenuGenerator;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.ClasspathUtils;

import java.io.IOException;

/**
 * This {@link Task} is used to register a new MenuGenerator to a given context.
 */
public class RegisterMenuGeneratorTask extends AbstractMenuGeneratorTask {

    /**
     * Lists built-in menu generator types, which can be specified with a short name rather
     * than full classname.
     */
    public static enum BuiltinType {
        xooki("org.apache.easyant.menu.XookiMenuGenerator");

        private String generator;
        private BuiltinType(String generator) {
            this.generator = generator;
        }

        public String getGeneratorClassName() {
            return generator;
        }
    }

    private String className;
    private BuiltinType type;
	private Path classpath;

	@Override
	public void execute() throws BuildException {
		if (getClassName() == null) {
			throw new BuildException("either className or type argument is required !");
		}

		MenuGenerator menuGenerator = (MenuGenerator) ClasspathUtils.newInstance(getClassName(), getClassLoader(), MenuGenerator.class);
		getMenuGeneratorForContext(getContext()).addMenuGenerator(menuGenerator);
	}

	/**
	 * Get the classname to register
	 * @return a classname
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * Set the classname to register
	 * @param className a classname
	 */
	public void setClassName(String className) {
		this.className = className;
	}

    public BuiltinType getType() {
        return type;
    }

    /**
     * Set a builtin type to use, as an alternative to {@link #setClassName(String)}.
     */
    public void setType(BuiltinType type) {
        this.type = type;
        if (type != null) {
            setClassName(type.getGeneratorClassName());
        }
    }

    protected AntClassLoader getClassLoader() {
		// defining a new specialized classloader and setting it as the thread
		// context classloader
		AntClassLoader loader = null;
		if (classpath != null) {
			loader = new AntClassLoader(this.getClass().getClassLoader(),
					getProject(), classpath, false);
		} else {
			loader = new AntClassLoader(this.getClass().getClassLoader(),
					false);
		}
		loader.setThreadContextLoader();
		return loader;
	}
	
	/**
	 * Get the classpath used to locate the specified classname
	 * @return a classpath
	 */
	public Path getClasspath() {
		return classpath;
	}
	
	
		/**
		 * The the classpath used to locate the specified classname
		 * 
		 * @param classpath
		 */
		public void setClasspath(Path classpath) {
			createClasspath().append(classpath);
		}

		/**
		 * Classpath to use, by reference, when compiling the rulebase
		 * 
		 * @param a reference to an existing classpath
		 */
		public void setClasspathref(Reference r) {
			createClasspath().setRefid(r);
		}

		/**
		 * Adds a path to the classpath.
		 * 
		 * @return created classpath
		 */
		public Path createClasspath() {
			if (this.classpath == null) {
				this.classpath = new Path(getProject());
			}
			return this.classpath.createPath();
		}

}
