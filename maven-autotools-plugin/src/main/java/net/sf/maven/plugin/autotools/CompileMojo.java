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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.InterpolationFilterReader;
import org.codehaus.plexus.util.IOUtil;


/**
 * @goal compile
 * @phase compile
 * @description run 'configure', 'make', and 'make install'
 */
public final class CompileMojo
extends AbstractMojo {

    /**
     * The dependencies directory.
     *
     * @parameter expression="${project.build.directory}/autotools/dependencies"
     */
    private File dependenciesDirectory;

    /**
     * The configure directory.
     *
     * @parameter expression="${project.build.directory}/autotools/configure"
     */
    private File configureDirectory;

    /**
     * The working directory.
     *
     * @parameter expression="${project.build.directory}/autotools/work"
     */
    private File workingDirectory;

    /**
     * The install directory.
     *
     * @parameter expression="${project.build.directory}/autotools/install"
     */
    private File installDirectory;

    /**
     * The autotools scripts directory.
     *
     * @parameter expression="${basedir}/src/main/autotools"
     */
    private File autotoolsMainDirectory;

    /**
     * The native source files directory.
     *
     * @parameter expression="${basedir}/src/main/native"
     */
    private File nativeMainDirectory;

    /**
     * Used to run child processes.
     */
    private ProcessExecutor exec = new DefaultProcessExecutor();


    /**
     * {@inheritDoc}
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void execute()
    throws MojoExecutionException {
        prepareBuild();
        configure();
        make();
    }


    private void prepareBuild()
    throws MojoExecutionException {
        try {
            configureDirectory.mkdirs();
            workingDirectory.mkdirs();
            installDirectory.mkdirs();
            makeSymlinks(autotoolsMainDirectory, configureDirectory);
            makeSymlinks(nativeMainDirectory, configureDirectory);
            if (!FileUtils.fileExists(configureDirectory, "Makefile.am")) {
                // If not produced by automake, it is highly probable that the
                // make script can not deal with source files in a neighboring
                // directory.  To play it safe, we link the the source files
                // into the working directory as well.
                makeSymlinks(nativeMainDirectory, workingDirectory);
            }
        } catch (IOException ex) {
            throw new MojoExecutionException(
                    "Failed to prepare build directories", ex);
        }
    }


    private void configure()
    throws MojoExecutionException {
        if (FileUtils.fileExists(configureDirectory, "Makefile")
            && !FileUtils.isOlderThanAnyOf(configureDirectory, "Makefile",
                                           "Makefile.in", "Makefile.am")) {
            // No need to run configure since there is an up-to-date makefile.
            return;
        }
        String configurePath = "configure";
        try {
            autoconf();
            File configureScript =
                new File(configureDirectory, configurePath);
            if (!configureScript.canExecute()) {
                configureScript.setExecutable(true);
            }
            configurePath =
                FileUtils.calculateRelativePath(workingDirectory,
                                                configureScript);
            File binDirectory = new File(installDirectory, "bin");
            binDirectory = makeOsArchDirectory(binDirectory);
            File libDirectory = new File(installDirectory, "lib");
            libDirectory = makeOsArchDirectory(libDirectory);
            File includeDirectory = new File(installDirectory, "include");
            String configure =
                configurePath
                + " --bindir=\""
                + FileUtils.fixAbsolutePathForUnixShell(binDirectory) + "\""
                + " --libdir=\""
                + FileUtils.fixAbsolutePathForUnixShell(libDirectory) + "\""
                + " --includedir=\""
                + FileUtils.fixAbsolutePathForUnixShell(includeDirectory)
                + "\"";
            String[] configureCommand = {
                    "sh", "-c", configure
            };
            if (getLog().isInfoEnabled()) {
                getLog().info("cd '" + workingDirectory + "'");
                getLog().info(Arrays.toString(configureCommand));
            }
            exec.execProcess(
                    configureCommand,
                    makeConfigureEnvironment(),
                    workingDirectory);
        } catch (Exception ex) {
            throw new MojoExecutionException("Failed to run '"
                                             + configurePath + "'"
                                             + " in directory '"
                                             + workingDirectory + "'",
                                             ex);
        }
    }


    private void make()
    throws MojoExecutionException {
        try {
            String[] makeCommand = {
                    "sh", "-c", "make"
            };
            exec.execProcess(makeCommand, null, workingDirectory);
            String[] makeInstallCommand = {
                    "sh", "-c", "make install"
            };
            exec.execProcess(makeInstallCommand, null, workingDirectory);
        } catch (Exception ex) {
            throw new MojoExecutionException("Failed to run \"make\"", ex);
        }
    }


    private void autoconf()
    throws Exception {
        List<String> commands = new ArrayList<String>();
        File autoscanPost = null;
        try {
            if (!FileUtils.fileExists(configureDirectory, "configure.ac")
                  && !FileUtils.fileExists(configureDirectory, "configure.in")
                  && !FileUtils.fileExists(configureDirectory, "Makefile.in")) {
                commands.add("autoscan");
                autoscanPost = extractScript("autoscan-post");
                commands.add("./" + autoscanPost.getName());
            }
            if (!FileUtils.fileExists(configureDirectory, "configure.in")
                  && !FileUtils.fileExists(configureDirectory, "Makefile.in")) {
                commands.add("aclocal");
                commands.add("autoheader");
                commands.add("libtoolize -c -f");
                commands.add("automake -c -f -a");
                createEmptyIfDoesNotExist(configureDirectory, "NEWS");
                createEmptyIfDoesNotExist(configureDirectory, "README");
                createEmptyIfDoesNotExist(configureDirectory, "AUTHORS");
                createEmptyIfDoesNotExist(configureDirectory, "ChangeLog");
            }
            if (!FileUtils.fileExists(configureDirectory, "configure")) {
                commands.add("autoconf");
            }
            if (getLog().isInfoEnabled()) {
                getLog().info("cd '" + configureDirectory + "'");
            }
            for (String command : commands) {
                String[] shellCommand = {
                        "sh", "-c", command
                };
                if (getLog().isInfoEnabled()) {
                    getLog().info(Arrays.toString(shellCommand));
                }
                exec.execProcess(
                        shellCommand,
                        null,
                        configureDirectory);
            }
        } finally {
            if (autoscanPost != null) {
                autoscanPost.delete();
            }
        }
    }


    private File extractScript(String scriptName)
    throws IOException {
        int rnd = Math.abs(new Random().nextInt());
        String temporaryScriptName = "." + scriptName + "-" + rnd;
        File script = new File(configureDirectory, temporaryScriptName);
        String encoding = "UTF-8";
        Map<String, String> variables = makeVariables();
        Reader reader =
                  new InterpolationFilterReader(
                          new InputStreamReader(
                                  getClass().getResourceAsStream(scriptName),
                                  "UTF-8"),
                          variables);
        try {
            Writer writer =
                          new OutputStreamWriter(
                                  new FileOutputStream(script),
                                  "UTF-8");
            try {
                IOUtil.copy(reader, writer);
            } finally {
                IOUtil.close(writer);
            }
        } finally {
            IOUtil.close(reader);
        }
        script.setExecutable(true);
        return script;
    }


    private Map<String, String> makeVariables() {
        Map<String, String> variables = new HashMap<String, String>();
        File[] sources = nativeMainDirectory.listFiles();
        String programName = null;
        for (File source : sources) {
            String sourceName = source.getName();
            variables.put("autoscan.sources", sourceName);
            int p = sourceName.lastIndexOf('.');
            if (programName == null && p > -1) {
                programName = sourceName.substring(0, p);
            }
        }
        if (programName == null) {
            programName = "a.out";
        }
        variables.put("autoscan.program", programName);
        return variables;
    }


    /**
     * Returns the environment variables used to call the configure
     * script.
     *
     * @return the environment
     * @throws IOException if an I/O error occurs
     */
    private Map<String, String> makeConfigureEnvironment()
    throws IOException {
        File includes = new File(dependenciesDirectory, "include");
        File libraries = new File(dependenciesDirectory, "lib");
        libraries = makeOsArchDirectory(libraries);
        Map<String, String> env = new HashMap<String, String>();
        env.putAll(System.getenv());
        env.put("CFLAGS",
                "-I" + FileUtils.fixAbsolutePathForUnixShell(includes));
        env.put("LDFLAGS",
                "-L" + FileUtils.fixAbsolutePathForUnixShell(libraries));
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
        Environment environment = Environment.getEnvironment();
        String arch = environment.getSystemArchitecture();
        String os = environment.getOperatingSystem();
        File archDirectory = new File(directory, arch);
        File osDirectory = new File(archDirectory, os);
        return osDirectory;
    }


    private void makeSymlinks(File sourceDirectory, File destinationDirectory)
    throws IOException {
        if (sourceDirectory == null) {
            return;
        }
        File[] files = sourceDirectory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                File childDestinationDirectory =
                      new File(destinationDirectory, file.getName());
                childDestinationDirectory.mkdir();
                makeSymlinks(file, childDestinationDirectory);
            } else if (file.isFile()) {
                File link = new File(destinationDirectory, file.getName());
                SymlinkUtils.createSymlink(link, file, true);
            }
        }
    }


    private void createEmptyIfDoesNotExist(File directory, String name)
    throws IOException {
        File file = new File(directory, name);
        if (!file.exists()) {
            new FileOutputStream(file).close();
        }
    }

}

