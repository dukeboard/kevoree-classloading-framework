package org.kevoree.kcl.api;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by duke on 9/24/14.
 */
public class Helper {

    public static File stream2File(InputStream in, String optName) throws IOException {
        File tempF = File.createTempFile("kcl_temp_stream", null);
        tempF.deleteOnExit();
        FileOutputStream fw = new FileOutputStream(tempF);
        byte[] buffer = new byte[1024];
        int len = in.read(buffer);
        while (len != -1) {
            fw.write(buffer, 0, len);
            len = in.read(buffer);
        }
        fw.close();
        return tempF;
    }

}
