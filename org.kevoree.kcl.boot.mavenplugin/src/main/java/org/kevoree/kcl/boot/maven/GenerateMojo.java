package org.kevoree.kcl.boot.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;
import org.kevoree.microkernel.impl.BootInfoImpl;
import org.kevoree.microkernel.impl.BootInfoLineImpl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by duke on 8/12/14.
 */

@Mojo(name = "generate", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class GenerateMojo extends AbstractMojo {

    @org.apache.maven.plugins.annotations.Component
    private DependencyTreeBuilder dependencyTreeBuilder;

    @Parameter(required = true, readonly = true, defaultValue = "${project}")
    public MavenProject project;

    @Parameter(defaultValue = "${localRepository}")
    private ArtifactRepository localRepository;

    @Parameter(defaultValue = "${project.build.directory}/classes")
    private File outputClasses;

    public String buildKey(Artifact a) {
        return "mvn:"+a.getGroupId() + ":" + a.getArtifactId() + ":" + a.getBaseVersion();
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
