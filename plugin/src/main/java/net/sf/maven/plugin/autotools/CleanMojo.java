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
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.sonatype.plexus.build.incremental.BuildContext;


/**
 */
@Mojo(name = "pre-clean", 
defaultPhase = LifecyclePhase.PRE_CLEAN)
public final class CleanMojo
extends AbstractMojo {

    /**
     * The project's target directory.
     */
   @Parameter(defaultValue = "${project.build.directory")
    private File targetDirectory;

   @Component
   private BuildContext buildContext;
   
    /**
     * {@inheritDoc}
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void execute()
    throws MojoExecutionException {
        try {
            SymlinkUtils.deleteSymlinks(targetDirectory);
            buildContext.refresh(targetDirectory);
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to delete symlinks", ex);
        }
    }

}

