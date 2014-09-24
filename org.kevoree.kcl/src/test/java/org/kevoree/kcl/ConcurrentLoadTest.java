package org.kevoree.kcl;

import org.junit.Test;
import org.kevoree.kcl.api.FlexyClassLoader;
import org.kevoree.kcl.api.Helper;
import org.kevoree.kcl.impl.FlexyClassLoaderImpl;
import org.kevoree.log.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 19/08/13
 * Time: 11:46
 */
public class ConcurrentLoadTest implements Runnable {


    @Test
    public void testConcurrentLoad() throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 4);
        for (int i = 0; i < Runtime.getRuntime().availableProcessors() * 4; i++) {
            pool.submit(new ConcurrentLoadTest());
        }
        pool.awaitTermination(1000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void run() {


        try {
            System.out.println("Perform simple KCL Test");
            FlexyClassLoader jar = new FlexyClassLoaderImpl();
            jar.load(Helper.stream2File(this.getClass().getClassLoader().getResourceAsStream("org.kevoree.kcl.jar"),"org.kevoree.kcl.jar"));

            FlexyClassLoader jarLog = new FlexyClassLoaderImpl();
            jarLog.load(Helper.stream2File(this.getClass().getClassLoader().getResourceAsStream("org.kevoree.log.jar"),"org.kevoree.log.jar"));

            jar.attachChild(jarLog);

            Class resolvedClass = jar.loadClass("org.kevoree.kcl.impl.FlexyClassLoaderImpl");
            assert (resolvedClass.getClassLoader().equals(jar));

            Class resolvedLogClass = jarLog.loadClass(Log.class.getName());  //std resolution of class
            assert (resolvedLogClass.getClassLoader().equals(jarLog));

            Class resolvedLogClassTransitive = jar.loadClass(Log.class.getName());
            assert (resolvedLogClassTransitive.getClassLoader().equals(jarLog)); // Log class should be resolved from the new KCL
            //TEst the transitive link
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
