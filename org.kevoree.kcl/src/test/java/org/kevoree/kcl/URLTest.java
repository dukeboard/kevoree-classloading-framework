package org.kevoree.kcl;

import org.junit.Test;
import org.kevoree.kcl.api.FlexyClassLoader;
import org.kevoree.kcl.api.FlexyClassLoaderFactory;
import org.kevoree.kcl.api.Helper;
import org.kevoree.log.Log;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Created by duke on 9/24/14.
 */
public class URLTest {

    @Test
    public void simpleTestFile() throws IOException, ClassNotFoundException {
        FlexyClassLoader jar = FlexyClassLoaderFactory.INSTANCE.create();
        File f = Helper.stream2File(this.getClass().getClassLoader().getResourceAsStream("org.kevoree.library.java.editor-5.0.5-SNAPSHOT.jar"), "org.kevoree.library.java.editor-5.0.5-SNAPSHOT.jar");
        jar.load(f);
        URL resolved = jar.getResource("images/");
        assert (resolved != null);
        System.out.println(resolved + "-" + resolved.getPath() + "-" + resolved.openStream().available());
    }

    @Test
    public void simpleTestFileClass() throws IOException, ClassNotFoundException {
        FlexyClassLoader jar = FlexyClassLoaderFactory.INSTANCE.create();
        File f = Helper.stream2File(this.getClass().getClassLoader().getResourceAsStream("org.kevoree.kcl.jar"), "org.kevoree.kcl.jar");
        jar.load(f);
        Class resolvedClass = jar.loadClass("org.kevoree.kcl.impl.FlexyClassLoaderImpl");
        System.out.println(resolvedClass.getClassLoader());
        assert (resolvedClass.getClassLoader().equals(jar));
        Class resolvedLogClass = jar.loadClass(Log.class.getName());
        assert (!resolvedLogClass.getClassLoader().equals(jar)); // Log class should be resolved from the System ClassLoader nor the new KCL (no binding)
    }

    /*
    @Test
    public void simpleTestDir() throws IOException, ClassNotFoundException {
        FlexyClassLoader jar = FlexyClassLoaderFactory.INSTANCE.create();
        jar.load(new File("/Users/duke/Documents/dev/dukeboard/kevoree-classloading-framework/org.kevoree.kcl/src/test/resources/org.kevoree.kcl"));
        Class resolvedClass = jar.loadClass("org.kevoree.kcl.impl.FlexyClassLoaderImpl");
        System.out.println(resolvedClass.getClassLoader());
        assert (resolvedClass.getClassLoader().equals(jar));
        Class resolvedLogClass = jar.loadClass(Log.class.getName());
        assert (!resolvedLogClass.getClassLoader().equals(jar)); // Log class should be resolved from the System ClassLoader nor the new KCL (no binding)
        URL resolved = jar.getResource("META-INF/");
        System.out.println(resolved.getPath());
    } */

}
