package org.kevoree.kcl.boot.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.kevoree.microkernel.BootInfo;

import java.io.File;
import java.io.FileWriter;

/**
 * Created by duke on 8/12/14.
 */

@Mojo(name = "generate", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class GenerateMojo extends AbstractKCLMojo {

    @Parameter(defaultValue = "${project.build.directory}/classes")
    private File outputClasses;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            BootInfo bootInfo = generate();
            new File(outputClasses.getPath() + File.separator + "KEV-INF").mkdirs();
            File fileJSON = new File(outputClasses.getPath() + File.separator + "KEV-INF" + File.separator + "bootinfo");
            FileWriter fout = new FileWriter(fileJSON);
            fout.write(bootInfo.toString());
            fout.flush();
            fout.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
