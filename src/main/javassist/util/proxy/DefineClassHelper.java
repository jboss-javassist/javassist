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

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.security.ProtectionDomain;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import sun.misc.Unsafe;

import javassist.CannotCompileException;
import javassist.bytecode.ClassFile;

/**
 * Helper class for invoking {@link ClassLoader#defineClass(String,byte[],int,int)}.
 *
 * @since 3.22
 */
public class DefineClassHelper {
    private static java.lang.reflect.Method defineClass1 = null;
    private static java.lang.reflect.Method defineClass2 = null;
    private static Unsafe sunMiscUnsafe = null;
 
    static {
        if (ClassFile.MAJOR_VERSION < ClassFile.JAVA_9)
            try {
                Class<?> cl = Class.forName("java.lang.ClassLoader");
                defineClass1 = SecurityActions.getDeclaredMethod(
                        cl,
                        "defineClass",
                        new Class[] { String.class, byte[].class,
                                      int.class, int.class });

                defineClass2 = SecurityActions.getDeclaredMethod(
                        cl,
                        "defineClass",
                        new Class[] { String.class, byte[].class,
                              int.class, int.class, ProtectionDomain.class });
            }
            catch (Exception e) {
                throw new RuntimeException("cannot initialize");
            }
        else
            try {
                Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                theUnsafe.setAccessible(true);
                sunMiscUnsafe = (sun.misc.Unsafe)theUnsafe.get(null);
            }
            catch (Throwable t) {}
    }

    /**
     * Loads a class file by a given class loader.
     *
     * <p>This first tries to use {@code sun.misc.Unsafe} to load a class.
     * Then it tries to use a {@code protected} method in {@code java.lang.ClassLoader}
     * via {@code PrivilegedAction}.  Since the latter approach is not available
     * any longer by default in Java 9 or later, the JVM argument
     * {@code --add-opens java.base/java.lang=ALL-UNNAMED} must be given to the JVM.
     * If this JVM argument cannot be given, {@link #toPublicClass(String,byte[])}
     * should be used instead.
     * </p>
     *
     * @param domain        if it is null, a default domain is used.
     * @since 3.22
     */
    public static Class<?> toClass(String className, ClassLoader loader,
                                   ProtectionDomain domain, byte[] bcode)
        throws CannotCompileException
    {
        if (ClassFile.MAJOR_VERSION >= ClassFile.JAVA_9)
            if (sunMiscUnsafe != null)
                try {
                    return sunMiscUnsafe.defineClass(className, bcode, 0, bcode.length,
                                                     loader, domain);
                }
                catch (Throwable t2) {}

        return toClass2(className, loader, domain, bcode);
    }

    /**
     * Loads a class file by {@code java.lang.invoke.MethodHandles.Lookup}.
     *
     * @since 3.22
     */
    static Class<?> toPublicClass(String className, byte[] bcode)
        throws CannotCompileException
    {
        try {
            Lookup lookup = MethodHandles.lookup();
            lookup = lookup.dropLookupMode(java.lang.invoke.MethodHandles.Lookup.PRIVATE);
            return lookup.defineClass(bcode);
        }
        catch (Throwable t) {
            throw new CannotCompileException(t);
        }
    }

    private static Class<?> toClass2(String cname, ClassLoader loader,
                                     ProtectionDomain domain, byte[] bcode)
        throws CannotCompileException
    {
        try {
            Method method;
            Object[] args;
            if (domain == null) {
                method = defineClass1;
                args = new Object[] { cname, bcode, Integer.valueOf(0),
                                      Integer.valueOf(bcode.length) };
            }
            else {
                method = defineClass2;
                args = new Object[] { cname, bcode, Integer.valueOf(0),
                                      Integer.valueOf(bcode.length), domain };
            }

            return toClass3(method, loader, args);
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (java.lang.reflect.InvocationTargetException e) {
            throw new CannotCompileException(e.getTargetException());
        }
        catch (Exception e) {
            throw new CannotCompileException(e);
        }
    }

    private static synchronized
    Class<?> toClass3(Method method, ClassLoader loader, Object[] args)
        throws Exception
    {
        SecurityActions.setAccessible(method, true);
        Class<?> clazz = (Class<?>)method.invoke(loader, args);
        SecurityActions.setAccessible(method, false);
        return clazz;
    }
}
