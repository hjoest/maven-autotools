/*
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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.File;
import java.io.OutputStream;
import java.util.Map;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.SilentLog;
import org.sonatype.plexus.build.incremental.BuildContext;

public class CompileMojoTest
      extends AbstractMojoTestCase {

    private static final String[] MOJO_TARGETS = {
          "dependencies",
          "configure",
          "working",
          "install",
          "build"
    };

    private ProcessExecutor exec;
    private BuildContext buildContext;

    public void setUp()
          throws Exception {
        super.setUp();
        exec = createStrictMock(ProcessExecutor.class);
        buildContext = createStrictMock(BuildContext.class);
    }


    public void testConfigure()
          throws Exception {
        String name = "configure";
        CompileMojo mojo = createCompileMojo(name);
        File installDirectory =
              getTestFile("target/test-harness/" + name + "/install");
        Environment env = mojo.getEnvironment();
        String host =
              env.getOperatingSystem() + "/" + env.getSystemArchitecture();
        String[][] commands = {
              { "sh", "-c",
                    "../configure/configure"
                          + " --silent"
                          + " --prefix=\""
                          + FileUtils.fixAbsolutePathForUnixShell(
                          new File(installDirectory, host)) + "\""
              },
              { "sh", "-c", "make" },
              { "sh", "-c", "make install" }
        };
        setupExpectations(name, commands);
        replay(exec);
        mojo.execute();
        verify(exec);
    }


    public void testAutoconf()
          throws Exception {
        String name = "autoconf";
        CompileMojo mojo = createCompileMojo(name);
        File installDirectory =
              getTestFile("target/test-harness/" + name + "/install");
        Environment env = mojo.getEnvironment();
        String host =
              env.getOperatingSystem() + "/" + env.getSystemArchitecture();
        String[][] commands = {
              { "sh", "-c", "autoconf" },
              { "sh", "-c",
                    "../configure/configure"
                          + " --silent"
                          + " --prefix=\""
                          + FileUtils.fixAbsolutePathForUnixShell(
                          new File(installDirectory, host)) + "\""
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
        Environment env = mojo.getEnvironment();
        String host =
              env.getOperatingSystem() + "/" + env.getSystemArchitecture();
        String[][] commands = {
              { "sh", "-c", "aclocal" },
              { "sh", "-c", "autoheader" },
              { "sh", "-c", CompileMojo.command("libtoolize") + " -c -f --quiet" },
              { "sh", "-c", "automake -c -f -a -W none" },
              { "sh", "-c", "autoconf" },
              { "sh", "-c",
                    "../configure/configure"
                          + " --silent"
                          + " --prefix=\""
                          + FileUtils.fixAbsolutePathForUnixShell(
                          new File(installDirectory, host)) + "\""
              },
              { "sh", "-c", "make" },
              { "sh", "-c", "make install" }
        };
        setupExpectations(name, commands);
        replay(exec);
        mojo.execute();
        verify(exec);
    }


    public void testAutoscan()
          throws Exception {
        String name = "autoscan";
        CompileMojo mojo = createCompileMojo(name);
        setVariableValueToObject(mojo, "generatedScriptPostfix", "XYZ");
        File installDirectory =
              getTestFile("target/test-harness/" + name + "/install");
        Environment env = mojo.getEnvironment();
        String host =
              env.getOperatingSystem() + "/" + env.getSystemArchitecture();
        String[][] commands = {
              { "sh", "-c", "autoscan" },
              { "sh", "-c", "./.autoscan-post-XYZ" },
              { "sh", "-c", "aclocal" },
              { "sh", "-c", "autoheader" },
              { "sh", "-c", CompileMojo.command("libtoolize") + " -c -f --quiet" },
              { "sh", "-c", "automake -c -f -a -W none" },
              { "sh", "-c", "autoconf" },
              { "sh", "-c",
                    "../configure/configure"
                          + " --silent"
                          + " --prefix=\""
                          + FileUtils.fixAbsolutePathForUnixShell(
                          new File(installDirectory, host)) + "\""
              },
              { "sh", "-c", "make" },
              { "sh", "-c", "make install" }
        };
        setupExpectations(name, commands);
        replay(exec);
        mojo.execute();
        verify(exec);
    }


    public void testPostinstall()
          throws Exception {
        String name = "postinstall";
        CompileMojo mojo = createCompileMojo(name);
        File installDirectory =
              getTestFile("target/test-harness/" + name + "/install");
        File autotoolsDirectory = findSourceDirectory(name, "autotools");
        String postinstallScript =
              new File(autotoolsDirectory, "postinstall.sh").getAbsolutePath();
        Environment env = mojo.getEnvironment();
        String host =
              env.getOperatingSystem() + "/" + env.getSystemArchitecture();
        String[][] commands = {
              { "sh", "-c", "aclocal" },
              { "sh", "-c", "autoheader" },
              { "sh", "-c", CompileMojo.command("libtoolize") + " -c -f --quiet" },
              { "sh", "-c", "automake -c -f -a -W none" },
              { "sh", "-c", "autoconf" },
              { "sh", "-c",
                    "../configure/configure"
                          + " --silent"
                          + " --prefix=\""
                          + FileUtils.fixAbsolutePathForUnixShell(
                          new File(installDirectory, host)) + "\""
              },
              { "sh", "-c", "make" },
              { "sh", "-c", "make install" },
              { "sh", postinstallScript}
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
        setVariableValueToObject(mojo, "autoreconf", false);
        setVariableValueToObject(mojo, "macroDirectoryName", "m4");
        setVariableValueToObject(
              mojo, "nativeMainDirectory",
              findSourceDirectory(testCase, "native"));
        File autotoolsDirectory = findSourceDirectory(testCase, "autotools");
        setVariableValueToObject(
              mojo, "autotoolsMainDirectory",
              autotoolsDirectory
        );
        setVariableValueToObject(
              mojo, "postInstallScript",
              new File(autotoolsDirectory, "postinstall.sh"));
        setVariableValueToObject(mojo, "exec", exec);
        setVariableValueToObject(mojo, "buildContext", buildContext);
        mojo.setLog(new SilentLog());
        return mojo;
    }


    @SuppressWarnings("unchecked")
    private void setupExpectations(String name, String[][] commands)
          throws Exception {
        exec.setStdout(isA(OutputStream.class));
        exec.setStderr(isA(OutputStream.class));
        File configureDirectory =
              getTestFile("target/test-harness/" + name + "/configure");
        File workingDirectory =
              getTestFile("target/test-harness/" + name + "/working");
        File installDirectory =
              getTestFile("target/test-harness/" + name + "/install");
        for (String[] command : commands) {
            File directory = configureDirectory;
            if (command[1].endsWith("/postinstall.sh")) {
                directory = installDirectory;
            } else if (command[2].startsWith("../configure/configure")
                  || command[2].equals("make")
                  || command[2].equals("make install")) {
                directory = workingDirectory;
            }
            exec.execProcess(
                  aryEq(command),
                  (Map<String, String>) anyObject(),
                  eq(directory));
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

