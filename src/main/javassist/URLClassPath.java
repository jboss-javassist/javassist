/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2003 Shigeru Chiba. All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */
package javassist;

import java.io.*;
import java.net.*;

/**
 * A class search-path specified with URL (http).
 *
 * @see javassist.ClassPath
 * @see ClassPool#insertClassPath(ClassPath)
 * @see ClassPool#appendClassPath(ClassPath)
 */
public class URLClassPath implements ClassPath {
    protected String hostname;
    protected int port;
    protected String directory;
    protected String packageName;

    /**
     * Creates a search path specified with URL (http).
     *
     * <p>This search path is used only if a requested
     * class name starts with the name specified by <code>packageName</code>.
     * If <code>packageName</code> is "mypack" and a requested class is
     * "mypack.sub.Test", then the given URL is used for loading that class.
     * If <code>packageName</code> is <code>null</code>, the URL is used
     * for loading any class.
     *
     * @param host              host name
     * @param port              port number
     * @param directory         directory name ending with "/".
     *                          It can be "/" (root directory).
     * @param packageName       package name.
     */
    public URLClassPath(String host, int port,
                        String directory, String packageName) {
        hostname = host;
        this.port = port;
        this.directory = directory;
        this.packageName = packageName;
    }

    public String toString() {
        return hostname + ":" + port + directory;
    }

    /**
     * Opens a class file with http.
     */
    public InputStream openClassfile(String classname) {
        try {
            if (packageName == null || classname.startsWith(packageName)) {
                String jarname
                    = directory + classname.replace('.', '/') + ".class";
                URLConnection con = fetchClass0(hostname, port, jarname);
                return con.getInputStream();
            }
        }
        catch (IOException e) {}
        return null;    // not found
    }

    /**
     * Closes this class path.
     */
    public void close() {}

    /**
     * Reads a class file on an http server.
     *
     * @param host              host name
     * @param port              port number
     * @param directory         directory name ending with "/".
     *                          It can be "/" (root directory).
     * @param classname         fully-qualified class name
     */
    public static byte[] fetchClass(String host, int port,
                                    String directory, String classname)
        throws IOException
    {
        byte[] b;
        URLConnection con = fetchClass0(host, port,
                directory + classname.replace('.', '/') + ".class");
        int size = con.getContentLength();
        InputStream s = con.getInputStream();
        if (size <= 0)
            b = ClassPoolTail.readStream(s);
        else {
            b = new byte[size];
            int len = 0;
            do {
                int n = s.read(b, len, size - len);
                if (n < 0) {
                    s.close();
                    throw new IOException("the stream was closed: "
                                          + classname);
                }
                len += n;
            } while (len < size);
        }

        s.close();
        return b;
    }

    private static URLConnection fetchClass0(String host, int port,
                                             String filename)
        throws IOException
    {
        URL url;
        try {
            url = new URL("http", host, port, filename);
        }
        catch (MalformedURLException e) {
            // should never reache here.
            throw new IOException("invalid URL?");
        }

        URLConnection con = url.openConnection();
        con.connect();
        return con;
    }
}
