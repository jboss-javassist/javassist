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

import javassist.tools.web.*;
import javassist.CannotCompileException;
import javassist.NotFoundException;
import javassist.ClassPool;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Vector;

/**
 * An AppletServer object is a web server that an ObjectImporter
 * communicates with.  It makes the objects specified by
 * <code>exportObject()</code> remotely accessible from applets.
 * If the classes of the exported objects are requested by the client-side
 * JVM, this web server sends proxy classes for the requested classes.
 *
 * @see javassist.tools.rmi.ObjectImporter
 */
public class AppletServer extends Webserver {
    private StubGenerator stubGen;
    private Hashtable exportedNames;
    private Vector exportedObjects;

    private static final byte[] okHeader
                                = "HTTP/1.0 200 OK\r\n\r\n".getBytes();

    /**
     * Constructs a web server.
     *
     * @param port      port number
     */
    public AppletServer(String port)
        throws IOException, NotFoundException, CannotCompileException
    {
        this(Integer.parseInt(port));
    }

    /**
     * Constructs a web server.
     *
     * @param port      port number
     */
    public AppletServer(int port)
        throws IOException, NotFoundException, CannotCompileException
    {
        this(ClassPool.getDefault(), new StubGenerator(), port);
    }

    /**
     * Constructs a web server.
     *
     * @param port      port number
     * @param src       the source of classs files.
     */
    public AppletServer(int port, ClassPool src)
        throws IOException, NotFoundException, CannotCompileException
    {
        this(new ClassPool(src), new StubGenerator(), port);
    }

    private AppletServer(ClassPool loader, StubGenerator gen, int port)
        throws IOException, NotFoundException, CannotCompileException
    {
        super(port);
        exportedNames = new Hashtable();
        exportedObjects = new Vector();
        stubGen = gen;
        addTranslator(loader, gen);
    }

    /**
     * Begins the HTTP service.
     */
    public void run() {
        super.run();
    }

    /**
     * Exports an object.
     * This method produces the bytecode of the proxy class used
     * to access the exported object.  A remote applet can load
     * the proxy class and call a method on the exported object.
     *
     * @param name      the name used for looking the object up.
     * @param obj       the exported object.
     * @return          the object identifier
     *
     * @see javassist.tools.rmi.ObjectImporter#lookupObject(String)
     */
    public synchronized int exportObject(String name, Object obj)
        throws CannotCompileException
    {
        Class clazz = obj.getClass();
        ExportedObject eo = new ExportedObject();
        eo.object = obj;
        eo.methods = clazz.getMethods();
        exportedObjects.addElement(eo);
        eo.identifier = exportedObjects.size() - 1;
        if (name != null)
            exportedNames.put(name, eo);

        try {
            stubGen.makeProxyClass(clazz);
        }
        catch (NotFoundException e) {
            throw new CannotCompileException(e);
        }

        return eo.identifier;
    }

    /**
     * Processes a request from a web browser (an ObjectImporter).
     */
    public void doReply(InputStream in, OutputStream out, String cmd)
        throws IOException, BadHttpRequest
    {
        if (cmd.startsWith("POST /rmi "))
            processRMI(in, out);
        else if (cmd.startsWith("POST /lookup "))
            lookupName(cmd, in, out);
        else
            super.doReply(in, out, cmd);
    }

    private void processRMI(InputStream ins, OutputStream outs)
        throws IOException
    {
        ObjectInputStream in = new ObjectInputStream(ins);

        int objectId = in.readInt();
        int methodId = in.readInt();
        Exception err = null;
        Object rvalue = null;
        try {
            ExportedObject eo
                = (ExportedObject)exportedObjects.elementAt(objectId);
            Object[] args = readParameters(in);
            rvalue = convertRvalue(eo.methods[methodId].invoke(eo.object,
                                                               args));
        }
        catch(Exception e) {
            err = e;
            logging2(e.toString());
        }

        outs.write(okHeader);
        ObjectOutputStream out = new ObjectOutputStream(outs);
        if (err != null) {
            out.writeBoolean(false);
            out.writeUTF(err.toString());
        }
        else
            try {
                out.writeBoolean(true);
                out.writeObject(rvalue);
            }
            catch (NotSerializableException e) {
                logging2(e.toString());
            }
            catch (InvalidClassException e) {
                logging2(e.toString());
            }

        out.flush();
        out.close();
        in.close();
    }

    private Object[] readParameters(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        int n = in.readInt();
        Object[] args = new Object[n];
        for (int i = 0; i < n; ++i) {
            Object a = in.readObject();
            if (a instanceof RemoteRef) {
                RemoteRef ref = (RemoteRef)a;
                ExportedObject eo
                    = (ExportedObject)exportedObjects.elementAt(ref.oid);
                a = eo.object;
            }

            args[i] = a;
        }

        return args;
    }

    private Object convertRvalue(Object rvalue)
        throws CannotCompileException
    {
        if (rvalue == null)
            return null;        // the return type is void.

        String classname = rvalue.getClass().getName();
        if (stubGen.isProxyClass(classname))
            return new RemoteRef(exportObject(null, rvalue), classname);
        else
            return rvalue;
    }

    private void lookupName(String cmd, InputStream ins, OutputStream outs)
        throws IOException
    {
        ObjectInputStream in = new ObjectInputStream(ins);
        String name = DataInputStream.readUTF(in);
        ExportedObject found = (ExportedObject)exportedNames.get(name);
        outs.write(okHeader);
        ObjectOutputStream out = new ObjectOutputStream(outs);
        if (found == null) {
            logging2(name + "not found.");
            out.writeInt(-1);           // error code
            out.writeUTF("error");
        }
        else {
            logging2(name);
            out.writeInt(found.identifier);
            out.writeUTF(found.object.getClass().getName());
        }

        out.flush();
        out.close();
        in.close();
    }
}

class ExportedObject {
    public int identifier;
    public Object object;
    public Method[] methods;
}
