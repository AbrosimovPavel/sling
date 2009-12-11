/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.jcr.jcrinstall.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.sling.runmode.RunMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Defines which folders are watched by JcrInstaller, based on
 * 	their names. To be accepted, a folder must have a name that
 *  matches the expression, followed by optional suffixes based
 *  on the current RunMode.
 *  
 *  See {@link FolderNameFilterTest} for details.    
 */
class FolderNameFilter {
    private final Pattern pattern;
    private final String regexp;
    private final RunMode runMode;
    private final String [] rootPaths;
    private Map<String, Integer> rootPriorities = new HashMap<String, Integer>();
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    /** getPriority computes priorities as follows: each root gets its own base priority,
     * 	and paths that match one or several run levels get an additional RUNMODE_PRIORITY_OFFSET 
     *  per matched run level
     */
    public static final int RUNMODE_PRIORITY_BOOST = 1;
    public static final int DEFAULT_ROOT_PRIORITY = 99;
    
    FolderNameFilter(String [] rootsConfig, String regexp, RunMode runMode) {
        this.regexp = regexp;
        this.pattern = Pattern.compile(regexp);
        this.runMode = runMode;
        
        // Each entry in rootsConfig is like /libs:100, where 100
        // is the priority.
        // Break that up into paths and priorities
        rootPaths = new String[rootsConfig.length];
        for(int i = 0; i < rootsConfig.length; i++) {
        	final String [] parts = rootsConfig[i].split(":");
        	rootPaths[i] = cleanupRootPath(parts[0]);
        	Integer priority = new Integer(DEFAULT_ROOT_PRIORITY);
        	if(parts.length > 1) {
        		try {
        			priority = Integer.parseInt(parts[1].trim());
        		} catch(NumberFormatException nfe) {
        			log.warn("Invalid priority in path definition '{}'", rootsConfig[i]);
        		}
        	}
        	rootPriorities.put(rootPaths[i], priority);
        	log.debug("Root path {} has priority {}", rootPaths[i], priority);
        }
    }
    
    String [] getRootPaths() {
    	return rootPaths;
    }
    
    static String cleanupRootPath(final String str) {
    	String result = str.trim();
    	if(!result.startsWith("/")) {
    		result = "/" + result;
    	}
    	if(result.endsWith("/")) {
    		result = result.substring(0, result.length() - 1);
    	}
    	return result;
    }
    
    /** If a folder at given path can contain installable resources
     * 	(according to our regexp and current RunMode), return the
     * 	priority to use for InstallableResource found in that folder.
     * 
     * 	@return -1 if path is not an installable folder, else resource priority  
     */
    int getPriority(final String path) {
    	int result = 0;
    	
        // If path contains dots after the last /, remove suffixes 
    	// starting with dots until path matches regexp, and accept 
    	// if all suffixes
        // are included in our list of runmodes
        final char DOT = '.';
        
        String prefix = path;
        final int lastSlash = prefix.lastIndexOf('/');
        if(lastSlash > 0) {
        	prefix = prefix.substring(lastSlash);
        }
        if(prefix.indexOf(DOT) > 0) {
            int pos = 0;
            final List<String> modes = new LinkedList<String>();
            while( (pos = prefix.lastIndexOf(DOT)) >= 0) {
                modes.add(prefix.substring(pos + 1));
                prefix = prefix.substring(0, pos);
                if(pattern.matcher(prefix).matches()) {
                    result = getRootPriority(path);
                    break;
                }
            }
            
            // If path prefix matches, check that all our runmodes match
            if(result > 0) {
                for(String m : modes) {
                    final String [] toTest = { m };
                    if(runMode.isActive(toTest)) {
                    	result += RUNMODE_PRIORITY_BOOST;
                    } else {
                        result = 0;
                        break;
                    }
                }
            }
            
            if(log.isDebugEnabled()) {
                log.debug("accept(" + path + ")=" + result + " (prefix=" + prefix + ", modes=" + modes + ")");
            }
            
        } else if(pattern.matcher(path).matches()) {
        	result = getRootPriority(path);
        }
        return result;
    }
    
    public String toString() {
        return getClass().getSimpleName() + " (" + regexp + "), RunMode=" + runMode;
    }
    
    int getRootPriority(String path) {
    	for(Map.Entry<String, Integer> e : rootPriorities.entrySet()) {
    		if(path.startsWith(e.getKey())) {
    			return e.getValue().intValue();
    		}
    	}
    	return 0;
    }
}