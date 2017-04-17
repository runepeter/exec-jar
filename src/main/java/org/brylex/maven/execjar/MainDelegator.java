package org.brylex.maven.execjar;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Properties;

/**
 * Created by <a href="mailto:rpbjo@nets.eu">Rune Peter Bjørnstad</a> on 01/04/2017.
 */
public class MainDelegator {
    public static void main(String[] args) throws Exception {

        try (InputStream is = MainDelegator.class.getResourceAsStream("/META-INF/exec-jar.properties")) {

            Properties properties = new Properties();
            properties.load(is);

            JarClassLoader jarClassLoader = new JarClassLoader(MainDelegator.class.getClassLoader());

            Class mainClass = jarClassLoader.loadClass(properties.getProperty("mainClass"));
            Method mainMethod = mainClass.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) args);
        }
    }
}
