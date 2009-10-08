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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import javassist.bytecode.stackmap.*;

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
        ConstPool cp = getConstPool();
        byte[] srcInfo = info;
        byte[] newInfo = new byte[srcInfo.length];
        int num = numOfEntries();
        ByteArray.write16bit(num, newInfo, 0);
        int pos = 2;
        for (int i = 0; i < num; i++) {
            int offset = ByteArray.readU16bit(srcInfo, pos);
            ByteArray.write16bit(offset, newInfo, pos);
            pos += 2;
            int numLoc = ByteArray.readU16bit(srcInfo, pos);
            ByteArray.write16bit(numLoc, newInfo, pos);
            pos = copyFrames(srcInfo, newInfo, pos + 2, numLoc, cp, newCp,
                             classnames);
            int numStack = ByteArray.readU16bit(srcInfo, pos);
            ByteArray.write16bit(numStack, newInfo, pos);
            pos = copyFrames(srcInfo, newInfo, pos + 2, numStack, cp, newCp,
                             classnames);
        }

        return new StackMap(newCp, newInfo);
    }

    private static int copyFrames(byte[] srcInfo, byte[] newInfo, int pos,
            int numFrames, ConstPool cp, ConstPool newCp, Map classnames) {
        for (int k = 0; k < numFrames; k++) {
            byte tag = srcInfo[pos];
            newInfo[pos] = tag;
            if (tag == OBJECT) {
                int clazz = ByteArray.readU16bit(srcInfo, pos + 1);
                clazz = cp.copy(clazz, newCp, classnames);
                ByteArray.write16bit(clazz, newInfo, pos + 1);
                pos += 3;
            }
            else if (tag == UNINIT) {
                ByteArray.write16bit(ByteArray.readU16bit(srcInfo, pos + 1),
                        newInfo, pos + 1);
                pos += 3;
            }
            else
                pos++;
        }

        return pos;
    }

    int i;

    static class NullType extends TypeData.BasicType {
        NullType() {
            super("null", StackMapTable.NULL);
        }
    }

    static final NullType nullType = new NullType();

    static class Conv extends StackMapTable.Walker {
        private ByteArrayOutputStream output;
        private ConstPool pool;
        private int offset;
        private TypedBlock frame;

        // more than one entries must be included!

        public Conv(byte[] data, MethodInfo minfo, CodeAttribute ca)
                throws BadBytecode {
            super(data);
            output = new ByteArrayOutputStream();
            pool = minfo.getConstPool();
            offset = -1;
            frame = TypedBlock.make(minfo, ca);
        }

        private void write16bit(int value) {
            output.write((byte) (value >>> 8));
            output.write((byte) value);
        }

        private void writeFrame() {
            int numLocals = frame.numLocals;
            int numStack = frame.stackTop;
        }

        private void writeTypes(TypeData[] types, int num) {
            for (int i = 0; i < num; i++) {
                TypeData t = types[i];
                int tag = t.getTypeTag();
                if (t.is2WordType())
                    i++;

                output.write(tag);
                if (tag == UNINIT) {
                    write16bit(t.getTypeData(pool));
                }
            }
        }

        public void sameFrame(int pos, int offsetDelta) {
            offset += offsetDelta + 1;
        }

        public void sameLocals(int pos, int offsetDelta, int stackTag,
                int stackData) {
            offset += offsetDelta + 1;
        }

        public void chopFrame(int pos, int offsetDelta, int k) {
            offset += offsetDelta + 1;
        }

        public void appendFrame(int pos, int offsetDelta, int[] tags, int[] data) {
            offset += offsetDelta + 1;
        }

        public void fullFrame(int pos, int offsetDelta, int[] localTags,
                int[] localData, int[] stackTags, int[] stackData) {
            offset += offsetDelta + 1;
        }
    }
}
