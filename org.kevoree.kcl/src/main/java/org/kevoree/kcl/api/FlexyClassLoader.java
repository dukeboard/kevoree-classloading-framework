package org.kevoree.kcl.api;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * Created by duke on 13/01/2014.
 */
public abstract class FlexyClassLoader extends ClassLoader {

    /* Attach Sub ClassLoader */
    public abstract void attachChild(FlexyClassLoader child);

    /* Detach Sub ClassLoader */
    public abstract void detachChild(FlexyClassLoader child);

    public abstract void load(File directory) throws IOException;

    public ResolutionPriority resolutionPriority = ResolutionPriority.CHILDS;

    public abstract String getKey();

    public abstract void setKey(String s);

    public abstract List<FlexyClassLoader> getSubClassLoaders();

    public abstract List<URL> getClassLoaderResources();

    public abstract List<URL> getAllResources();
}
