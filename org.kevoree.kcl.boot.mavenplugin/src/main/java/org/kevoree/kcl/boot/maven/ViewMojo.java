package org.kevoree.kcl.boot.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.kevoree.kcl.view.WebView;
import org.kevoree.microkernel.BootInfo;

import java.net.URI;

/**
 * Created by duke on 8/12/14.
 */

@Mojo(name = "view", requiresDependencyResolution = ResolutionScope.COMPILE)
public class ViewMojo extends AbstractKCLMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            BootInfo bootInfo = generate();
            WebView view = new WebView();
            view.display(bootInfo);
            getLog().info("Browse to http://localhost:8080");
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().browse(new URI("http://localhost:8080"));
            }
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
