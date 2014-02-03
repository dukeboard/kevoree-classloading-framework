package org.kevoree.kcl;

import org.junit.Test;
import org.kevoree.kcl.api.FlexyClassLoader;
import org.kevoree.kcl.api.FlexyClassLoaderFactory;
import org.kevoree.kcl.impl.FlexyClassLoaderImpl;
import org.kevoree.log.Log;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 19/08/13
 * Time: 11:47
 */
public class SimpleTest {

    @Test
    public void simpleTest() throws IOException, ClassNotFoundException {
        System.out.println("Perform simple KCL Test");
        FlexyClassLoader jar = FlexyClassLoaderFactory.INSTANCE.create();
        jar.load(this.getClass().getClassLoader().getResourceAsStream("org.kevoree.kcl.jar"));
        Class resolvedClass = jar.loadClass("org.kevoree.kcl.impl.FlexyClassLoaderImpl");

        System.out.println(resolvedClass.getClassLoader());

        assert (resolvedClass.getClassLoader().equals(jar));

        Class resolvedLogClass = jar.loadClass(Log.class.getName());
        assert (!resolvedLogClass.getClassLoader().equals(jar)); // Log class should be resolved from the System ClassLoader nor the new KCL (no binding)

    }

    @Test
    public void linkedTest() throws IOException, ClassNotFoundException {

        FlexyClassLoaderImpl systemEnabledKCL = new FlexyClassLoaderImpl();

        System.out.println("Perform simple KCL Test");
        FlexyClassLoader jar = new FlexyClassLoaderImpl();
        //jar.isolateFromSystem();
        jar.load(this.getClass().getClassLoader().getResourceAsStream("org.kevoree.kcl.jar"));
        systemEnabledKCL.attachChild(jar);

        FlexyClassLoaderImpl jarLog = new FlexyClassLoaderImpl();
        jarLog.load(this.getClass().getClassLoader().getResourceAsStream("org.kevoree.log.jar"));
        //jarLog.isolateFromSystem();
        systemEnabledKCL.attachChild(jarLog);

        jar.attachChild(jarLog);

        Class resolvedClass = systemEnabledKCL.loadClass("org.kevoree.kcl.impl.FlexyClassLoaderImpl");
        //assert (resolvedClass.getClassLoader().equals(systemEnabledKCL));

        Class resolvedLogClass = systemEnabledKCL.loadClass(Log.class.getName());  //std resolution of class
        // assert (resolvedLogClass.getClassLoader().equals(jarLog));

        Class resolvedLogClassTransitive = systemEnabledKCL.loadClass(Log.class.getName());
        //assert (resolvedLogClassTransitive.getClassLoader().equals(jarLog)); // Log class should be resolved from the new KCL
        //TEst the transitive link


        InputStream stream = systemEnabledKCL.getResourceAsStream("META-INF/maven/org.kevoree.kcl/org.kevoree.kcl/pom.xml");
        System.out.println(stream);

    }

}
