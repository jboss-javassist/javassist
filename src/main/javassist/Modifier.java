/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2003 Shigeru Chiba. All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */
package javassist;

import javassist.bytecode.AccessFlag;

/**
 * The Modifier class provides static methods and constants to decode
 * class and member access modifiers.  The constant values are equivalent
 * to the corresponding values in <code>javassist.bytecode.AccessFlag</code>.
 *
 * <p>All the methods/constants in this class are compatible with
 * ones in <code>java.lang.reflect.Modifier</code>.
 *
 * @see CtClass#getModifiers()
 */
public class Modifier {
    public static final int PUBLIC    = AccessFlag.PUBLIC;
    public static final int PRIVATE   = AccessFlag.PRIVATE;
    public static final int PROTECTED = AccessFlag.PROTECTED;
    public static final int STATIC    = AccessFlag.STATIC;
    public static final int FINAL     = AccessFlag.FINAL;
    public static final int SYNCHRONIZED = AccessFlag.SYNCHRONIZED;
    public static final int VOLATILE  = AccessFlag.VOLATILE;
    public static final int TRANSIENT = AccessFlag.TRANSIENT;
    public static final int NATIVE    = AccessFlag.NATIVE;
    public static final int INTERFACE = AccessFlag.INTERFACE;
    public static final int ABSTRACT  = AccessFlag.ABSTRACT;
    public static final int STRICT    = AccessFlag.STRICT;

    /**
     * Returns true if the modifiers include the <tt>public</tt>
     * modifier.
     */
    public static boolean isPublic(int mod) {
        return (mod & PUBLIC) != 0;
    }

    /**
     * Returns true if the modifiers include the <tt>private</tt>
     * modifier.
     */
    public static boolean isPrivate(int mod) {
        return (mod & PRIVATE) != 0;
    }

    /**
     * Returns true if the modifiers include the <tt>protected</tt>
     * modifier.
     */
    public static boolean isProtected(int mod) {
        return (mod & PROTECTED) != 0;
    }

    /**
     * Returns true if the modifiers include the <tt>static</tt>
     * modifier.
     */
    public static boolean isStatic(int mod) {
        return (mod & STATIC) != 0;
    }

    /**
     * Returns true if the modifiers include the <tt>final</tt>
     * modifier.
     */
    public static boolean isFinal(int mod) {
        return (mod & FINAL) != 0;
    }

    /**
     * Returns true if the modifiers include the <tt>synchronized</tt>
     * modifier.
     */
    public static boolean isSynchronized(int mod) {
        return (mod & SYNCHRONIZED) != 0;
    }

    /**
     * Returns true if the modifiers include the <tt>volatile</tt>
     * modifier.
     */
    public static boolean isVolatile(int mod) {
        return (mod & VOLATILE) != 0;
    }

    /**
     * Returns true if the modifiers include the <tt>transient</tt>
     * modifier.
     */
    public static boolean isTransient(int mod) {
        return (mod & TRANSIENT) != 0;
    }

    /**
     * Returns true if the modifiers include the <tt>native</tt>
     * modifier.
     */
    public static boolean isNative(int mod) {
        return (mod & NATIVE) != 0;
    }

    /**
     * Returns true if the modifiers include the <tt>interface</tt>
     * modifier.
     */
    public static boolean isInterface(int mod) {
        return (mod & INTERFACE) != 0;
    }

    /**
     * Returns true if the modifiers include the <tt>abstract</tt>
     * modifier.
     */
    public static boolean isAbstract(int mod) {
        return (mod & ABSTRACT) != 0;
    }

    /**
     * Returns true if the modifiers include the <tt>strictfp</tt>
     * modifier.
     */
    public static boolean isStrict(int mod) {
        return (mod & STRICT) != 0;
    }

    /**
     * Truns the public bit on.  The protected and private bits are
     * cleared.
     */
    public static int setPublic(int mod) {
        return (mod & ~(PRIVATE | PROTECTED)) | PUBLIC;
    }

    /**
     * Truns the protected bit on.  The protected and public bits are
     * cleared.
     */
    public static int setProtected(int mod) {
        return (mod & ~(PRIVATE | PUBLIC)) | PROTECTED;
    }

    /**
     * Truns the private bit on.  The protected and private bits are
     * cleared.
     */
    public static int setPrivate(int mod) {
        return (mod & ~(PROTECTED | PUBLIC)) | PRIVATE;
    }

    /**
     * Clears the public, protected, and private bits.
     */
    public static int setPackage(int mod) {
        return (mod & ~(PROTECTED | PUBLIC | PRIVATE));
    }

    /**
     * Clears a specified bit in <code>mod</code>.
     */
    public static int clear(int mod, int clearBit) {
        return mod & ~clearBit;
    }

    public static String toString(int mod) {
        return java.lang.reflect.Modifier.toString(mod);
    }
}
