package org.kevoree.kcl.boot;

import org.kevoree.microkernel.KevoreeKernel;
import org.kevoree.microkernel.impl.KevoreeMicroKernelImpl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Created by duke on 8/14/14.
 */
public class App {

    public static void main(String[] args) {
        String booturl = System.getProperty("boot");
        if (booturl == null) {
            System.err.println("Please specify an url like mvn:<group>:<artifact>:<version|latest|release>");
        } else {
            KevoreeKernel kernel = new KevoreeMicroKernelImpl();
            File resolved = kernel.getResolver().resolve(booturl, kernel.getSnapshotURLS());
            if (resolved == null) {
                System.err.println("File not found with url " + booturl);
            } else {
                try {
                    JarFile jar = new JarFile(resolved);
                    ZipEntry ze = jar.getEntry("KEV-INF/bootinfo");
                    InputStream is = jar.getInputStream(ze);
                    if (is != null) {
                        kernel.boot(is);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
