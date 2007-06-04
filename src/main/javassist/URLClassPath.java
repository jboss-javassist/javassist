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
     * If <code>packageName</code> is "org.javassist." and a requested class is
     * "org.javassist.test.Main", then the given URL is used for loading that class.
     * The <code>URLClassPath</code> obtains a class file from:
     *
     * <ul><pre>http://www.javassist.org:80/java/classes/org/javassist/test/Main.class
     * </pre></ul>
     *
     * <p>Here, we assume that <code>host</code> is "www.javassist.org",
     * <code>port</code> is 80, and <code>directory</code> is "/java/classes/".
     *
     * <p>If <code>packageName</code> is <code>null</code>, the URL is used
     * for loading any class.
     *
     * @param host              host name
     * @param port              port number
     * @param directory         directory name ending with "/".
     *                          It can be "/" (root directory).
     *                          It must start with "/".
     * @param packageName       package name.  It must end with "." (dot).
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
     *
     * @return null if the class file could not be found. 
     */
    public InputStream openClassfile(String classname) {
        try {
            URLConnection con = openClassfile0(classname);
            if (con != null)
                return con.getInputStream();
        }
        catch (IOException e) {}
        return null;        // not found
    }

    private URLConnection openClassfile0(String classname) throws IOException {
        if (packageName == null || classname.startsWith(packageName)) {
            String jarname
                    = directory + classname.replace('.', '/') + ".class";
            return fetchClass0(hostname, port, jarname);
        }
        else
            return null;    // not found
    }

    /**
     * Returns the URL.
     *
     * @return null if the class file could not be obtained. 
     */
    public URL find(String classname) {
        try {
            URLConnection con = openClassfile0(classname);
            InputStream is = con.getInputStream();
            if (is != null) {
                is.close();
                return con.getURL();
            }
        }
        catch (IOException e) {}
        return null; 
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
     *                          It must start with "/".
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
        try {
            if (size <= 0)
                b = ClassPoolTail.readStream(s);
            else {
                b = new byte[size];
                int len = 0;
                do {
                    int n = s.read(b, len, size - len);
                    if (n < 0)
                        throw new IOException("the stream was closed: "
                                              + classname);

                    len += n;
                } while (len < size);
            }
        }
        finally {
            s.close();
        }

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
