/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2006 Shigeru Chiba. All Rights Reserved.
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

/**
 * A proxy object is converted into an instance of this class
 * when it is written to an output stream.
 *
 * @see RuntimeSupport#makeSerializedProxy(Object)
 */
class SerializedProxy implements Serializable {
    private String superClass;
    private String[] interfaces;
    private MethodFilter filter;
    private MethodHandler handler;

    SerializedProxy(Class proxy, MethodFilter f, MethodHandler h) {
        filter = f;
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

    Object readResolve() throws ObjectStreamException {
        try {
            int n = interfaces.length;
            Class[] infs = new Class[n];
            for (int i = 0; i < n; i++)
                infs[i] = Class.forName(interfaces[i]);

            ProxyFactory f = new ProxyFactory();
            f.setSuperclass(Class.forName(superClass));
            f.setInterfaces(infs);
            f.setFilter(filter);
            f.setHandler(handler);
            return f.createClass().newInstance();
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
