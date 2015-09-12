/*
 * Copyright (C) 2006-2013 Holger Joest <holger@joest.org>
 * Copyright (C) 2010-2013 Hannes Schmidt <hannes.schmidt@gmail.com>
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
import java.io.FilenameFilter;
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
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.maven.plugin.autotools.common.Const;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.InterpolationFilterReader;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.plexus.build.incremental.BuildContext;


/**
 * Runs autotools autogen.sh, configure, make, make install
 */
@Mojo(name = "compile",
      defaultPhase = LifecyclePhase.COMPILE,
      requiresProject = true)
public final class CompileMojo
      extends AbstractMojo {

   /**
    * The build directory.  It needs to be skipped in making symlinks, incase the autotools folder is the root directory. (which is common)
    */
   @Parameter(defaultValue = "${project.build.directory}")
   private File buildDirectory;

   /**
    * The dependencies directory.
    */
   @Parameter(defaultValue = "${project.build.directory}/autotools/dependencies")
   private File dependenciesDirectory;

   /**
    * The configure directory.
    */
   @Parameter(defaultValue = "${project.build.directory}/autotools/configure")
   private File configureDirectory;

   /**
    * The working directory.
    */
   @Parameter(defaultValue = "${project.build.directory}/autotools/work")
   private File workingDirectory;

   /**
    * The install directory.
    */
   @Parameter(defaultValue = "${project.build.directory}/autotools/install")
   private File installDirectory;

   /**
    * The autotools scripts directory.
    */
   @Parameter(defaultValue = "${basedir}/src/main/autotools", readonly = false)
   private File autotoolsMainDirectory;

   /**
    * The native source files directory.
    */
   @Parameter(defaultValue = "${basedir}/src/main/native", readonly = false)
   private File nativeMainDirectory;

   /**
    * Set 'true' if you want verbose output.
    */
   @Parameter
   private boolean verbose;

   /**
    * Specify 'true' if you want to override pkgconfigdir to the maven dependencies directory
    */
   @Parameter(defaultValue = "true")
   private boolean overridePkgConfigDir;

   /**
    * Unix shell script for arbitrary post processing after 'make install'.
    * If it exists and if it is executable, it will be run in the platform
    * specific install directory, e.g. <code>target/autotools/install/</code>.
    */
   @Parameter(defaultValue = "${basedir}/src/main/autotools/postinstall.sh")
   private File postInstallScript;

   /**
    * Artifacts containing additional autoconf macros.
    */
   @Parameter
   private MacroDependency[] macroDependencies;

   @Component
   private ArtifactFactory artifactFactory;

   @Component
   private ArtifactResolver resolver;

   @Parameter(defaultValue = "${localRepository}")
   private ArtifactRepository localRepository;

   @Parameter(defaultValue = "${project.remoteArtifactRepositories}")
   private List<ArtifactRepository> remoteRepositories;

   @Parameter(defaultValue = "${project}")
   private org.apache.maven.project.MavenProject mavenProject;

   @Component
   private BuildContext buildContext;


   // TODO: this should really be determined automatically from AC_CONFIG_MACRO_DIR in configure.ac
   /**
    * The name of the sub-directory of {@link #autotoolsMainDirectory} in which
    * m4 macros reside. Note that ATM this setting needs to be consistent with
    * AC_CONFIG_MACRO_DIR in configure.ac and -I flags in ACLOCAL_AMFLAGS in
    * Makefile.am.
    */
   @Parameter(defaultValue = "m4")
   private String macroDirectoryName;

   /**
    * 'autogen.sh' script.
    */
   @Parameter(defaultValue = "autogen.sh")
   private File autogenScript;

   /**
    * Additional parameters to pass to the ./configure script. Can't use --bindir, --libdir or --includedir.
    */
   @Parameter(property = "autotools.configureArgs")
   private String configureArgs;

   /**
    * Additional environment variables to set before running configure.
    */
   @Parameter
   private Map<String,String> configureEnv;

   /**
    * Additional environment variables to set before running make.
    */
   @Parameter
   private Map<String,String> makeEnv;

   /**
    * Host environment definition
    *
    * Example:
    * <pre>
    * &lt;environment&gt;
    * &lt;arch&gt;arm&lt;/arch&gt;
    * &lt;os&gt;linux&lt;/os&gt;
    * &lt;host&gt;arm-linux-gnueabihf&lt;/host&gt;
    * &lt;/environment&gt;
    * </pre>
    */
   @Parameter
   private Environment environment;

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
    * Postfix for generated scripts.
    */
   @Parameter
   private String generatedScriptPostfix;

   private File realWorkDirectory;
   private File realDepDirectory;
   private File realInstallDirectory;

   Environment getEnvironment() {
      if(environment==null) {
         getLog().info("Using detected environment");
         environment = new Environment();
      }
      return environment;
   }

   /**
    * {@inheritDoc}
    * @see org.apache.maven.plugin.AbstractMojo#execute()
    */
   public void execute()
         throws MojoExecutionException {
      getLog().info("Compiling Autotools Project");
      realWorkDirectory = getEnvironment().makeOsArchDirectory(workingDirectory);
      realInstallDirectory = getEnvironment().makeOsArchDirectory(installDirectory);
      realDepDirectory = getEnvironment().makeOsArchDirectory(dependenciesDirectory);
      if (repeated.alreadyRun(getClass().getName(),
            nativeMainDirectory,
            autotoolsMainDirectory,
            configureDirectory,
            realDepDirectory,
            realInstallDirectory,
            realWorkDirectory,
            autogenScript,
            configureArgs,
            configureEnv,
            makeEnv,
            overridePkgConfigDir)) {
         getLog().info("Skipping repeated execution");
         return;
      }
      initLogging();
      prepareBuild();
      autogen();
      configure();
      make();
      postInstall();
   }


   private void prepareBuild()
         throws MojoExecutionException {
      try {
         configureDirectory.mkdirs();
         realWorkDirectory.mkdirs();
         realInstallDirectory.mkdirs();
         makeSymlinks(autotoolsMainDirectory, configureDirectory);
         if(!autotoolsMainDirectory.equals(nativeMainDirectory)) { //no need for both symlinks if the folders are the same
            makeSymlinks(nativeMainDirectory, configureDirectory);
         }
         makeM4Directory();
         writeM4Macros();
         if (!FileUtils.fileExists(configureDirectory, "Makefile.am")) {
            // If not produced by automake, it is highly probable that the
            // make script can not deal with source files in a neighboring
            // directory.  To play it safe, we link the source files into
            // the working directory as well.
            makeSymlinks(nativeMainDirectory, realWorkDirectory);
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


   private void makeM4Directory() {
      if (!FileUtils.fileExists(configureDirectory, macroDirectoryName)) {
         new File(configureDirectory, macroDirectoryName).mkdir();
      }
   }


   private void configure()
         throws MojoExecutionException {
      if (FileUtils.fileExists(configureDirectory, "Makefile")
            && !FileUtils.isOlderThanAnyOf(configureDirectory, "Makefile",
            "Makefile.in", "Makefile.am")) {
         getLog().info("No need to run configure since there is an up-to-date makefile.");
         return;
      }
      String configurePath = "configure";
      try {
         getLog().info("Running configure");
         File configureScript = new File(configureDirectory, configurePath);
         if (!configureScript.canExecute()) {
            configureScript.setExecutable(true);
         }
         configurePath = FileUtils.calculateRelativePath(realWorkDirectory, configureScript);
         String configure = String.format("%s %s --prefix=\"%s\"",
               configurePath,
               verbose ? "" : " --silent", FileUtils.fixAbsolutePathForUnixShell(realInstallDirectory));

         if(getEnvironment().isCrossCompiling()) {
            configure += " --host="+getEnvironment().getHost();
         }
         if(overridePkgConfigDir) {
            File depLibDir = new File(realDepDirectory, "lib");
            File pkgconfigDir = new File(depLibDir, "pkgconfig");
            configure += " PKG_CONFIG_PATH=" + pkgconfigDir.getAbsolutePath();
         }

         if (!StringUtils.isEmpty(configureArgs)) {
            configure += " " + configureArgs;
         }
         String[] configureCommand = {
               "sh", "-c", configure
         };
         if (verbose && getLog().isInfoEnabled()) {
            getLog().info("cd '" + realWorkDirectory + "'");
            getLog().info(Arrays.toString(configureCommand));
         }
         exec.execProcess(
               configureCommand,
               makeConfigureEnvironment(),
               realWorkDirectory);
         buildContext.refresh(realWorkDirectory);
      } catch (Exception ex) {
         throw new MojoExecutionException("Failed to run '"
               + configurePath + "'"
               + " in directory '"
               + realWorkDirectory + "'",
               ex);
      }
   }


   private void make()
         throws MojoExecutionException {
      getLog().info("running make");
      try {
         String[] makeCommand = {
               "sh", "-c", "make"
         };
         exec.execProcess(makeCommand,
               makeMakeEnvironment(),
               realWorkDirectory);
         String[] makeInstallCommand = {
               "sh", "-c", "make install"
         };
         exec.execProcess(makeInstallCommand,
               makeMakeEnvironment(),
               realWorkDirectory);
         moveDLLsToLibDirectory();
         File libDir = new File(realInstallDirectory, "lib");
         getLog().debug("Processing libtool archives in " + libDir.getAbsolutePath());
         FileUtils.replace(getLog(), realDepDirectory.getAbsolutePath(), Const.INSTALLDIR_PLACEHOLDER, libDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
               return name.endsWith(".la");
            }
         }));
         FileUtils.replace(getLog(), realInstallDirectory.getAbsolutePath(), Const.INSTALLDIR_PLACEHOLDER, libDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
               return name.endsWith(".la");
            }
         }));
         File pkgconfigDir = new File(libDir, "pkgconfig");
         if(pkgconfigDir.exists()) {
            FileUtils.replace(getLog(), realInstallDirectory.getAbsolutePath(), Const.INSTALLDIR_PLACEHOLDER, pkgconfigDir.listFiles(new FilenameFilter() {
               @Override
               public boolean accept(File dir, String name) {
                  return name.endsWith(".pc");
               }
            }));
         }
         buildContext.refresh(realWorkDirectory);
      } catch (Exception ex) {
         throw new MojoExecutionException("Failed to run \"make\"", ex);
      }
   }


   private void postInstall()
         throws MojoExecutionException {
      if (postInstallScript == null || !postInstallScript.exists()) {
         getLog().info("No post install script.");
         return;
      }
      try {
         String[] postInstallCommand = {
               "sh", postInstallScript.getAbsolutePath()
         };
         exec.execProcess(postInstallCommand,
               null,
               realInstallDirectory);
      } catch (Exception ex) {
         throw new MojoExecutionException("Failed to run \""
               + postInstallScript + "\"", ex);
      }
   }

   /**
    * This is a hack necessary for Windows to move DLLs to the right
    * location after 'make install'.
    */
   private void moveDLLsToLibDirectory()
         throws IOException {
      if (getEnvironment().isWindows()) {
         File libDirectory = new File(realInstallDirectory, "lib");
         File invalidBinDir = new File(libDirectory, "../bin");
         if (invalidBinDir.exists()) {
            File[] children = invalidBinDir.listFiles();
            for (int k = 0; k < children.length; ++k) {
               File child = children[k];
               File movedChild = new File(libDirectory,
                     child.getName());
               FileUtils.rename(child, movedChild);
            }
         }
         invalidBinDir.delete();
      }
   }


   private void autogen()
         throws MojoExecutionException {
      if (FileUtils.fileExists(configureDirectory, "configure")
            || !FileUtils.isOlderThanAnyOf(configureDirectory, "configure", "configure.ac", "configure.in")) {
         getLog().info("Skipping autogen script, configure script exists");
         return;
      }
      File script = new File(configureDirectory, autogenScript.getName());
      getLog().info("Generating configure script");
      if(!script.exists()) {
         throw new MojoExecutionException("autogen script does not exist: " + script.getAbsolutePath());
      }
      if (!script.canExecute()) {
         script.setExecutable(true);
      }
      String[] shellCommand = {
            "sh", "-c", script.getAbsolutePath()
      };
      if (verbose && getLog().isInfoEnabled()) {
         getLog().info("cd '" + configureDirectory + "'");
         getLog().info(Arrays.toString(shellCommand));
      }
      try {
         exec.execProcess(
               shellCommand,
               null,
               configureDirectory);
      }
      catch (IOException e) {
         throw new MojoExecutionException("Error invoking autogen", e);
      }
      catch (InterruptedException e) {
         throw new MojoExecutionException("Error invoking autogen", e);
      }
   }

   /**
    * Returns the environment variables used to call the configure
    * script.
    *
    * @return the environment
    * @throws IOException if an I/O error occurs
    */
   private Map<String,String> makeConfigureEnvironment()
         throws IOException {
      File includes = new File(realDepDirectory, "include");
      File libraries = new File(realDepDirectory, "lib");
      Map<String,String> env = new HashMap<String,String>(System.getenv());
      if (configureEnv != null) {
         for(Entry<String, String> entry : configureEnv.entrySet()) {
            env.put(entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
         }
      }
      mergeEnvVar(env, "CFLAGS", "-I"
            + FileUtils.fixAbsolutePathForUnixShell(includes));
      mergeEnvVar(env, "CXXFLAGS", "-I"
            + FileUtils.fixAbsolutePathForUnixShell(includes));
      mergeEnvVar(env, "LDFLAGS", "-L"
            + FileUtils.fixAbsolutePathForUnixShell(libraries));
      return env;
   }

   private Map<String,String> makeMakeEnvironment()
         throws IOException {
      Map<String,String> env = new HashMap<String,String>(System.getenv());
      if (makeEnv != null) {
         for(Entry<String, String> entry : makeEnv.entrySet()) {
            env.put(entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
         }
      }
      return env;
   }

   private void makeSymlinks(File sourceDirectory, File destinationDirectory)
         throws IOException {
      if (sourceDirectory == null) {
         return;
      }
      //don't make symlinks from build directory to avoid recursive loop
      if(sourceDirectory.getAbsolutePath().equals(buildDirectory.getAbsolutePath())) {
         return;
      }
      getLog().debug(String.format("Making Symbolic links %s -> %s", sourceDirectory.getAbsolutePath(), destinationDirectory.getAbsolutePath()));
      File[] files = sourceDirectory.listFiles();
      if (files == null) {
         return;
      }
      for (File file : files) {
         if (file.getName().startsWith(".")) {
            continue; // TODO: this might be to broad
         }
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

   private void writeM4Macros()
         throws IOException,
         ArtifactResolutionException,
         ArtifactNotFoundException {
      if (FileUtils.fileExists(configureDirectory, "acinclude.m4")
            || !FileUtils.fileExists(configureDirectory, "configure.ac")) {
         return;
      }
      getLog().debug("Writing M4 Macros");
      List<Artifact> macroArtifacts = resolveDependencies(macroDependencies);
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
      File m4Directory = new File(configureDirectory, "m4");
      m4Directory.mkdir();
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
            String macroFilename = macro.toLowerCase() + ".m4";
            File macroFile = new File(m4Directory, macroFilename);
            if (!macroFile.exists()) {
               URL macroUrl = findResource(macroArtifacts, macro + ".m4");
               if (macroUrl != null) {
                  if (getLog().isDebugEnabled()) {
                     getLog().debug("Copying macro " + macro
                           + " to m4/" + macroFilename
                           + ", reading from " + macroUrl);
                  }
                  FileUtils.copyURLToFile(macroUrl, macroFile);
               }
            }
         } finally {
            alreadyProcessed.add(macro);
         }
      }
      reader.close();
   }


   private URL findResource(List<Artifact> artifacts, String resourcePath)
         throws IOException {
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
         url = CompileMojo.class.getResource("m4/" + resourcePath.toLowerCase());
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
   //TODO fix this with modern resolution code
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

   private static void mergeEnvVar(Map<String,String> env,
         String key,
         String value) {
      env.put(key, env.containsKey(key) ? env.get(key) + " " + value : value);
   }
}
