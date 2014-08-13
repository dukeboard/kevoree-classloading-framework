package org.kevoree.microkernel.impl;

import org.kevoree.kcl.api.FlexyClassLoader;
import org.kevoree.kcl.api.FlexyClassLoaderFactory;
import org.kevoree.kcl.impl.FlexyClassLoaderImpl;
import org.kevoree.log.Log;
import org.kevoree.microkernel.BootInfo;
import org.kevoree.microkernel.BootInfoLine;
import org.kevoree.microkernel.KevoreeKernel;
import org.kevoree.resolver.MavenResolver;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by duke on 8/12/14.
 */
public class KevoreeMicroKernelImpl implements KevoreeKernel {

    private Map<String, FlexyClassLoader> classloaders = new ConcurrentHashMap<String, FlexyClassLoader>();
    private MavenResolver resolver = new MavenResolver();

    private static final String centralURL = "http://repo1.maven.org/maven2";
    private static final String ossURL = "https://oss.sonatype.org/content/groups/public";
    private static final FlexyClassLoaderImpl system = new FlexyClassLoaderImpl();

    public KevoreeMicroKernelImpl() {
        system.lockLinks();
    }

    @Override
    public FlexyClassLoader get(String key) {
        if (key.contains("org.kevoree.kcl") || key.contains("org.kevoree.maven.resolver")) {
            return system;
        }
        return classloaders.get(key);
    }

    @Override
    public FlexyClassLoader put(String key, File in) {
        FlexyClassLoader cached = get(key);
        if (cached != null) {
            return cached;
        }
        FlexyClassLoader newKCL = FlexyClassLoaderFactory.INSTANCE.create();
        newKCL.setKey(key);
        try {
            FileInputStream fop = new FileInputStream(in);
            newKCL.load(fop);
            fop.close();
        } catch (Exception e) {
            Log.error("Error while open param file in KevoreeMicroKernel", e);
            return null;
        }
        classloaders.put(key, newKCL);
        return newKCL;
    }

    @Override
    public void drop(String key) {
        classloaders.remove(key);
    }

    @Override
    public FlexyClassLoader install(String key, String mavenURL) {
        FlexyClassLoader cached = get(key);
        if (cached != null) {
            return cached;
        }
        File resolved;
        if (mavenURL.endsWith("SNAPSHOT")) {
            Set<String> inUseURLS = new HashSet<String>();
            inUseURLS.add(ossURL);
            inUseURLS.add(centralURL);
            resolved = resolver.resolve(mavenURL, inUseURLS);
        } else {
            Set<String> inUseURLS = new HashSet<String>();
            inUseURLS.add(centralURL);
            resolved = resolver.resolve(mavenURL, inUseURLS);
        }
        if (resolved != null) {
            return put(key, resolved);
        } else {
            return null;
        }
    }

    @Override
    public MavenResolver getResolver() {
        return resolver;
    }

    @Override
    public java.util.Collection<FlexyClassLoader> getClassLoaders() {
        return classloaders.values();
    }

    @Override
    public boolean boot() {
        try {
            InputStream is = this.getClass().getClassLoader().getResourceAsStream("KEV-INF/bootinfo");
            BootInfo bootInfo = BootInfoBuilder.read(is);
            //we install deploy units
            for (BootInfoLine line : bootInfo.getLines()) {
                if (get(line.getURL()) == null) {
                    install(line.getURL(), line.getURL());
                }
            }
            //we link everything
            for (BootInfoLine line : bootInfo.getLines()) {
                FlexyClassLoader kcl = get(line.getURL());
                for (String dep : line.getDependencies()) {
                    kcl.attachChild(get(dep));
                }
            }
            //finally we lock everything to avoid to kill Kevoree from outside
            for (BootInfoLine line : bootInfo.getLines()) {
                FlexyClassLoaderImpl kcl = (FlexyClassLoaderImpl) get(line.getURL());
                kcl.lockLinks();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


}
