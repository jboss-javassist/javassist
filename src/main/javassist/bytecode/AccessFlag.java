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
package javassist.bytecode;

/**
 * A support class providing static methods and constants
 * for access modifiers such as public, rivate, ...
 */
public class AccessFlag {
    public static final int PUBLIC    = 0x0001;
    public static final int PRIVATE   = 0x0002;
    public static final int PROTECTED = 0x0004;
    public static final int STATIC    = 0x0008;
    public static final int FINAL     = 0x0010;
    public static final int SYNCHRONIZED = 0x0020;
    public static final int VOLATILE  = 0x0040;
    public static final int TRANSIENT = 0x0080;
    public static final int NATIVE    = 0x0100;
    public static final int INTERFACE = 0x0200;
    public static final int ABSTRACT  = 0x0400;
    public static final int STRICT    = 0x0800;
    public static final int SUPER     = 0x0020;

    // Note: 0x0020 is assigned to both ACC_SUPER and ACC_SYNCHRONIZED
    // although java.lang.reflect.Modifier does not recognize ACC_SUPER.

    /**
     * Truns the public bit on.  The protected and private bits are
     * cleared.
     */
    public static int setPublic(int accflags) {
        return (accflags & ~(PRIVATE | PROTECTED)) | PUBLIC;
    }

    /**
     * Truns the protected bit on.  The protected and public bits are
     * cleared.
     */
    public static int setProtected(int accflags) {
        return (accflags & ~(PRIVATE | PUBLIC)) | PROTECTED;
    }

    /**
     * Truns the private bit on.  The protected and private bits are
     * cleared.
     */
    public static int setPrivate(int accflags) {
        return (accflags & ~(PROTECTED | PUBLIC)) | PRIVATE;
    }

    /**
     * Clears the public, protected, and private bits.
     */
    public static int setPackage(int accflags) {
        return (accflags & ~(PROTECTED | PUBLIC | PRIVATE));
    }

    /**
     * Clears a specified bit in <code>accflags</code>.
     */
    public static int clear(int accflags, int clearBit) {
        return accflags & ~clearBit;
    }

    /**
     * Converts a javassist.Modifier into
     * a javassist.bytecode.AccessFlag.
     *
     * @param modifier          javassist.Modifier
     */
    public static int of(int modifier) {
        return modifier;
    }

    /**
     * Converts a javassist.bytecode.AccessFlag
     * into a javassist.Modifier.
     *
     * @param accflags          javassist.bytecode.Accessflag
     */
    public static int toModifier(int accflags) {
        return accflags;
    }
}
