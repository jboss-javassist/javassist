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
 * A collection of static methods for reading and writing a byte array.
 */
public class ByteArray {
    /**
     * Reads an unsigned 16bit integer at the index.
     */
    public static int readU16bit(byte[] code, int index) {
        return ((code[index] & 0xff) << 8) | (code[index + 1] & 0xff);
    }

    /**
     * Reads a signed 16bit integer at the index.
     */
    public static int readS16bit(byte[] code, int index) {
        return (code[index] << 8) | (code[index + 1] & 0xff);
    }

    /**
     * Writes a 16bit integer at the index.
     */
    public static void write16bit(int value, byte[] code, int index) {
        code[index] = (byte)(value >>> 8);
        code[index + 1] = (byte)value;
    }

    /**
     * Reads a 32bit integer at the index.
     */
    public static int read32bit(byte[] code, int index) {
        return (code[index] << 24) | ((code[index + 1] & 0xff) << 16)
               | ((code[index + 2] & 0xff) << 8) | (code[index + 3] & 0xff);
    }

    /**
     * Writes a 32bit integer at the index.
     */
    public static void write32bit(int value, byte[] code, int index) {
        code[index] = (byte)(value >>> 24);
        code[index + 1] = (byte)(value >>> 16);
        code[index + 2] = (byte)(value >>> 8);
        code[index + 3] = (byte)value;
    }

    /**
     * Copies a 32bit integer.
     *
     * @param src       the source byte array.
     * @param isrc      the index into the source byte array.
     * @param dest      the destination byte array.
     * @param idest     the index into the destination byte array.
     */
    static void copy32bit(byte[] src, int isrc, byte[] dest, int idest) {
        dest[idest] = src[isrc];
        dest[idest + 1] = src[isrc + 1];
        dest[idest + 2] = src[isrc + 2];
        dest[idest + 3] = src[isrc + 3];
    }
}
