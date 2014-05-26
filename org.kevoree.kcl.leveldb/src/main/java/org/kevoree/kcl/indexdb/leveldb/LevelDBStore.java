package org.kevoree.kcl.indexdb.leveldb;

import org.iq80.leveldb.*;

import static org.fusesource.leveldbjni.JniDBFactory.*;

import java.io.*;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

import org.kevoree.kcl.api.IndexDB;
import org.kevoree.log.Log;

/**
 * Created by duke on 05/02/2014.
 */
public class LevelDBStore implements IndexDB {

    private File storagePath = null;
    private DB db;

    public LevelDBStore(File storagePath) {
        this.storagePath = storagePath;
        Options options = new Options();
        options.compressionType(CompressionType.NONE);
        options.createIfMissing(true);
        try {
            db = factory.open(this.storagePath, options);
        } catch (Exception e) {
            Log.error("", e);
        }
    }

    public void close() {
        try {
            db.close();
        } catch (IOException e) {
            Log.error("", e);
        }
    }

    @Override
    public byte[] get(String name) {
        return db.get(bytes(name));
    }

    @Override
    public byte[] get(URL name) {
        return db.get(bytes(name.getPath()));
    }

    @Override
    public void set(String name, byte[] payload) {
        db.put(bytes(name), payload);
    }

    @Override
    public void loadJar(File file) {
        //TODO check if already present
        WriteBatch batch = db.createWriteBatch();
        try {
            JarFile jarFile = new JarFile(file);
            Enumeration entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = (JarEntry) entries.nextElement();
                if (!entry.isDirectory()) {
                    InputStream st = jarFile.getInputStream(entry);
                    byte[] dataSet = new byte[st.available()];
                    DataInputStream dataIs = new DataInputStream(st);
                    dataIs.readFully(dataSet);
                    batch.put(bytes(entry.getName()), dataSet);
                }
            }
            db.write(batch);
        } catch (IOException e) {
            Log.error("", e);
        } finally {
            try {
                batch.close();
            } catch (IOException e) {
                Log.error("", e);
            }
        }
    }

    @Override
    public void loadJar(InputStream jarFile) {
        JarInputStream jarInputStream = null;
        try {
            WriteBatch batch = db.createWriteBatch();
            jarInputStream = new JarInputStream(jarFile);
            JarEntry jarEntry;
            do {
                jarEntry = jarInputStream.getNextJarEntry();
                if (jarEntry != null && !jarEntry.isDirectory()) {
                    byte[] b = new byte[2048];
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    int len = 0;
                    while (len != -1) {
                        len = jarInputStream.read(b);
                        if (len > 0) {
                            out.write(b, 0, len);
                        }
                    }
                    out.flush();
                    out.close();
                    batch.put(bytes(jarEntry.getName()), out.toByteArray());
                }
            } while (jarEntry != null);
            jarInputStream.close();
            db.write(batch);
        } catch (IOException ioe) {
            Log.error("", ioe);
        }
    }

    @Override
    public URL getURL(String name) {
        return null;
    }

    @Override
    public List<URL> getURLS(String name) {
        return null;
    }

    @Override
    public boolean contains(String name) {
        return get(name) != null;
    }


}
