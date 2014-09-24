package org.kevoree.kcl.api;

import java.io.File;
import java.net.URL;
import java.util.List;

/**
 * Created by duke on 05/02/2014.
 */
public interface IndexDB {

    public byte[] getClassBytes(String name);

    public void loadJar(File jarFile);

    public List<URL> get(String name);

    public boolean contains(String name);

}
