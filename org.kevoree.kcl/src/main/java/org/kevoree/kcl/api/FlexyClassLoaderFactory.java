package org.kevoree.kcl.api;

import org.kevoree.kcl.impl.FlexyClassLoaderImpl;

/**
 * Created by duke on 13/01/2014.
 */
public interface FlexyClassLoaderFactory {

    public FlexyClassLoader create();

    public static FlexyClassLoaderFactory INSTANCE = new FlexyClassLoaderFactory() {

        @Override
        public FlexyClassLoader create() {
            return new FlexyClassLoaderImpl();
        }

    };

}
