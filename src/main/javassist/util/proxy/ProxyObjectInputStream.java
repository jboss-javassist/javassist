/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2010 Shigeru Chiba. All Rights Reserved.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

/**
 * An input stream class which knows how to deserialize proxies created via {@link ProxyFactory} and
 * serializedo via a {@link ProxyObjectOutputStream}. It must be used when deserialising proxies created
 * from a proxy factory configured with {@link ProxyFactory#useWriteReplace} set to false.
 *
 * @author Andrew Dinn
 */
public class ProxyObjectInputStream extends ObjectInputStream
{
    /**
     * create an input stream which can be used to deserialize an object graph which includes proxies created
     * using class ProxyFactory. the classloader used to resolve proxy superclass and interface names
     * read from the input stream will default to the current thread's context class loader or the system
     * classloader if the context class loader is null.
     * @param in
     * @throws java.io.StreamCorruptedException whenever ObjectInputStream would also do so
     * @throws	IOException whenever ObjectInputStream would also do so
     * @throws	SecurityException whenever ObjectInputStream would also do so
     * @throws NullPointerException if in is null
     */
    public ProxyObjectInputStream(InputStream in) throws IOException
    {
        super(in);
        loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
        }
    }

    /**
     * Reset the loader to be
     * @param loader
     */
    public void setClassLoader(ClassLoader loader)
    {
        if (loader != null) {
            this.loader = loader;
        } else {
            loader = ClassLoader.getSystemClassLoader();
        }
    }

    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
        boolean isProxy = readBoolean();
        if (isProxy) {
            String name = (String)readObject();
            Class superClass = loader.loadClass(name);
            int length = readInt();
            Class[] interfaces = new Class[length];
            for (int i = 0; i < length; i++) {
                name = (String)readObject();
                interfaces[i] = loader.loadClass(name);
            }
            length = readInt();
            byte[] signature = new byte[length];
            read(signature);
            ProxyFactory factory = new ProxyFactory();
            // we must always use the cache and never use writeReplace when using
            // ProxyObjectOutputStream and ProxyObjectInputStream
            factory.setUseCache(true);
            factory.setUseWriteReplace(false);
            factory.setSuperclass(superClass);
            factory.setInterfaces(interfaces);
            Class proxyClass = factory.createClass(signature);
            return ObjectStreamClass.lookup(proxyClass);
        } else {
            return super.readClassDescriptor();
        }
    }

    /**
     * the loader to use to resolve classes for proxy superclass and interface names read
     * from the stream. defaults to the context class loader of the thread which creates
     * the input stream or the system class loader if the context class loader is null.
     */
    private ClassLoader loader;
}