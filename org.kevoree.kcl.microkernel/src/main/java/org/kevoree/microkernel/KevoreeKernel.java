package org.kevoree.microkernel;

import org.kevoree.kcl.api.FlexyClassLoader;
import org.kevoree.resolver.MavenResolver;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

/**
 * Created by duke on 8/12/14.
 */
/*
 *  This interface is not Thread Safe !
  * */
public interface KevoreeKernel {

    public FlexyClassLoader get(String key);

    public FlexyClassLoader put(String key, File kcl);

    public void drop(String key);

    public FlexyClassLoader install(String key, String mavenURL);

    public MavenResolver getResolver();

    public java.util.Collection<FlexyClassLoader> getClassLoaders();

    public void boot();

    public void boot(InputStream is);

    public Set<String> getReleaseURLS();

    public Set<String> getSnapshotURLS();

    public static final ThreadLocal<KevoreeKernel> self = new ThreadLocal<KevoreeKernel>();

    public void boot(BootInfo bootInfo);

    public List<FlexyClassLoader> locate(String className);

}
