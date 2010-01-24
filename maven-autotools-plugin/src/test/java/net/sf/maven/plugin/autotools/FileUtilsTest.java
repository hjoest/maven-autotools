package net.sf.maven.plugin.autotools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import junit.framework.TestCase;


public class FileUtilsTest
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


    public void testCalculateRelativePath()
    throws Exception {
        File sampleDirectory = createDirectory(root, "a/b/c");
        File sample = createFile(sampleDirectory, "sample.txt", "Simple");
        String result = FileUtils.calculateRelativePath(root, sample);
        assertEquals("a/b/c/sample.txt", result);
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
        File directory = File.createTempFile("fileutils-", "");
        directory.delete();
        directory.mkdir();
        return directory;
    }

}

