package org.kevoree.kcl.impl;

import java.io.*;
import java.net.URL;

/**
 * Created by duke on 03/02/2014.
 */
public class FlexyClassLoaderWrapper extends FlexyClassLoaderImpl {

    private ClassLoader legacy;

    public FlexyClassLoaderWrapper(ClassLoader cl) {
        this.legacy = cl;
    }

    protected FlexyClassLoaderWrapper() {
        super();
    }

    protected InputStream internal_getResourceAsStream(String name) {
        return this.legacy.getResourceAsStream(name);
    }

    protected URL internal_getResource(String name) {
        return this.legacy.getResource(name);
    }

    @Override
    public Class getLoadedClass(String className) {
        try {
            return this.legacy.loadClass(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

}
