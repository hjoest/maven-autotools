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

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;


/**
 * Loosely based on {@see org.apache.maven.plugin.jar.AbstractJarMojo}
 * written <a href="evenisse@apache.org">Emmanuel Venisse</a>.
 *
 * @author <a href="hjoest@users.sourceforge.net">Holger Joest</a>
 * @author <a href="evenisse@apache.org">Emmanuel Venisse</a>
 *
 * @goal jar
 * @phase package
 * @requiresProject
 * @description packing of native artifacts in a jar
 */
public final class PackMojo
extends AbstractMojo {

    private static final String[] INCLUDES = new String[]{ "**/**" };

    private static final String[] EXCLUDES = new String[0];


    /**
     * Directory containing the generated JAR.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private File outputDirectory;

    /**
     * The install directory.
     *
     * @parameter expression="${project.build.directory}/autotools/install"
     */
    private File installDirectory;

    /**
     * Name of the generated JAR.
     *
     * @parameter alias="jarName" expression="${project.build.finalName}"
     * @required
     */
    private String finalName;

    /**
     * The Jar archiver.
     *
     * @parameter
     *     expression="${component.org.codehaus.plexus.archiver.Archiver#jar}"
     * @required
     */
    private JarArchiver jarArchiver;

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @component
     */
    private MavenProjectHelper projectHelper;

    /**
     * The maven archive configuration to use.
     *
     * @parameter
     */
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

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
                                installDirectory,
                                outputDirectory)) {
            getLog().info("Skipping repeated execution");
            return;
        }
        // Nudge archiver to use the mode of the input file system 
        jarArchiver.setDefaultFileMode( 0 );
        jarArchiver.setDefaultDirectoryMode( 0 );
        File jarFile = createArchive();
        Environment environment = Environment.getEnvironment();
        String classifier = environment.getClassifier();
        projectHelper.attachArtifact(project, "jar", classifier, jarFile);
    }


    /**
     * The root directory of the jar.
     * @return the root directory
     */
    private File getContentDirectory() {
        return installDirectory;
    }


    /**
     * Creates the archive.
     *
     * @return the JAR archive
     * @throws MojoExecutionException if an error occurs
     */
    private File createArchive()
    throws MojoExecutionException {
        Environment environment = Environment.getEnvironment();
        File jarFile =
            makeJarFile(outputDirectory, finalName,
                        environment.getClassifier());
        MavenArchiver archiver = new MavenArchiver();
        archiver.setArchiver(jarArchiver);
        archiver.setOutputFile(jarFile);
        try {
            File contentDirectory = getContentDirectory();
            if (!contentDirectory.exists()) {
                getLog().warn(
                        "JAR will be empty"
                        + " - no content was marked for inclusion!");
            } else {
                archiver.getArchiver().addDirectory(
                        contentDirectory, INCLUDES, EXCLUDES
                );
            }
            archiver.createArchive(project, archive);
            return jarFile;
        } catch (Exception ex) {
            throw new MojoExecutionException("Error creating JAR archive", ex);
        }
    }


    /**
     * Constructs the file name of the java archive.
     *
     * @param basedir the base directory
     * @param finalName the artifact's final name
     * @param classifier the attachment classifier
     * @return the file name
     */
    private static File makeJarFile(File basedir,
                                    String finalName, String classifier) {
        return new File(basedir, finalName + "-" + classifier + ".jar");
    }

}

