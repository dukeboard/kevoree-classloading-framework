package org.kevoree.microkernel.impl;

import org.kevoree.microkernel.BootInfo;

import java.io.*;

/**
 * Created by duke on 8/12/14.
 */
public class BootInfoBuilder {

    public static BootInfo read(InputStream is) {
        BootInfoImpl impl = new BootInfoImpl();
        BufferedReader bis = new BufferedReader(new InputStreamReader(is));
        try {
            String line = bis.readLine();
            while (line != null) {
                if (line.startsWith(BootInfoImpl.mainLineIdentifier)) {
                    impl.setMain(line.substring(5).trim());
                } else {
                    BootInfoLineImpl bootInfoLine = new BootInfoLineImpl();
                    //DO code
                    String[] elems = line.split(",");
                    if (elems.length > 0) {
                        bootInfoLine.setUrl(elems[0]);
                        for (int i = 1; i < elems.length; i++) {
                            bootInfoLine.getDependencies().add(elems[i]);
                        }
                    }
                    impl.getLines().add(bootInfoLine);
                }
                line = bis.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return impl;
    }

    public static void main(String[] args) {
        System.out.println("Test Boot");
        System.out.println(read(new ByteArrayInputStream("main titi\na\nb,c,d\nt\nc,4".getBytes())));
    }

}
