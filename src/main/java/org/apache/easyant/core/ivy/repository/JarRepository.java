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
package org.apache.easyant.core.ivy.repository;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ivy.plugins.repository.url.URLRepository;

/**
 * JarRepository extends the default URLRepository provided by ivy but introduce a
 * way to list files/directories inside a jar.
 */
public class JarRepository extends URLRepository {

    private static final Pattern URL_PATTERN = Pattern.compile("([^!]+)!/(.*)");
    private static final Directory emptyNode = new Directory();

    /** lazily cache jar file directories to speed up repository searches and reporting */
    private HashMap<String, Directory> jarCache = new HashMap<String, Directory>();

    public List list(String parent) throws IOException {
    	URL url = new URL(parent);    	
    	if ("jar".equals(url.getProtocol())) {
            //extract path in parent
            Matcher matcher = URL_PATTERN.matcher(parent);
            matcher.find();
            
            String baseUrl = matcher.group(1);
            String path = matcher.group(2);
            
            //find the parent path in the directory.
            Directory directory = getDirectoryFromJar(baseUrl, url).findEntry(path);
            return new ArrayList<String>(directory.getChildren());   
        
        // Hook required to embed Easyant in OSGi container
        // Equinox used a custom "bundleresource" protocol 
        // (Other OSGi implementations like Felix use file:// ou jar:// standard protocols)        
        }else if ("bundleresource".equals(url.getProtocol())) {        	          
             String baseUrl = url.getHost();
             String path = url.getPath();
             //find the parent path in the directory.
             Directory directory = getDirectoryFromEquinoxBundle(baseUrl, url).findEntry(path);
             return new ArrayList<String>(directory.getChildren());
		}    	       
        return super.list(parent);
    }

    @Override
    public void put(File source, String destination, boolean overwrite) throws IOException {
        super.put(source, destination, overwrite);
        //purge directory cache
        flush(destination);
    }

    /**
     * get a sorted list of entries in the given jar file
     * @param baseName the base URL of the jar file
     * @param jarUrl complete URL to the entry being searched
     */
    private Directory getDirectoryFromJar(String baseName, URL jarUrl) throws IOException {
        synchronized (jarCache) {
            Directory cached = jarCache.get(baseName);
            if (cached == null) {                                         
                JarURLConnection conn = (JarURLConnection) jarUrl.openConnection();
                JarFile file = conn.getJarFile();
                if (file == null) {
                    return emptyNode;
                } else {
                    cached = new Directory();
                    for (Enumeration<JarEntry> entries = file.entries(); entries.hasMoreElements(); ) {
                        JarEntry entry = entries.nextElement();
                        cached.addEntry(entry.getName());
                    }
                    jarCache.put(baseName, cached);
                }                
            }
            return cached;
        }
    }
    
    private static Method getLocalURL;
    /**
     * get a sorted list of entries in the given OSGi bundle
     * @param baseName the base URL of the OSGi bundle
     * @param jarUrl complete URL to the entry being searched
     */
    private Directory getDirectoryFromEquinoxBundle(String baseName, URL jarUrl) throws IOException {
        synchronized (jarCache) {
            Directory cached = jarCache.get(baseName);
            if (cached == null) {
            	try {
	                URLConnection conn = jarUrl.openConnection();
	                if(getLocalURL==null && "org.eclipse.osgi.framework.internal.core.BundleURLConnection".equals(conn.getClass().getName())){
	                	JarRepository.getLocalURL = conn.getClass().getMethod("getLocalURL", null);
						getLocalURL.setAccessible(true);
	                }
	                if(getLocalURL!=null && conn!=null){
	                	URL localJarUrl = (URL)getLocalURL.invoke(conn, null);	                	
	                	Directory directory = getDirectoryFromJar(baseName, localJarUrl);	                	
	                	return directory;
	                }
            	} catch (Throwable throwable) {
					throw new IOException("Cannot get directory from Equinox OSGi bundle", throwable);
				}              
            }
            return cached;
        }
    }

    /** flush the cached directory for the given jar file */
    private void flush(String jarUrl) throws IOException {
        jarUrl = getBaseUrl(jarUrl);
        synchronized (jarCache) {
            jarCache.remove(jarUrl);
        }
    }

    /** retrieve the part of <code>jarUrl</code> that refers to the jar file, stripping any specific entry from the end */
    private String getBaseUrl(String jarUrl) {
        int separator = jarUrl.indexOf('!');
        if (separator > 0) {
            jarUrl = jarUrl.substring(0, separator);
        }
        return jarUrl;
    }

    /** a logical directory in a Jar file */
    private static class Directory {

        private HashMap<String, Directory> children = new HashMap<String, Directory>();

        public Set<String> getChildren() {
            return children.keySet();
        }

        /**
         * Create subdirectory entries for the path <code>name</code> rooted
         * at this directory.
         */
        public void addEntry(String name) {
            String[] path = name.split("/");
            Directory entry = this;
            for (int i = 0; i < path.length; ++i) {
                String childName = path[i];
                if (childName.length() > 0) {
                    Directory child = entry.children.get(childName);
                    if (child == null) {
                        entry.children.put(childName, child = new Directory());
                    }
                    entry = child;
                }
            }
        }

        /**
         * Find the subdirectory named <code>path</code> rooted
         * at this directory.
         */
        public Directory findEntry(String name) {
            String[] path = name.split("/");
            Directory entry = this;
            for (int i = 0; entry != null && i < path.length; ++i) {
                String childName = path[i];
                if (childName.length() > 0) {
                    entry = entry.children.get(childName);
                }
            }
            return entry == null ? emptyNode : entry;
        }

    }
}
