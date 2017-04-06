package org.brylex.maven.execjar;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created by <a href="mailto:rpbjo@nets.eu">Rune Peter Bjørnstad</a> on 01/04/2017.
 */
public class JarClassLoader extends URLClassLoader {

    public JarClassLoader(ClassLoader systemClassLoader) {
        super(urls(getUrls(systemClassLoader), url("file://./etc/")), systemClassLoader.getParent());
    }

    private static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean isServerJar(String fileName) {
        return fileName != null && fileName.startsWith("META-INF/jars/") && fileName.toLowerCase().endsWith(".jar");
    }

    private static File jarEntryAsFile(JarFile jarFile, JarEntry jarEntry) throws IOException {

        InputStream input = null;
        OutputStream output = null;
        try {
            String name = jarEntry.getName().replace('/', '_');
            int i = name.lastIndexOf(".");
            String extension = i > -1 ? name.substring(i) : "";
            File file = File.createTempFile(name.substring(0, name.length() - extension.length()) + ".", extension);
            file.deleteOnExit();

            input = jarFile.getInputStream(jarEntry);
            output = new FileOutputStream(file);
            int readCount;
            byte[] buffer = new byte[4096];
            while ((readCount = input.read(buffer)) != -1) {
                output.write(buffer, 0, readCount);
            }
            return file;
        } finally {
            close(input);
            close(output);
        }
    }

    private static URL[] urls(URL[] urls, URL... additionalUrls) {


        List<URL> list = new ArrayList<URL>(Arrays.asList(urls));
        try {
            ProtectionDomain protectionDomain = JarClassLoader.class.getProtectionDomain();
            CodeSource codeSource = protectionDomain.getCodeSource();
            URL rootJarUrl = codeSource.getLocation();

            File warFile = new File(rootJarUrl.getPath());
            JarFile jarFile = new JarFile(warFile);

            Enumeration<JarEntry> jarEntries = jarFile.entries();
            while (jarEntries.hasMoreElements()) {
                JarEntry jarEntry = jarEntries.nextElement();
                if (isServerJar(jarEntry.getName())) {
                    File file = jarEntryAsFile(jarFile, jarEntry);
                    list.add(file.toURI().toURL());
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Unable to instantiate server classloader.", e);
        }


        for (URL additionalUrl : additionalUrls) {
            list.add(additionalUrl);
        }

        URL[] array = new URL[list.size()];
        return list.toArray(array);
    }

    private static URL url(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static URL[] getUrls(ClassLoader cl) {
        return ((URLClassLoader) cl).getURLs();
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        try {
            Class<?> clazz = findLoadedClass(name);
            if (clazz == null) {
                clazz = findClass(name);
                if (resolve)
                    resolveClass(clazz);
            }
            return clazz;
        } catch (ClassNotFoundException e) {
            return super.loadClass(name, resolve);
        }
    }
}
