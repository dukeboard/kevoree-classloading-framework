package org.kevoree.kcl.view;

import org.kevoree.microkernel.impl.BootInfoBuilder;

import java.io.ByteArrayInputStream;

/**
 * Created by duke on 8/14/14.
 */
public class App {

    public static void main(String[] args) {
        WebView view = new WebView();
        view.display(BootInfoBuilder.read(new ByteArrayInputStream("a,b\nb\nc,a\n".getBytes())));

    }

}
