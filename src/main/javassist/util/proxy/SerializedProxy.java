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

package javassist.util.proxy;

import java.io.Serializable;
import java.io.ObjectStreamException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;

/**
 * A proxy object is converted into an instance of this class
 * when it is written to an output stream.
 *
 * @see RuntimeSupport#makeSerializedProxy(Object)
 */
class SerializedProxy implements Serializable {
    private String superClass;
    private String[] interfaces;
    private byte[] filterSignature;
    private MethodHandler handler;

    SerializedProxy(Class proxy, byte[] sig, MethodHandler h) {
        filterSignature = sig;
        handler = h;
        superClass = proxy.getSuperclass().getName();
        Class[] infs = proxy.getInterfaces();
        int n = infs.length;
        interfaces = new String[n - 1];
        String setterInf = ProxyObject.class.getName();
        for (int i = 0; i < n; i++) {
            String name = infs[i].getName();
            if (!name.equals(setterInf))
                interfaces[i] = name;
        }
    }

    /**
     * Load class.
     *
     * @param className the class name
     * @return loaded class
     * @throws ClassNotFoundException for any error
     */
    protected Class loadClass(final String className) throws ClassNotFoundException {
        try {
            return (Class)AccessController.doPrivileged(new PrivilegedExceptionAction(){
                public Object run() throws Exception{
                    ClassLoader cl = Thread.currentThread().getContextClassLoader();
                    return Class.forName(className, true, cl);
                }
            });
        }
        catch (PrivilegedActionException pae) {
            throw new RuntimeException("cannot load the class: " + className, pae.getException());
        }
    }

    Object readResolve() throws ObjectStreamException {
        try {
            int n = interfaces.length;
            Class[] infs = new Class[n];
            for (int i = 0; i < n; i++)
                infs[i] = loadClass(interfaces[i]);

            ProxyFactory f = new ProxyFactory();
            f.setSuperclass(loadClass(superClass));
            f.setInterfaces(infs);
            ProxyObject proxy = (ProxyObject)f.createClass(filterSignature).newInstance();
            proxy.setHandler(handler);
            return proxy;
        }
        catch (ClassNotFoundException e) {
            throw new java.io.InvalidClassException(e.getMessage());
        }
        catch (InstantiationException e2) {
            throw new java.io.InvalidObjectException(e2.getMessage());
        }
        catch (IllegalAccessException e3) {
            throw new java.io.InvalidClassException(e3.getMessage());
        }
    }
}
