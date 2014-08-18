package org.kevoree.kcl.boot.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;
import org.kevoree.kcl.view.WebView;
import org.kevoree.microkernel.impl.BootInfoImpl;
import org.kevoree.microkernel.impl.BootInfoLineImpl;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by duke on 8/12/14.
 */

@Mojo(name = "view", requiresDependencyResolution = ResolutionScope.COMPILE)
public class ViewMojo extends AbstractMojo {

    @org.apache.maven.plugins.annotations.Component
    private DependencyTreeBuilder dependencyTreeBuilder;

    @Parameter(required = true, readonly = true, defaultValue = "${project}")
    public MavenProject project;

    @Parameter(defaultValue = "${localRepository}")
    private ArtifactRepository localRepository;

    @Parameter
    public String main;

    public String buildKey(Artifact a) {
        return "mvn:" + a.getGroupId() + ":" + a.getArtifactId() + ":" + a.getBaseVersion();
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            final BootInfoImpl bootInfo = new BootInfoImpl();
            ArtifactFilter artifactFilter = new ScopeArtifactFilter(Artifact.SCOPE_COMPILE);
            DependencyNode graph = dependencyTreeBuilder.buildDependencyTree(project, localRepository, artifactFilter);
            final Map<String, String> alreadyProcess = new HashMap<String, String>();
            graph.accept(new DependencyNodeVisitor() {
                @Override
                public boolean visit(DependencyNode dependencyNode) {
                    String key = buildKey(dependencyNode.getArtifact());
                    if (!alreadyProcess.containsKey(key)) {
                        alreadyProcess.put(key, key);
                        BootInfoLineImpl bootInfoLine = new BootInfoLineImpl();
                        bootInfoLine.setUrl(key);
                        for (DependencyNode child : dependencyNode.getChildren()) {
                            bootInfoLine.getDependencies().add(buildKey(child.getArtifact()));
                        }
                        bootInfo.getLines().add(bootInfoLine);
                    }
                    return true;
                }

                @Override
                public boolean endVisit(DependencyNode dependencyNode) {
                    return true;
                }
            });
            if (main != null) {
                bootInfo.setMain(main);
            }

            WebView view = new WebView();
            view.display(bootInfo);

            getLog().info("Browse to http://localhost:8080");

            Thread.currentThread().join();

            if(java.awt.Desktop.isDesktopSupported())
            {
                java.awt.Desktop.getDesktop().browse(new URI("http://localhost:8080"));
            } else {
                getLog().info("Can't browse to");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
