package org.kevoree.kcl.impl;

import org.kevoree.kcl.api.FlexyClassLoader;
import org.kevoree.kcl.api.IndexDB;
import org.kevoree.kcl.api.ResolutionPriority;
import org.kevoree.log.Log;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by duke on 10/01/2014.
 */
public class FlexyClassLoaderImpl extends FlexyClassLoader {

    /* ClassLoader overridden methods */
    @Override
    public Class loadClass(String className) throws ClassNotFoundException {
        return loadClass(className, true);
    }

    @Override
    public Class loadClass(String className, boolean resolveIt) throws ClassNotFoundException {
        KlassLoadRequest request = new KlassLoadRequest();
        request.className = className;
        Class result = internal_loadClass(request);
        if (result == null) {
            if (Log.TRACE) {
                Log.trace("KCL Class not resolved " + className + " from " + this.key);
                Log.trace("Passed FlexClassLoader, childs : " + getSubClassLoaders().size());
                for (String klassLoader : request.passedKlassLoader) {
                    Log.trace("-->" + klassLoader);
                }
            }
            throw new ClassNotFoundException(className);
        }
        return result;
    }
    /* End of Class Load specific engine */


    @Override
    public void load(InputStream child) throws IOException {
        if (child != null) {
            classpathResources.loadJar(child);
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
        classpathResources = new LazyJarIndexDB(this);
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
        return classpathResources.get(className2);
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

    public Class graphLoadClass(KlassLoadRequest request) {
        //cut graph cyclic search
        Class result = null;
        Collections.sort(subClassLoaders, scoreSorter);
        for (ClassLoader subCL : subClassLoaders) {
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
        if (request.className.startsWith("java")) {
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
        //if still not found try to call to entire graph
        if (result == null) {
            result = graphLoadClass(request);
        }
        //if priority to CHILDS nets now try parent CL
        if (result == null && resolutionPriority.equals(ResolutionPriority.CHILDS)) {
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
        //If still not found and not system basic class, lets try again
        if (result == null && !request.className.startsWith("java")/*dangerous optimization*/) {
            try {
                result = findSystemClass(request.className);
            } catch (ClassNotFoundException e) {
            }
        }
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
        if (name.endsWith(".class")) {
            byte[] res = null;
            if (name != null) {
                res = this.classpathResources.get(name);
            }
            if (res != null) {
                return new ByteArrayInputStream(res);
            }
        }
        URL url = this.classpathResources.getURL(name);
        if (url != null) {
            if (url.toString().startsWith("file:kclstream:")) {
                return new ByteArrayInputStream(this.classpathResources.get(url));
            } else {
                try {
                    return url.openStream();
                } catch (IOException e) {
                    return null;
                }
            }
        } else {
            byte[] res = this.classpathResources.get(name);
            if (res != null) {
                return new ByteArrayInputStream(res);
            }
            //STRANGE ERROR
            return null;
        }
    }

    protected URL internal_getResource(String s) {
        if ((classpathResources).contains(s)) {
            if ((classpathResources).getURL(s).toString().startsWith("file:kclstream:")) {
                String cleanName;
                if (s.contains("/")) {
                    cleanName = s.substring(s.lastIndexOf("/") + 1);
                } else {
                    cleanName = s;
                }
                try {
                    File tFile = File.createTempFile("dummy_kcl_temp", cleanName);
                    tFile.deleteOnExit();
                    FileOutputStream tWriter = new FileOutputStream(tFile);
                    tWriter.write((classpathResources).get((classpathResources).getURL(s)));
                    tWriter.close();
                    return new URL("file:///" + tFile.getAbsolutePath());
                } catch (Exception e) {
                    return null;
                }
            } else {
                //SIMPLY RETURN URL
                return (classpathResources).getURL(s);
            }
        } else {
            return null;
        }
    }


    private List<URL> internal_findResources(String p1) {
        if (classpathResources.contains(p1)) {
            List<URL> urls = (classpathResources).getURLS(p1);
            List<URL> resolvedUrl = new ArrayList<URL>();
            for (URL u : urls) {
                if (u.toString().startsWith("file:kclstream:")) {
                    String cleanName;
                    if (p1.contains("/")) {
                        cleanName = p1.substring(p1.lastIndexOf("/") + 1);
                    } else {
                        cleanName = p1;
                    }
                    try {
                        File tFile = File.createTempFile("dummy_kcl_temp", cleanName);
                        tFile.deleteOnExit();
                        FileOutputStream tWriter = new FileOutputStream(tFile);
                        tWriter.write(classpathResources.get(u));
                        tWriter.close();
                        resolvedUrl.add(new URL("file:///" + tFile.getAbsolutePath()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    resolvedUrl.add(u);
                }
            }
            return resolvedUrl;
        } else {
            return new ArrayList<URL>();
        }
    }


    @Override
    public java.util.Enumeration<URL> findResources(String p1) throws IOException {
        List<URL> selfRes = internal_findResources(p1);
        //Then call on all
        for (ClassLoader sub : subClassLoaders) {
            java.util.Enumeration<URL> subEnum;
            if (sub instanceof FlexyClassLoaderImpl) {
                subEnum = Collections.enumeration(((FlexyClassLoaderImpl) sub).internal_findResources(p1));
            } else {
                subEnum = sub.getResources(p1);
            }
            while (subEnum.hasMoreElements()) {
                URL subElem = subEnum.nextElement();
                if (!selfRes.contains(subElem)) {
                    selfRes.add(subElem);
                }
            }
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
        Collections.sort(subClassLoaders, scoreSorter);
        FlexyClassLoaderImpl result = null;
        for (FlexyClassLoader subCL : subClassLoaders) {
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

}
