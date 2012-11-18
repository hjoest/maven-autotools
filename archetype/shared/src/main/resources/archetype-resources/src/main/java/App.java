package ${package};

import java.io.File;

/**
 * Hello world!
 */
public class App {

    public String getGreeting() {
        return getGreeting0();
    }

    private native String getGreeting0();

    public static void main(String[] args) {
        loadNativeLibrary();
        App app = new App();
        System.out.println(app.getGreeting());
    }

    private static void loadNativeLibrary() {
        File nativeLibrary =
              new File("target/autotools/install/lib/"
                        + getSystemArchitecture() + "/"
                        + getOperatingSystem() + "/"
                        + "lib${artifactId}" + getSharedExt());
        System.load(nativeLibrary.getAbsolutePath());
    }

    private static String getOperatingSystem() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.startsWith("windows")) {
            os = "windows";
        }
        return os.replaceAll("[ /]", "");
    }

    private static String getSystemArchitecture() {
        String arch = System.getProperty("os.arch").toLowerCase();
        if (arch.equals("i386")) {
           arch = "x86";
        } else if (arch.equals("amd64")) {
           arch = "x86_64";
        } else if (arch.equals("powerpc")) {
           arch = "ppc";
        }
        return arch;
    }

    private static String getSharedExt() {
        String os = getOperatingSystem();
        if ("windows".equals(os)) {
            return ".dll";
        } else if ("macosx".equals(os)) {
            return ".dylib";
        }
        return ".so";
    }

}
