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
    public static final int BRIDGE    = 0x0040;     // for method_info
    public static final int TRANSIENT = 0x0080;
    public static final int VARARGS   = 0x0080;     // for method_info
    public static final int NATIVE    = 0x0100;
    public static final int INTERFACE = 0x0200;
    public static final int ABSTRACT  = 0x0400;
    public static final int STRICT    = 0x0800;
    public static final int SYNTHETIC = 0x1000;
    public static final int ANNOTATION = 0x2000;
    public static final int ENUM      = 0x4000;

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
     * Returns true if the access flags include the public bit.
     */
    public static boolean isPublic(int accflags) {
        return (accflags & PUBLIC) != 0;
    }

    /**
     * Returns true if the access flags include the protected bit.
     */
    public static boolean isProtected(int accflags) {
        return (accflags & PROTECTED) != 0;
    }

    /**
     * Returns true if the access flags include the private bit.
     */
    public static boolean isPrivate(int accflags) {
        return (accflags & PRIVATE) != 0;
    }

    /**
     * Returns true if the access flags include neither public, protected,
     * or private.
     */
    public static boolean isPackage(int accflags) {
        return (accflags & (PROTECTED | PUBLIC | PRIVATE)) == 0;
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
