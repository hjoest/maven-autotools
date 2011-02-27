package net.sf.maven.plugin.autotools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class SymlinkUtils {

    protected SymlinkUtils() {
    }


    public static void createSymlink(File link, File target)
    throws IOException {
        createSymlink(link, target, false);
    }


    public static void createSymlink(File link, File target, boolean forced)
    throws IOException {
        if (link == null) {
            throw new NullPointerException("Parameter 'link' must not be null");
        }
        File linkDirectory = link.getParentFile();
        if (!linkDirectory.exists()) {
            throw new IOException("Directory " + linkDirectory
                                  + " does not exist");
        }
        if (!linkDirectory.isDirectory()) {
            throw new IOException("Path " + linkDirectory
                                  + " is not a directory");
        }
        if (target.getAbsolutePath().indexOf('\'') > -1) {
            throw new IOException("Path name " + target.getPath()
                                  + " contains single quotes");
        }
        if (link.getPath().indexOf('\'') > -1) {
            throw new IOException("Link name " + link.getPath()
                                  + " contains single quotes");
        }
        if (target.isDirectory()) {
            throw new IOException("Path " + target + " is a directory");
        }
        String relativePath =
            FileUtils.calculateRelativePath(linkDirectory, target);
        String forcedOption = forced ? "-f" : "";
        String[] command = {
                "sh", "-c",
                "ln -s " + forcedOption
                         + " '" + relativePath + "'"
                         + " '" + link.getName() + "'"
        };
        ProcessExecutor pe = new DefaultProcessExecutor();
        try {
            pe.execProcess(
                    command,
                    null,
                    linkDirectory);
        } catch (InterruptedException ex) {
            throw new IOException("Failed to create symlink " + link, ex);
        }
    }


    public static File resolveSymlink(File link)
    throws IOException {
        File linkDirectory = link.getParentFile();
        if (!linkDirectory.exists()) {
            throw new IOException("Symbolic link not found " + link);
        }
        if (!linkDirectory.isDirectory()) {
            throw new IOException("Symbolic link not found " + link);
        }
        if (link.getPath().indexOf('\'') > -1) {
            throw new IOException("Link name " + link.getPath()
                                  + " contains single quotes");
        }
        String[] command = {
                "sh", "-c",
                "readlink '" + link.getName() + "'"
        };
        ProcessExecutor pe = new DefaultProcessExecutor();
        try {
            ByteArrayOutputStream stdoutCapture = new ByteArrayOutputStream();
            pe.setStdout(stdoutCapture);
            try {
                Map<String, String> env = new HashMap<String, String>();
                String encoding = "UTF-8";
                if (Environment.getEnvironment().isWindows()) {
                    encoding = "CP1252";
                }
                env.put("LC_CTYPE", "C." + encoding);
                pe.execProcess(
                        command,
                        env,
                        linkDirectory);
                stdoutCapture.flush();
                String resolvedPath = stdoutCapture.toString(encoding).trim();
                if (resolvedPath.startsWith("/")) {
                    return new File(resolvedPath).getCanonicalFile();
                }
                return new File(linkDirectory, resolvedPath).getCanonicalFile();
            } finally {
                stdoutCapture.close();
            }
        } catch (InterruptedException ex) {
            throw new IOException("Failed to resolve symlink " + link, ex);
        }
    }


    /**
     * Deletes symlinks.  The argument may either denote a directory
     * or a file.  In case of a directory, it will recursively
     * delete all symlinks found below.
     *
     * @param file a directory or a file
     * @throws IOException if an I/O error occurs
     * @see org.codehaus.plexus.util.FileUtils#deleteDirectory(java.io.File)
     */
    public static void deleteSymlinks(File file)
    throws IOException {
        if (file == null) {
            return;
        }
        File canonical = file.getCanonicalFile();
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (int k = 0; k < children.length; ++k) {
                    File child = children[k];
                    File childCanonical = child.getCanonicalFile();
                    if (childCanonical.getParentFile().equals(canonical)
                            || !childCanonical.isDirectory()) {
                        deleteSymlinks(child);
                    }
                }
            }
        } else if (!file.equals(canonical)) {
            File parent = file.getParentFile();
            File parentCanonical = parent.getCanonicalFile();
            if (parent.equals(parentCanonical)) {
                file.delete();
            }
        }
    }

}

