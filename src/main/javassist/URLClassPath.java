/*
 * This file is part of the Javassist toolkit.
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * either http://www.mozilla.org/MPL/.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.  See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is Javassist.
 *
 * The Initial Developer of the Original Code is Shigeru Chiba.  Portions
 * created by Shigeru Chiba are Copyright (C) 1999-2003 Shigeru Chiba.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * The development of this software is supported in part by the PRESTO
 * program (Sakigake Kenkyu 21) of Japan Science and Technology Corporation.
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

    /*
     * Creates a search path specified with URL (http).
     *
     * <p>This search path is used only if a requested
     * class name starts with the name specified by <code>packageName</code>.
     * If <code>packageName</code> is "mypack" and a requested class is
     * "mypack.sub.Test", then the given URL is used for loading that class.
     * If <code>packageName</code> is <code>null</code>, the URL is used
     * for loading any class.
     *
     * @param host		host name
     * @param port		port number
     * @param directory		directory name ending with "/".
     *				It can be "/" (root directory).
     * @param packageName	package name.
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
	return null;	// not found
    }

    /**
     * Reads a class file on an http server.
     *
     * @param host		host name
     * @param port		port number
     * @param directory		directory name ending with "/".
     *				It can be "/" (root directory).
     * @param classname		fully-qualified class name
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
