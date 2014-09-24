package org.kevoree.kcl.impl;

import org.kevoree.kcl.api.FlexyClassLoader;
import org.kevoree.kcl.api.IndexDB;
import org.kevoree.kcl.api.ResolutionPriority;
import org.kevoree.log.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by duke on 10/01/2014.
 */
public class FlexyClassLoaderImpl extends FlexyClassLoader {

    @Override
    public Class loadClass(String className) throws ClassNotFoundException {
        return loadClass(className, true);
    }

    public Class loadLocalOnly(String className) throws ClassNotFoundException {
        Class result = getLoadedClass(className);
        if (result == null) {
            byte[] bytes = loadClassBytes(className);
            if (bytes != null) {
                synchronized (lock) {
                    result = getLoadedClass(className);
                    if (result == null) {
                        result = internal_defineClass(className, bytes);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public Class loadClass(String className, boolean resolveIt) throws ClassNotFoundException {

        KlassLoadRequest request = new KlassLoadRequest();
        request.className = className;
        Class result = internal_loadClass(request);
        //Still not found, if thread current thread is a FlexyClassLoader try also
        if (result == null) {
            ClassLoader threadContextCL = Thread.currentThread().getContextClassLoader();
            if (threadContextCL instanceof FlexyClassLoaderImpl) {
                FlexyClassLoaderImpl castedCL = (FlexyClassLoaderImpl) threadContextCL;
                if (!request.passedKlassLoader.contains((castedCL).getKey())) {
                    result = (castedCL).internal_loadClass(request);
                }
            }
        }
        if (result == null && resolutionPriority.equals(ResolutionPriority.CHILDS)) {
            ClassLoader parentCL = getParent();
            if (parentCL != null && parentCL != ClassLoader.getSystemClassLoader()) {
                if (parentCL instanceof FlexyClassLoaderImpl) {
                    FlexyClassLoaderImpl parentCastedCL = (FlexyClassLoaderImpl) parentCL;
                    result = parentCastedCL.internal_loadClass(request);
                } else {
                    try {
                        result = parentCL.loadClass(request.className);
                    } catch (ClassNotFoundException e) {
                    }
                }
            }
            if (result == null) {
                ClassLoader current = this.getClass().getClassLoader();
                if (current != parentCL) {
                    if (current instanceof FlexyClassLoaderImpl) {
                        FlexyClassLoaderImpl currentCasted = (FlexyClassLoaderImpl) current;
                        result = currentCasted.internal_loadClass(request);
                    } else {
                        try {
                            result = current.loadClass(request.className);
                        } catch (ClassNotFoundException e) {
                        }
                    }
                }
            }
        }


        //If still not found and not system basic class, lets try again
        if (result == null && !request.className.startsWith("java") && !request.className.startsWith("javax")/*dangerous optimization*/) {
            try {
                result = findSystemClass(request.className);
            } catch (ClassNotFoundException e) {
            }
        }
        if (result == null) {
            if (Log.TRACE) {
                Log.trace("KCL Class not resolved " + className + " from " + this.key);
                Log.trace("Passed FlexClassLoader, childs : " + getSubClassLoaders().size());
                for (String klassLoader : request.passedKlassLoader) {
                    Log.trace("-->" + klassLoader);
                }
                if (Thread.currentThread().getContextClassLoader() instanceof FlexyClassLoader) {
                    Log.trace("Thread current KCL: {}", ((FlexyClassLoader) Thread.currentThread().getContextClassLoader()).getKey());
                } else {
                    if (Thread.currentThread().getContextClassLoader() != null) {
                        Log.trace("Thread current : {}", Thread.currentThread().getContextClassLoader().toString());
                    } else {
                        Log.trace("Thread current CL is null !");
                    }
                }
            }
            throw new ClassNotFoundException(className);
        }
        return result;
    }

    @Override
    public void load(File directory) throws IOException {
        if (directory != null) {
            classpathResources.loadJar(directory);
        } else {
            Log.error("Can't add null stream");
        }
    }

    @Override
    public void attachChild(FlexyClassLoader child) {
        if (child != null) {
            if (!locked) {
                if (!subClassLoaders.contains(child)) {
                    subClassLoaders.add(child);
                }
            }
        } else {
            Log.error("Can't add null child classloader");
        }
    }

    @Override
    public void detachChild(FlexyClassLoader child) {
        if (child != null) {
            cleanupLinks(child);
        }
    }

    protected IndexDB classpathResources = null;

    public FlexyClassLoaderImpl() {
        classpathResources = new JarIndexDB(this);
    }

    public FlexyClassLoaderImpl(IndexDB db) {
        classpathResources = db;
    }

    /* Native Library Management */
    private HashMap<String, String> nativeMap = new HashMap<String, String>();

    /* Explicitly declare Native Mapping */
    public void addNativeMapping(String name, String url) {
        nativeMap.put(name, url);
    }

    /* Check if explicit mapping found, otherwise call standard method */
    public String findLibrary(String p1) {
        //TODO automatic resolution of all .so, .dll, dylib, etc ....
        if (nativeMap.containsKey(p1)) {
            return nativeMap.get(p1);
        } else {
            return super.findLibrary(p1);
        }
    }
    /* End Native Library Management */


    /* Special Loader management */
    public ArrayList<SpecialLoader> specialloaders = new ArrayList<SpecialLoader>();

    protected void addSpecialLoaders(SpecialLoader l) {
        specialloaders.add(l);
    }

    public ArrayList<SpecialLoader> getSpecialLoaders() {
        return specialloaders;
    }
    /* End Special Loader management */

    protected ArrayList<FlexyClassLoader> subClassLoaders = new ArrayList<FlexyClassLoader>();

    public void cleanupLinks(ClassLoader c) {
        //TODO CHECK USED
        subClassLoaders.remove(c);
    }

    public byte[] loadClassBytes(String className) {
        String className2 = formatClassName(className);
        return classpathResources.getClassBytes(className2);
    }

    private String formatClassName(String className) {
        String classNameT = className.replace('/', '~');
        classNameT = classNameT.replace('.', '/') + ".class";
        classNameT = classNameT.replace('~', '/');
        return classNameT;
    }

    private boolean locked = false;

    public void lockLinks() {
        locked = true;
    }

    public boolean isLocked() {
        return locked;
    }

    public List<FlexyClassLoader> getSubClassLoaders() {
        return subClassLoaders;
    }

    private Map<Integer, Integer> scoreMap = new ConcurrentHashMap<Integer, Integer>();

    private int getScore(ClassLoader kcl) {
        if (scoreMap.containsKey(kcl.hashCode())) {
            return scoreMap.get(kcl.hashCode());
        } else {
            return 0;
        }
    }

    private Integer incScore(ClassLoader kcl) {
        scoreMap.put(kcl.hashCode(), getScore(kcl) + 1);
        return scoreMap.get(kcl.hashCode());
    }

    public Class getLoadedClass(String className) {
        return findLoadedClass(className);
    }

    protected Class internal_defineClass(String className, byte[] bytes) {
        if (className.contains(".")) {
            String packageName = className.substring(0, className.lastIndexOf('.'));
            if (getPackage(packageName) == null) {
                try {
                    definePackage(packageName, null, null, null, null, null, null, null);
                } catch (Throwable e) {
                    Log.debug("Error while defining package ", e);
                }
            }
        }
        return defineClass(className, bytes, 0, bytes.length);
    }

    private Comparator scoreSorter = new Comparator<FlexyClassLoader>() {
        public boolean equals(Object p0) {
            throw new UnsupportedOperationException();
        }

        public int compare(FlexyClassLoader p0, FlexyClassLoader p1) {
            if (getScore(p0) == getScore(p1)) {
                return 0;
            }
            if (getScore(p0) > getScore(p1)) {
                return 1;
            }
            return -1;
        }
    };
/*
    private void checkSubClassloadersSorted() {
        if(subClassLoaderModified) {
            Collections.sort(subClassLoaders, scoreSorter);
            subClassLoaderModified = false;
        }
    }*/

    public Class graphLoadClass(KlassLoadRequest request) {
        //cut graph cyclic search
        Class result = null;
        ArrayList<FlexyClassLoader> tempSubs = new ArrayList(subClassLoaders);
        Collections.sort(tempSubs, scoreSorter);
        for (ClassLoader subCL : tempSubs) {
            if (subCL instanceof FlexyClassLoader) {
                if (!request.passedKlassLoader.contains(((FlexyClassLoader) subCL).getKey())) {
                    FlexyClassLoaderImpl subKCL = (FlexyClassLoaderImpl) subCL;
                    result = subKCL.internal_loadClass(request);
                }
            } else {
                try {
                    result = subCL.loadClass(request.className);
                } catch (ClassNotFoundException nf) {
                    //workaround take hashCode as Key
                    request.passedKlassLoader.add("" + subCL.hashCode());
                    return null;
                }
            }
            if (result != null) {
                incScore(subCL);
                return result;
            }
        }
        return result;
    }

    private Object lock = new Object();

    public Class internal_loadClass(KlassLoadRequest request) {
        Class result = null;
        //if system class try directly
        if (request.className.startsWith("java") || request.className.startsWith("javax")) {
            try {
                result = findSystemClass(request.className);
            } catch (ClassNotFoundException e) {
            }
        }
        //if still not found try local load
        if (result == null) {
            result = getLoadedClass(request.className);
            if (result == null) {
                byte[] bytes = loadClassBytes(request.className);
                if (bytes != null) {
                    synchronized (lock) {
                        result = getLoadedClass(request.className);
                        if (result == null) {
                            result = internal_defineClass(request.className, bytes);
                        }
                    }
                }
            }
        }
        request.passedKlassLoader.add(getKey());

        //TODO check if there no risk of cycle
        if (result == null && resolutionPriority.equals(ResolutionPriority.PARENT)) {
            ClassLoader parentCL = getParent();
            if (parentCL != null) {
                if (parentCL instanceof FlexyClassLoaderImpl) {
                    FlexyClassLoaderImpl parentCastedCL = (FlexyClassLoaderImpl) parentCL;
                    result = parentCastedCL.internal_loadClass(request);
                } else {
                    try {
                        result = parentCL.loadClass(request.className);
                    } catch (ClassNotFoundException e) {

                    }
                }
            }
            if (result == null) {
                ClassLoader current = this.getClass().getClassLoader();
                if (current != parentCL) {
                    if (current instanceof FlexyClassLoaderImpl) {
                        FlexyClassLoaderImpl currentCasted = (FlexyClassLoaderImpl) current;
                        result = currentCasted.internal_loadClass(request);
                    } else {
                        try {
                            result = current.loadClass(request.className);
                        } catch (ClassNotFoundException e) {
                        }
                    }
                }
            }
        }
        //if still not found try to call to entire graph
        if (result == null) {
            result = graphLoadClass(request);
        }
        return result;
    }


    public InputStream getResourceAsStream(String name) {
        FlexyClassLoaderImpl resolved = resourceOwnerResolution(name);
        if (resolved != null) {
            return resolved.internal_getResourceAsStream(name);
        } else {
            return null;
        }
    }


    @Override
    protected URL findResource(String name) {
        FlexyClassLoaderImpl resolved = resourceOwnerResolution(name);
        if (resolved != null) {
            return resolved.internal_getResource(name);
        } else {
            return null;
        }
    }

    public URL getResource(String s) {
        return findResource(s);
    }

    protected InputStream internal_getResourceAsStream(String name) {
        List<URL> urls = this.classpathResources.get(name);
        if (urls != null) {
            try {
                return urls.get(0).openStream();
            } catch (IOException e) {
                Log.error("Error in KCL while opening URL ", e);
                return null;
            }
        } else {
            return null;
        }
    }

    protected URL internal_getResource(String name) {
        List<URL> urls = this.classpathResources.get(name);
        if (urls != null) {
            return urls.get(0);
        } else {
            return null;
        }
    }


    private List<URL> internal_getResources(String name) {
        List<URL> urls = this.classpathResources.get(name);
        if (urls == null) {
            urls = new ArrayList<URL>();
        }
        return urls;
    }

    @Override
    public java.util.Enumeration<URL> findResources(String name) throws IOException {
        List<URL> selfRes = new ArrayList<URL>();
        List<FlexyClassLoaderImpl> potentials = resourcesOwnerResolution(name);
        for (FlexyClassLoaderImpl sub : potentials) {
            selfRes.addAll(sub.internal_getResources(name));
        }
        return Collections.enumeration(selfRes);
    }

    /* Key Management */
    private String key = UUID.randomUUID().toString();

    public void setKey(String k) {
        key = k;
    }

    public String getKey() {
        return key;
    }

    private FlexyClassLoaderImpl resourceOwnerResolution(String name) {
        KlassLoadRequest request = new KlassLoadRequest();
        request.className = name;
        return graphResourceOwnerResolution(request);
    }

    public FlexyClassLoaderImpl graphResourceOwnerResolution(KlassLoadRequest request) {
        request.passedKlassLoader.add(this.getKey());
        if ((classpathResources).contains(request.className)) {
            return this;
        }
        FlexyClassLoaderImpl result = null;
        ArrayList<FlexyClassLoader> tempSubs = new ArrayList(subClassLoaders);
        Collections.sort(tempSubs, scoreSorter);
        for (FlexyClassLoader subCL : tempSubs) {
            if (!request.passedKlassLoader.contains((subCL).getKey())) {
                FlexyClassLoaderImpl subKCL = (FlexyClassLoaderImpl) subCL;
                result = subKCL.graphResourceOwnerResolution(request);
            }
            if (result != null) {
                incScore(subCL);
                return result;
            }
        }
        return null;
    }

    private List<FlexyClassLoaderImpl> resourcesOwnerResolution(String name) {
        KlassLoadRequest request = new KlassLoadRequest();
        request.className = name;
        return graphResourcesOwnerResolution(request);
    }

    public List<FlexyClassLoaderImpl> graphResourcesOwnerResolution(KlassLoadRequest request) {
        List<FlexyClassLoaderImpl> result = new ArrayList<FlexyClassLoaderImpl>();
        request.passedKlassLoader.add(this.getKey());
        if ((classpathResources).contains(request.className)) {
            result.add(this);
        }
        ArrayList<FlexyClassLoader> tempSubs = new ArrayList(subClassLoaders);
        Collections.sort(tempSubs, scoreSorter);
        for (FlexyClassLoader subCL : tempSubs) {
            if (!request.passedKlassLoader.contains((subCL).getKey())) {
                FlexyClassLoaderImpl subKCL = (FlexyClassLoaderImpl) subCL;
                result.addAll(subKCL.graphResourcesOwnerResolution(request));
            }
        }
        return result;
    }

}
