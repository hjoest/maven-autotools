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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.plexus.build.incremental.BuildContext;


/**
 */
@Mojo(name = "test", 
defaultPhase = LifecyclePhase.TEST)
public final class RunTestsMojo
extends AbstractMojo {

    /**
     * The working directory.
     */
    @Parameter(defaultValue = "${project.build.directory}/autotools/work")
    private File workingDirectory;

    /**
     * Used to run child processes.
     */
    private ProcessExecutor exec = new DefaultProcessExecutor();

    /**
     * Used to avoid running this mojo multiple times with exactly the
     * same configuration.
     */
    private RepeatedExecutions repeated = new RepeatedExecutions();
    
    @Component
    private BuildContext buildContext;
    
    @Parameter
    private Environment environment;

    @Parameter(property = "autotools.test.skip", defaultValue = "${maven.test.skip}")
    private boolean skipTests;

    /**
     * {@inheritDoc}
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void execute()
    throws MojoExecutionException {
        File workDir = getEnvironment().makeOsArchDirectory(workingDirectory);
        if (repeated.alreadyRun(getClass().getName(),
                                workDir)) {
            getLog().info("Skipping repeated execution");
            return;
        }
        if(getEnvironment().isCrossCompiling() || skipTests) {
           getLog().info("Skipping tests.");
           return;
        }
        initLogging();
        try {
            workDir.mkdirs();
            String[] makeCheckCommand = {
                    "make",
                    "check"
            };
            exec.execProcess(makeCheckCommand,
                             null,
                  workDir);
            buildContext.refresh(workDir);
        } catch (Exception ex) {
            throw new MojoExecutionException("Failed to run \"make\"", ex);
        }
    }
    
    private Environment getEnvironment() {
       if(environment==null) {
          environment = new Environment();
       }
       return environment;
    }


    private void initLogging() {
        StreamLogAdapter sla = new StreamLogAdapter(getLog());
        exec.setStdout(sla.getStdout());
        exec.setStderr(sla.getStderr());
    }

}

