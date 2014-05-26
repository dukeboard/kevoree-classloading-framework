package org.kevoree.kcl.leveldb;

import org.junit.Test;
import org.kevoree.kcl.api.FlexyClassLoader;
import org.kevoree.kcl.api.FlexyClassLoaderFactory;
import org.kevoree.kcl.api.ResolutionPriority;
import org.kevoree.kcl.impl.FlexyClassLoaderImpl;
import org.kevoree.kcl.indexdb.leveldb.LevelDBStore;
import org.kevoree.log.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 19/08/13
 * Time: 11:47
 */
public class SimpleTest {

    @Test
    public void simpleTest() throws IOException, ClassNotFoundException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {

        Log.setPrintCaller(true);

        System.out.println("Perform simple KCL Test");

        Path temp = Files.createTempDirectory("KCLTEST");
        File tempDB = temp.toFile();

        System.out.println(temp);

        final LevelDBStore db = new LevelDBStore(tempDB);

        FlexyClassLoader jar = new FlexyClassLoaderImpl(db);
        jar.resolutionPriority = ResolutionPriority.CHILDS;

        jar.load(this.getClass().getClassLoader().getResourceAsStream("org.kevoree.platform.standalone-3.2.4-SNAPSHOT.jar"));
        Class clazz = jar.loadClass("org.kevoree.platform.standalone.App");
        Method meth = clazz.getMethod("main", String[].class);
        String[] params = null;
        meth.invoke(null, (Object) params);

        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        db.close();

    }

}
