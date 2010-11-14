/*
 * Copyright (C) 2006-2010 Holger Joest <hjoest@users.sourceforge.net>
 * Copyright (C) 2010 Hannes Schmidt <hannes.schmidt@gmail.com>
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.InterpolationFilterReader;
import org.codehaus.plexus.util.StringUtils;


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
     * Set 'true' if you want verbose output.
     *
     * @parameter
     */
    private boolean verbose;

    /**
     * Artifacts containing additional autoconf macros.
     *
     * @parameter
     */
    private MacroDependency[] macroDependencies;

    /**
     * @component
     */
    private ArtifactFactory artifactFactory;

    /**
     * @component
     */
    private ArtifactResolver resolver;

    /**
     * @parameter default-value="${localRepository}"
     */
    private ArtifactRepository localRepository;

    /**
     * @parameter default-value="${project.remoteArtifactRepositories}"
     */
    private List<ArtifactRepository> remoteRepositories;

    /**
     * @parameter default-value="${project}"
     */
    private org.apache.maven.project.MavenProject mavenProject;

    
    // TODO: this should really be determined automatically from AC_CONFIG_MACRO_DIR in configure.ac
	/**
	 * The name of the sub-directory of {@link #autotoolsMainDirectory} in which
	 * m4 macros reside. Note that ATM this setting needs to be consistent with
	 * AC_CONFIG_MACRO_DIR in configure.ac and -I flags in ACLOCAL_AMFLAGS in
	 * Makefile.am.
	 * 
	 * @parameter default-value="m4" 
	 */
	private String macroDirectoryName;

	/**
	 * Whether to use 'autoreconf'. If false, a more brittle, hard-coded
	 * sequence of aclocal, autoheader, libtoolize, automake and autoconf will
	 * be used.
	 * 
	 * @parameter default-value="true"
	 */
	private boolean autoreconf;
	
	
	/**
	 * Additional parameters to pass to the ./configure script. Can't use --bindir, --libdir or --includedir.
	 * 
	 * @parameter
	 */
	private String configureArgs;

	
	/**
	 * Additional environment variables to set before running configure.
	 * 
	 * @parameter
	 */
	private Map<String,String> configureEnv;
	
    
    /**
     * Used to run child processes.
     */
    private ProcessExecutor exec = new DefaultProcessExecutor();

    /**
     * Used to avoid running this mojo multiple times with exactly the
     * same configuration.
     */
    private RepeatedExecutions repeated = new RepeatedExecutions();

    /**
     * {@inheritDoc}
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void execute()
    throws MojoExecutionException {
        if (repeated.alreadyRun(getClass().getName(),
                                nativeMainDirectory,
                                autotoolsMainDirectory,
                                installDirectory,
                                workingDirectory,
                                configureDirectory,
                                dependenciesDirectory)) {
            getLog().info("Skipping repeated execution");
            return;
        }
        initLogging();
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
            makeM4Directory();
            writeM4Macros();
            if (!FileUtils.fileExists(configureDirectory, "Makefile.am")) {
                // If not produced by automake, it is highly probable that the
                // make script can not deal with source files in a neighboring
                // directory.  To play it safe, we link the source files into
                // the working directory as well.
                makeSymlinks(nativeMainDirectory, workingDirectory);
            }
        } catch (IOException ex) {
            throw new MojoExecutionException(
                    "Failed to prepare build directories", ex);
        } catch (ArtifactNotFoundException ex) {
            throw new MojoExecutionException("Could not find artifact", ex);
        } catch (ArtifactResolutionException ex) {
            throw new MojoExecutionException("Failed to resolve artifact", ex);
        }
    }


	private void makeM4Directory()
	{
		if( ! FileUtils.fileExists( configureDirectory, macroDirectoryName ) ) {
			new File( configureDirectory, macroDirectoryName ).mkdir();
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
                + (verbose ? "" : " --silent")
                + " --bindir=\""
                + FileUtils.fixAbsolutePathForUnixShell(binDirectory) + "\""
                + " --libdir=\""
                + FileUtils.fixAbsolutePathForUnixShell(libDirectory) + "\""
                + " --includedir=\""
                + FileUtils.fixAbsolutePathForUnixShell(includeDirectory) + "\""
                + (StringUtils.isEmpty(configureArgs) ? "" : " " + configureArgs);
            String[] configureCommand = {
                    "sh", "-c", configure
            };
            if (verbose && getLog().isInfoEnabled()) {
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
            exec.execProcess(makeCommand,
                             null,
                             workingDirectory);
            String[] makeInstallCommand = {
                    "sh", "-c", "make install"
            };
            exec.execProcess(makeInstallCommand,
                             null,
                             workingDirectory);
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
            	if( ! autoreconf ) {
	                commands.add("aclocal");
	                commands.add("autoheader");
	                commands.add( command( "libtoolize" ) + " -c -f" + (verbose ? "" : " --quiet"));
	                commands.add("automake -c -f -a" + (verbose ? "" : " -W none"));
            	}
                createEmptyIfDoesNotExist(configureDirectory, "NEWS");
                createEmptyIfDoesNotExist(configureDirectory, "README");
                createEmptyIfDoesNotExist(configureDirectory, "AUTHORS");
                createEmptyIfDoesNotExist(configureDirectory, "ChangeLog");
                createEmptyIfDoesNotExist(configureDirectory, "COPYING");
            }
			if( ! FileUtils.fileExists( configureDirectory, "configure" ) ) {
				if( autoreconf ) {
					commands.add( "autoreconf --install" + ( verbose ? " --verbose" : "" ) );
				} else {
					commands.add( "autoconf" );
				}
			}
            if (verbose && getLog().isInfoEnabled()) {
                getLog().info("cd '" + configureDirectory + "'");
            }
            for (String command : commands) {
                String[] shellCommand = {
                        "sh", "-c", command
                };
                if (verbose && getLog().isInfoEnabled()) {
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


	static String command( final String command )
	{
		final String envVar = "MAVEN_AUTOTOOLS_" + command.toUpperCase();
		final String envValue = System.getenv( envVar );
		return StringUtils.isEmpty( envValue ) ? command : envValue;
	}


    private File extractScript(String scriptName)
    throws IOException {
        int rnd = Math.abs(new Random().nextInt());
        String temporaryScriptName = "." + scriptName + "-" + rnd;
        File script = new File(configureDirectory, temporaryScriptName);
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
	private Map<String,String> makeConfigureEnvironment() throws IOException
	{
		File includes = new File( dependenciesDirectory, "include" );
		File libraries = new File( dependenciesDirectory, "lib" );
		libraries = makeOsArchDirectory( libraries );
		Map<String,String> env = new HashMap<String,String>( System.getenv() );
		if( configureEnv != null ) env.putAll( configureEnv );
		mergeEnvVar( env, "CFLAGS", "-I" + FileUtils.fixAbsolutePathForUnixShell( includes ) );
		mergeEnvVar( env, "LDFLAGS", "-L" + FileUtils.fixAbsolutePathForUnixShell( libraries ) );
		return env;
	}

	private void mergeEnvVar( Map<String,String> env, final String key, final String value )
	{
		env.put( key, env.containsKey( key ) ? env.get( key ) + " " + value : value );
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
			if( file.getName().startsWith( "." ) ) continue; // TODO: this might be to broad
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

    
    // TODO: This should really be replaced by simply dropping the m4's in the m4 directory

    private void writeM4Macros()
    throws IOException,
           ArtifactResolutionException,
           ArtifactNotFoundException {
        if (FileUtils.fileExists(configureDirectory, "acinclude.m4")
                || !FileUtils.fileExists(configureDirectory, "configure.ac")) {
            return;
        }
        List<Artifact> macroArtifacts = resolveDependencies(macroDependencies);
        File target = new File(configureDirectory, "acinclude.m4");
        File configure =
            SymlinkUtils.resolveSymlink(
                    new File(configureDirectory, "configure.ac"));
        BufferedReader reader =
            new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(configure), "UTF-8"));
        Set<String> alreadyProcessed = new HashSet<String>();
        Pattern pattern = Pattern.compile("([A-Z_]+[A-Z0-9_]*).*");
        String line = null;
        while ((line = reader.readLine()) != null) {
            Matcher matcher = pattern.matcher(line);
            if (!matcher.matches()) {
                continue;
            }
            String macro = matcher.group(1);
            if (alreadyProcessed.contains(macro)) {
                continue;
            }
            try {
                URL macroUrl = findResource(macroArtifacts, macro + ".m4");
                if (macroUrl != null) {
                    if (getLog().isDebugEnabled()) {
                        getLog().debug("Appending macro " + macro
                                       + " to acinclude.m4,"
                                       + " reading from " + macroUrl);
                    }
                    FileUtils.appendURLToFile(macroUrl, target);
                }
            } finally {
                alreadyProcessed.add(macro);
            }
        }
    }


    private URL findResource(List<Artifact> artifacts, String resourcePath)
            throws MalformedURLException, IOException {
        URL url = null;
        for (Artifact artifact : artifacts) {
            File file = artifact.getFile();
            if (file == null || !file.isFile()) {
                continue;
            }
            URL fileURL = file.toURI().toURL();
            JarFile jarFile = new JarFile(file);
            try {
                JarEntry entry = jarFile.getJarEntry(resourcePath);
                if (entry != null) {
                    url = new URL("jar:" + fileURL.toExternalForm()
                                  + "!/" + resourcePath);
                    break;
                }
            } finally {
                jarFile.close();
            }
        }
        if (url == null) {
            url = CompileMojo.class.getResource("m4/" + resourcePath);
        }
        return url;
    }


    private List<Artifact> resolveDependencies(ArtifactDependency[] dependencies)
    throws ArtifactResolutionException,
           ArtifactNotFoundException {
        List<Artifact> artifacts = new ArrayList<Artifact>();
        if (dependencies != null) {
            for (ArtifactDependency dependency : dependencies) {
                Artifact artifact = resolveDependency(dependency);
                artifacts.add(artifact);
            }
        }
        return artifacts;
    }


    @SuppressWarnings("unchecked")
    private Artifact resolveDependency(ArtifactDependency dependency)
            throws ArtifactResolutionException, ArtifactNotFoundException {
        MavenProject parent = mavenProject.getParent();
        if (parent != null) {
            List<MavenProject>  sisters = parent.getCollectedProjects();
            if (sisters != null) {
                for (MavenProject sister : sisters) {
                    Artifact artifact = sister.getArtifact();
                    if (artifact != null) {
                        return artifact;
                    }
                }
            }
        }
        Artifact artifact =
            artifactFactory.createArtifact(
                    dependency.getGroupId(),
                    dependency.getArtifactId(),
                    dependency.getVersion(),
                    null, "jar");
        resolver.resolve(artifact,
                         remoteRepositories,
                         localRepository);
        return artifact;
    }


    private void initLogging() {
        StreamLogAdapter sla = new StreamLogAdapter(getLog());
        exec.setStdout(sla.getStdout());
        exec.setStderr(sla.getStderr());
    }
}

