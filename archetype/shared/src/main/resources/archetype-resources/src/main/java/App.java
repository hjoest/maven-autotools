package ${package};

import java.io.File;


/**
 * Hello world!
 */
public class App
{

    public String getGreeting()
    {
        return getGreeting0();
    }


    private native String getGreeting0();


    public static void main( String[] args )
    {
        loadNativeLibrary();
        App app = new App();
        System.out.println( app.getGreeting() );
    }


    private static void loadNativeLibrary() {
        File nativeLibrary =
              new File("target/autotools/install/lib/i386/linux/lib${artifactId}.so");
        System.load(nativeLibrary.getAbsolutePath());
    }

}
