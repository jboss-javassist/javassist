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

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;

import javassist.CannotCompileException;
import javassist.bytecode.StackMapTable.InsertLocal;
import javassist.bytecode.StackMapTable.NewRemover;
import javassist.bytecode.StackMapTable.Shifter;

/**
 * Another <code>stack_map</code> attribute defined in CLDC 1.1 for J2ME.
 *
 * <p>This is an entry in the attributes table of a Code attribute.
 * It was introduced by J2ME CLDC 1.1 (JSR 139) for pre-verification.
 *
 * <p>According to the CLDC specification, the sizes of some fields are not 16bit
 * but 32bit if the code size is more than 64K or the number of the local variables
 * is more than 64K.  However, for the J2ME CLDC technology, they are always 16bit.
 * The implementation of the StackMap class assumes they are 16bit.  
 *
 * @see MethodInfo#doPreverify
 * @see StackMapTable
 * @since 3.12
 */
public class StackMap extends AttributeInfo {
    /**
     * The name of this attribute <code>"StackMap"</code>.
     */
    public static final String tag = "StackMap";


    /**
     * Constructs a <code>stack_map</code> attribute.
     */
    StackMap(ConstPool cp, byte[] newInfo) {
        super(cp, tag, newInfo);
    }

    StackMap(ConstPool cp, int name_id, DataInputStream in)
        throws IOException
    {
        super(cp, name_id, in);
    }

    /**
     * Returns <code>number_of_entries</code>.
     */
    public int numOfEntries() {
    	return ByteArray.readU16bit(info, 0);
    }

    /**
     * <code>Top_variable_info.tag</code>.
     */
    public static final int TOP = 0;

    /**
     * <code>Integer_variable_info.tag</code>.
     */
    public static final int INTEGER = 1;

    /**
     * <code>Float_variable_info.tag</code>.
     */
    public static final int FLOAT = 2;

    /**
     * <code>Double_variable_info.tag</code>.
     */
    public static final int DOUBLE = 3;

    /**
     * <code>Long_variable_info.tag</code>.
     */
    public static final int LONG = 4;

    /**
     * <code>Null_variable_info.tag</code>.
     */
    public static final int NULL = 5;

    /**
     * <code>UninitializedThis_variable_info.tag</code>.
     */
    public static final int THIS = 6;

    /**
     * <code>Object_variable_info.tag</code>.
     */
    public static final int OBJECT = 7;

    /**
     * <code>Uninitialized_variable_info.tag</code>.
     */
    public static final int UNINIT = 8;

    /**
     * Makes a copy.
     */
    public AttributeInfo copy(ConstPool newCp, Map classnames) {
        Copier copier = new Copier(this, newCp, classnames);
        copier.visit();
        return copier.getStackMap();
    }

    /**
     * A code walker for a StackMap attribute.
     */
    public static class Walker {
        byte[] info;

        /**
         * Constructs a walker.
         */
        public Walker(StackMap sm) {
            info = sm.get();
        }

        /**
         * Visits each entry of the stack map frames. 
         */
        public void visit() {
            int num = ByteArray.readU16bit(info, 0);
            int pos = 2;
            for (int i = 0; i < num; i++) {
                int offset = ByteArray.readU16bit(info, pos);
                int numLoc = ByteArray.readU16bit(info, pos + 2);
                pos = locals(pos + 4, offset, numLoc);
                int numStack = ByteArray.readU16bit(info, pos);
                pos = stack(pos + 2, offset, numStack);
            }
        }

        /**
         * Invoked when <code>locals</code> of <code>stack_map_frame</code>
         * is visited.  
         */
        public int locals(int pos, int offset, int num) {
            return typeInfoArray(pos, offset, num, true);
        }

        /**
         * Invoked when <code>stack</code> of <code>stack_map_frame</code>
         * is visited.  
         */
        public int stack(int pos, int offset, int num) {
            return typeInfoArray(pos, offset, num, false);
        }

        /**
         * Invoked when an array of <code>verification_type_info</code> is
         * visited.
         *
         * @param num       the number of elements.
         * @param isLocals  true if this array is for <code>locals</code>.
         *                  false if it is for <code>stack</code>.
         */
        public int typeInfoArray(int pos, int offset, int num, boolean isLocals) {
            for (int k = 0; k < num; k++)
                pos = typeInfoArray2(k, pos);

            return pos;
        }

        int typeInfoArray2(int k, int pos) {
            byte tag = info[pos];
            if (tag == OBJECT) {
                int clazz = ByteArray.readU16bit(info, pos + 1);
                objectVariable(pos, clazz);
                pos += 3;
            }
            else if (tag == UNINIT) {
                int offsetOfNew = ByteArray.readU16bit(info, pos + 1);
                uninitialized(pos, offsetOfNew);
                pos += 3;
            }
            else {
                typeInfo(pos, tag);
                pos++;
            }

            return pos;
        }

        /**
         * Invoked when an element of <code>verification_type_info</code>
         * (except <code>Object_variable_info</code> and
         * <code>Uninitialized_variable_info</code>) is visited.
         */
        public void typeInfo(int pos, byte tag) {}

        /**
         * Invoked when an element of type <code>Object_variable_info</code>
         * is visited.
         */
        public void objectVariable(int pos, int clazz) {}

        /**
         * Invoked when an element of type <code>Uninitialized_variable_info</code>
         * is visited.
         */
        public void uninitialized(int pos, int offset) {}
    }

    static class Copier extends Walker {
        byte[] dest;
        ConstPool srcCp, destCp;
        Map classnames;

        Copier(StackMap map, ConstPool newCp, Map classnames) {
            super(map);
            srcCp = map.getConstPool();
            dest = new byte[info.length];
            destCp = newCp;
            this.classnames = classnames;
        }
        
        public void visit() {
            int num = ByteArray.readU16bit(info, 0);
            ByteArray.write16bit(num, dest, 0);
            super.visit();
        }

        public int locals(int pos, int offset, int num) {
            ByteArray.write16bit(offset, dest, pos - 4);
            return super.locals(pos, offset, num);
        }

        public int typeInfoArray(int pos, int offset, int num, boolean isLocals) {
            ByteArray.write16bit(num, dest, pos - 2);
            return super.typeInfoArray(pos, offset, num, isLocals);
        }

        public void typeInfo(int pos, byte tag) {
            dest[pos] = tag;
        }

        public void objectVariable(int pos, int clazz) {
            dest[pos] = OBJECT;
            int newClazz = srcCp.copy(clazz, destCp, classnames);
            ByteArray.write16bit(newClazz, dest, pos + 1);
        }

        public void uninitialized(int pos, int offset) {
            dest[pos] = UNINIT;
            ByteArray.write16bit(offset, dest, pos + 1);
        }

        public StackMap getStackMap() {
            return new StackMap(destCp, dest);
        }
    }

    /**
     * Updates this stack map table when a new local variable is inserted
     * for a new parameter.
     *
     * @param index          the index of the added local variable.
     * @param tag            the type tag of that local variable.
     *                       It is available by <code>StackMapTable.typeTagOf(char)</code>.
     * @param classInfo      the index of the <code>CONSTANT_Class_info</code> structure
     *                       in a constant pool table.  This should be zero unless the tag
     *                       is <code>ITEM_Object</code>.
     *
     * @see javassist.CtBehavior#addParameter(javassist.CtClass)
     * @see StackMapTable#typeTagOf(char)
     * @see ConstPool
     */
    public void insertLocal(int index, int tag, int classInfo)
        throws BadBytecode
    {
        byte[] data = new InsertLocal(this, index, tag, classInfo).doit();
        this.set(data);
    }

    static class SimpleCopy extends Walker {
        Writer writer;

        SimpleCopy(StackMap map) {
            super(map);
            writer = new Writer();
        }

        byte[] doit() {
            visit();
            return writer.toByteArray();
        }

        public void visit() {
            int num = ByteArray.readU16bit(info, 0);
            writer.write16bit(num);
            super.visit();
        }

        public int locals(int pos, int offset, int num) {
            writer.write16bit(offset);
            return super.locals(pos, offset, num);
        }

        public int typeInfoArray(int pos, int offset, int num, boolean isLocals) {
            writer.write16bit(num);
            return super.typeInfoArray(pos, offset, num, isLocals);
        }

        public void typeInfo(int pos, byte tag) {
            writer.writeVerifyTypeInfo(tag, 0);
        }

        public void objectVariable(int pos, int clazz) {
            writer.writeVerifyTypeInfo(OBJECT, clazz);
        }

        public void uninitialized(int pos, int offset) {
            writer.writeVerifyTypeInfo(UNINIT, offset);
        }
    }

    static class InsertLocal extends SimpleCopy {
        private int varIndex;
        private int varTag, varData;

        InsertLocal(StackMap map, int varIndex, int varTag, int varData) {
            super(map);
            this.varIndex = varIndex;
            this.varTag = varTag;
            this.varData = varData;
        }

        public int typeInfoArray(int pos, int offset, int num, boolean isLocals) {
            if (!isLocals || num < varIndex)
                return super.typeInfoArray(pos, offset, num, isLocals);

            writer.write16bit(num + 1);
            for (int k = 0; k < num; k++) {
                if (k == varIndex)
                    writeVarTypeInfo();

                pos = typeInfoArray2(k, pos);
            }

            if (num == varIndex)
                writeVarTypeInfo();

            return pos;
        }

        private void writeVarTypeInfo() {
            if (varTag == OBJECT)
                writer.writeVerifyTypeInfo(OBJECT, varData);
            else if (varTag == UNINIT)
                writer.writeVerifyTypeInfo(UNINIT, varData);
            else
                writer.writeVerifyTypeInfo(varTag, 0);
        }
    }

    void shiftPc(int where, int gapSize, boolean exclusive)
        throws BadBytecode
    {
        new Shifter(this, where, gapSize, exclusive).visit();
    }

    static class Shifter extends Walker {
        private int where, gap;
        private boolean exclusive;

        public Shifter(StackMap smt, int where, int gap, boolean exclusive) {
            super(smt);
            this.where = where;
            this.gap = gap;
            this.exclusive = exclusive;
        }

        public int locals(int pos, int offset, int num) {
            if (exclusive ? where <= offset : where < offset)
                ByteArray.write16bit(offset + gap, info, pos - 4);

            return super.locals(pos, offset, num);
        }
    }

    /**
     * Undocumented method.  Do not use; internal-use only.
     *
     * <p>This method is for javassist.convert.TransformNew.
     * It is called to update the stack map when
     * the NEW opcode (and the following DUP) is removed. 
     *
     * @param where     the position of the removed NEW opcode.
     */
     public void removeNew(int where) throws CannotCompileException {
         byte[] data = new NewRemover(this, where).doit();
         this.set(data);
    }

    static class NewRemover extends SimpleCopy {
        int posOfNew;

        NewRemover(StackMap map, int where) {
            super(map);
            posOfNew = where;
        }

        public int stack(int pos, int offset, int num) {
            return stackTypeInfoArray(pos, offset, num);
        }

        private int stackTypeInfoArray(int pos, int offset, int num) {
            int p = pos;
            int count = 0;
            for (int k = 0; k < num; k++) {
                byte tag = info[p];
                if (tag == OBJECT)
                    p += 3;
                else if (tag == UNINIT) {
                    int offsetOfNew = ByteArray.readU16bit(info, p + 1);
                    if (offsetOfNew == posOfNew)
                        count++;

                    p += 3;
                }
                else
                    p++;
            }

            writer.write16bit(num - count);
            for (int k = 0; k < num; k++) {
                byte tag = info[pos];
                if (tag == OBJECT) {
                    int clazz = ByteArray.readU16bit(info, pos + 1);
                    objectVariable(pos, clazz);
                    pos += 3;
                }
                else if (tag == UNINIT) {
                    int offsetOfNew = ByteArray.readU16bit(info, pos + 1);
                    if (offsetOfNew != posOfNew)
                        uninitialized(pos, offsetOfNew);

                    pos += 3;
                }
                else {
                    typeInfo(pos, tag);
                    pos++;
                }
            }

            return pos;
        }
    }

    /**
     * Prints this stack map.
     */
    public void print(java.io.PrintWriter out) {
        new Printer(this, out).print();
    }

    static class Printer extends Walker {
        private java.io.PrintWriter writer;

        public Printer(StackMap map, java.io.PrintWriter out) {
            super(map);
            writer = out;
        }

        public void print() {
            int num = ByteArray.readU16bit(info, 0);
            writer.println(num + " entries");
            visit();
        }

        public int locals(int pos, int offset, int num) {
            writer.println("  * offset " + offset);
            return super.locals(pos, offset, num);
        }
    }

    /**
     * Internal use only.
     */
    public static class Writer {
        // see javassist.bytecode.stackmap.MapMaker

        private ByteArrayOutputStream output;

        /**
         * Constructs a writer.
         */
        public Writer() {
            output = new ByteArrayOutputStream();
        }

        /**
         * Converts the written data into a byte array.
         */
        public byte[] toByteArray() {
            return output.toByteArray();
        }

        /**
         * Converts to a <code>StackMap</code> attribute.
         */
        public StackMap toStackMap(ConstPool cp) {
            return new StackMap(cp, output.toByteArray());
        }

        /**
         * Writes a <code>union verification_type_info</code> value.
         *
         * @param data      <code>cpool_index</code> or <code>offset</code>.
         */
        public void writeVerifyTypeInfo(int tag, int data) {
            output.write(tag);
            if (tag == StackMap.OBJECT || tag == StackMap.UNINIT)
                write16bit(data);
        }

        /**
         * Writes a 16bit value.
         */
        public void write16bit(int value) {
            output.write((value >>> 8) & 0xff);
            output.write(value & 0xff);
        }
    }
}
