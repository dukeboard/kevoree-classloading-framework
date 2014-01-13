package org.kevoree.kcl.impl;

import org.kevoree.kcl.api.FlexyClassLoader;
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
        if (result == null && Log.DEBUG) {
            Log.debug("KCL Class not resolved " + className);
            Log.debug("Passed FlexClassLoader, childs : " + getSubClassLoaders().size());
            for (String klassLoader : request.passedKlassLoader) {
                Log.debug("-->" + klassLoader);
            }
            throw new ClassNotFoundException(className);
        }
        return result;
    }


    @Override
    public void load(InputStream child) throws IOException {
        if (child != null) {
            classpathResources.loadJar(child);
        } else {
            Log.error("Can't add null stream");
        }
    }

    @Override
    public void load(URL child) throws IOException {
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

    protected KevoreeLazyJarResources classpathResources = null;

    public FlexyClassLoaderImpl() {
        classpathResources = new KevoreeLazyJarResources(this);
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

    protected ArrayList<ClassLoader> subClassLoaders = new ArrayList<ClassLoader>();

    public void cleanupLinks(ClassLoader c) {
        //TODO CHECK USED
        subClassLoaders.remove(c);
    }


    public byte[] loadClassBytes(String className) {
        String className2 = formatClassName(className);
        return classpathResources.getResource(className2);
    }

    private String formatClassName(String className) {
        String classNameT = className.replace('/', '~');
        classNameT = classNameT.replace('.', '/') + ".class";
        classNameT = classNameT.replace('~', '/');
        return classNameT;
    }

    public List<URL> getLoadedURLs() {
        return classpathResources.getLoadedURLs();
    }

    public List<URL> getLinkedLoadedURLs() {
        List<URL> resultURL = new ArrayList<URL>();
        ArrayList<ClassLoader> alreadyPassed = new ArrayList<ClassLoader>();
        internal_getAllLoadedURLs(resultURL, alreadyPassed);
        return resultURL;
    }

    private void internal_getAllLoadedURLs(List<URL> res, List<ClassLoader> cls) {
        cls.add(this);
        res.addAll((classpathResources).getLoadedURLs());
        for (ClassLoader l : subClassLoaders) {
            if (l instanceof FlexyClassLoaderImpl && !cls.contains(l)) {
                ((FlexyClassLoaderImpl) l).internal_getAllLoadedURLs(res, cls);
            }
        }
    }

    private boolean locked = false;

    public void lockLinks() {
        locked = true;
    }

    public void setLazyLoad(boolean lazyload) {
        (classpathResources).setLazyLoad(lazyload);
    }

    public List<ClassLoader> getSubClassLoaders() {
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

    public Class internal_defineClass(String className, byte[] bytes) {
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

    private Comparator scoreSorter = new Comparator<ClassLoader>() {
        public boolean equals(Object p0) {
            throw new UnsupportedOperationException();
        }

        public int compare(ClassLoader p0, ClassLoader p1) {
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
        if (result == null && !request.className.startsWith("java")) {
            try {
                result = findSystemClass(request.className);
            } catch (ClassNotFoundException e) {
            }
        }
        return result;
    }


    public InputStream getResourceAsStream(String name) {
        InputStream resolved = internal_getResourceAsStream(name);
        if (resolved != null) {
            return resolved;
        }
        for (ClassLoader sub : subClassLoaders) {
            if (sub instanceof FlexyClassLoaderImpl) {
                resolved = ((FlexyClassLoaderImpl) sub).internal_getResourceAsStream(name);
            } else {
                resolved = sub.getResourceAsStream(name);
            }
            if (resolved != null) {
                return resolved;
            }
        }
        return resolved;
    }


    @Override
    protected URL findResource(String s) {
        URL urlInternal = internal_getResource(s);
        if (urlInternal == null) {
            for (ClassLoader sub : subClassLoaders) {
                if (sub instanceof FlexyClassLoaderImpl) {
                    urlInternal = ((FlexyClassLoaderImpl) sub).internal_getResource(s);
                } else {
                    urlInternal = sub.getResource(s);
                }
                if (urlInternal != null) {
                    return urlInternal;
                }
            }
            return null;
        } else {
            return urlInternal;
        }
    }

    private InputStream internal_getResourceAsStream(String name) {
        if (name.endsWith(".class")) {
            byte[] res = null;
            if (name != null) {
                res = this.classpathResources.getResource(name);
            }
            if (res != null) {
                return new ByteArrayInputStream(res);
            }
        }
        URL url = this.classpathResources.getResourceURL(name);
        if (url != null) {
            if (url.toString().startsWith("file:kclstream:")) {
                return new ByteArrayInputStream(this.classpathResources.getResourceContent(url));
            } else {
                try {
                    return url.openStream();
                } catch (IOException e) {
                    return null;
                }
            }
        } else {
            //STRANGE ERROR
            return null;
        }
    }


    private List<URL> internal_findResources(String p1) {
        if (classpathResources.containResource(p1)) {
            List<URL> urls = (classpathResources).getResourceURLS(p1);
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
                        tWriter.write(classpathResources.getResourceContent(u));
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

    public URL getResource(String s) {
        return findResource(s);
    }

    private URL internal_getResource(String s) {
        if ((classpathResources).containResource(s)) {
            if ((classpathResources).getResourceURL(s).toString().startsWith("file:kclstream:")) {
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
                    tWriter.write((classpathResources).getResourceContent((classpathResources).getResourceURL(s)));
                    tWriter.close();
                    return new URL("file:///" + tFile.getAbsolutePath());
                } catch (Exception e) {
                    return null;
                }
            } else {
                //SIMPLY RETURN URL
                return (classpathResources).getResourceURL(s);
            }
        } else {
            return null;
        }
    }

    private String key = UUID.randomUUID().toString();

    public void setKey(String k) {
        key = k;
    }

    public String getKey() {
        return key;
    }


}
