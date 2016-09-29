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
            System.out.println("Perform concurrent KCL test");
            FlexyClassLoader fcl0 = new FlexyClassLoaderImpl();
            fcl0.load(Helper.stream2File(
                    this.getClass().getClassLoader().getResourceAsStream("org.kevoree.kcl.jar"),
                    "org.kevoree.kcl.jar"));

            FlexyClassLoader fcl1 = new FlexyClassLoaderImpl();
            fcl1.load(Helper.stream2File(
                    this.getClass().getClassLoader().getResourceAsStream("org.kevoree.log.jar"),
                    "org.kevoree.log.jar"));

            fcl0.attachChild(fcl1);

            Class resolvedClass = fcl0.loadClass(FlexyClassLoaderImpl.class.getName());
            assert (resolvedClass.getClassLoader().equals(fcl0));

            Class resolvedLogClass = fcl1.loadClass(Log.class.getName());  //std resolution of class
            assert (resolvedLogClass.getClassLoader().equals(fcl1));

            Class resolvedLogClassTransitive = fcl0.loadClass(Log.class.getName());
            assert (resolvedLogClassTransitive.getClassLoader().equals(fcl1)); // Log class should be resolved from the new KCL
            //TEst the transitive link
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
