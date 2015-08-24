/*
 * Copyright (C) 2006-2013 Holger Joest <holger@joest.org>
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.Charset;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.Files;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.IOUtil;


public class FileUtils
extends org.codehaus.plexus.util.FileUtils {

    protected FileUtils() {
    }


    public static boolean fileExists(@Nonnull File directory, @Nonnull String name) {
        try {
            File file = SymlinkUtils.resolveSymlink(new File(directory, name));
            return file.exists();
        } catch (IOException ex) {
            return false;
        }
    }


    public static void appendURLToFile(@Nonnull URL source, @Nonnull File destination)
    throws IOException {
        File temp = destination;
        int c = 1;
        while (temp.exists()) {
            temp = new File(destination.getPath() + "." + c);
            c++;
        }
        copyURLToFile(source, temp);
        if (temp != destination) {
            OutputStream ostream = new FileOutputStream(destination, true);
            try {
                InputStream istream = new FileInputStream(temp);
                try {
                    IOUtil.copy(istream, ostream);
                } finally {
                    istream.close();
                }
            } finally {
                ostream.close();
            }
            temp.delete();
        }
    }


    public static boolean isOlderThanAnyOf(@Nonnull File directory, @Nonnull String name,
          @Nonnull String... others) {
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


    public static String calculateRelativePath(@Nonnull File base, @Nonnull File target)
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
            if (Environment.getBuildEnvironment().isWindows()) {
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
    public static String fixAbsolutePathForUnixShell(@Nonnull File file) {
        return fixAbsolutePathForUnixShell(file.getAbsolutePath());
    }


    /**
     * Returns the fixed path for the cygwin platform.
     *
     * @param path an absolute path
     * @return the fixed path
     */
    public static String fixAbsolutePathForUnixShell(@Nonnull String path) {
        if (Environment.getBuildEnvironment().isWindows()
                && path.length() > 2
                && path.charAt(1) == ':') {
            path = "/cygdrive/"
                + path.charAt(0)
                + path.substring(2);
            path = path.replace('\\', '/');
        }
        return path;
    }

   /**
    * Replace all occurences, in <code>files</code>, of <code>regex</code> with <code>replace</code>
    * @param regex    the original text to replace
    * @param replace    the placeholder
    * @param files   the files
    */
    public static void replace(Log log, @Nonnull String regex, @Nullable String replace, File...files) throws IOException {
        if(files==null) return;
        Preconditions.checkArgument(!Strings.isNullOrEmpty(regex), "regex is empty");
        replace = Strings.nullToEmpty(replace);
        for(File file : files) {
            log.debug(String.format("Replacing [%s] with [%s] in [%s]", regex, replace, file.getAbsolutePath()));
            String text = Files.toString(file, Charset.defaultCharset());
            Files.write(text.replaceAll(regex, replace), file, Charset.defaultCharset());
        }
    }
}

