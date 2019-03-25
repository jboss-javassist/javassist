/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999- Shigeru Chiba. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later,
 * or the Apache License Version 2.0.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */

package javassist.util.proxy;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.ProtectionDomain;

import javassist.CannotCompileException;
import javassist.bytecode.ClassFile;

/**
 * A helper class for implementing <code>ProxyFactory</code>.
 * The users of <code>ProxyFactory</code> do not have to see this class.
 *
 * @see ProxyFactory
 */
public class FactoryHelper {
    /**
     * Returns an index for accessing arrays in this class.
     *
     * @throws RuntimeException     if a given type is not a primitive type.
     */
    public static final int typeIndex(Class<?> type) {
        for (int i = 0; i < primitiveTypes.length; i++)
            if (primitiveTypes[i] == type)
                return i;

        throw new RuntimeException("bad type:" + type.getName());
    }

    /**
     * <code>Class</code> objects representing primitive types.
     */
    public static final Class<?>[] primitiveTypes = {
        Boolean.TYPE, Byte.TYPE, Character.TYPE, Short.TYPE, Integer.TYPE,
        Long.TYPE, Float.TYPE, Double.TYPE, Void.TYPE
    };

    /**
     * The fully-qualified names of wrapper classes for primitive types.
     */
    public static final String[] wrapperTypes = {
        "java.lang.Boolean", "java.lang.Byte", "java.lang.Character",
        "java.lang.Short", "java.lang.Integer", "java.lang.Long",
        "java.lang.Float", "java.lang.Double", "java.lang.Void"
    };

    /**
     * The descriptors of the constructors of wrapper classes.
     */
    public static final String[] wrapperDesc = {
        "(Z)V", "(B)V", "(C)V", "(S)V", "(I)V", "(J)V",
        "(F)V", "(D)V"
    };

    /**
     * The names of methods for obtaining a primitive value
     * from a wrapper object.  For example, <code>intValue()</code>
     * is such a method for obtaining an integer value from a
     * <code>java.lang.Integer</code> object.
     */
    public static final String[] unwarpMethods = {
        "booleanValue", "byteValue", "charValue", "shortValue",
        "intValue", "longValue", "floatValue", "doubleValue"
    };

    /**
     * The descriptors of the unwrapping methods contained
     * in <code>unwrapMethods</code>.
     */
    public static final String[] unwrapDesc = {
        "()Z", "()B", "()C", "()S", "()I", "()J", "()F", "()D" 
    };

    /**
     * The data size of primitive types.  <code>long</code>
     * and <code>double</code> are 2; the others are 1.
     */
    public static final int[] dataSize = {
        1, 1, 1, 1, 1, 2, 1, 2
    };

    /**
     * Loads a class file by a given class loader.
     * This method uses a default protection domain for the class
     * but it may not work with a security manager or a signed jar file.
     *
     * @see #toClass(ClassFile,Class,ClassLoader,ProtectionDomain)
     * @deprecated
     */
    public static Class<?> toClass(ClassFile cf, ClassLoader loader)
        throws CannotCompileException
    {
        return toClass(cf, null, loader, null);
    }

    /**
     * Loads a class file by a given class loader.
     *
     * @param loader        The class loader.  It can be null if {@code neighbor}
     *                      is not null.
     *
     * @param domain        if it is null, a default domain is used.
     * @since 3.3
     *
     * @see #toClass(ClassFile,Class,ClassLoader,ProtectionDomain)
     * @deprecated
     */
    public static Class<?> toClass(ClassFile cf, ClassLoader loader, ProtectionDomain domain)
        throws CannotCompileException
    {
        return toClass(cf, null, loader, domain);
    }

    /**
     * Loads a class file by a given class loader.
     *
     * @param neighbor      a class belonging to the same package that
     *                      the loaded class belongs to.
     *                      It can be null.
     * @param loader        The class loader.  It can be null if {@code neighbor}
     *                      is not null.
     * @param domain        if it is null, a default domain is used.
     * @since 3.3
     */
    public static Class<?> toClass(ClassFile cf, Class<?> neighbor,
                                   ClassLoader loader, ProtectionDomain domain)
        throws CannotCompileException
    {
        try {
            byte[] b = toBytecode(cf);
            if (ProxyFactory.onlyPublicMethods)
                return DefineClassHelper.toPublicClass(cf.getName(), b);
            else
                return DefineClassHelper.toClass(cf.getName(), neighbor,
                                                 loader, domain, b);
        }
        catch (IOException e) {
            throw new CannotCompileException(e);
        }
     }

    /**
     * Loads a class file by a given lookup.
     *
     * @param lookup        used to define the class.
     * @since 3.24
     */
    public static Class<?> toClass(ClassFile cf, java.lang.invoke.MethodHandles.Lookup lookup)
        throws CannotCompileException
    {
        try {
            byte[] b = toBytecode(cf);
            return DefineClassHelper.toClass(lookup, b);
        }
        catch (IOException e) {
            throw new CannotCompileException(e);
        }
     }

    private static byte[] toBytecode(ClassFile cf) throws IOException {
        ByteArrayOutputStream barray = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(barray);
        try {
            cf.write(out);
        }
        finally {
            out.close();
        }

        return barray.toByteArray();
    }

    /**
     * Writes a class file.
     */
    public static void writeFile(ClassFile cf, String directoryName)
            throws CannotCompileException {
        try {
            writeFile0(cf, directoryName);
        }
        catch (IOException e) {
            throw new CannotCompileException(e);
        }
    }

    private static void writeFile0(ClassFile cf, String directoryName)
            throws CannotCompileException, IOException {
        String classname = cf.getName();
        String filename = directoryName + File.separatorChar
                + classname.replace('.', File.separatorChar) + ".class";
        int pos = filename.lastIndexOf(File.separatorChar);
        if (pos > 0) {
            String dir = filename.substring(0, pos);
            if (!dir.equals("."))
                new File(dir).mkdirs();
        }

        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(
                new FileOutputStream(filename)));
        try {
            cf.write(out);
        }
        catch (IOException e) {
            throw e;
        }
        finally {
            out.close();
        }
    }
}
