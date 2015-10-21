package org.kevoree.kcl.impl;

import org.kevoree.kcl.api.Helper;
import org.kevoree.kcl.api.IndexDB;
import org.kevoree.log.Log;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created by duke on 12/01/2014.
 */
public class JarIndexDB implements IndexDB {

    private FlexyClassLoaderImpl parent;

    public JarIndexDB(FlexyClassLoaderImpl p) {
        this.parent = p;
    }

    Map<String, byte[]> jarClassCaches = new HashMap<String, byte[]>();
    Map<String, List<URL>> jarContentURLs = new HashMap<String, List<URL>>();

    @Override
    public byte[] getClassBytes(String name) {
        return jarClassCaches.get(name);
    }

    public boolean contains(String name) {
        if (jarContentURLs.get(name) != null) {
            return !jarContentURLs.get(name).isEmpty();
        } else {
            return false;
        }
    }

    @Override
    public List<URL> get(String name) {
        return jarContentURLs.get(name);
    }


    public void loadJar(JarFile jarInput, File origin) throws IOException {
        final Enumeration<JarEntry> entries = jarInput.entries();
        while (entries.hasMoreElements()) {
            final JarEntry entry = entries.nextElement();
            boolean filtered = false;
            if (parent != null) {
                SpecialLoader extentionSelected = null;
                for (SpecialLoader r : parent.getSpecialLoaders()) {
                    if (entry.getName().endsWith(r.getExtension())) {
                        extentionSelected = r;
                        break;
                    }
                }
                if (extentionSelected != null) {
                    extentionSelected.doLoad(entry.getName(), jarInput.getInputStream(entry));
                    filtered = true;
                }
                if (!filtered) {
                    if (entry.getName().endsWith(".jar")) {
                        loadJar(Helper.stream2File(jarInput.getInputStream(entry), entry.getName()));
                    }
                    if (entry.getName().endsWith(".class")) {
                        byte[] b = new byte[2048];
                        InputStream fis = jarInput.getInputStream(entry);
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        int len = 0;
                        while (len != -1) {
                            len = fis.read(b);
                            if (len > 0) {
                                out.write(b, 0, len);
                            }
                        }
                        out.flush();
                        out.close();
                        fis.close();
                        jarClassCaches.put(entry.getName(), out.toByteArray());
                    }
                    List<URL> rurl = jarContentURLs.get(entry.getName());
                    if (rurl == null) {
                        rurl = new ArrayList<URL>();
                        jarContentURLs.put(entry.getName(), rurl);
                    }
                    String url = "jar:file:" + origin.getAbsolutePath() + "!/" + entry.getName();
                    rurl.add(new URL(url));
                    if(url.endsWith(File.separator)){
                        String cleaned = entry.getName().substring(0,entry.getName().length()-1);
                        List<URL> rurl2 = jarContentURLs.get(cleaned);
                        if (rurl2 == null) {
                            rurl2 = new ArrayList<URL>();
                            jarContentURLs.put(cleaned, rurl2);
                        }
                        String url2 = "jar:file:" + origin.getAbsolutePath() + "!/" + cleaned;
                        rurl2.add(new URL(url2));
                    }
                }
            }
        }
    }

    @Override
    public void loadJar(File jarFile) {
        try {
            if (jarFile.isDirectory()) {
                addFiles(jarFile, jarFile.toURI().toURL(), jarFile.getAbsolutePath());
                return;
            }
            loadJar(new JarFile(jarFile), jarFile);
        } catch (FileNotFoundException e) {
            Log.error("", e);
        } catch (MalformedURLException e) {
            Log.error("", e);
        } catch (IOException e) {
            Log.error("", e);
        }
    }

    private void addFiles(File file, URL baseurl, String base) throws IOException {
        boolean filtered = false;
        if (parent != null) {
            SpecialLoader extentionSelected = null;
            for (SpecialLoader r : parent.getSpecialLoaders()) {
                if (file.getName().endsWith(r.getExtension())) {
                    extentionSelected = r;
                    break;
                }
            }
            if (extentionSelected != null) {
                FileInputStream fis = new FileInputStream(file);
                extentionSelected.doLoad(file.getAbsolutePath(), fis);
                fis.close();
                filtered = true;
            }
        }
        if (!filtered) {
            String absPath = file.getAbsolutePath();
            absPath = absPath.replace(base, "");
            if (absPath.startsWith(File.separator)) {
                absPath = absPath.substring(1);
            }
            if (file.isDirectory()) {
                absPath = absPath + "/";
            }
            if (file.getName().endsWith(".jar")) {
                Log.debug("KCL Found sub Jar => {}", file.getName());
                loadJar(Helper.stream2File(new FileInputStream(file), file.getName()));
            }
            if (file.getName().endsWith(".class")) {
                byte[] b = new byte[2048];
                FileInputStream fis = new FileInputStream(file);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                int len = 0;
                while (len != -1) {
                    len = fis.read(b);
                    if (len > 0) {
                        out.write(b, 0, len);
                    }
                }
                out.flush();
                out.close();
                fis.close();
                jarClassCaches.put(absPath.replace(File.separator,"/"), out.toByteArray());
            }
            List<URL> rurl = jarContentURLs.get(absPath);
            if (rurl == null) {
                rurl = new ArrayList<URL>();
                jarContentURLs.put(absPath, rurl);
            }
            rurl.add(file.toURI().toURL());
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                addFiles(child, baseurl, base);
            }
        }
    }


}
