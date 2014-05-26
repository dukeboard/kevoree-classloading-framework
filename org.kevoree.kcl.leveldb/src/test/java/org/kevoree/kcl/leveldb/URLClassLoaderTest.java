package org.kevoree.kcl.leveldb;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Created by duke on 05/02/2014.
 */
public class URLClassLoaderTest {

    @Test
    public void test() throws ClassNotFoundException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        URLClassLoader cl = new URLClassLoader(new URL[]{this.getClass().getResource("/org.kevoree.platform.standalone-3.2.4-SNAPSHOT.jar")});
        Class clazz = cl.loadClass("org.kevoree.platform.standalone.App");
        Method meth = clazz.getMethod("main", String[].class);
        String[] params = null;
        meth.invoke(null, (Object) params);

        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}
