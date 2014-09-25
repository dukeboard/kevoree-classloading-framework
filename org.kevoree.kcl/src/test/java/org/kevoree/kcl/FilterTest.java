package org.kevoree.kcl;

import org.junit.Test;
import org.kevoree.kcl.api.FlexyClassLoader;
import org.kevoree.kcl.api.Helper;
import org.kevoree.kcl.impl.FlexyClassLoaderImpl;
import org.kevoree.kcl.impl.ProxyClassLoaderImpl;

import java.io.IOException;

/**
 * Created by duke on 9/25/14.
 */
public class FilterTest {

    @Test
    public void linkedTest() throws IOException, ClassNotFoundException {

        FlexyClassLoaderImpl systemEnabledKCL = new FlexyClassLoaderImpl();
        FlexyClassLoader jar = new FlexyClassLoaderImpl();
        jar.load(Helper.stream2File(this.getClass().getClassLoader().getResourceAsStream("org.kevoree.kcl.jar"), "org.kevoree.kcl.jar"));
        systemEnabledKCL.attachChild(jar);
        FlexyClassLoaderImpl jarLog = new FlexyClassLoaderImpl();
        jarLog.load(Helper.stream2File(this.getClass().getClassLoader().getResourceAsStream("org.kevoree.log.jar"), "org.kevoree.log.jar"));
        systemEnabledKCL.attachChild(jarLog);
        jar.attachChild(jarLog);


        ProxyClassLoaderImpl filteredKCL = new ProxyClassLoaderImpl().addFilter("org.kevoree.kcl");
        Class rf2=null;
        try {
            rf2 = filteredKCL.loadClass("org.kevoree.kcl.impl.FlexyClassLoaderImpl");
        } catch (Exception e) {
        }
        assert (rf2 == null);
    }

}
