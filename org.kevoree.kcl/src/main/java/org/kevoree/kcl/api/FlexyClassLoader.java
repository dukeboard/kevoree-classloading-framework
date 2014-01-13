package org.kevoree.kcl.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Created by duke on 13/01/2014.
 */
public abstract class FlexyClassLoader extends ClassLoader {

    /* Attach Sub ClassLoader */
    public abstract void attachChild(FlexyClassLoader child);

    /* Detach Sub ClassLoader */
    public abstract void detachChild(FlexyClassLoader child);

    /* Load content from stream */
    public abstract void load(InputStream child) throws IOException;

    /* Load content from url */
    public abstract void load(URL child) throws IOException;

    public ResolutionPriority resolutionPriority = ResolutionPriority.CHILDS;

}
