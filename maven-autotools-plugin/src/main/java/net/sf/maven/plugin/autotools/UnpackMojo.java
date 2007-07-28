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
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;


/**
 * Goal that unpacks the project dependencies from the repository to a defined
 * location.
 *
 * @goal unpack
 * @phase process-sources
 * @requiresDependencyResolution compile
 */
public class UnpackMojo
extends AbstractMojo {

    /**
     * Directory containing the generated artifacts.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private File outputDirectory;

    /**
     * To look up Archiver/UnArchiver implementations.
     *
     * @parameter expression=
     *     "${component.org.codehaus.plexus.archiver.manager.ArchiverManager}"
     * @required
     * @readonly
     */
    private ArchiverManager archiverManager;

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;


    /**
     * {@inheritDoc}
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void execute()
    throws MojoExecutionException {
        try {
            String classifier = getClassifier();
            Set<Artifact> artifacts = getProjectArtifacts();
            for (Artifact artifact : artifacts) {
                if (project.getArtifactId().equals(artifact.getArtifactId())
                        && project.getGroupId().equals(artifact.getGroupId())) {
                    continue;
                }
                if (artifact.getClassifier() == null) {
                    File archive = artifact.getFile();
                    String archiveName = archive.getName();
                    if (archiveName.endsWith(".jar")) {
                        String attachedArchiveName =
                            archiveName.substring(
                                    0,
                                    archiveName.length() - 4)
                                    + "-" + classifier + ".jar";
                        File attachedArchive =
                            new File(archive.getParentFile(),
                                    attachedArchiveName);
                        if (attachedArchive.exists()) {
                            File unpackDirectory =
                                new File(outputDirectory, "make-dependencies");
                            unpack(attachedArchive, unpackDirectory);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            throw new MojoExecutionException("Error unpacking archive", ex);
        }
    }


    /**
     * @param archive the archive to unpack
     * @param unpackDirectory the output directory
     * @throws Exception if an error occurs
     */
    private void unpack(File archive, File unpackDirectory)
    throws Exception {
        unpackDirectory.mkdirs();
        UnArchiver unarchiver =
            archiverManager.getUnArchiver("jar");
        unarchiver.setSourceFile(archive);
        unarchiver.setDestDirectory(unpackDirectory);
        unarchiver.setOverwrite(true);
        unarchiver.extract();
    }


    /**
     * @return the attached artifact classifier
     */
    private String getClassifier() {
        Environment env = Environment.getEnvironment();
        return "native"
        + "-" + env.getSystemArchitecture()
        + "-" + env.getOperatingSystem();
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

