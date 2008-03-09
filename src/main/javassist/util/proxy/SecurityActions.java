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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

class SecurityActions {
    static Method[] getDeclaredMethods(final Class clazz) {
        if (System.getSecurityManager() == null)
            return clazz.getDeclaredMethods();
        else {
            return (Method[]) AccessController
                    .doPrivileged(new PrivilegedAction() {
                        public Object run() {
                            return clazz.getDeclaredMethods();
                        }
                    });
        }
    }

    static Constructor[] getDeclaredConstructors(final Class clazz) {
        if (System.getSecurityManager() == null)
            return clazz.getDeclaredConstructors();
        else {
            return (Constructor[]) AccessController
                    .doPrivileged(new PrivilegedAction() {
                        public Object run() {
                            return clazz.getDeclaredConstructors();
                        }
                    });
        }
    }

    static Method getDeclaredMethod(final Class clazz, final String name,
            final Class[] types) throws NoSuchMethodException {
        if (System.getSecurityManager() == null)
            return clazz.getDeclaredMethod(name, types);
        else {
            try {
                return (Method) AccessController
                        .doPrivileged(new PrivilegedExceptionAction() {
                            public Object run() throws Exception {
                                return clazz.getDeclaredMethod(name, types);
                            }
                        });
            }
            catch (PrivilegedActionException e) {
                if (e.getCause() instanceof NoSuchMethodException)
                    throw (NoSuchMethodException) e.getCause();

                throw new RuntimeException(e.getCause());
            }
        }
    }

    static Constructor getDeclaredConstructor(final Class clazz,
                                              final Class[] types)
        throws NoSuchMethodException
    {
        if (System.getSecurityManager() == null)
            return clazz.getDeclaredConstructor(types);
        else {
            try {
                return (Constructor) AccessController
                        .doPrivileged(new PrivilegedExceptionAction() {
                            public Object run() throws Exception {
                                return clazz.getDeclaredConstructor(types);
                            }
                        });
            }
            catch (PrivilegedActionException e) {
                if (e.getCause() instanceof NoSuchMethodException)
                    throw (NoSuchMethodException) e.getCause();

                throw new RuntimeException(e.getCause());
            }
        }
    }

    static void setAccessible(final AccessibleObject ao,
                              final boolean accessible) {
        if (System.getSecurityManager() == null)
            ao.setAccessible(accessible);
        else {
            AccessController.doPrivileged(new PrivilegedAction() {
                public Object run() {
                    ao.setAccessible(accessible);
                    return null;
                }
            });
        }
    }

    static void set(final Field fld, final Object target, final Object value)
        throws IllegalAccessException
    {
        if (System.getSecurityManager() == null)
            fld.set(target, value);
        else {
            try {
                AccessController.doPrivileged(new PrivilegedExceptionAction() {
                    public Object run() throws Exception {
                        fld.set(target, value);
                        return null;
                    }
                });
            }
            catch (PrivilegedActionException e) {
                if (e.getCause() instanceof NoSuchMethodException)
                    throw (IllegalAccessException) e.getCause();

                throw new RuntimeException(e.getCause());
            }
        }
    }
}
