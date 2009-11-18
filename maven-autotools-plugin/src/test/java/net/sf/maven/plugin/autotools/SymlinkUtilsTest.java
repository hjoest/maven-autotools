package net.sf.maven.plugin.autotools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import junit.framework.TestCase;

import org.codehaus.plexus.util.FileUtils;


public class SymlinkUtilsTest
extends TestCase {

    private File root;


    @Override
    protected void setUp()
    throws Exception {
        root = makeTestRoot();
    }


    @Override
    protected void tearDown()
    throws Exception {
        if (root != null) {
            FileUtils.deleteDirectory(root);
        }
        root = null;
    }


    public void testCreateSimpleSymlink()
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


    public void testCreateNastySymlink()
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


    public void testCreateDotdotSymlink()
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


    public void testCreateSymlinkWithUnicodeCharacters()
    throws Exception {
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


    public void testFailCreateSymlinkWithSingleQuote()
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
        return directory;
    }

}

