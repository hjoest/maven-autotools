/*
 * Copyright (C) 2006-2009 Holger Joest <hjoest@users.sourceforge.net>
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


/**
 *
 */
public final class Environment {

    /** */
    private static Environment environment = new Environment();


    /**
     * No instantiation.
     */
    private Environment() {
    }


    /**
     * Returns the name of the operating system.
     *
     * @return the operating system name
     */
    public String getOperatingSystem() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.startsWith("windows")) {
            os = "windows";
        } else if (os.startsWith("netware")) {
            os = "netware";
        }
        os = os.replaceAll("[ /]", "");
        return os;
    }


    /**
     * Returns the system architecture.
     *
     * @return the architecture
     */
    public String getSystemArchitecture() {
        String arch = System.getProperty("os.arch").toLowerCase();
        if (arch.equals("x86")) {
           arch = "i386";
        }
        return arch;
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

