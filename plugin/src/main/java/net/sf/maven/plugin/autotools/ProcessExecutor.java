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
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;


/**
 * Running child processes.
 */
public interface ProcessExecutor {

    /**
     * Set the standard output stream.
     *
     * @param stdout the standard output stream
     */
    void setStdout(OutputStream stdout);


    /**
     * Set the standard error stream.
     *
     * @param stdout the standard error stream
     */
    void setStderr(OutputStream stderr);


    /**
     * Executes the specified command in a child process passing stdout
     * and stderr to System.out and System.err respectively.
     *
     * @param command the command as a string array
     * @param env the environment variables
     * @param workingDirectory the working directory (getcwd)
     * @throws IOException if an I/O error occurs or if the child process
     *                     terminates with a non-zero code
     * @throws InterruptedException if the child process is interrupted
     */
    void execProcess(
            String[] command,
            Map<String, String> env,
            File workingDirectory)
    throws IOException, InterruptedException;

}

