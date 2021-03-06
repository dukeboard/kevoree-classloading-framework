package org.kevoree.kcl.boot.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;
import org.kevoree.microkernel.BootInfo;
import org.kevoree.microkernel.BootInfoLine;
import org.kevoree.microkernel.impl.BootInfoImpl;
import org.kevoree.microkernel.impl.BootInfoLineImpl;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by duke on 8/18/14.
 */
public abstract class AbstractKCLMojo extends org.apache.maven.plugin.AbstractMojo {

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

    public BootInfo generate() throws DependencyTreeBuilderException {
        final BootInfoImpl bootInfo = new BootInfoImpl();
        ArtifactFilter artifactFilter = new ScopeArtifactFilter(Artifact.SCOPE_COMPILE_PLUS_RUNTIME);
        DependencyNode graph = dependencyTreeBuilder.buildDependencyTree(project, localRepository, artifactFilter);
        final Map<String, String> alreadyProcess = new HashMap<String, String>();
        graph.accept(new DependencyNodeVisitor() {
            @Override
            public boolean visit(DependencyNode dependencyNode) {
                if ("test".equalsIgnoreCase(dependencyNode.getArtifact().getScope())) {
                    return false;
                } else {
                    final String key = buildKey(dependencyNode.getArtifact());
                    BootInfoLine bootInfoLine = null;
                    for (BootInfoLine line : bootInfo.getLines()) {
                        if (line.getURL().equals(key)) {
                            bootInfoLine = line;
                        }
                    }
                    if (bootInfoLine == null) {
                        bootInfoLine = new BootInfoLineImpl();
                        ((BootInfoLineImpl) bootInfoLine).setUrl(key);
                        bootInfo.getLines().add(bootInfoLine);
                    }
                    for (DependencyNode child : dependencyNode.getChildren()) {
                        if (!"test".equalsIgnoreCase(child.getArtifact().getScope())) {
                            bootInfoLine.getDependencies().add(buildKey(child.getArtifact()));
                        }
                    }
                    return true;
                }
            }

            @Override
            public boolean endVisit(DependencyNode dependencyNode) {
                return true;
            }
        });
        if (main != null) {
            bootInfo.setMain(main);
        }
        return bootInfo;
    }


}
