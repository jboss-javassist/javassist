/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999- Shigeru Chiba. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later,
 * or the Apache License Version 2.0.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */

package javassist;

import java.io.InputStream;
import java.lang.reflect.Layer;
import java.lang.reflect.Module;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * A search-path for obtaining a class file
 * by <code>getResourceAsStream()</code> in <code>java.lang.reflect.Module</code>.
 *
 * @see ClassPool#insertClassPath(ClassPath)
 * @see ClassPool#appendClassPath(ClassPath)
 * @see LoaderClassPath
 * @see ClassClassPath
 */
public class ModuleClassPath implements ClassPath {
    private HashMap packages = new HashMap();

    ModuleClassPath() { this(Layer.boot()); }

    /**
     * Constructs a search path.
     *
     * @param layer         the layer used to obtain a class file.
     */
    public ModuleClassPath(Layer layer) {
        while (layer != null) {
            Set modules = layer.modules();
            Iterator it = modules.iterator();
            while (it.hasNext())
                addPackages((Module)it.next());

            layer = layer.parent().orElse(null);
        }
    }

    /**
     * Constructs a search path.
     *
     * @param m         the module used to obtain a class file.
     */
    public ModuleClassPath(Module m) {
        addPackages(m);
    }

    private void addPackages(Module m) {
        String[] names = m.getPackages();
        for (int i = 0; i < names.length; i++)
            packages.put(names[i], m);
    }

    /**
     * Obtains a class file by <code>getResourceAsStream()</code>.
     */
    public InputStream openClassfile(String classname) throws NotFoundException {
        String filename = classname.replace('.', '/') + ".class";
        Module m = (Module)packages.get(getPackageName(classname));
        if (m == null)
            return null;
        else
            try {
                return m.getResourceAsStream(filename);
            }
            catch (java.io.IOException e) {
                throw new NotFoundException(classname, e);
            }
    }

    /**
     * Obtains the URL of the specified class file.
     *
     * @return null if the class file could not be found. 
     */
    public URL find(String classname) {
        String filename = classname.replace('.', '/') + ".class";
        Module m = (Module)packages.get(getPackageName(classname));
        if (m == null)
            return null;
        else
            try {
                InputStream is = m.getResourceAsStream(filename);
                if (is == null)
                    return null;
                else {
                    is.close();
                    return new URL("jar:file:unknown.jar!/" + filename);
                }
            }
            catch (java.io.IOException e) {
                return null;
            }
    }

    private static String getPackageName(String name) {
        int i = name.lastIndexOf('.');
        if (i > 0)
            return name.substring(0, i);
        else
            return "";
    }

    /**
     * Does nothing.
     */
    public void close() {}
}
