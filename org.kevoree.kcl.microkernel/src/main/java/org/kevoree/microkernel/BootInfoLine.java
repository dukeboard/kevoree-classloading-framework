package org.kevoree.microkernel;

import java.util.List;

/**
 * Created by duke on 8/12/14.
 */
public interface BootInfoLine {

    public String getURL();

    public List<String> getDependencies();

}
