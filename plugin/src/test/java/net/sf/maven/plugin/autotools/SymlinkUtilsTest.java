/*
 * Copyright (C) 2006-2010 Holger Joest <hjoest@users.sourceforge.net>
 * Copyright (C) 2010 Hannes Schmidt <hannes.schmidt@gmail.com>
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;


public class SymlinkUtilsTest {

    private File root;


    @Before
    public void setUp()
    throws Exception {
        root = makeTestRoot();
    }


    @After
    public void tearDown()
    throws Exception {
        if (root != null) {
            try {
                SymlinkUtils.deleteSymlinks(root);
            } finally {
                FileUtils.deleteDirectory(root);
            }
        }
        root = null;
    }


    @Test
    public void createSimpleSymlink()
    throws Exception {
        File targetDirectory = createDirectory(root, "a/b1/c1/d1/e1");
        File target = createFile(targetDirectory, "target.txt", "Simple");
        File linkDirectory = createDirectory(root, "a/b2/c2/d2/e2/f2");
        File link = new File(linkDirectory, "link");
        SymlinkUtils.createSymlink(link, target);
        File resolved = SymlinkUtils.resolveSymlink(link);
        String content = FileUtils.fileRead(resolved);
        assertEquals("Simple", content);
        assertEquals(target.getCanonicalFile(), resolved);
    }


    @Test
    public void createNastySymlink()
    throws Exception {
        File targetDirectory = createDirectory(root, "a/x[$y]/(z)/-/u v/_");
        File target = createFile(targetDirectory, "target.txt", "Nasty");
        File linkDirectory = createDirectory(root, "a/b2/c2/d2/e2/f2/g2");
        File link = new File(linkDirectory, "link");
        SymlinkUtils.createSymlink(link, target);
        File resolved = SymlinkUtils.resolveSymlink(link);
        String content = FileUtils.fileRead(resolved);
        assertEquals("Nasty", content);
        assertEquals(target.getCanonicalFile(), resolved);
    }


    @Test
    public void createDotdotSymlink()
    throws Exception {
        createDirectory(root, "a/x/y/z");
        File targetDirectory = createDirectory(root, "a/x/y/_");
        createFile(targetDirectory, "target.txt", "Dotdot");
        File target = new File(root, "a/x/y/z/../_/target.txt");
        File linkDirectory = createDirectory(root, "a/b2/c2/d2/e2/f2/g2");
        File link = new File(linkDirectory, "link");
        SymlinkUtils.createSymlink(link, target);
        File resolved = SymlinkUtils.resolveSymlink(link);
        String content = FileUtils.fileRead(resolved);
        assertEquals("Dotdot", content);
        assertEquals(target.getCanonicalFile(), resolved);
    }


    @Test
    public void createSymlinkWithUnicodeCharacters()
    throws Exception {
        // FIXME: Found no way to fix this on Windows...
        Assume.assumeTrue(Environment.getEnvironment().isWindows() == false);
        File targetDirectory = createDirectory(root, "a/b/c/\u00f6/\u0153");
        File target = createFile(targetDirectory, "target.txt", "Woowoo");
        File linkDirectory = createDirectory(root, "a/b2/c2/d2/e2");
        File link = new File(linkDirectory, "link");
        SymlinkUtils.createSymlink(link, target);
        File resolved = SymlinkUtils.resolveSymlink(link);
        String content = FileUtils.fileRead(resolved);
        assertEquals("Woowoo", content);
        assertEquals(target.getCanonicalFile(), resolved);
    }


    @Test
    public void failCreateSymlinkWithSingleQuote()
    throws Exception {
        File linkDirectory = createDirectory(root, "a/b2/c2/d2");
        File target = new File(root, "a/single'quote.txt");
        File link = new File(linkDirectory, "link");
        IOException exception = null;
        try {
            SymlinkUtils.createSymlink(link, target);
        } catch (IOException ex) {
            exception = ex;
        }
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("contains single quotes"));
    }


    private File createDirectory(File root, String path) {
        File directory = new File(root, path);
        directory.mkdirs();
        return directory;
    }


    private File createFile(File targetDirectory, String name, String content)
    throws FileNotFoundException,
            IOException, UnsupportedEncodingException {
        File target = new File(targetDirectory, name);
        OutputStream ostream = new FileOutputStream(target);
        try {
            ostream.write(content.getBytes("ASCII"));
        } finally {
            ostream.close();
        }
        return target;
    }


    private static File makeTestRoot()
    throws IOException {
        File directory = File.createTempFile("symlinkutils-", "");
        directory.delete();
        directory.mkdir();
        return directory.getCanonicalFile();
    }

}

