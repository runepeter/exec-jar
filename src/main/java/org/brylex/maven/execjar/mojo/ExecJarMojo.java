package org.brylex.maven.execjar.mojo;

import com.google.common.io.ByteStreams;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.IOUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
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
    private MavenProjectHelper projectHelper;

    @Parameter(defaultValue = "${localRepository}", readonly = true, required = true)
    private ArtifactRepository localRepository;

    @Parameter(defaultValue = "${project.build.directory}", required = true)
    private File outputDirectory;

    @Parameter(defaultValue = "${project.build.finalName}", required = true, readonly = true)
    private String finalName;

    @Parameter(name = "mainClass", required = true)
    private String mainClass;

    @Parameter(name = "binary", required = false, defaultValue = "false")
    private boolean binary;

    private List<ArtifactRepository> repositories;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info("\n### Executable JAR Plugin ###\n\n");
        getLog().info("     Artifact: " + project.getArtifact());
        getLog().info("   Main class: " + mainClass);

        if (!project.getArtifact().getType().equalsIgnoreCase("jar")) {
            throw new MojoFailureException("Uber JAR plugin can ONLY be used on JAR-type artifacts.");
        }

        final Path jarPath = new File(outputDirectory, finalName + "-uber.jar").toPath();
        final File outputFile = jarPath.toFile();
        outputDirectory.mkdirs();

        Manifest manifest = new Manifest();
        Attributes global = manifest.getMainAttributes();
        global.put(Attributes.Name.MANIFEST_VERSION, project.getVersion());
        global.put(Attributes.Name.MAIN_CLASS, "org.brylex.maven.execjar.MainDelegator");

        try (JarOutputStream os = new JarOutputStream(new FileOutputStream(outputFile), manifest)) {

            os.putNextEntry(new JarEntry("META-INF/"));
            os.putNextEntry(new JarEntry("META-INF/jars/"));

            addArtifact(project.getArtifact(), os);
            for (Artifact artifact : project.getArtifacts()) {
                addArtifact(artifact, os);
            }

            for (Artifact artifact : project.getPluginArtifacts()) {
                if (artifact.getArtifactId().equals("exec-jar")) {

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

            JarEntry entry = new JarEntry("META-INF/exec-jar.properties");
            os.putNextEntry(entry);
            Properties properties = new Properties();
            properties.setProperty("mainClass", mainClass);
            properties.store(os, "Created by User JAR Plugin");
            getLog().info("Successfully created Uber JAR [" + jarPath + "].");

            projectHelper.attachArtifact(project, "jar", "uber", jarPath.toFile());

        } catch (Exception e) {
            throw new MojoExecutionException("Unable to create executable JAR.", e);
        }

        if (binary) {
            final Path binPath = new File(outputDirectory, finalName + "-bin").toPath();
            try (OutputStream os = Files.newOutputStream(binPath, StandardOpenOption.APPEND, StandardOpenOption.CREATE_NEW)) {

                try (InputStream is = ExecJarMojo.class.getResourceAsStream("/stub.sh")) {
                    ByteStreams.copy(is, os);
                }

                Files.copy(jarPath, os);
                Files.setPosixFilePermissions(binPath, PosixFilePermissions.fromString("rwxr-xr-x"));

                getLog().info("Successfully created Linux binary [" + binPath + "].");

                projectHelper.attachArtifact(project, "bin", binPath.toFile());

            } catch (IOException e) {

                throw new MojoExecutionException("Unable to create Linux binary.", e);
            }
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
