package org.brylex.maven.execjar.mojo;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;

import java.io.File;

import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

/**
 * Created by <a href="mailto:rpbjo@nets.eu">Rune Peter Bjørnstad</a> on 02/04/2017.
 */
public class ExecJarMojoTest extends AbstractMojoTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testName() throws Exception {


        final File pomFile = new File("src/test/resources/project-1/pom.xml");

        Mojo uber = lookupMojo("exec-jar", pomFile);
        assertNotNull(uber);

        Mojo mojo = configureMojo(uber, "exec-jar", pomFile);
        setVariableValueToObject(mojo, "project", getMockMavenProject());

        //mojo.execute();
    }

    private MavenProject getMockMavenProject()
    {
        MavenProject mp = new MavenProject();
        mp.getBuild().setDirectory( "target" );
        mp.getBuild().setOutputDirectory( "target/classes" );
        mp.getBuild().setSourceDirectory( "src/main/java" );
        mp.getBuild().setTestOutputDirectory( "target/test-classes" );
        return mp;
    }

}
