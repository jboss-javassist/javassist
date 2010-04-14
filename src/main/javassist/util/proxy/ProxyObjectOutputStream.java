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
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;

/**
 * An input stream class which knows how to serialize proxies created via {@link ProxyFactory}. It must
 * be used when serialising proxies created from a proxy factory configured with
 * {@link ProxyFactory#useWriteReplace} set to false. Subsequent deserialization of the serialized data
 * must employ a {@link ProxyObjectInputStream}
 *
 * @author Andrew Dinn
 */
public class ProxyObjectOutputStream extends ObjectOutputStream
{
    /**
     * create an output stream which can be used to serialize an object graph which includes proxies created
     * using class ProxyFactory
     * @param out
     * @throws IOException whenever ObjectOutputStream would also do so
     * @throws SecurityException whenever ObjectOutputStream would also do so
     * @throws NullPointerException if out is null
     */
    public ProxyObjectOutputStream(OutputStream out) throws IOException
    {
        super(out);
    }

    protected void writeClassDescriptor(ObjectStreamClass desc) throws IOException {
        Class cl = desc.forClass();
        if (ProxyFactory.isProxyClass(cl)) {
            writeBoolean(true);
            Class superClass = cl.getSuperclass();
            Class[] interfaces = cl.getInterfaces();
            byte[] signature = ProxyFactory.getFilterSignature(cl);
            String name = superClass.getName();
            writeObject(name);
            // we don't write the marker interface ProxyObject
            writeInt(interfaces.length - 1);
            for (int i = 0; i < interfaces.length; i++) {
                Class interfaze = interfaces[i];
                if (interfaze != ProxyObject.class) {
                    name = interfaces[i].getName();
                    writeObject(name);
                }
            }
            writeInt(signature.length);
            write(signature);
        } else {
            writeBoolean(false);
            super.writeClassDescriptor(desc);
        }
    }
}
