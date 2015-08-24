/*
 * Copyright (C) 2006-2013 Holger Joest <holger@joest.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.sf.maven.plugin.autotools;

import java.io.File;
import java.util.Map;
import java.util.HashMap;

import org.codehaus.plexus.util.StringUtils;


/**
 *
 */
public final class Environment {

   private static Environment environment = new Environment();
   
   public static Environment getBuildEnvironment() {
      return environment;
   }
   
    /** Operating System */
    private String os;

    /** Architecture */
    private String arch;
    
    /** --host parameter */
    private String host;

    /** 
     * &lt;platformMapping&gt;
     *     &lt;windows&gt;evil&lt;/windows&gt;
     *     &lt;x86&gt;i386&lt;/x86&gt;
     *     &lt;macosx.ppc&gt;mac.power&lt;/macosx.ppc&gt;
     * &lt;/platformMapping&gt;    
     */
    private Map<String, String> platformMapping;


    /**
     * Default to current system.
     */
    public Environment() {
        os = System.getProperty("os.name").toLowerCase();
        if (os.startsWith("windows")) {
            os = "windows";
        }
        os = os.replaceAll("[ /]", "");
        arch = System.getProperty("os.arch").toLowerCase();
        if (arch.equals("i386")) {
           arch = "x86";
        } else if (arch.equals("amd64")) {
           arch = "x86_64";
        } else if (arch.equals("powerpc")) {
           arch = "ppc";
        }
        platformMapping = new HashMap<String, String>();
    }


    public boolean isWindows() {
        return "windows".equals(os);
    }

    public boolean isX86() {
        return "x86".equals(arch);
    }

    public boolean isX86_64() {
        return "x86_64".equals(arch);
    }

    /**
     * Returns the name of the operating system.
     *
     * @return the operating system name
     */
    public String getOperatingSystem() {
        String mapped = platformMapping.get(os + "." + arch);
        if (mapped != null && mapped.indexOf('.') > -1) {
            mapped = mapped.split(".")[0];
        } else {
            mapped = platformMapping.get(os);
        }
        if (mapped != null) {
            return mapped;
        }
        return os;
    }

    public void setOperatingSystem(String os) {
       this.os = os;
    }

    /**
     * Returns the system architecture.
     *
     * @return the architecture
     */
    public String getSystemArchitecture() {
        String mapped = platformMapping.get(os + "." + arch);
        if (mapped != null && mapped.indexOf('.') > -1) {
            mapped = mapped.split(".")[1];
        } else {
            mapped = platformMapping.get(arch);
        }
        if (mapped != null) {
            return mapped;
        }
        return arch;
    }
    
    public void setSystemArchitecture(String arch) {
       this.arch = arch;
    }

    public String getHost() {
       return host;
    }
    
    public void setHost(String host) {
       this.host = host;
    }

    /**
     * Provide alternative names for target platforms.
     */
    public void applyPlatformMapping(Map<String, String> platformMapping) {
        Map<String, String> clonedMapping = new HashMap<String, String>();
        if (platformMapping != null) {
            clonedMapping.putAll(platformMapping);
        }
        this.platformMapping = clonedMapping;
    }

    /**
     * @return the attached artifact classifier
     */
    public String getClassifier() {
        return String.format("%s-%s", getOperatingSystem(), getSystemArchitecture());
    }
    
    /**
     * Whether we are cross compiling.
     * @return    true if host parameter has been specified, false otherwise
     */
    public boolean isCrossCompiling() {
       return !StringUtils.isEmpty(host);
    }

   /**
    * Appends system architecture and operating system name to
    * a given path.
    *
    * @param directory a directory
    * @return the directory with architecture and os appended
    */
   public File makeOsArchDirectory(File directory) {
      File osDirectory = new File(directory, getOperatingSystem());
      return new File(osDirectory, getSystemArchitecture());
   }
}
