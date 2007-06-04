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

package javassist.tools.rmi;

import java.io.*;
import java.net.*;
import java.applet.Applet;
import java.lang.reflect.*;

/**
 * The object importer enables applets to call a method on a remote
 * object running on the <code>Webserver</code> (the <b>main</b> class of this
 * package).
 *
 * <p>To access the remote
 * object, the applet first calls <code>lookupObject()</code> and
 * obtains a proxy object, which is a reference to that object.
 * The class name of the proxy object is identical to that of
 * the remote object.
 * The proxy object provides the same set of methods as the remote object.
 * If one of the methods is invoked on the proxy object,
 * the invocation is delegated to the remote object.
 * From the viewpoint of the applet, therefore, the two objects are
 * identical. The applet can access the object on the server
 * with the regular Java syntax without concern about the actual
 * location.
 *
 * <p>The methods remotely called by the applet must be <code>public</code>.
 * This is true even if the applet's class and the remote object's classs
 * belong to the same package.
 *
 * <p>If class X is a class of remote objects, a subclass of X must be
 * also a class of remote objects.  On the other hand, this restriction
 * is not applied to the superclass of X.  The class X does not have to
 * contain a constructor taking no arguments.
 *
 * <p>The parameters to a remote method is passed in the <i>call-by-value</i>
 * manner.  Thus all the parameter classes must implement
 * <code>java.io.Serializable</code>.  However, if the parameter is the
 * proxy object, the reference to the remote object instead of a copy of
 * the object is passed to the method.
 *
 * <p>Because of the limitations of the current implementation,
 * <ul>
 * <li>The parameter objects cannot contain the proxy
 * object as a field value.
 * <li>If class <code>C</code> is of the remote object, then
 * the applet cannot instantiate <code>C</code> locally or remotely.
 * </ul>
 *
 * <p>All the exceptions thrown by the remote object are converted
 * into <code>RemoteException</code>.  Since this exception is a subclass
 * of <code>RuntimeException</code>, the caller method does not need
 * to catch the exception.  However, good programs should catch
 * the <code>RuntimeException</code>.
 *
 * @see javassist.tools.rmi.AppletServer
 * @see javassist.tools.rmi.RemoteException
 * @see javassist.tools.web.Viewer
 */
public class ObjectImporter implements java.io.Serializable {
    private final byte[] endofline = { 0x0d, 0x0a };
    private String servername, orgServername;
    private int port, orgPort;

    protected byte[] lookupCommand = "POST /lookup HTTP/1.0".getBytes();
    protected byte[] rmiCommand = "POST /rmi HTTP/1.0".getBytes();

    /**
     * Constructs an object importer.
     *
     * <p>Remote objects are imported from the web server that the given
     * applet has been loaded from.
     *
     * @param applet    the applet loaded from the <code>Webserver</code>.
     */
    public ObjectImporter(Applet applet) {
        URL codebase = applet.getCodeBase();
        orgServername = servername = codebase.getHost();
        orgPort = port = codebase.getPort();
    }

    /**
     * Constructs an object importer.
     *
     * <p>If you run a program with <code>javassist.tools.web.Viewer</code>,
     * you can construct an object importer as follows:
     *
     * <ul><pre>
     * Viewer v = (Viewer)this.getClass().getClassLoader();
     * ObjectImporter oi = new ObjectImporter(v.getServer(), v.getPort());
     * </pre></ul>
     *
     * @see javassist.tools.web.Viewer
     */
    public ObjectImporter(String servername, int port) {
        this.orgServername = this.servername = servername;
        this.orgPort = this.port = port;
    }

    /**
     * Finds the object exported by a server with the specified name.
     * If the object is not found, this method returns null.
     *
     * @param name      the name of the exported object.
     * @return          the proxy object or null.
     */
    public Object getObject(String name) {
        try {
            return lookupObject(name);
        }
        catch (ObjectNotFoundException e) {
            return null;
        }
    }

    /**
     * Sets an http proxy server.  After this method is called, the object
     * importer connects a server through the http proxy server.
     */
    public void setHttpProxy(String host, int port) {
        String proxyHeader = "POST http://" + orgServername + ":" + orgPort;
        String cmd = proxyHeader + "/lookup HTTP/1.0";
        lookupCommand = cmd.getBytes();
        cmd = proxyHeader + "/rmi HTTP/1.0";
        rmiCommand = cmd.getBytes();
        this.servername = host;
        this.port = port;
    }

    /**
     * Finds the object exported by the server with the specified name.
     * It sends a POST request to the server (via an http proxy server
     * if needed).
     *
     * @param name      the name of the exported object.
     * @return          the proxy object.
     */
    public Object lookupObject(String name) throws ObjectNotFoundException
    {
        try {
            Socket sock = new Socket(servername, port);
            OutputStream out = sock.getOutputStream();
            out.write(lookupCommand);
            out.write(endofline);
            out.write(endofline);

            ObjectOutputStream dout = new ObjectOutputStream(out);
            dout.writeUTF(name);
            dout.flush();

            InputStream in = new BufferedInputStream(sock.getInputStream());
            skipHeader(in);
            ObjectInputStream din = new ObjectInputStream(in);
            int n = din.readInt();
            String classname = din.readUTF();
            din.close();
            dout.close();
            sock.close();

            if (n >= 0)
                return createProxy(n, classname);
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new ObjectNotFoundException(name, e);
        }

        throw new ObjectNotFoundException(name);
    }

    private static final Class[] proxyConstructorParamTypes
        = new Class[] { ObjectImporter.class, int.class };

    private Object createProxy(int oid, String classname) throws Exception {
        Class c = Class.forName(classname);
        Constructor cons = c.getConstructor(proxyConstructorParamTypes);
        return cons.newInstance(new Object[] { this, new Integer(oid) });
    }

    /**
     * Calls a method on a remote object.
     * It sends a POST request to the server (via an http proxy server
     * if needed).
     *
     * <p>This method is called by only proxy objects.
     */
    public Object call(int objectid, int methodid, Object[] args)
        throws RemoteException
    {
        boolean result;
        Object rvalue;
        String errmsg;

        try {
            /* This method establishes a raw tcp connection for sending
             * a POST message.  Thus the object cannot communicate a
             * remote object beyond a fire wall.  To avoid this problem,
             * the connection should be established with a mechanism
             * collaborating a proxy server.  Unfortunately, java.lang.URL
             * does not seem to provide such a mechanism.
             *
             * You might think that using HttpURLConnection is a better
             * way than constructing a raw tcp connection.  Unfortunately,
             * URL.openConnection() does not return an HttpURLConnection
             * object in Netscape's JVM.  It returns a
             * netscape.net.URLConnection object.
             *
             * lookupObject() has the same problem.
             */
            Socket sock = new Socket(servername, port);
            OutputStream out = new BufferedOutputStream(
                                                sock.getOutputStream());
            out.write(rmiCommand);
            out.write(endofline);
            out.write(endofline);

            ObjectOutputStream dout = new ObjectOutputStream(out);
            dout.writeInt(objectid);
            dout.writeInt(methodid);
            writeParameters(dout, args);
            dout.flush();

            InputStream ins = new BufferedInputStream(sock.getInputStream());
            skipHeader(ins);
            ObjectInputStream din = new ObjectInputStream(ins);
            result = din.readBoolean();
            rvalue = null;
            errmsg = null;
            if (result)
                rvalue = din.readObject();
            else
                errmsg = din.readUTF();

            din.close();
            dout.close();
            sock.close();

            if (rvalue instanceof RemoteRef) {
                RemoteRef ref = (RemoteRef)rvalue;
                rvalue = createProxy(ref.oid, ref.classname);
            }
        }
        catch (ClassNotFoundException e) {
            throw new RemoteException(e);
        }
        catch (IOException e) {
            throw new RemoteException(e);
        }
        catch (Exception e) {
            throw new RemoteException(e);
        }

        if (result)
            return rvalue;
        else
            throw new RemoteException(errmsg);
    }

    private void skipHeader(InputStream in) throws IOException {
        int len;
        do {
            int c;
            len = 0;
            while ((c = in.read()) >= 0 && c != 0x0d)
                ++len;

            in.read();  /* skip 0x0a (LF) */
        } while (len > 0);
    }

    private void writeParameters(ObjectOutputStream dout, Object[] params)
        throws IOException
    {
        int n = params.length;
        dout.writeInt(n);
        for (int i = 0; i < n; ++i)
            if (params[i] instanceof Proxy) {
                Proxy p = (Proxy)params[i];
                dout.writeObject(new RemoteRef(p._getObjectId()));
            }
            else
                dout.writeObject(params[i]);
    }
}
