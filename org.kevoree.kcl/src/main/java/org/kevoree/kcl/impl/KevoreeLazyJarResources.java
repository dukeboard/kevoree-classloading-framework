package org.kevoree.kcl.impl;

import org.kevoree.log.Log;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Created by duke on 12/01/2014.
 */
public class KevoreeLazyJarResources {

    private FlexyClassLoaderImpl parent;

    public KevoreeLazyJarResources(FlexyClassLoaderImpl p) {
        this.parent = p;
    }

    public byte[] getResource(String name) {
        try {
            return getJarEntryContents(name);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    Map<String, byte[]> jarEntryContents = new HashMap<String, byte[]>();
    Map<String, URL> jarContentURL = new HashMap<String, URL>();
    List<URL> lastLoadedJars = new ArrayList<URL>();
    private java.util.HashMap<String, List<URL>> detectedResourcesURL = new java.util.HashMap<String, List<URL>>();
    private java.util.HashMap<URL, byte[]> detectedResources = new java.util.HashMap<URL, byte[]>();

    public List<URL> getLoadedURLs() {
        return lastLoadedJars;
    }

    boolean lazyload = true;

    public void setLazyLoad(boolean lazyload) {
        this.lazyload = lazyload;
    }

    public String getLastLoadedJar() {
        if (lastLoadedJars.size() > 0) {
            return lastLoadedJars.get(0).toString();
        } else {
            return "streamKCL";
        }
    }

    public void loadJar(InputStream jarStream) throws IOException {
        loadJar(jarStream, null);
    }

    public void loadJar(URL url) throws IOException {
        InputStream inS = null;
        try {
            inS = url.openStream();
            lastLoadedJars.add(url);
            loadJar(inS, url);
        } finally {
            if (inS != null) {
                try {
                    inS.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void loadJar(String jarFile) throws IOException {
        FileInputStream fis = null;
        try {
            File f = new File(jarFile);
            fis = new FileInputStream(jarFile);
            URL url = new URL("file:" + f.getAbsolutePath());
            lastLoadedJars.add(url);
            loadJar(fis, url);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public List<URL> getResourceURLS(String name) {
        if (containResource(name)) {
            return detectedResourcesURL.get(name);
        } else {
            return new ArrayList<URL>();
        }
    }

    public boolean containResource(String name) {
        if (detectedResourcesURL.get(name) != null) {
            return !detectedResourcesURL.get(name).isEmpty();
        } else {
            return false;
        }
    }

    public URL getResourceURL(String name) {
        if (containResource(name)) {
            return detectedResourcesURL.get(name).get(0);
        } else {
            return null;
        }
    }


    public void loadJar(InputStream jarStream, URL baseurl) throws IOException {
        BufferedInputStream bis = null;
        JarInputStream jis = null;
        try {
            bis = new BufferedInputStream(jarStream);
            jis = new JarInputStream(bis);
            JarEntry jarEntry = jis.getNextJarEntry();
            while (jarEntry != null) {
                if (!jarEntry.isDirectory()) {
                    boolean filtered = false;
                    if (parent != null) {
                        KevoreeResourcesLoader extentionSelected = null;
                        for (KevoreeResourcesLoader r : parent.getSpecialLoaders()) {
                            if (jarEntry.getName().endsWith(r.getExtension())) {
                                extentionSelected = r;
                                break;
                            }
                        }
                        if (extentionSelected != null) {
                            extentionSelected.doLoad(jarEntry.getName(), jis);
                            filtered = true;
                        }
                    }
                    if (!filtered) {
                        if (jarContentURL.containsKey(jarEntry.getName())) {
                            break;
                        } else {
                            if (baseurl != null && lazyload) {
                                if (jarEntry.getName().endsWith(".class")) {
                                    jarContentURL.put(jarEntry.getName(), new URL("jar:" + baseurl + "!/" + jarEntry.getName()));
                                } else {
                                    if (!detectedResourcesURL.containsKey(jarEntry.getName())) {
                                        detectedResourcesURL.put(jarEntry.getName(), new ArrayList<URL>());
                                    }
                                    List<URL> tempL = detectedResourcesURL.get(jarEntry.getName());
                                    tempL.add(new URL("jar:" + baseurl + "!/" + jarEntry.getName()));
                                }
                            } else {
                                if (!jarEntry.getName().endsWith(".class") && baseurl != null) {
                                    //IF URL OK , DON'T COPY RESOURCES

                                    List<URL> rurl = detectedResourcesURL.get(jarEntry.getName());
                                    if (rurl == null) {
                                        rurl = new ArrayList<URL>();
                                    }
                                    detectedResourcesURL.put(jarEntry.getName(), rurl);
                                    rurl.add(new URL("jar:" + baseurl + "!/" + jarEntry.getName()));
                                } else {
                                    byte[] b = new byte[2048];
                                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                                    int len = 0;
                                    while (len != -1) {
                                        len = jis.read(b);
                                        if (len > 0) {
                                            out.write(b, 0, len);
                                        }
                                    }
                                    out.flush();
                                    out.close();
                                    String key_url = "file:kclstream:" + jarStream.hashCode() + jarEntry.getName();
                                    if (jarEntry.getName().endsWith(".class")) {
                                        jarContentURL.put(jarEntry.getName(), new URL(key_url));
                                    } else {
                                        List<URL> rurl = detectedResourcesURL.get(jarEntry.getName());
                                        if (rurl == null) {
                                            rurl = new ArrayList<URL>();
                                            detectedResourcesURL.put(jarEntry.getName(), rurl);
                                        }
                                        rurl.add(new URL(key_url));
                                    }
                                    if (jarEntry.getName().endsWith(".jar")) {
                                        if (baseurl != null) {
                                            URL subRUL = new URL("jar:" + baseurl + "!/" + jarEntry.getName());
                                            lastLoadedJars.add(subRUL);
                                        }
                                        Log.debug("KCL Found sub Jar => {}", jarEntry.getName());
                                        loadJar(new ByteArrayInputStream(out.toByteArray()));
                                    } else {
                                        if (jarEntry.getName().endsWith(".class")) {
                                            jarEntryContents.put(jarEntry.getName(), out.toByteArray());
                                        } else {
                                            detectedResources.put(new URL(key_url), out.toByteArray());
                                        }
                                    }

                                }
                            }
                        }
                    }
                }
                jarEntry = jis.getNextJarEntry();
            }
        } finally {
            if (jis != null) {
                jis.close();
            }
            if (bis != null) {
                bis.close();
            }
        }
    }


    protected byte[] getJarEntryContents(String name) throws IOException {
        if (jarContentURL.containsKey(name)) {
            if (jarEntryContents.containsKey(name)) {
                return jarEntryContents.get(name);
            } else {
                if (jarContentURL.get(name) != null) {
                    InputStream stream = null;
                    try {
                        byte[] b = new byte[2048];
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        int len = 0;
                        stream = jarContentURL.get(name).openStream();
                        while (stream.available() > 0) {
                            len = stream.read(b);
                            if (len > 0) {
                                out.write(b, 0, len);
                            }
                        }
                        out.flush();
                        out.close();
                        jarEntryContents.put(name, out.toByteArray());
                        return out.toByteArray();
                    } finally {
                        if (stream != null) {
                            stream.close();
                        }
                    }
                } else {
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    public byte[] getResourceContent(URL resUrl) {
        if (detectedResources.containsKey(resUrl)) {
            return detectedResources.get(resUrl);
        } else {
            if (!resUrl.toString().startsWith("file:kclstream:")) {
                InputStream stream = null;
                try {
                    byte[] b = new byte[2048];
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    int len = 0;
                    stream = resUrl.openStream();
                    while (stream.available() > 0) {
                        len = stream.read(b);
                        if (len > 0) {
                            out.write(b, 0, len);
                        }
                    }
                    out.flush();
                    out.close();
                    detectedResources.put(resUrl, out.toByteArray());
                    return out.toByteArray();
                } catch (Exception e) {
                    Log.warn("Error while copying " + resUrl, e);
                    return null;
                } finally {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                return null;
            }
        }
    }

}
