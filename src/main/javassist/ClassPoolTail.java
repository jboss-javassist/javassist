/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2007 Shigeru Chiba. All Rights Reserved.
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
                return f.getCanonicalFile().toURI().toURL();
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

final class JarDirClassPath implements ClassPath {
    JarClassPath[] jars;

    JarDirClassPath(String dirName) throws NotFoundException {
        File[] files = new File(dirName).listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                name = name.toLowerCase();
                return name.endsWith(".jar") || name.endsWith(".zip");
            }
        });

        if (files != null) {
            jars = new JarClassPath[files.length];
            for (int i = 0; i < files.length; i++)
                jars[i] = new JarClassPath(files[i].getPath());
        }
    }

    public InputStream openClassfile(String classname) throws NotFoundException {
        if (jars != null)
            for (int i = 0; i < jars.length; i++) {
                InputStream is = jars[i].openClassfile(classname);
                if (is != null)
                    return is;
            }

        return null;    // not found
    }

    public URL find(String classname) {
        if (jars != null)
            for (int i = 0; i < jars.length; i++) {
                URL url = jars[i].find(classname);
                if (url != null)
                    return url;
            }

        return null;    // not found
    }

    public void close() {
        if (jars != null)
            for (int i = 0; i < jars.length; i++)
                jars[i].close();
    }
}

final class JarClassPath implements ClassPath {
    JarFile jarfile;
    String jarfileURL;

    JarClassPath(String pathname) throws NotFoundException {
        try {
            jarfile = new JarFile(pathname);
            jarfileURL = new File(pathname).getCanonicalFile()
                                           .toURI().toURL().toString();
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

final class ClassPoolTail {
    protected ClassPathList pathList;
    private Hashtable packages;         // should be synchronized.

    public ClassPoolTail() {
        pathList = null;
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
        return appendClassPath(new ClassClassPath());
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
        String lower = pathname.toLowerCase();
        if (lower.endsWith(".jar") || lower.endsWith(".zip"))
            return new JarClassPath(pathname);

        int len = pathname.length();
        if (len > 2 && pathname.charAt(len - 1) == '*'
            && (pathname.charAt(len - 2) == '/'
                || pathname.charAt(len - 2) == File.separatorChar)) {
            String dir = pathname.substring(0, len - 2);
            return new JarDirClassPath(dir);
        }

        return new DirClassPath(pathname);
    }

    /**
     * You can record "System" so that java.lang.System can be quickly
     * found although "System" is not a package name.
     */
    public void recordInvalidClassName(String name) {
        packages.put(name, name);
    }

    /**
     * This method does not close the output stream.
     */
    void writeClassfile(String classname, OutputStream out)
        throws NotFoundException, IOException, CannotCompileException
    {
        InputStream fin = openClassfile(classname);
        if (fin == null)
            throw new NotFoundException(classname);

        try {
            copyStream(fin, out);
        }
        finally {
            fin.close();
        }
    }

    /*
    -- faster version --
    void checkClassName(String classname) throws NotFoundException {
        if (find(classname) == null)
            throw new NotFoundException(classname);
    }

    -- slower version --

    void checkClassName(String classname) throws NotFoundException {
        InputStream fin = openClassfile(classname);
        try {
            fin.close();
        }
        catch (IOException e) {}
    }
    */


    /**
     * Opens the class file for the class specified by
     * <code>classname</code>.
     *
     * @param classname             a fully-qualified class name
     * @return null                 if the file has not been found.
     * @throws NotFoundException    if any error is reported by ClassPath.
     */
    InputStream openClassfile(String classname)
        throws NotFoundException
    {
        if (packages.get(classname) != null)
            return null;    // not found

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
            return null;    // not found
    }

    /**
     * Searches the class path to obtain the URL of the class file
     * specified by classname.  It is also used to determine whether
     * the class file exists.
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
     * Reads from an input stream until it reaches the end.
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

    /**
     * Reads from an input stream and write to an output stream
     * until it reaches the end.  This method does not close the
     * streams.
     */
    public static void copyStream(InputStream fin, OutputStream fout)
        throws IOException
    {
        int bufsize = 4096;
        for (int i = 0; i < 8; ++i) {
            byte[] buf = new byte[bufsize];
            int size = 0;
            int len = 0;
            do {
                len = fin.read(buf, size, bufsize - size);
                if (len >= 0)
                    size += len;
                else {
                    fout.write(buf, 0, size);
                    return;
                }
            } while (size < bufsize);
            fout.write(buf);
            bufsize *= 2;
        }

        throw new IOException("too much data");
    }
}
