package org.kevoree.microkernel.impl;

import org.kevoree.kcl.api.FlexyClassLoader;
import org.kevoree.kcl.api.FlexyClassLoaderFactory;
import org.kevoree.kcl.api.ResolutionPriority;
import org.kevoree.kcl.impl.FlexyClassLoaderImpl;
import org.kevoree.log.Log;
import org.kevoree.microkernel.BootInfo;
import org.kevoree.microkernel.BootInfoLine;
import org.kevoree.microkernel.KevoreeKernel;
import org.kevoree.resolver.MavenResolver;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.*;
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

    private ThreadGroup threadGroup;

    public KevoreeMicroKernelImpl() {
        threadGroup = new ThreadGroup("KevoreeKernel.TG");
        system.lockLinks();
        system.setKey("kcl://system");
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
        newKCL.resolutionPriority = ResolutionPriority.CHILDS;
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
        FlexyClassLoaderImpl kcl = (FlexyClassLoaderImpl) classloaders.get(key);
        if (kcl.isLocked()) {
            return;
        } else {
            classloaders.remove(key);
            for (FlexyClassLoader subs : getClassLoaders()) {
                subs.detachChild(kcl);
            }
        }
    }

    public Set<String> getSnapshotURLS() {
        Set<String> inUseURLS = new HashSet<String>();
        inUseURLS.add(ossURL);
        inUseURLS.add(centralURL);
        return inUseURLS;
    }

    public Set<String> getReleaseURLS() {
        Set<String> inUseURLS = new HashSet<String>();
        inUseURLS.add(centralURL);
        return inUseURLS;
    }

    @Override
    public FlexyClassLoader install(String key, String mavenURL) {
        if (mavenURL.equals(system.getKey())) {
            classloaders.put(key, system);
        }
        FlexyClassLoader cached = get(key);
        if (cached != null) {
            return cached;
        }
        File resolved;
        if (mavenURL.endsWith("SNAPSHOT")) {
            resolved = resolver.resolve(mavenURL, getSnapshotURLS());
        } else {

            resolved = resolver.resolve(mavenURL, getReleaseURLS());
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
    public void boot() {
        try {
            InputStream is = this.getClass().getClassLoader().getResourceAsStream("KEV-INF/bootinfo");
            boot(is);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void boot(InputStream is) {
        final BootInfo bootInfo = BootInfoBuilder.read(is);
        boot(bootInfo);
    }


    @Override
    public void boot(final BootInfo bootInfo) {
        try {
            //we install deploy units
            for (BootInfoLine line : bootInfo.getLines()) {
                if (get(line.getURL()) == null) {
                    FlexyClassLoader fcl = install(line.getURL(), line.getURL());
                    Log.trace("install {}, result={}", line.getURL(), fcl != null);
                }
            }
            //we link everything
            for (BootInfoLine line : bootInfo.getLines()) {
                FlexyClassLoader kcl = get(line.getURL());
                for (String dep : line.getDependencies()) {
                    Log.trace("Link {} -> {}", kcl.getKey(), dep);
                    kcl.attachChild(get(dep));
                }
            }
            //finally we lock everything to avoid to kill Kevoree from outside
            for (BootInfoLine line : bootInfo.getLines()) {
                FlexyClassLoaderImpl kcl = (FlexyClassLoaderImpl) get(line.getURL());
                if (kcl == null) {
                    Log.warn("Boot procedure is incomplete, the following dependency has not been installed : {}", line.getURL());
                } else {
                    kcl.lockLinks();
                }
            }
            if (bootInfo.getMain() != null) {
                final Boolean[] resl = new Boolean[1];
                resl[0] = false;
                for (final FlexyClassLoader loader : getClassLoaders()) {
                    if (!resl[0]) {
                        try {
                            final KevoreeKernel self = this;
                            Thread t = new Thread(threadGroup, threadGroup.getName() + ".boot") {
                                @Override
                                public void run() {
                                    Thread.currentThread().setContextClassLoader(loader);
                                    KevoreeKernel.self.set(self);
                                    try {
                                        Class cls = ((FlexyClassLoaderImpl) loader).loadLocalOnly(bootInfo.getMain());
                                        if (cls != null) {
                                            if (classloaders.values().contains(cls.getClassLoader())) {
                                                Method meth = cls.getMethod("main", String[].class);
                                                String[] params = new String[0];
                                                Log.trace("KevoreeKernel will execute main method on {} from {}", cls.getName(), loader.getKey());
                                                meth.invoke(null, (Object) params);
                                                resl[0] = true;
                                            }
                                        }
                                    } catch (java.lang.ClassNotFoundException e) {
                                        resl[0] = false;
                                        //NOP, we are looking for all
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        resl[0] = true; //we still have tried, so quite boot mode
                                    }
                                }
                            };
                            t.start();
                            t.join();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<FlexyClassLoader> locate(String className) {
        List<FlexyClassLoader> selected = new ArrayList<FlexyClassLoader>();
        for (final FlexyClassLoader loader : getClassLoaders()) {
            try {
                Class cls = ((FlexyClassLoaderImpl) loader).loadLocalOnly(className);
                if (cls != null) {
                    selected.add(loader);
                }
            } catch (ClassNotFoundException ignore) {
            }
        }
        return selected;
    }

    @Override
    public void stop() {
        Thread[] subThread = new Thread[Integer.MAX_VALUE];
        threadGroup.enumerate(subThread, true);
        for (Thread t : subThread) {
            if (t != null) {
                t.interrupt();
                if (t.isAlive()) { //still alive, kill it
                    t.stop();
                }
            }
        }
    }

    @Override
    public void reboot(BootInfo bootInfo) {
        stop();
        classloaders.clear();
        boot(bootInfo);
    }

    @Override
    public void reboot(InputStream is) {
        reboot(BootInfoBuilder.read(is));
    }

}
