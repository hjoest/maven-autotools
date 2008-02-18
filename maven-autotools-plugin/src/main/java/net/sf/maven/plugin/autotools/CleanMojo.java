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
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;


/**
 * @goal clean
 * @phase clean
 * @description clean target directory
 */
public final class CleanMojo
extends AbstractMojo {

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
            delete(dependenciesDirectory);
            delete(workingDirectory);
            delete(installDirectory);
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to delete files", ex);
        }
    }


    /**
     * Deletes the given file or directory recursively.
     *
     * @param file the file or directory to delete
     * @throws IOException if an I/O error occurs
     */
    private void delete(File file)
    throws IOException {
        if (file == null) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            File fileCanonical = file.getCanonicalFile();
            for (int k = 0; k < children.length; ++k) {
                File child = children[k];
                File childCanonical = child.getCanonicalFile();
                if (childCanonical.getParentFile().equals(fileCanonical)
                        || !childCanonical.isDirectory()) {
                    delete(child);
                }
            }
        }
        System.out.println("DELETE: " + file.getAbsolutePath());
        file.delete();
    }

}

