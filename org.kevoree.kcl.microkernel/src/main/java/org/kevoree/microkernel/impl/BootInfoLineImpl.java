package org.kevoree.microkernel.impl;

import org.kevoree.microkernel.BootInfoLine;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by duke on 8/12/14.
 */
public class BootInfoLineImpl implements BootInfoLine {

    private String url;

    public void setUrl(String url) {
        this.url = url;
    }

    private List<String> dependencies = new ArrayList<String>();

    @Override
    public String getURL() {
        return url;
    }

    @Override
    public List<String> getDependencies() {
        return dependencies;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(url);
        if (dependencies.size() > 0) {
            for (String dep : dependencies) {
                builder.append(",");
                builder.append(dep);
            }
        }
        return builder.toString();
    }
}
