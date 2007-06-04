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

package javassist.tools.web;

import java.io.*;
import java.net.*;

/**
 * A sample applet viewer.
 *
 * <p>This is a sort of applet viewer that can run any program even if
 * the main class is not a subclass of <code>Applet</code>.
 * This viewwer first calls <code>main()</code> in the main class.
 *
 * <p>To run, you should type:
 *
 * <ul><code>% java javassist.tools.web.Viewer <i>host port</i> Main arg1, ...</code></ul>
 *
 * <p>This command calls <code>Main.main()</code> with <code>arg1,...</code>
 * All classes including <code>Main</code> are fetched from
 * a server http://<i>host</i>:<i>port</i>.
 * Only the class file for <code>Viewer</code> must exist
 * on a local file system at the client side; even other
 * <code>javassist.*</code> classes are not needed at the client side.
 * <code>Viewer</code> uses only Java core API classes.
 *
 * <p>Note: since a <code>Viewer</code> object is a class loader,
 * a program loaded by this object can call a method in <code>Viewer</code>.
 * For example, you can write something like this:
 *
 * <ul><pre>
 * Viewer v = (Viewer)this.getClass().getClassLoader();
 * String port = v.getPort();
 * </pre></ul>
 *
 */
public class Viewer extends ClassLoader {
    private String server;
    private int port;

    /**
     * Starts a program.
     */
    public static void main(String[] args) throws Throwable {
        if (args.length >= 3) {
            Viewer cl = new Viewer(args[0], Integer.parseInt(args[1]));
            String[] args2 = new String[args.length - 3];
            System.arraycopy(args, 3, args2, 0, args.length - 3);
            cl.run(args[2], args2);
        }
        else
            System.err.println(
        "Usage: java javassist.tools.web.Viewer <host> <port> class [args ...]");
    }

    /**
     * Constructs a viewer.
     *
     * @param host              server name
     * @param p                 port number
     */
    public Viewer(String host, int p) {
        server = host;
        port = p;
    }

    /**
     * Returns the server name.
     */
    public String getServer() { return server; }

    /**
     * Returns the port number.
     */
    public int getPort() { return port; }

    /**
     * Invokes main() in the class specified by <code>classname</code>.
     *
     * @param classname         executed class
     * @param args              the arguments passed to <code>main()</code>.
     */
    public void run(String classname, String[] args)
        throws Throwable
    {
        Class c = loadClass(classname);
        try {
            c.getDeclaredMethod("main", new Class[] { String[].class })
                .invoke(null, new Object[] { args });
        }
        catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    /**
     * Requests the class loader to load a class.
     */
    protected synchronized Class loadClass(String name, boolean resolve)
        throws ClassNotFoundException
    {
        Class c = findLoadedClass(name);
        if (c == null)
            c = findClass(name);

        if (c == null)
            throw new ClassNotFoundException(name);

        if (resolve)
            resolveClass(c);

        return c;
    }

    /**
     * Finds the specified class.  The implementation in this class
     * fetches the class from the http server.  If the class is
     * either <code>java.*</code>, <code>javax.*</code>, or
     * <code>Viewer</code>, then it is loaded by the parent class
     * loader.
     *
     * <p>This method can be overridden by a subclass of
     * <code>Viewer</code>.
     */
    protected Class findClass(String name) throws ClassNotFoundException {
        Class c = null;
        if (name.startsWith("java.") || name.startsWith("javax.")
            || name.equals("javassist.tools.web.Viewer"))
            c = findSystemClass(name);

        if (c == null)
            try {
                byte[] b = fetchClass(name);
                if (b != null)
                    c = defineClass(name, b, 0, b.length);
            }
        catch (Exception e) {
        }

        return c;
    }

    /**
     * Fetches the class file of the specified class from the http
     * server.
     */
    protected byte[] fetchClass(String classname) throws Exception
    {
        byte[] b;
        URL url = new URL("http", server, port,
                          "/" + classname.replace('.', '/') + ".class");
        URLConnection con = url.openConnection();
        con.connect();
        int size = con.getContentLength();
        InputStream s = con.getInputStream();
        if (size <= 0)
            b = readStream(s);
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

    private byte[] readStream(InputStream fin) throws IOException {
        byte[] buf = new byte[4096];
        int size = 0;
        int len = 0;
        do {
            size += len;
            if (buf.length - size <= 0) {
                byte[] newbuf = new byte[buf.length * 2];
                System.arraycopy(buf, 0, newbuf, 0, size);
                buf = newbuf;
            }

            len = fin.read(buf, size, buf.length - size);
        } while (len >= 0);

        byte[] result = new byte[size];
        System.arraycopy(buf, 0, result, 0, size);
        return result;
    }
}
