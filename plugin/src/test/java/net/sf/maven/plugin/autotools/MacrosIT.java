package net.sf.maven.plugin.autotools;

import io.takari.maven.testing.TestResources;
import io.takari.maven.testing.executor.MavenExecutionResult;
import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.junit.MavenJUnitTestRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;


/**
 * Integration test for macros
 */
@RunWith(MavenJUnitTestRunner.class)
@MavenVersions({"3.2.5"})
public class MacrosIT {
   
   @Rule
   public final TestResources resources = new TestResources();

   public final MavenRuntime mavenRuntime;

   public MacrosIT(MavenRuntime.MavenRuntimeBuilder builder) throws Exception {
       this.mavenRuntime = builder.withCliOptions( "-X" ).build();
   }

   @Test
   public void buildDeployAndRun() throws Exception {
       File basedir = resources.getBasedir("macros");
       MavenExecutionResult result = mavenRuntime
               .forProject(basedir)
               .execute("clean",
                       "install");

       result.assertErrorFreeLog();
   }


}
