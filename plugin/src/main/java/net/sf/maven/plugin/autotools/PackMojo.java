/*
 * Copyright (C) 2004-2013 Emmanuel Venisse <evenisse@apache.org>
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

import java.io.File;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.tar.TarArchiver;

/**
 * Loosely based on {@see org.apache.maven.plugin.jar.AbstractJarMojo}
 * written by <a href="evenisse@apache.org">Emmanuel Venisse</a>.
 *
 * @author <a href="evenisse@apache.org">Emmanuel Venisse</a>
 * @author <a href="holger@joest.org">Holger Joest</a>
 * @author <a href="hannes.schmidt@gmail.com">Hannes Schmidt</a>
 *
 */
@Mojo(name = "jar", 
   defaultPhase = LifecyclePhase.PACKAGE,
   requiresProject = true)
public final class PackMojo
extends AbstractMojo {

    private static final String[] INCLUDES = new String[]{ "**/**" };

    private static final String[] EXCLUDES = new String[0];


    /**
     * Directory containing the generated JAR.
     */
    @Parameter(defaultValue = "${project.build.directory}", required = true)
    private File outputDirectory;

    /**
     * The install directory.
     */
    @Parameter(defaultValue = "${project.build.directory}/autotools/install")
    private File installDirectory;

    /**
     * Name of the generated JAR.
     */
    @Parameter(alias = "jarName",  defaultValue = "${project.build.finalName}", required = true)
    private String finalName;

    /**
     * The Tar archiver.
     */
    @Component(role = org.codehaus.plexus.archiver.Archiver.class, hint = "tar")
    private TarArchiver tarArchiver;

    /**
     * The maven project.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;
    
    @Parameter
    private Environment environment;

    /**
     * @component
     */
    @Component
    private MavenProjectHelper projectHelper;

    /**
     * The maven archive configuration to use.
     */
    @Parameter
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * Used to avoid running this mojo multiple times with exactly the
     * same configuration.
     */
    private RepeatedExecutions repeated = new RepeatedExecutions();

    @Component( role = org.apache.maven.artifact.handler.ArtifactHandler.class, hint = "tar.gz" )
    private ArtifactHandler tarArtifactHandler;


    /**
     * {@inheritDoc}
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void execute()
    throws MojoExecutionException {
        if (repeated.alreadyRun(getClass().getName(),
                                installDirectory,
                                outputDirectory)) {
            getLog().info("Skipping repeated execution");
            return;
        }
        // Nudge archiver to use the mode of the input file system
        tarArchiver.setDefaultFileMode( 0 );
        tarArchiver.setDefaultDirectoryMode( 0 );
        File tarFile = createArchive();
        String classifier = getEnvironment().getClassifier();
        if(project.getPackaging().equals("tar.gz")) {
            Artifact tarArtifact = new DefaultArtifact( project.getGroupId(), project.getArtifactId(),
                                       project.getVersion(), "compile",
                                       "tar.gz", getEnvironment().getClassifier(),
                                       tarArtifactHandler );
            tarArtifact.setFile(tarFile);
            project.setArtifact(tarArtifact);
        } else {
            projectHelper.attachArtifact(project, "tar.gz", classifier, tarFile);
        }
    }
    
    private Environment getEnvironment() {
       if(environment==null) {
          environment = new Environment();
       }
       return environment;
    }

    /**
     * The root directory of the jar.
     * @return the root directory
     */
    private File getContentDirectory() {
        return getEnvironment().makeOsArchDirectory(installDirectory);
    }


    /**
     * Creates the archive.
     *
     * @return the JAR archive
     * @throws MojoExecutionException if an error occurs
     */
    private File createArchive()
    throws MojoExecutionException {
        File tarFile =
            makeTarFile(outputDirectory, finalName, getEnvironment().getClassifier());
        TarArchiver.TarCompressionMethod mode = new TarArchiver.TarCompressionMethod();
        mode.setValue("gzip");
        tarArchiver.setCompression(mode);
        tarArchiver.setDestFile(tarFile);
        try {
            File contentDirectory = getContentDirectory();
            if (!contentDirectory.exists()) {
                getLog().warn("TAR will be empty - no content was marked for inclusion!");
            } else {
                tarArchiver.addDirectory(contentDirectory, INCLUDES, EXCLUDES);
            }
            tarArchiver.createArchive();
            return tarFile;
        } catch (Exception ex) {
            throw new MojoExecutionException("Error creating TAR archive", ex);
        }
    }


    /**
     * Constructs the file name of the archive.
     *
     * @param basedir the base directory
     * @param finalName the artifact's final name
     * @param classifier the attachment classifier
     * @return the file name
     */
    private static File makeTarFile(File basedir, String finalName, String classifier) {
        return new File(basedir, finalName + "-" + classifier + ".tar.gz");
    }

}

