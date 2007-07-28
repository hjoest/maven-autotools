/*
 * Copyright (C) 2006-2007 Holger Joest <hjoest@users.sourceforge.net>
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
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;


/**
 * @goal make
 * @phase compile
 * @description run 'configure', 'make', and 'make install'
 */
public final class MakeMojo
extends AbstractMojo {

    /**
     * The configure script.
     *
     * @parameter expression="${basedir}/src/main/make/configure"
     */
    private File configure;

    /**
     * The dependencies directory.
     *
     * @parameter expression="${project.build.directory}/make-dependencies"
     */
    private File dependenciesDirectory;

    /**
     * The working directory.
     *
     * @parameter expression="${project.build.directory}/make-work"
     */
    private File workingDirectory;

    /**
     * The install directory.
     *
     * @parameter expression="${project.build.directory}/make-install"
     */
    private File installDirectory;


    /**
     * {@inheritDoc}
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void execute()
    throws MojoExecutionException {
        try {
            workingDirectory.mkdirs();
            installDirectory.mkdirs();
            File makefile =
                new File(workingDirectory, "Makefile");
            File makefileIn =
                new File(configure.getParentFile(), "Makefile.in");
            if (makefileIn.lastModified() > makefile.lastModified()) {
                getLog().info(
                        "Running the \"configure\" script to"
                        + " adapt the native package to this system.");
                String configurePath = makePath(configure);
                File binDirectory = new File(installDirectory, "bin");
                binDirectory = makeOsArchDirectory(binDirectory);
                File libDirectory = new File(installDirectory, "lib");
                libDirectory = makeOsArchDirectory(libDirectory);
                File includeDirectory = new File(installDirectory, "include");
                String[] configureCommand = {
                        "sh", "-c",
                        configurePath
                        + " --enable-maintainer-mode"
                        + " --disable-dependency-tracking"
                        + " --bindir=\"" + makePath(binDirectory) + "\""
                        + " --libdir=\"" + makePath(libDirectory) + "\""
                        + " --includedir=\"" + makePath(includeDirectory) + "\""
                };
                ProcessExecutor pe = new ProcessExecutor();
                pe.execProcess(
                        configureCommand,
                        makeConfigureEnvironment(),
                        workingDirectory);
            }
        } catch (MojoExecutionException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new MojoExecutionException("Failed to run \"configure\"", ex);
        }
        ProcessExecutor pe = new ProcessExecutor();
        try {
            String[] makeCommand = {
                    "make"
            };
            pe.execProcess(makeCommand, null, workingDirectory);
            String[] makeInstallCommand = {
                    "make",
                    "install"
            };
            pe.execProcess(makeInstallCommand, null, workingDirectory);
        } catch (MojoExecutionException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new MojoExecutionException("Failed to run \"make\"", ex);
        }
    }


    /**
     * Returns the environment variables used to call the configure
     * script.
     *
     * @return the environment
     */
    private Map<String, String> makeConfigureEnvironment() {
        File includes = new File(dependenciesDirectory, "include");
        File libraries = new File(dependenciesDirectory, "lib");
        libraries = makeOsArchDirectory(libraries);
        Map<String, String> env = new HashMap<String, String>();
        env.putAll(System.getenv());
        env.put("CFLAGS", "-I" + makePath(includes));
        env.put("LDFLAGS", "-L" + makePath(libraries));
        return env;
    }


    /**
     * Appends system architecture and operating system name to
     * a given path.
     *
     * @param directory a directory
     * @return the directory with architecture and os appended
     */
    private File makeOsArchDirectory(File directory) {
        String arch = Environment.getEnvironment().getSystemArchitecture();
        String os = Environment.getEnvironment().getOperatingSystem();
        File archDirectory = new File(directory, arch);
        File osDirectory = new File(archDirectory, os);
        return osDirectory;
    }


    /**
     * Returns the path for a given file, eventually fixed for
     * the cygwin platform.
     *
     * @param file the file
     * @return the fixed path
     */
    private String makePath(File file) {
        return makePath(file.getAbsolutePath());
    }


    /**
     * Returns the fixed path for the cygwin platform.
     *
     * @param path a path
     * @return the fixed path
     */
    private String makePath(String path) {
        String os = Environment.getEnvironment().getOperatingSystem();
        if ("windows".equals(os)
                && path.length() > 2
                && path.charAt(1) == ':') {
            path = "/cygdrive/"
                + path.charAt(0)
                + path.substring(2);
            path = path.replace('\\', '/');
        }
        return path;
    }

}

