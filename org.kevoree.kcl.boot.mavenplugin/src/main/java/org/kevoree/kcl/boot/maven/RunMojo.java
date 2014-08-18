package org.kevoree.kcl.boot.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.kevoree.microkernel.BootInfo;
import org.kevoree.microkernel.KevoreeKernel;
import org.kevoree.microkernel.impl.KevoreeMicroKernelImpl;

/**
 * Created by duke on 8/12/14.
 */

@Mojo(name = "run", requiresDependencyResolution = ResolutionScope.NONE)
public class RunMojo extends AbstractKCLMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            BootInfo bootInfo = generate();
            KevoreeKernel kernel = new KevoreeMicroKernelImpl();
            kernel.boot(bootInfo);
            getLog().info("KevoreeKernel boot executed");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
