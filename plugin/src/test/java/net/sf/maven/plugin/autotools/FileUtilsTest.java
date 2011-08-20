package net.sf.maven.plugin.autotools;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class FileUtilsTest {

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
            FileUtils.deleteDirectory(root);
        }
        root = null;
    }


    @Test
    public void calculateRelativePath()
    throws Exception {
        File sampleDirectory = createDirectory(root, "a/b/c");
        File sample = createFile(sampleDirectory, "sample.txt", "Simple");
        String result = FileUtils.calculateRelativePath(root, sample);
        assertEquals("a/b/c/sample.txt", result);
    }


    @Test
    public void calculateMoreComplicatedRelativePath()
    throws Exception {
        File sampleDirectory = createDirectory(root, "a/b/c");
        File parentDirectory = sampleDirectory.getParentFile();
        File nephewDirectory = createDirectory(parentDirectory, "d/e");
        File sample = createFile(nephewDirectory, "sample.txt", "Simple");
        String result = FileUtils.calculateRelativePath(sampleDirectory,
                                                        sample);
        assertEquals("../d/e/sample.txt", result);
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

