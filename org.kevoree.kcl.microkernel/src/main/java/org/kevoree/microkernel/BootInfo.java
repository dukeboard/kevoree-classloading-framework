package org.kevoree.microkernel;

import java.util.List;

/**
 * Created by duke on 8/12/14.
 */
public interface BootInfo {

    List<BootInfoLine> getLines();

    String getMain();

    void setMain(String m);

    public static final String mainLineIdentifier = "main";

}
