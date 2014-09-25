package org.kevoree.kcl.impl;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by duke on 9/25/14.
 */
public class ProxyClassLoaderImpl extends FlexyClassLoaderImpl {

    private List<String> packageNames = new ArrayList<String>();

    public ProxyClassLoaderImpl addFilter(String filter) {
        packageNames.add(filter);
        return this;
    }

    private boolean accept(String name) {
        for (int i = 0; i < packageNames.size(); i++) {
            if (name.startsWith(packageNames.get(i))) {
                return false;
            }
        }
        return true;
    }

    public Class internal_loadClass(KlassLoadRequest request) {
        if (accept(request.className)) {
            return super.internal_loadClass(request);
        } else {
            request.passedKlassLoader.add(getKey());
            return null;
        }
    }

    @Override
    public Class loadClass(String className, boolean resolveIt) throws ClassNotFoundException {
        if (accept(className)) {
            return super.loadClass(className, true);
        } else {
            throw new ClassNotFoundException(className);
        }
    }

}
