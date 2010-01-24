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

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.logging.Log;


/**
 * Writes output to Maven's logger.
 */
public class StreamLogAdapter {

    private Log logger;


    public StreamLogAdapter(Log logger) {
        this.logger = logger;
    }


    public OutputStream getStdout() {
        return new LineBreakingOutputStream() {
            @Override
            public void writeLine(String line)
            throws IOException {
                if (!SUPPRESS_LINES.contains(line.trim())) {
                    logger.info(line);
                }
            }
        };
    }


    public OutputStream getStderr() {
        return new LineBreakingOutputStream() {
            @Override
            public void writeLine(String line)
            throws IOException {
                if (!line.contains(": installing ")
                        && !SUPPRESS_LINES.contains(line.trim())) {
                    logger.warn(line);
                }
            }
        };
    }

    private static final Set<String> SUPPRESS_LINES = new HashSet<String>();

    static {
        for (String s : new String[] {
            "-----------------------------------"
                + "-----------------------------------",
            // Unix/Linux:
            "If you ever happen to want to link against installed libraries",
            "in a given directory, LIBDIR, you must either use libtool, and",
            "specify the full pathname of the library, or use the `-LLIBDIR'",
            "flag during linking and do at least one of the following:",
            "- add LIBDIR to the `LD_LIBRARY_PATH' environment variable",
            "during execution",
            "- add LIBDIR to the `LD_RUN_PATH' environment variable",
            "during linking",
            "- use the `-Wl,-rpath -Wl,LIBDIR' linker flag",
            "- have your system administrator add LIBDIR to `/etc/ld.so.conf'",
            "See any operating system documentation about shared libraries for",
            "more information, such as the ld(1) and ld.so(8) manual pages.",
            // Windows:
            "- add LIBDIR to the `PATH' environment variable",
            "- use the `-LLIBDIR' linker flag"
        }) {
            SUPPRESS_LINES.add(s);
        }
    }

}

