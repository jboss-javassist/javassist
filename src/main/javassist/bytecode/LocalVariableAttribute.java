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
 * <code>LocalVariableTable_attribute</code>.
 */
public class LocalVariableAttribute extends AttributeInfo {
    /**
     * The name of this attribute <code>"LocalVariableTable"</code>.
     */
    public static final String tag = "LocalVariableTable";

    /**
     * The name of the attribute <code>"LocalVariableTypeTable"</code>.
     */
    public static final String typeTag = "LocalVariableTypeTable";

    /**
     * Constructs an empty LocalVariableTable.
     */
    public LocalVariableAttribute(ConstPool cp) {
        super(cp, tag, new byte[2]);
        ByteArray.write16bit(0, info, 0);
    }

    /**
     * Constructs an empty LocalVariableTable.
     *
     * @param name      the attribute name.
     *                  <code>LocalVariableAttribute.tag</code> or
     *                  <code>LocalVariableAttribute.typeTag</code>.
     * @see #tag
     * @see #typeTag
     * @since 3.1
     * @deprecated
     */
    public LocalVariableAttribute(ConstPool cp, String name) {
        super(cp, name, new byte[2]);
        ByteArray.write16bit(0, info, 0);
    }

    LocalVariableAttribute(ConstPool cp, int n, DataInputStream in)
        throws IOException
    {
        super(cp, n, in);
    }

    LocalVariableAttribute(ConstPool cp, String name, byte[] i) {
        super(cp, name, i);
    }

    /**
     * Appends a new entry to <code>local_variable_table</code>.
     *
     * @param startPc           <code>start_pc</code>
     * @param length            <code>length</code>
     * @param nameIndex         <code>name_index</code>
     * @param descriptorIndex   <code>descriptor_index</code>
     * @param index             <code>index</code>
     */
    public void addEntry(int startPc, int length, int nameIndex,
                         int descriptorIndex, int index) {
        int size = info.length;
        byte[] newInfo = new byte[size + 10];
        ByteArray.write16bit(tableLength() + 1, newInfo, 0);
        for (int i = 2; i < size; ++i)
            newInfo[i] = info[i];

        ByteArray.write16bit(startPc, newInfo, size);
        ByteArray.write16bit(length, newInfo, size + 2);
        ByteArray.write16bit(nameIndex, newInfo, size + 4);
        ByteArray.write16bit(descriptorIndex, newInfo, size + 6);
        ByteArray.write16bit(index, newInfo, size + 8);
        info = newInfo;
    }

    void renameClass(String oldname, String newname) {
        ConstPool cp = getConstPool();
        int n = tableLength();
        for (int i = 0; i < n; ++i) {
            int pos = i * 10 + 2;
            int index = ByteArray.readU16bit(info, pos + 6);
            if (index != 0) {
                String desc = cp.getUtf8Info(index);
                desc = renameEntry(desc, oldname, newname);
                ByteArray.write16bit(cp.addUtf8Info(desc), info, pos + 6);
            }
        }
    }

    String renameEntry(String desc, String oldname, String newname) {
        return Descriptor.rename(desc, oldname, newname);
    }

    void renameClass(Map classnames) {
        ConstPool cp = getConstPool();
        int n = tableLength();
        for (int i = 0; i < n; ++i) {
            int pos = i * 10 + 2;
            int index = ByteArray.readU16bit(info, pos + 6);
            if (index != 0) {
                String desc = cp.getUtf8Info(index);
                desc = renameEntry(desc, classnames);
                ByteArray.write16bit(cp.addUtf8Info(desc), info, pos + 6);
            }
        }
    }

    String renameEntry(String desc, Map classnames) {
        return Descriptor.rename(desc, classnames);
    }

    /**
     * For each <code>local_variable_table[i].index</code>,
     * this method increases <code>index</code> by <code>delta</code>.
     *
     * @param lessThan      the index does not change if it
     *                      is less than this value.
     */
    public void shiftIndex(int lessThan, int delta) {
        int size = info.length;
        for (int i = 2; i < size; i += 10){
            int org = ByteArray.readU16bit(info, i + 8);
            if (org >= lessThan)
                ByteArray.write16bit(org + delta, info, i + 8);
        }
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

            /* if pc == 0, then the local variable is a method parameter.
             */
            if (pc > where || (exclusive && pc == where && pc != 0))
                ByteArray.write16bit(pc + gapLength, info, pos);
            else if (pc + len > where || (exclusive && pc + len == where))
                ByteArray.write16bit(len + gapLength, info, pos + 2);
        }
    }

    /**
     * Returns the value of <code>local_variable_table[i].name_index</code>.
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
     * Returns the value of
     * <code>local_variable_table[i].descriptor_index</code>.
     * This represents the type descriptor of the local variable.
     * <p>
     * If this attribute represents a LocalVariableTypeTable attribute,
     * this method returns the value of
     * <code>local_variable_type_table[i].signature_index</code>.
     * It represents the type of the local variable.
     *
     * @param i         the i-th entry.
     */
    public int descriptorIndex(int i) {
        return ByteArray.readU16bit(info, i * 10 + 8);
    }

    /**
     * This method is equivalent to <code>descriptorIndex()</code>.
     * If this attribute represents a LocalVariableTypeTable attribute,
     * this method should be used instead of <code>descriptorIndex()</code>
     * since the method name is more appropriate.
     * 
     * @param i         the i-th entry.
     * @see #descriptorIndex(int)
     * @see SignatureAttribute#toFieldSignature(String)
     */
    public int signatureIndex(int i) {
        return descriptorIndex(i);
    }

    /**
     * Returns the type descriptor of the local variable
     * specified by <code>local_variable_table[i].descriptor_index</code>.
     * <p>
     * If this attribute represents a LocalVariableTypeTable attribute,
     * this method returns the type signature of the local variable
     * specified by <code>local_variable_type_table[i].signature_index</code>.
      *
     * @param i         the i-th entry.
     */
    public String descriptor(int i) {
        return getConstPool().getUtf8Info(descriptorIndex(i));
    }

    /**
     * This method is equivalent to <code>descriptor()</code>.
     * If this attribute represents a LocalVariableTypeTable attribute,
     * this method should be used instead of <code>descriptor()</code>
     * since the method name is more appropriate.
     *
     * <p>To parse the string, call <code>toFieldSignature(String)</code>
     * in <code>SignatureAttribute</code>.
     *
     * @param i         the i-th entry.
     * @see #descriptor(int)
     * @see SignatureAttribute#toFieldSignature(String)
     */
    public String signature(int i) {
        return descriptor(i);
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
        LocalVariableAttribute attr = makeThisAttr(newCp, dest);
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

            if (type != 0)  {
                String sig = cp.getUtf8Info(type);
                sig = Descriptor.rename(sig, classnames);
                type = newCp.addUtf8Info(sig);
            }

            ByteArray.write16bit(type, dest, j + 6);
            ByteArray.write16bit(index, dest, j + 8);
            j += 10;
        }

        return attr;
    }

    // LocalVariableTypeAttribute overrides this method.
    LocalVariableAttribute makeThisAttr(ConstPool cp, byte[] dest) {
        return new LocalVariableAttribute(cp, tag, dest);
    }
}
