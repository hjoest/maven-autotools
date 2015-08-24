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
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Set;

import net.sf.maven.plugin.autotools.common.Const;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;


/**
 * Goal that unpacks the project dependencies from the repository to a defined
 * location.
 */
@Mojo(name = "unpackDependencies",
      defaultPhase = LifecyclePhase.GENERATE_SOURCES,
      requiresProject = true,
      requiresDependencyResolution = ResolutionScope.COMPILE)
public class UnpackDependenciesMojo
      extends AbstractMojo {

   /**
    * The dependencies directory.
    */
   @Parameter(defaultValue = "${project.build.directory}/autotools/dependencies", required = true)
   private File dependenciesDirectory;

   /**
    * To look up Archiver/UnArchiver implementations.
    */
   @Component(role = org.codehaus.plexus.archiver.manager.ArchiverManager.class)
   private ArchiverManager archiverManager;

   /**
    * The maven project.
    */
   @Parameter(defaultValue = "${project}", required = true, readonly = true)
   private MavenProject project;

   @Parameter
   private Environment environment;

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
            dependenciesDirectory)) {
         getLog().info("Skipping repeated execution");
         return;
      }
      try {
         File depDirectory = getEnvironment().makeOsArchDirectory(dependenciesDirectory);
         Set<Artifact> artifacts = getProjectArtifacts();
         boolean anyNativeDependencies = false;
         for (Artifact artifact : artifacts) {
            if (project.getArtifactId().equals(artifact.getArtifactId())
                  && project.getGroupId().equals(artifact.getGroupId())) {
               continue;
            }
            
            if(artifact.getType().equals("tar.gz") &&
                  artifact.getClassifier().equals(getEnvironment().getClassifier())) {
               anyNativeDependencies = true;
               unpack(artifact.getFile(), depDirectory);
               File depLibDirectory = new File(depDirectory, "lib");
               FileUtils.replace(getLog(), Const.INSTALLDIR_PLACEHOLDER, depDirectory.getAbsolutePath(), depLibDirectory.listFiles(new FilenameFilter() {
                  @Override
                  public boolean accept(File dir, String name) {
                     return name.endsWith(".la");
                  }
               }));
               File pkgconfigDir = new File(depLibDirectory, "pkgconfig");
               if(pkgconfigDir.exists()) {
                  FileUtils.replace(getLog(), Const.INSTALLDIR_PLACEHOLDER, depDirectory.getAbsolutePath(), pkgconfigDir.listFiles(new FilenameFilter() {
                     @Override
                     public boolean accept(File dir, String name) {
                        return name.endsWith(".pc");
                     }
                  }));
               }
            }
//            else if(artifact.getType().equals("so") || artifact.getType().equals("a")) {
               //resolve the header archive
               //               Artifact harArtifact = new DefaultArtifact( artifact.getGroupId(), artifact.getArtifactId(),
               //                     artifact.getVersion(), artifact.getScope(),
               //                     Const.ArtifactType.NATIVE_HEADER_ARCHIVE, "",
               //                     harArtifactHandler );
               //
               //               File resolvedHarArtifactFile = artifactResolverHelper.resolveArtifactToFile( harArtifact );
               //               log.debug( "Resolved har artifact file : " + resolvedHarArtifactFile );
//            }
         }
         if (!anyNativeDependencies) {
            getLog().info("No native dependencies");
         }
      } catch (Exception ex) {
         throw new MojoExecutionException("Error unpacking archive", ex);
      }
   }

   private Environment getEnvironment() {
      if(environment==null) {
         environment = new Environment();
      }
      return environment;
   }

   /**
    * @param archive the archive to unpack
    * @param unpackDirectory the output directory
    * @throws Exception if an error occurs
    */
   private void unpack(File archive, File unpackDirectory)
         throws Exception {
      unpackDirectory.mkdirs();
      UnArchiver unarchiver = archiverManager.getUnArchiver("tar.gz");
      unarchiver.setSourceFile(archive);
      unarchiver.setDestDirectory(unpackDirectory);
      unarchiver.setOverwrite(true);
      unarchiver.extract();
      unpackDirectory.setReadable(true);
      for(File file : unpackDirectory.listFiles()) {
         file.setReadable(true, false);
      }
   }

   /**
    * Replace placeholders installation directory in all libtool archives
    * @param libDirectory  lib folder
    */
   private void processLibtoolArchives(File libDirectory) throws IOException {

   }


   /**
    * Returns the dependency artifacts of this project.
    *
    * @return the project's artifacts
    */
   @SuppressWarnings("unchecked")
   private Set<Artifact> getProjectArtifacts() {
      return project.getArtifacts();
   }

}

