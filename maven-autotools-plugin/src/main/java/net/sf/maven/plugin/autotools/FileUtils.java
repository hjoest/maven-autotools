package net.sf.maven.plugin.autotools;

import java.io.File;
import java.io.IOException;


public class FileUtils
extends org.codehaus.plexus.util.FileUtils {

    protected FileUtils() {
    }


    public static boolean fileExists(File directory, String name) {
        try {
            File file = SymlinkUtils.resolveSymlink(new File(directory, name));
            return file.exists();
        } catch (IOException ex) {
            return false;
        }
    }


    public static boolean isOlderThanAnyOf(File directory, String name,
                                           String... others) {
        File file = new File(directory, name);
        if (!file.exists()) {
            return true;
        }
        for (String other : others) {
            File otherFile = new File(directory, other);
            if (otherFile.exists()
                    && file.lastModified() < otherFile.lastModified()) {
                return true;
            }
        }
        return false;
    }


    public static String calculateRelativePath(File base, File target)
    throws IOException {
        String targetPath =
            target.getAbsolutePath().replace(File.separatorChar, '/');
        if (base == null) {
            return targetPath;
        }
        String basePath =
            base.getAbsolutePath().replace(File.separatorChar, '/');
        int minlen = Math.min(basePath.length(), targetPath.length());
        int p = 0, bp = 0;
        while (p < minlen && basePath.charAt(p) == targetPath.charAt(p)) {
            if (basePath.charAt(p) == '/') {
                bp = p;
            }
            ++p;
        }
        if (p == basePath.length()) {
            bp = p;
        }
        if (bp == 0 || bp > targetPath.length()) {
            String os = Environment.getEnvironment().getOperatingSystem();
            if ("windows".equals(os)) {
                return fixAbsolutePathForUnixShell(targetPath);
            }
            return targetPath;
        }
        String relative = targetPath.substring(bp + 1);
        p = bp;
        while (p < basePath.length()) {
            if (basePath.charAt(p) == '/') {
                relative = "../" + relative;
            }
            ++p;
        }
        return relative;
    }


    /**
     * Returns the fixed path for the cygwin platform.
     *
     * @param file a file
     * @return the fixed absolute path
     */
    public static String fixAbsolutePathForUnixShell(File file) {
        return fixAbsolutePathForUnixShell(file.getAbsolutePath());
    }


    /**
     * Returns the fixed path for the cygwin platform.
     *
     * @param path an absolute path
     * @return the fixed path
     */
    public static String fixAbsolutePathForUnixShell(String path) {
        String os = Environment.getEnvironment().getOperatingSystem();
        if ("windows".equals(os)
                && path.length() > 2
                && path.charAt(1) == ':') {
            path = "/cygdrive/"
                + path.charAt(0)
                + path.substring(2);
            path = path.replace('\\', '/');
        }
        return path;
    }

}

