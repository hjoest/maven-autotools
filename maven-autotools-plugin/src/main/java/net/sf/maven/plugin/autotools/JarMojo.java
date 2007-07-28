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
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;


/**
 * @goal jar
 * @phase package
 * @requiresProject
 * @description packing of native artifacts in a jar
 */
public final class JarMojo
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
     * The maven archive configuration to use.
     *
     * @parameter
     */
    private MavenArchiveConfiguration archive =
        new MavenArchiveConfiguration();

    /**
     * @component
     */
    private MavenProjectHelper projectHelper;

    /**
     * Location of the local repository.
     *
     * @parameter expression="${localRepository}"
     * @readonly
     * @required
     */
    private org.apache.maven.artifact.repository.ArtifactRepository local;

    /**
     * The install directory.
     *
     * @parameter expression="${project.build.directory}/make-install"
     */
    private File installDirectory;


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
     * The root directory of the jar.
     * @return the root directory
     */
    private File getClassesDirectory() {
        return installDirectory;
    }


    /**
     * Creates the archive.
     *
     * @return the JAR archive
     * @throws MojoExecutionException if an error occurs
     */
    public File createArchive()
    throws MojoExecutionException {
        File jarFile =
            makeJarFile(outputDirectory, finalName, getClassifier());

        MavenArchiver archiver = new MavenArchiver();
        archiver.setArchiver(jarArchiver);
        archiver.setOutputFile(jarFile);

        try {
            File contentDirectory = getClassesDirectory();
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
     * {@inheritDoc}
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void execute()
    throws MojoExecutionException {
        File jarFile = createArchive();

        String classifier = getClassifier();
        projectHelper.attachArtifact(project, "jar", classifier, jarFile);

        String[] assemblyClassifiers =
            findAttachmentClassifiersInAssemblyDescriptors();
        for (int k = 0; k < assemblyClassifiers.length; ++k) {
            String assemblyClassifier = assemblyClassifiers[k];
            if (assemblyClassifier.equals(classifier)) {
                continue;
            }
            File assemblyClassifierJarFile =
                makeJarFile(outputDirectory, finalName, assemblyClassifier);
            projectHelper.attachArtifact(
                    project,
                    "jar",
                    assemblyClassifier,
                    assemblyClassifierJarFile);
            File existing = existsInLocalRepository(assemblyClassifier);
            try {
                if (existing != null) {
                    FileUtils.copyFile(existing, assemblyClassifierJarFile);
                } else {
                    MavenArchiver archiver = new MavenArchiver();
                    MavenArchiveConfiguration assemblyClassifierArchive =
                        new MavenArchiveConfiguration();
                    JarArchiver jarArchiver = new JarArchiver();
                    archiver.setArchiver(jarArchiver);
                    archiver.setOutputFile(assemblyClassifierJarFile);
                    archiver.createArchive(project, assemblyClassifierArchive);
                }
            } catch (Exception ex) {
                throw new MojoExecutionException(
                        "Failed to create archive "
                        + assemblyClassifierJarFile, ex);
            }
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
    private static File makeJarFile(
            File basedir,
            String finalName,
            String classifier) {
        if (classifier == null) {
            classifier = "";
        } else if (classifier.trim().length() > 0
                && !classifier.startsWith("-")) {
            classifier = "-" + classifier;
        }

        return new File(basedir, finalName + classifier + ".jar");
    }


    /**
     * Check if an archive with the specified classifier exists in the
     * local repository.
     *
     * @param classifier the classifier
     * @return if it exists
     */
    private File existsInLocalRepository(String classifier) {
        Artifact artifact = ArtifactUtils.copyArtifact(project.getArtifact());
        artifact.setResolvedVersion("0.2-SNAPSHOT");
        artifact.setRepository(local);
        File archive = new File(local.getBasedir(), local.pathOf(artifact));
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
                return attachedArchive;
            }
        }
        return null;
    }


    /**
     * Finds attachment classifiers in all assembly descriptors of the
     * current maven project and it's ancestors.
     *
     * @return the attachment classifiers
     * @throws MojoExecutionException if any error occurs
     */
    private String[] findAttachmentClassifiersInAssemblyDescriptors()
    throws MojoExecutionException {
        Set<File> assemblyDescriptors = new HashSet<File>();
        MavenProject p = project;
        while (p != null) {
            assemblyDescriptors.addAll(findAssemblyDescriptors(p));
            p = p.getParent();
        }

        Set<String> classifiers = new HashSet<String>();
        for (File descriptor : assemblyDescriptors) {
            try {
                InputStream istream = new FileInputStream(descriptor);
                try {
                    Xpp3Dom descriptorDom = Xpp3DomBuilder.build(istream, null);
                    findValuesInDom(
                            descriptorDom,
                            "attachmentClassifier",
                            classifiers);
                } finally {
                    istream.close();
                }
            } catch (Exception ex) {
                throw new MojoExecutionException(
                        "Failed to read assembly descriptor "
                        + descriptor.getPath(), ex);
            }
        }
        return classifiers.toArray(new String[classifiers.size()]);
    }


    /**
     * Find the assembly descriptors for a given project.
     *
     * @param p the maven project
     * @return a set of assembly descriptor files
     */
    @SuppressWarnings("unchecked")
    private Set<File> findAssemblyDescriptors(MavenProject p) {
        List<Plugin> plugins = p.getBuildPlugins();
        Set<File> result = new HashSet<File>();
        for (Plugin plugin : plugins) {
            if ("maven-assembly-plugin".equals(plugin.getArtifactId())) {
                Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();
                if (configuration == null) {
                    continue;
                }
                Xpp3Dom descriptors = configuration.getChild("descriptors");
                if (descriptors == null) {
                    continue;
                }
                Xpp3Dom[] descriptorChildren = descriptors.getChildren();
                for (int k = 0; k < descriptorChildren.length; ++k) {
                    String fileName = descriptorChildren[k].getValue().trim();
                    File assemblyDescriptor =
                        new File(p.getBasedir(), fileName);
                    if (assemblyDescriptor.exists()) {
                        result.add(assemblyDescriptor);
                    }
                }
            }
        }
        return result;
    }


    /**
     * Finds values of all child elements in a dom with the given element name.
     *
     * @param dom the dom
     * @param element the element name to search for
     * @param values a set to add the values to
     */
    private void findValuesInDom(
            Xpp3Dom dom, String element, Set<String> values) {
        if (dom == null) {
            return;
        }
        Xpp3Dom[] children = dom.getChildren();
        if (children.length > 0) {
            for (int k = 0; k < children.length; ++k) {
                findValuesInDom(children[k], element, values);
            }
        } else if (dom.getName().equals(element)) {
            values.add(dom.getValue());
        }
    }

}

