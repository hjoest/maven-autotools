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

import java.util.Map;
import java.util.HashMap;


/**
 *
 */
public final class Environment {

    /** */
    private static Environment environment = new Environment();

    /** */
    private String os;

    /** */
    private String arch;

    /** */
    private Map<String, String> platformMapping;


    /**
     * No public instantiation.
     */
    private Environment() {
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


    public boolean isLinux() {
        return "linux".equals(os);
    }


    public boolean isMacOSX() {
        return "macosx".equals(os);
    }


    public boolean isX86() {
        return "x86".equals(arch);
    }


    public boolean isX86_64() {
        return "x86_64".equals(arch);
    }


    public boolean isPPC() {
        return "ppc".equals(arch);
    }


    public boolean isPPC64() {
        return "ppc64".equals(arch);
    }


    public boolean isSparc() {
        return "sparc".equals(arch);
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
        Environment env = Environment.getEnvironment();
        return "native"
            + "-" + env.getSystemArchitecture()
            + "-" + env.getOperatingSystem();
    }


    /**
     * Returns the environment.
     *
     * @return the environment
     */
    public static Environment getEnvironment() {
        return environment;
    }

}
