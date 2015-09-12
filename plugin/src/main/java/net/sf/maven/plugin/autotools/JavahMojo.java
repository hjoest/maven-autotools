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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.Scanner;
import org.objectweb.asm.ClassReader;
import org.sonatype.plexus.build.incremental.BuildContext;


/**
 */
@Mojo(name = "javah", 
defaultPhase = LifecyclePhase.COMPILE)
public final class JavahMojo
extends AbstractMojo {

   /**
    * The configure directory.
    */
   @Parameter(defaultValue = "${project.build.directory}/autotools/configure")
   private File configureDirectory;

   /**
    * The classes directory.
    */
   @Parameter(defaultValue = "${project.build.directory}/classes")
   private File classesDirectory;

   /**
    * The name of the cumulative JNI header file.
    */
   @Parameter(defaultValue = "cumulative-jni.h")
   private String cumulativeHeader;

   @Component
   private BuildContext buildContext;

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
    * {@inheritDoc}
    * @see org.apache.maven.plugin.AbstractMojo#execute()
    */
   public void execute2()
         throws MojoExecutionException {
      if (repeated.alreadyRun(getClass().getName(),
            configureDirectory,
            classesDirectory)) {
         getLog().info("Skipping repeated execution");
         return;
      }
      initLogging();
      try {
         String[] classFiles =
               FileUtils.getFilesFromExtension(
                     classesDirectory.getAbsolutePath(),
                     new String[] { "class" });
         if (classFiles.length == 0) {
            getLog().info("No classes to generate headers for");
            return;
         }
         String[] nativeClassFiles = getNativeClasses(classFiles);
         if (nativeClassFiles.length == 0) {
            getLog().info("No classes with native methods");
            return;
         }
         String javah = getJavaToolPath("javah");
         String[] javahCommand = {
               javah,
               "-classpath", classesDirectory.getAbsolutePath(),
               "-d", configureDirectory.getAbsolutePath(),
               null
         };
         List<String> headerNames = new ArrayList<String>();
         for (String classFile : nativeClassFiles) {
            classFile =
                  FileUtils.calculateRelativePath(classesDirectory,
                        new File(classFile));
            String className = classFile;
            if (className.endsWith(".class")) {
               className = className.substring(0, className.length() - 6);
            }
            className = className.replace('/', '.');
            javahCommand[javahCommand.length - 1] = className;
            getLog().info("Generating headers for class " + className);
            exec.execProcess(javahCommand, null, null);
            String headerName = className.replace('.', '_') + ".h";
            headerNames.add(headerName);
         }
         writerCumulativeHeaderFile(headerNames);
      } catch (Exception ex) {
         ex.printStackTrace();
         throw new MojoExecutionException("Failed to run javah", ex);
      }
   }

   /**
    * {@inheritDoc}
    * @see org.apache.maven.plugin.AbstractMojo#execute()
    */
   public void execute()
         throws MojoExecutionException {
      if (repeated.alreadyRun(getClass().getName(),
            configureDirectory,
            classesDirectory)) {
         getLog().info("Skipping repeated execution");
         return;
      }
      if(!classesDirectory.exists()) {
         getLog().info("Skipping javah.  No classes");
         return;
      }
      initLogging();
      try {
         Scanner scanner = buildContext.newScanner( classesDirectory );
         scanner.setIncludes( new String[] {"*.class"} );
         scanner.scan();
         String[] classFiles = scanner.getIncludedFiles();
         if (classFiles.length == 0) {
            getLog().info("No classes to generate headers for");
            return;
         }
         String[] nativeClassFiles = getNativeClasses(classFiles);
         if (nativeClassFiles.length == 0) {
            getLog().info("No classes with native methods");
            return;
         }
         String javah = getJavaToolPath("javah");
         String[] javahCommand = {
               javah,
               "-classpath", classesDirectory.getAbsolutePath(),
               "-d", configureDirectory.getAbsolutePath(),
               null
         };
         List<String> headerNames = new ArrayList<String>();

         for ( String filename : classFiles ) {
            String classFile =
                  FileUtils.calculateRelativePath(classesDirectory, new File( scanner.getBasedir(), filename ));
            String className = classFile;
            if (className.endsWith(".class")) {
               className = className.substring(0, className.length() - 6);
            }
            className = className.replace('/', '.');
            javahCommand[javahCommand.length - 1] = className;
            getLog().info("Generating headers for class " + className);
            exec.execProcess(javahCommand, null, null);
            String headerName = className.replace('.', '_') + ".h";
            headerNames.add(headerName);
         }
         writerCumulativeHeaderFile(headerNames);
      } catch (Exception ex) {
         ex.printStackTrace();
         throw new MojoExecutionException("Failed to run javah", ex);
      }
   }

   private String getJavaToolPath(String tool) {
      if (environment.isWindows()) {
         tool += ".exe";
      }
      File binDir = new File(getJavaHome(), "bin");
      File result = new File(binDir, tool);
      return result.getAbsolutePath();
   }


   private static String getJavaHome() {
      String path = System.getProperty("java.home");
      if (path == null) {
         throw new RuntimeException("Property 'java.home' not set");
      }
      File pathDir = new File(path);
      if (pathDir.getName().equals("jre")) {
         pathDir = pathDir.getParentFile();
      }
      return pathDir.getAbsolutePath();
   }


   private String[] getNativeClasses(String[] classFiles) {
      List<String> nativeClassFiles = new ArrayList<String>();
      for (String classFile : classFiles) {
         if (isNativeClass(classFile)) {
            nativeClassFiles.add(classFile);
         }
      }
      return nativeClassFiles.toArray(new String[nativeClassFiles.size()]);
   }


   private boolean isNativeClass(String classFile) {
      try {
         InputStream stream = new FileInputStream(classFile);
         try {
            ClassReader cr = new ClassReader(stream);
            NativeMethodsFinder cv = new NativeMethodsFinder();
            cr.accept(cv, ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE);
            return cv.hasNativeMethods();
         } finally {
            stream.close();
         }
      } catch (IOException ex) {
         return false;
      }
   }


   private void writerCumulativeHeaderFile(List<String> headerNames)
         throws IOException {
      PrintWriter writer = new PrintWriter( buildContext.newFileOutputStream(new File(configureDirectory, cumulativeHeader)));
      try {
         writer.println("/* DO NOT EDIT THIS FILE"
               + " - it is machine generated */");
         writer.println("#ifndef Included__CUMULATIVE_JNI_HEADER");
         writer.println("#define Included__CUMULATIVE_JNI_HEADER");
         for (String headerName : headerNames) {
            writer.println("#include \"" + headerName + "\"");
         }
         writer.println("#endif");
      } finally {
         writer.close();
      }
   }


   private void initLogging() {
      StreamLogAdapter sla = new StreamLogAdapter(getLog());
      exec.setStdout(sla.getStdout());
      exec.setStderr(sla.getStderr());
   }

}

