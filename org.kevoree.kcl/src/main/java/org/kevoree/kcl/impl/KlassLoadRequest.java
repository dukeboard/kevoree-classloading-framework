package org.kevoree.kcl.impl;

import java.util.HashSet;

/**
 * Created by duke on 13/01/2014.
 */
public class KlassLoadRequest {

    public String className;

    public HashSet<String> passedKlassLoader = new HashSet<String>();

}
