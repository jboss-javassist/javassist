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
import java.net.URL;
import java.lang.ref.WeakReference;

/**
 * A class search-path representing a class loader.
 *
 * <p>It is used for obtaining a class file from the given
 * class loader by <code>getResourceAsStream()</code>.
 * The <code>LoaderClassPath</code> refers to the class loader through
 * <code>WeakReference</code>.  If the class loader is garbage collected,
 * the other search pathes are examined.
 *
 * <p>The given class loader must have both <code>getResourceAsStream()</code>
 * and <code>getResource()</code>.
 * 
 * <p>Class files in a named module are private to that module.
 * This method cannot obtain class files in named modules.
 * </p>
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @author Shigeru Chiba
 *
 * @see ClassPool#insertClassPath(ClassPath)
 * @see ClassPool#appendClassPath(ClassPath)
 * @see ClassClassPath
 * @see ModuleClassPath
 */
public class LoaderClassPath implements ClassPath {
    private WeakReference clref;

    /**
     * If true, this search path implicitly includes
     * a {@code ModuleClassPath} as a fallback.
     * For backward compatibility, this field is set to true
     * if the JVM is Java 9 or later.  It can be false in
     * Java 9 but the behavior of {@code LoadClassPath} will
     * be different from its behavior in Java 8 or older.
     *
     * <p>This field must be false if the JVM is Java 8 or older.
     *
     * @since 3.21
     */
    public static boolean fallbackOnModuleClassPath
        = javassist.bytecode.ClassFile.MAJOR_VERSION >= javassist.bytecode.ClassFile.JAVA_9;

    private static ModuleClassPath moduleClassPath = null;

    private boolean doFallback;

    /**
     * Creates a search path representing a class loader.
     */
    public LoaderClassPath(ClassLoader cl) {
        this(cl, fallbackOnModuleClassPath);
    }

    LoaderClassPath(ClassLoader cl, boolean fallback) {
        clref = new WeakReference(cl);
        doFallback = fallback;
        if (fallback)
            synchronized (LoaderClassPath.class) {
                if (moduleClassPath == null)
                    moduleClassPath = new ModuleClassPath();
            }
    }

    public String toString() {
        Object cl = null;
        if (clref != null)
            cl = clref.get();

        return cl == null ? "<null>" : cl.toString();
    }

    /**
     * Obtains a class file from the class loader.
     * This method calls <code>getResourceAsStream(String)</code>
     * on the class loader.
     */
    public InputStream openClassfile(String classname) throws NotFoundException {
        String cname = classname.replace('.', '/') + ".class";
        ClassLoader cl = (ClassLoader)clref.get();
        if (cl == null)
            return null;        // not found
        else {
            InputStream is = cl.getResourceAsStream(cname);
            if (is == null && doFallback)
                return moduleClassPath.openClassfile(classname);
            else
                return is;
        }
    }

    /**
     * Obtains the URL of the specified class file.
     * This method calls <code>getResource(String)</code>
     * on the class loader.
     *
     * @return null if the class file could not be found. 
     */
    public URL find(String classname) {
        String cname = classname.replace('.', '/') + ".class";
        ClassLoader cl = (ClassLoader)clref.get();
        if (cl == null)
            return null;        // not found
        else {
            URL url = cl.getResource(cname);
            if (url == null && doFallback)
                return moduleClassPath.find(classname);
            else
                return url;
        }
    }

    /**
     * Closes this class path.
     */
    public void close() {
        clref = null;
    }
}
