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

package javassist.bytecode;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;

/**
 * <code>LineNumberTable_attribute</code>.
 */
public class LineNumberAttribute extends AttributeInfo {
    /**
     * The name of this attribute <code>"LineNumberTable"</code>.
     */
    public static final String tag = "LineNumberTable";

    LineNumberAttribute(ConstPool cp, int n, DataInputStream in)
        throws IOException
    {
        super(cp, n, in);
    }

    private LineNumberAttribute(ConstPool cp, byte[] i) {
        super(cp, tag, i);
    }

    /**
     * Returns <code>line_number_table_length</code>.
     * This represents the number of entries in the table.
     */
    public int tableLength() {
        return ByteArray.readU16bit(info, 0);
    }

    /**
     * Returns <code>line_number_table[i].start_pc</code>.
     * This represents the index into the code array at which the code
     * for a new line in the original source file begins.
     *
     * @param i         the i-th entry.
     */
    public int startPc(int i) {
        return ByteArray.readU16bit(info, i * 4 + 2);
    }

    /**
     * Returns <code>line_number_table[i].line_number</code>.
     * This represents the corresponding line number in the original
     * source file.
     *
     * @param i         the i-th entry.
     */
    public int lineNumber(int i) {
        return ByteArray.readU16bit(info, i * 4 + 4);
    }

    /**
     * Returns the line number corresponding to the specified bytecode.
     *
     * @param pc        the index into the code array.
     */
    public int toLineNumber(int pc) {
        int n = tableLength();
        int i = 0;
        for (; i < n; ++i)
            if (pc < startPc(i))
                if (i == 0)
                    return lineNumber(0);
                else
                    break;

        return lineNumber(i - 1);
    }

    /**
     * Returns the index into the code array at which the code for
     * the specified line begins.
     *
     * @param line      the line number.
     * @return          -1 if the specified line is not found.
     */
    public int toStartPc(int line) {
        int n = tableLength();
        for (int i = 0; i < n; ++i)
            if (line == lineNumber(i))
                return startPc(i);

        return -1;
    }

    /**
     * Used as a return type of <code>toNearPc()</code>.
     */
    static public class Pc {
        /**
         * The index into the code array.
         */ 
        public int index;
        /**
         * The line number.
         */
        public int line;
    }

    /**
     * Returns the index into the code array at which the code for
     * the specified line (or the nearest line after the specified one)
     * begins.
     *
     * @param line      the line number.
     * @return          a pair of the index and the line number of the
     *                  bytecode at that index.
     */
    public Pc toNearPc(int line) {
        int n = tableLength();
        int nearPc = 0;
        int distance = 0;
        if (n > 0) {
            distance = lineNumber(0) - line;
            nearPc = startPc(0);
        }

        for (int i = 1; i < n; ++i) {
            int d = lineNumber(i) - line;
            if ((d < 0 && d > distance)
                || (d >= 0 && (d < distance || distance < 0))) { 
                    distance = d;
                    nearPc = startPc(i);
            }
        }

        Pc res = new Pc();
        res.index = nearPc;
        res.line = line + distance;
        return res;
    }

    /**
     * Makes a copy.
     *
     * @param newCp     the constant pool table used by the new copy.
     * @param classnames        should be null.
     */
    public AttributeInfo copy(ConstPool newCp, Map classnames) {
        byte[] src = info;
        int num = src.length;
        byte[] dest = new byte[num];
        for (int i = 0; i < num; ++i)
            dest[i] = src[i];

        LineNumberAttribute attr = new LineNumberAttribute(newCp, dest);
        return attr;
    }

    /**
     * Adjusts start_pc if bytecode is inserted in a method body.
     */
    void shiftPc(int where, int gapLength, boolean exclusive) {
        int n = tableLength();
        for (int i = 0; i < n; ++i) {
            int pos = i * 4 + 2;
            int pc = ByteArray.readU16bit(info, pos);
            if (pc > where || (exclusive && pc == where))
                ByteArray.write16bit(pc + gapLength, info, pos);
        }
    }
}
