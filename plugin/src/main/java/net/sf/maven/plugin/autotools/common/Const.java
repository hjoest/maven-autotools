package net.sf.maven.plugin.autotools.common;

/**
 * @author kedzie
 */
public interface Const {
   String INSTALLDIR_PLACEHOLDER = "@INSTALLDIR@";

   public interface ArtifactType {
      String NATIVE_HEADER_ARCHIVE = "har";
      String NATIVE_SHARED_LIBRARY = "so";
      String NATIVE_STATIC_LIBRARY = "a";
      String NATIVE_ARCHIVE = "tar.gz";
   }
}
