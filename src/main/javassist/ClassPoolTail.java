/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2003 Shigeru Chiba. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */

package javassist;

import java.io.*;
import java.util.jar.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;

final class ClassPathList {
    ClassPathList next;
    ClassPath path;

    ClassPathList(ClassPath p, ClassPathList n) {
        next = n;
        path = p;
    }
}


final class SystemClassPath implements ClassPath {
    Class thisClass;

    SystemClassPath() {
        /* The value of thisClass was this.getClass() in early versions:
         *
         *     thisClass = this.getClass();
         *
         * However, this made openClassfile() not search all the system
         * class paths if javassist.jar is put in jre/lib/ext/
         * (with JDK1.4).
         */
        thisClass = java.lang.Object.class;
    }

    public InputStream openClassfile(String classname) {
        String jarname = "/" + classname.replace('.', '/') + ".class";
        return thisClass.getResourceAsStream(jarname);
    }

    public URL find(String classname) {
        String jarname = "/" + classname.replace('.', '/') + ".class";
        return thisClass.getResource(jarname);
    }

    public void close() {}

    public String toString() {
        return "*system class path*";
    }
}


final class DirClassPath implements ClassPath {
    String directory;

    DirClassPath(String dirName) {
        directory = dirName;
    }

    public InputStream openClassfile(String classname) {
        try {
            char sep = File.separatorChar;
            String filename = directory + sep
                + classname.replace('.', sep) + ".class";
            return new FileInputStream(filename.toString());
        }
        catch (FileNotFoundException e) {}
        catch (SecurityException e) {}
        return null;
    }

    public URL find(String classname) {
        char sep = File.separatorChar;
        String filename = directory + sep
            + classname.replace('.', sep) + ".class";
        File f = new File(filename);
        if (f.exists())
            try {
                return f.getCanonicalFile().toURL();
            }
            catch (MalformedURLException e) {}
            catch (IOException e) {}

        return null;
    }

    public void close() {}

    public String toString() {
        return directory;
    }
}


final class JarClassPath implements ClassPath {
    JarFile jarfile;
    String jarfileURL;

    JarClassPath(String pathname) throws NotFoundException {
        try {
            jarfile = new JarFile(pathname);
            jarfileURL = new File(pathname).getCanonicalFile()
                                           .toURL().toString();
            return;
        }
        catch (IOException e) {}
        throw new NotFoundException(pathname);
    }

    public InputStream openClassfile(String classname)
        throws NotFoundException
    {
        try {
            String jarname = classname.replace('.', '/') + ".class";
            JarEntry je = jarfile.getJarEntry(jarname);
            if (je != null)
                return jarfile.getInputStream(je);
            else
                return null;    // not found
        }
        catch (IOException e) {}
        throw new NotFoundException("broken jar file?: "
                                    + jarfile.getName());
    }

    public URL find(String classname) {
        String jarname = classname.replace('.', '/') + ".class";
        JarEntry je = jarfile.getJarEntry(jarname);
        if (je != null)
            try {
                return new URL("jar:" + jarfileURL + "!/" + jarname);
            }
            catch (MalformedURLException e) {}

        return null;            // not found
    }

    public void close() {
        try {
            jarfile.close();
            jarfile = null;
        }
        catch (IOException e) {}
    }

    public String toString() {
        return jarfile == null ? "<null>" : jarfile.toString();
    }
}

final class ClassPoolTail extends ClassPool {
    protected ClassPathList pathList;
    private Class thisClass;
    private Hashtable packages;         // should be synchronized.

    public ClassPoolTail() {
        pathList = null;
        thisClass = getClass();
        packages = new Hashtable();
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("[class path: ");
        ClassPathList list = pathList;
        while (list != null) {
            buf.append(list.path.toString());
            buf.append(File.pathSeparatorChar);
            list = list.next;
        }

        buf.append(']');
        return buf.toString();
    }

    /**
     * You can record "System" so that java.lang.System can be quickly
     * found although "System" is not a package name.
     */
    public void recordInvalidClassName(String name) {
        packages.put(name, name);
    }

    public byte[] write(String classname)
        throws NotFoundException, IOException
    {
        return readClassfile(classname);
    }

    public void write(String classname, DataOutputStream out)
        throws NotFoundException, CannotCompileException, IOException
    {
        byte[] b = write(classname);
        out.write(b, 0, b.length);
    }

    public CtClass get(String classname) throws NotFoundException {
        throw new RuntimeException("fatal error");
    }

    public CtClass makeClass(String classname) {
        throw new RuntimeException("fatal error");
    }

    void checkClassName(String classname) throws NotFoundException {
        if (find(classname) == null)
            throw new NotFoundException(classname);
    }

    /* slower version.

    void checkClassName(String classname) throws NotFoundException {
        InputStream fin = openClassfile(classname);
        try {
            fin.close();
        }
        catch (IOException e) {}
    }
    */

    public synchronized ClassPath insertClassPath(ClassPath cp) {
        pathList = new ClassPathList(cp, pathList);
        return cp;
    }

    public synchronized ClassPath appendClassPath(ClassPath cp) {
        ClassPathList tail = new ClassPathList(cp, null);
        ClassPathList list = pathList;
        if (list == null)
            pathList = tail;
        else {
            while (list.next != null)
                list = list.next;

            list.next = tail;
        }

        return cp;
    }

    public synchronized void removeClassPath(ClassPath cp) {
        ClassPathList list = pathList;
        if (list != null)
            if (list.path == cp)
                pathList = list.next;
            else {
                while (list.next != null)
                    if (list.next.path == cp)
                        list.next = list.next.next;
                    else
                        list = list.next;
            }

        cp.close();
    }

    public ClassPath appendSystemPath() {
        return appendClassPath(new SystemClassPath());
    }

    public ClassPath insertClassPath(String pathname)
        throws NotFoundException
    {
        return insertClassPath(makePathObject(pathname));
    }

    public ClassPath appendClassPath(String pathname)
        throws NotFoundException
    {
        return appendClassPath(makePathObject(pathname));
    }

    private static ClassPath makePathObject(String pathname)
        throws NotFoundException
    {
        int i = pathname.lastIndexOf('.');
        if (i >= 0) {
            String ext = pathname.substring(i).toLowerCase();
            if (ext.equals(".jar") || ext.equals(".zip"))
                return new JarClassPath(pathname);
        }

        return new DirClassPath(pathname);
    }

    /**
     * Obtains the contents of the class file for the class
     * specified by <code>classname</code>.
     *
     * @param classname         a fully-qualified class name
     */
    byte[] readClassfile(String classname)
        throws NotFoundException, IOException
    {
        InputStream fin = openClassfile(classname);
        byte[] b;
        try {
            b = readStream(fin);
        }
        finally {
            fin.close();
        }

        return b;
    }

    /**
     * Opens the class file for the class specified by
     * <code>classname</code>.
     *
     * @param classname         a fully-qualified class name
     */
    public InputStream openClassfile(String classname)
        throws NotFoundException
    {
        if (packages.get(classname) != null)
            throw new NotFoundException(classname);

        ClassPathList list = pathList;
        InputStream ins = null;
        NotFoundException error = null;
        while (list != null) {
            try {
                ins = list.path.openClassfile(classname);
            }
            catch (NotFoundException e) {
                if (error == null)
                    error = e;
            }

            if (ins == null)
                list = list.next;
            else
                return ins;
        }

        if (error != null)
            throw error;
        else
            throw new NotFoundException(classname);
    }

    /**
     * Obtains the URL of the class file specified by classname.
     *
     * @param classname     a fully-qualified class name.
     * @return null if the class file could not be found.
     */
    public URL find(String classname) {
        if (packages.get(classname) != null)
            return null;

        ClassPathList list = pathList;
        URL url = null;
        while (list != null) {
            url = list.path.find(classname);
            if (url == null)
                list = list.next;
            else
                return url;
        }

        return null;
    }

    /**
     * Reads an input stream until it reaches the end.
     *
     * @return          the contents of that input stream
     */
    public static byte[] readStream(InputStream fin) throws IOException {
        byte[][] bufs = new byte[8][];
        int bufsize = 4096;

        for (int i = 0; i < 8; ++i) {
            bufs[i] = new byte[bufsize];
            int size = 0;
            int len = 0;
            do {
                len = fin.read(bufs[i], size, bufsize - size);
                if (len >= 0)
                    size += len;
                else {
                    byte[] result = new byte[bufsize - 4096 + size];
                    int s = 0;
                    for (int j = 0; j < i; ++j) {
                        System.arraycopy(bufs[j], 0, result, s, s + 4096);
                        s = s + s + 4096;
                    }

                    System.arraycopy(bufs[i], 0, result, s, size);
                    return result;
                }
            } while (size < bufsize);
            bufsize *= 2;
        }

        throw new IOException("too much data");
    }
}
