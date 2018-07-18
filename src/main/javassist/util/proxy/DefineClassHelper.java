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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.List;

import javassist.CannotCompileException;
import javassist.bytecode.ClassFile;

/**
 * Helper class for invoking {@link ClassLoader#defineClass(String,byte[],int,int)}.
 *
 * @since 3.22
 */
public class DefineClassHelper
{

    private static enum SecuredPrivileged
    {
        JAVA_9 {
            final class ReferencedUnsafe
            {
                private final SecurityActions.TheUnsafe sunMiscUnsafeTheUnsafe;
                private final MethodHandle defineClass;

                ReferencedUnsafe(SecurityActions.TheUnsafe usf, MethodHandle meth)
                {
                    this.sunMiscUnsafeTheUnsafe = usf;
                    this.defineClass = meth;
                }

                Class<?> defineClass(String name, byte[] b, int off, int len,
                        ClassLoader loader, ProtectionDomain protectionDomain)
                                throws ClassFormatError {
                    try {
                        if (getCallerClass.invoke(stack) != SecuredPrivileged.JAVA_9.getClass())
                            throw new IllegalAccessError("Access denied for caller.");
                    } catch (Exception e) {
                        throw new RuntimeException("cannot initialize", e);
                    }
                    try {
                        return (Class<?>) defineClass.invokeWithArguments(
                                sunMiscUnsafeTheUnsafe.theUnsafe,
                                name, b, off, len, loader, protectionDomain);
                    } catch (Throwable e) {
                        if (e instanceof RuntimeException) throw (RuntimeException) e;
                        if (e instanceof ClassFormatError) throw (ClassFormatError) e;
                        throw new ClassFormatError(e.getMessage());
                    }
                }
            }
            private final Object stack;
            private final Method getCallerClass;
            {
                Class<?> stackWalkerClass = null;
                try {
                    stackWalkerClass = Class.forName("java.lang.StackWalker");
                } catch (ClassNotFoundException e) {
                    // Skip initialization when the class doesn't exist i.e. we are on JDK < 9
                }
                if (stackWalkerClass != null) {
                    try {
                        Class<?> optionClass = Class.forName("java.lang.StackWalker$Option");
                        stack = stackWalkerClass.getMethod("getInstance", optionClass)
                                // The first one is RETAIN_CLASS_REFERENCE
                                .invoke(null, optionClass.getEnumConstants()[0]);
                        getCallerClass = stackWalkerClass.getMethod("getCallerClass");
                    } catch (Throwable e) {
                        throw new RuntimeException("cannot initialize", e);
                    }
                } else {
                    stack = null;
                    getCallerClass = null;
                }
            }
            private final ReferencedUnsafe sunMiscUnsafe = getReferencedUnsafe();
            private final ReferencedUnsafe getReferencedUnsafe()
            {
                try {
                    if (null != SecuredPrivileged.JAVA_9
                            && getCallerClass.invoke(stack) != this.getClass())
                        throw new IllegalAccessError("Access denied for caller.");
                } catch (Exception e) {
                    throw new RuntimeException("cannot initialize", e);
                }
                try {
                    SecurityActions.TheUnsafe usf = SecurityActions.getSunMiscUnsafeAnonymously();
                    List<Method> defineClassMethod = usf.methods.get("defineClass");
                    // On Java 11+ the defineClass method does not exist anymore
                    if (null == defineClassMethod)
                        return null;
                    MethodHandle meth = MethodHandles.lookup()
                            .unreflect(defineClassMethod.get(0));
                    return new ReferencedUnsafe(usf, meth);
                } catch (Throwable e) {
                    throw new RuntimeException("cannot initialize", e);
                }
            }

            @Override
            public Class<?> defineClass(String name, byte[] b, int off, int len,
                    ClassLoader loader, ProtectionDomain protectionDomain)
                            throws ClassFormatError
            {
                try {
                    if (getCallerClass.invoke(stack) != DefineClassHelper.class)
                        throw new IllegalAccessError("Access denied for caller.");
                } catch (Exception e) {
                    throw new RuntimeException("cannot initialize", e);
                }
                return sunMiscUnsafe.defineClass(name, b, off, len, loader,
                        protectionDomain);
            }
        },
        JAVA_7 {
            private final SecurityActions stack = SecurityActions.stack;
            private final MethodHandle defineClass = getDefineClassMethodHandle();
            private final MethodHandle getDefineClassMethodHandle()
            {
                if (null != SecuredPrivileged.JAVA_7
                        && stack.getCallerClass() != this.getClass())
                    throw new IllegalAccessError("Access denied for caller.");
                try {
                    return SecurityActions.getMethodHandle(ClassLoader.class,
                        "defineClass", new Class[] {
                            String.class, byte[].class, int.class, int.class,
                            ProtectionDomain.class
                        });
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException("cannot initialize", e);
                }
            }

            @Override
            protected Class<?> defineClass(String name, byte[] b, int off, int len,
                    ClassLoader loader, ProtectionDomain protectionDomain) throws ClassFormatError
            {
                if (stack.getCallerClass() != DefineClassHelper.class)
                    throw new IllegalAccessError("Access denied for caller.");
                try {
                    return (Class<?>) defineClass.invokeWithArguments(
                            loader, name, b, off, len, protectionDomain);
                } catch (Throwable e) {
                    if (e instanceof RuntimeException) throw (RuntimeException) e;
                    if (e instanceof ClassFormatError) throw (ClassFormatError) e;
                    throw new ClassFormatError(e.getMessage());
                }
            }
        },
        JAVA_OTHER {
            private final Method defineClass = getDefineClassMethod();
            private final SecurityActions stack = SecurityActions.stack;
            private final Method getDefineClassMethod() {
                if (null != SecuredPrivileged.JAVA_OTHER
                        && stack.getCallerClass() != this.getClass())
                    throw new IllegalAccessError("Access denied for caller.");
                try {
                    return SecurityActions.getDeclaredMethod(ClassLoader.class,
                        "defineClass", new Class[] {
                                String.class, byte[].class, int.class, int.class, ProtectionDomain.class
                        });
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException("cannot initialize", e);
                }
            }

            @Override
            protected Class<?> defineClass(String name, byte[] b, int off, int len,
                    ClassLoader loader, ProtectionDomain protectionDomain) throws ClassFormatError
            {
                if (stack.getCallerClass() != DefineClassHelper.class)
                    throw new IllegalAccessError("Access denied for caller.");
                try {
                    SecurityActions.setAccessible(defineClass, true);
                    return (Class<?>) defineClass.invoke(loader, new Object[] {
                            name, b, off, len, protectionDomain
                        });
                } catch (Throwable e) {
                    if (e instanceof ClassFormatError) throw (ClassFormatError) e;
                    if (e instanceof RuntimeException) throw (RuntimeException) e;
                    throw new ClassFormatError(e.getMessage());
                }
                finally {
                    SecurityActions.setAccessible(defineClass, false);
                }
            }

        };

        protected abstract Class<?> defineClass(String name, byte[] b, int off, int len,
                ClassLoader loader, ProtectionDomain protectionDomain) throws ClassFormatError;
    }

    // Java 11+ removed sun.misc.Unsafe.defineClass, so we fallback to invoking defineClass on
    // ClassLoader until we have an implementation that uses MethodHandles.Lookup.defineClass
    private static final SecuredPrivileged privileged = ClassFile.MAJOR_VERSION > ClassFile.JAVA_10
            ? SecuredPrivileged.JAVA_OTHER
            : ClassFile.MAJOR_VERSION >= ClassFile.JAVA_9
                ? SecuredPrivileged.JAVA_9
                : ClassFile.MAJOR_VERSION >= ClassFile.JAVA_7
                        ? SecuredPrivileged.JAVA_7
                        : SecuredPrivileged.JAVA_OTHER;

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
        try {
            return privileged.defineClass(className, bcode, 0, bcode.length, loader, domain);
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (ClassFormatError e) {
            Throwable t = e.getCause();
            throw new CannotCompileException(t == null ? e : t);
        }
        catch (Exception e) {
            throw new CannotCompileException(e);
        }
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

    private DefineClassHelper() {}
}
