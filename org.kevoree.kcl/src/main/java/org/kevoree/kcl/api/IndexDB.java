package org.kevoree.kcl.api;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

/**
 * Created by duke on 05/02/2014.
 */
public interface IndexDB {

    public byte[] get(String name);

    public byte[] get(URL name);

    public void set(String name, byte[] payload);

    public void loadJar(File jarFile);

    public void loadJar(InputStream jarFile);

    public URL getURL(String name);

    public List<URL> getURLS(String name);

    public boolean contains(String name);

}
