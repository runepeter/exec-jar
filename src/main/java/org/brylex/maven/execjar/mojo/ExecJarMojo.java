package org.brylex.maven.execjar.mojo;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.IOUtil;

import java.io.*;
import java.util.List;
import java.util.Properties;
import java.util.jar.*;

/**
 * Created by <a href="mailto:rpbjo@nets.eu">Rune Peter Bjørnstad</a> on 01/04/2017.
 */
@Mojo(name = "exec-jar", threadSafe = true, defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class ExecJarMojo extends AbstractMojo implements Contextualizable {

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Component
    private DependencyGraphBuilder dependencyGraphBuilder;

    @Parameter(defaultValue = "${localRepository}", readonly = true, required = true)
    private ArtifactRepository localRepository;

    @Parameter(name = "mainClass", required = true)
    private String mainClass;

    private List<ArtifactRepository> repositories;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info("\n### Executable JAR Plugin ###\n\n");
        getLog().info("   MAIN: " + mainClass);

        final File outputFile = new File("target/jalla.jar");
        outputFile.getParentFile().mkdirs();

        Manifest manifest = new Manifest();
        Attributes global = manifest.getMainAttributes();
        global.put(Attributes.Name.MANIFEST_VERSION, project.getVersion());
        global.put(Attributes.Name.MAIN_CLASS, "org.brylex.maven.execjar.MainDelegate");

        try (JarOutputStream os = new JarOutputStream(new FileOutputStream(outputFile), manifest)) {

            os.putNextEntry(new JarEntry("META-INF/"));
            os.putNextEntry(new JarEntry("META-INF/jars/"));

            addArtifact(project.getArtifact(), os);
            for (Artifact artifact : project.getArtifacts()) {
                addArtifact(artifact, os);
            }

            for (Artifact artifact : project.getPluginArtifacts()) {
                if (artifact.getArtifactId().equals("execjar-jar")) {

                    artifact = localRepository.find(artifact);

                    try (JarInputStream jis = new JarInputStream(new FileInputStream(artifact.getFile()))) {
                        JarEntry entry;
                        while ((entry = jis.getNextJarEntry()) != null) {
                            if (entry.getName().endsWith(".class") && !entry.getName().endsWith("Mojo.class")) {

                                JarEntry jj = new JarEntry(entry.getName());
                                jj.setTime(entry.getTime());
                                os.putNextEntry(jj);
                                IOUtil.copy(jis, os);
                            }
                        }
                    }
                }
            }

            JarEntry entry = new JarEntry("META-INF/execjar-jar.properties");
            os.putNextEntry(entry);
            Properties properties = new Properties();
            properties.setProperty("mainClass", "Balla");
            properties.store(os, "JALLA");

        } catch (Exception e) {
            throw new MojoExecutionException("Unable to create execjar-jar.", e);
        }
    }

    private void addArtifact(Artifact artifact, JarOutputStream target) throws IOException {

        final String name = "META-INF/jars/" + artifact.getFile().getName();

        JarEntry entry = new JarEntry(name);
        entry.setTime(artifact.getFile().lastModified());
        target.putNextEntry(entry);

        try (InputStream is = new BufferedInputStream(new FileInputStream(artifact.getFile()))) {
            IOUtil.copy(is, target);
        }
    }

    @Override
    public void contextualize(Context context) throws ContextException {

    }
}
