/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2003 Shigeru Chiba. All Rights Reserved.
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
 * <code>LocalVariableTable_attribute</code>.
 */
public class LocalVariableAttribute extends AttributeInfo {
    /**
     * The name of this attribute <code>"LocalVariableTable"</code>.
     */
    public static final String tag = "LocalVariableTable";

    LocalVariableAttribute(ConstPool cp, int n, DataInputStream in)
        throws IOException
    {
        super(cp, n, in);
    }

    private LocalVariableAttribute(ConstPool cp, byte[] i) {
        super(cp, tag, i);
    }

    /**
     * Returns <code>local_variable_table_length</code>.
     * This represents the number of entries in the table.
     */
    public int tableLength() {
        return ByteArray.readU16bit(info, 0);
    }

    /**
     * Returns <code>local_variable_table[i].start_pc</code>.
     * This represents the index into the code array from which the local
     * variable is effective.
     *
     * @param i         the i-th entry.
     */
    public int startPc(int i) {
        return ByteArray.readU16bit(info, i * 10 + 2);
    }

    /**
     * Returns <code>local_variable_table[i].length</code>.
     * This represents the length of the code region in which the local
     * variable is effective.
     *
     * @param i         the i-th entry.
     */
    public int codeLength(int i) {
        return ByteArray.readU16bit(info, i * 10 + 4);
    }

    /**
     * Adjusts start_pc and length if bytecode is inserted in a method body.
     */
    void shiftPc(int where, int gapLength, boolean exclusive) {
        int n = tableLength();
        for (int i = 0; i < n; ++i) {
            int pos = i * 10 + 2;
            int pc = ByteArray.readU16bit(info, pos);
            int len = ByteArray.readU16bit(info, pos + 2);
            if (pc > where || (exclusive && pc == where))
                ByteArray.write16bit(pc + gapLength, info, pos);
            else if (pc + len > where)
                ByteArray.write16bit(len + gapLength, info, pos + 2);
        }
    }

    /**
     * Returns <code>local_variable_table[i].name_index</code>.
     * This represents the name of the local variable.
     *
     * @param i         the i-th entry.
     */
    public int nameIndex(int i) {
        return ByteArray.readU16bit(info, i * 10 + 6);
    }

    /**
     * Returns the name of the local variable
     * specified by <code>local_variable_table[i].name_index</code>.
     *
     * @param i         the i-th entry.
     */
    public String variableName(int i) {
        return getConstPool().getUtf8Info(nameIndex(i));
    }

    /**
     * Returns <code>local_variable_table[i].descriptor_index</code>.
     * This represents the type descriptor of the local variable.
     *
     * @param i         the i-th entry.
     */
    public int descriptorIndex(int i) {
        return ByteArray.readU16bit(info, i * 10 + 8);
    }

    /**
     * Returns the type descriptor of the local variable
     * specified by <code>local_variable_table[i].descriptor_index</code>.
     *
     * @param i         the i-th entry.
     */
    public String descriptor(int i) {
        return getConstPool().getUtf8Info(descriptorIndex(i));
    }

    /**
     * Returns <code>local_variable_table[i].index</code>.
     * This represents the index of the local variable.
     *
     * @param i         the i-th entry.
     */
    public int index(int i) {
        return ByteArray.readU16bit(info, i * 10 + 10);
    }

    /**
     * Makes a copy.
     *
     * @param newCp     the constant pool table used by the new copy.
     * @param classnames        should be null.
     */
    public AttributeInfo copy(ConstPool newCp, Map classnames) {
        byte[] src = get();
        byte[] dest = new byte[src.length];
        ConstPool cp = getConstPool();
        LocalVariableAttribute attr = new LocalVariableAttribute(newCp, dest);
        int n = ByteArray.readU16bit(src, 0);
        ByteArray.write16bit(n, dest, 0);
        int j = 2;
        for (int i = 0; i < n; ++i) {
            int start = ByteArray.readU16bit(src, j);
            int len = ByteArray.readU16bit(src, j + 2);
            int name = ByteArray.readU16bit(src, j + 4);
            int type = ByteArray.readU16bit(src, j + 6);
            int index = ByteArray.readU16bit(src, j + 8);

            ByteArray.write16bit(start, dest, j);
            ByteArray.write16bit(len, dest, j + 2);
            if (name != 0)
                name = cp.copy(name, newCp, null);

            ByteArray.write16bit(name, dest, j + 4);

            if (type != 0)
                type = cp.copy(type, newCp, null);

            ByteArray.write16bit(type, dest, j + 6);
            ByteArray.write16bit(index, dest, j + 8);
            j += 10;
        }

        return attr;
    }
}
