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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.File;
import java.util.Map;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.SilentLog;


public class CompileMojoTest
extends AbstractMojoTestCase {

    private static final String[] MOJO_TARGETS = {
        "dependencies",
        "configure",
        "working",
        "install"
    };

    private ProcessExecutor exec;


    public void setUp()
    throws Exception {
        super.setUp();
        exec = createStrictMock(ProcessExecutor.class);
    }


    public void testAutoconfOnly()
    throws Exception {
        String name = "autoconf";
        CompileMojo mojo = createCompileMojo(name);
        File installDirectory =
            getTestFile("target/test-harness/" + name + "/install");
        Environment env = Environment.getEnvironment();
        String host =
            env.getSystemArchitecture() + "/" + env.getOperatingSystem();
        String[][] commands = {
                { "sh", "-c", "autoconf" },
                { "sh", "-c",
                    "../configure/configure"
                    + " --silent"
                    + " --bindir=\""
                    + FileUtils.fixAbsolutePathForUnixShell(
                          new File(installDirectory, "bin/" + host)) + "\""
                    + " --libdir=\""
                    + FileUtils.fixAbsolutePathForUnixShell(
                          new File(installDirectory, "lib/" + host)) + "\""
                    + " --includedir=\""
                    + FileUtils.fixAbsolutePathForUnixShell(
                          new File(installDirectory, "include")) + "\""
                },
                { "sh", "-c", "make" },
                { "sh", "-c", "make install" }
        };
        setupExpectations(name, commands);
        replay(exec);
        mojo.execute();
        verify(exec);
    }


    public void testAutomake()
    throws Exception {
        String name = "automake";
        CompileMojo mojo = createCompileMojo(name);
        File installDirectory =
            getTestFile("target/test-harness/" + name + "/install");
        Environment env = Environment.getEnvironment();
        String host =
            env.getSystemArchitecture() + "/" + env.getOperatingSystem();
        String[][] commands = {
                { "sh", "-c", "aclocal" },
                { "sh", "-c", "autoheader" },
                { "sh", "-c", "libtoolize -c -f --quiet" },
                { "sh", "-c", "automake -c -f -a -W none" },
                { "sh", "-c", "autoconf" },
                { "sh", "-c",
                    "../configure/configure"
                    + " --silent"
                    + " --bindir=\""
                    + FileUtils.fixAbsolutePathForUnixShell(
                          new File(installDirectory, "bin/" + host)) + "\""
                    + " --libdir=\""
                    + FileUtils.fixAbsolutePathForUnixShell(
                          new File(installDirectory, "lib/" + host)) + "\""
                    + " --includedir=\""
                    + FileUtils.fixAbsolutePathForUnixShell(
                          new File(installDirectory, "include")) + "\""
                },
                { "sh", "-c", "make" },
                { "sh", "-c", "make install" }
        };
        setupExpectations(name, commands);
        replay(exec);
        mojo.execute();
        verify(exec);
    }


    private CompileMojo createCompileMojo(String testCase)
    throws Exception {
        CompileMojo mojo = new CompileMojo();
        for (String target : MOJO_TARGETS) {
            File directory = createOrScrubTargetDirectory(testCase, target);
            setVariableValueToObject(mojo, target + "Directory", directory);
        }
        setVariableValueToObject(
                mojo, "nativeMainDirectory",
                findSourceDirectory(testCase, "native"));
        setVariableValueToObject(
                mojo, "autotoolsMainDirectory",
                findSourceDirectory(testCase, "autotools"));
        setVariableValueToObject(mojo, "exec", exec);
        mojo.setLog(new SilentLog());
        return mojo;
    }


    @SuppressWarnings("unchecked")
    private void setupExpectations(String name, String[][] commands)
    throws Exception {
        File configureDirectory =
            getTestFile("target/test-harness/" + name + "/configure");
        File workingDirectory =
            getTestFile("target/test-harness/" + name + "/working");
        int p = 0;
        for (String[] command : commands) {
            File directory = configureDirectory;
            if (p >= commands.length - 3) {
                directory = workingDirectory;
            }
            exec.execProcess(
                    aryEq(command),
                    (Map<String, String>) anyObject(),
                    eq(directory));
            ++p;
        }
    }


    private File createOrScrubTargetDirectory(String testCase, String target)
    throws Exception {
        File parent = getTestFile("target/test-harness/" + testCase);
        File directory = new File(parent, target);
        if (directory.exists()) {
            FileUtils.deleteDirectory(directory);
        }
        directory.mkdirs();
        return directory;
    }


    private File findSourceDirectory(String testCase, String source) {
        return new File(
                getBasedir(),
                "src/test/resources/test-harness/"
                + testCase + "/src/main/" + source);
    }

}

