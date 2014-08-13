package org.kevoree.microkernel.impl;

import org.kevoree.microkernel.BootInfo;
import org.kevoree.microkernel.BootInfoLine;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by duke on 8/12/14.
 */
public class BootInfoImpl implements BootInfo {

    private List<BootInfoLine> lines = new ArrayList<BootInfoLine>();

    @Override
    public List<BootInfoLine> getLines() {
        return lines;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (BootInfoLine line : lines) {
            builder.append(line.toString());
            builder.append("\n");
        }
        return builder.toString();
    }
}
