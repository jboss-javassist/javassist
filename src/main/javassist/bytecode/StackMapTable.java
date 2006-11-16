/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2006 Shigeru Chiba. All Rights Reserved.
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
import java.io.DataOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * <code>stack_map</code> attribute.
 *
 * <p>This is an entry in the attributes table of a Code attribute.
 * It was introduced by J2SE 6 for the process of verification by
 * typechecking.
 *
 * @since 3.4
 */
public class StackMapTable extends AttributeInfo {
    /**
     * The name of this attribute <code>"StackMapTable"</code>.
     */
    public static final String tag = "StackMapTable";

    /**
     * Constructs a <code>stack_map</code> attribute.
     */
    private StackMapTable(ConstPool cp, byte[] newInfo) {
        super(cp, tag, newInfo);
    }

    StackMapTable(ConstPool cp, int name_id, DataInputStream in)
        throws IOException
    {
        super(cp, name_id, in);
    }

    /**
     * Makes a copy.
     */
    public AttributeInfo copy(ConstPool newCp, Map classnames) {
        int s = info.length;
        byte[] newInfo = new byte[s];
        System.arraycopy(info, 0, newInfo, 0, s);
        return new StackMapTable(newCp, newInfo);
    }

    void write(DataOutputStream out) throws IOException {
        super.write(out);
    }

    /**
     * <code>Top_variable_info.tag</code>.
     */
    public static final int TOP = 0;

    /**
     * <code>Float_variable_info.tag</code>.
     */
    public static final int INTEGER = 1;

    /**
     * <code>Integer_variable_info.tag</code>.
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
     * A code walker for a StackMapTable attribute.
     */
    static class Walker {
        byte[] info;
        int numOfEntries;

        /**
         * Constructs a walker.
         *
         * @param data      the <code>info</code> field of the
         *                  <code>attribute_info</code> structure.
         */
        public Walker(byte[] data) {
            info = data;
            numOfEntries = ByteArray.readU16bit(data, 0);
        }

        /**
         * Returns the number of the entries.
         */
        public final int size() { return numOfEntries; }

        /**
         * Visits each entry of the stack map frames. 
         */
        public final void parse() throws BadBytecode {
            int n = numOfEntries;
            int pos = 2;
            for (int i = 0; i < n; i++)
                pos = stackMapFrames(pos, i);
        }

        /**
         * Invoked when the next entry of the stack map frames is visited.
         *
         * @param pos       the position of the frame in the <code>info</code>
         *                  field of <code>attribute_info</code> structure.
         * @param nth       the frame is the N-th
         *                  (0, 1st, 2nd, 3rd, 4th, ...) entry. 
         * @return          the position of the next frame.
         */
        int stackMapFrames(int pos, int nth) throws BadBytecode {
            int type = info[pos] & 0xff;
            if (type < 64) {
                sameFrame(pos, type);
                pos++;
            }
            else if (type < 128)
                pos = sameLocals(pos, type);
            else if (type < 247)
                throw new BadBytecode("bad frame_type in StackMapTable");
            else if (type == 247)   // SAME_LOCALS_1_STACK_ITEM_EXTENDED
                pos = sameLocals(pos, type);
            else if (type < 251) {
                int offset = ByteArray.readU16bit(info, pos + 1);
                chopFrame(pos, offset, 251 - type);
                pos += 3;
            }
            else if (type == 251) { // SAME_FRAME_EXTENDED
                int offset = ByteArray.readU16bit(info, pos + 1);
                sameFrame(pos, offset);
                pos += 3;
            }
            else if (type < 255)
                pos = appendFrame(pos, type);
            else    // FULL_FRAME
                pos = fullFrame(pos);

            return pos;
        }

        /**
         * Invoked if the visited frame is a <code>same_frame</code> or
         * a <code>same_frame_extended</code>.
         *
         * @param pos       the position of this frame in the <code>info</code>
         *                  field of <code>attribute_info</code> structure.
         * @param offsetDelta
         */
        public void sameFrame(int pos, int offsetDelta) {}

        private int sameLocals(int pos, int type) {
            int offset;
            if (type < 128)
                offset = type - 64;
            else { // type == 247
                offset = ByteArray.readU16bit(info, pos + 1);
                pos += 2;
            }

            int tag = info[pos + 1] & 0xff;
            int data = 0;
            if (tag == OBJECT || tag == UNINIT) {
                data = ByteArray.readU16bit(info, pos + 2);
                pos += 2;
            }

            sameLocals(pos, offset, tag, data);
            return pos + 2;
        }

        /**
         * Invoked if the visited frame is a <code>same_locals_1_stack_item_frame</code>
         * or a <code>same_locals_1_stack_item_frame_extended</code>.
         *
         * @param pos               the position.
         * @param offsetDelta
         * @param stackTag          <code>stack[0].tag</code>.
         * @param stackData         <code>stack[0].cpool_index</code>
         *                          if the tag is <code>OBJECT</code>,
         *                          or <code>stack[0].offset</code>
         *                          if the tag is <code>UNINIT</code>.
         */
        public void sameLocals(int pos, int offsetDelta, int stackTag, int stackData) {}

        /**
         * Invoked if the visited frame is a <code>chop_frame</code>.
         * 
         * @param pos               the position.
         * @param offsetDelta
         * @param k                 the <cod>k</code> last locals are absent. 
         */
        public void chopFrame(int pos, int offsetDelta, int k) {}

        private int appendFrame(int pos, int type) {
            int k = type - 251;
            int offset = ByteArray.readU16bit(info, pos + 1);
            int[] tags = new int[k];
            int[] data = new int[k];
            int p = pos + 3;
            for (int i = 0; i < k; i++) {
                int tag = info[p] & 0xff;
                tags[i] = tag;
                if (tag == OBJECT || tag == UNINIT) {
                    data[i] = ByteArray.readU16bit(info, p + 1);
                    p += 3;
                }
                else {
                    data[i] = 0;
                    p++;
                }
            }

            appendFrame(pos, offset, tags, data);
            return p;
        }

        /**
         * Invoked if the visited frame is a <code>append_frame</code>.
         *
         * @param pos           the position.
         * @param offsetDelta
         * @param tags          <code>locals[i].tag</code>.
         * @param data          <code>locals[i].cpool_index</code>
         *                      or <cod>locals[i].offset</code>.
         */
        public void appendFrame(int pos, int offsetDelta, int[] tags, int[] data) {} 

        private int fullFrame(int pos) {
            int offset = ByteArray.readU16bit(info, pos + 1);
            int numOfLocals = ByteArray.readU16bit(info, pos + 3);
            int[] localsTags = new int[numOfLocals];
            int[] localsData = new int[numOfLocals];
            int p = verifyTypeInfo(pos + 5, numOfLocals, localsTags, localsData);
            int numOfItems = ByteArray.readU16bit(info, p);
            int[] itemsTags = new int[numOfItems];
            int[] itemsData = new int[numOfItems];
            p = verifyTypeInfo(p + 2, numOfItems, itemsTags, itemsData);
            fullFrame(pos, offset, localsTags, localsData, itemsTags, itemsData);
            return p;
        }

        /**
         * Invoked if the visited frame is <code>full_frame</code>.
         *
         * @param pos               the position.
         * @param offsetDelta
         * @param localTags         <code>locals[i].tag</code>
         * @param localData         <code>locals[i].cpool_index</code>
         *                          or <code>locals[i].offset</code>
         * @param stackTags         <code>stack[i].tag</code>
         * @param stackData         <code>stack[i].cpool_index</code>
         *                          or <code>stack[i].offset</code>
         */
        void fullFrame(int pos, int offsetDelta, int[] localTags, int[] localData,
                       int[] stackTags, int[] stackData) {}

        private int verifyTypeInfo(int pos, int n, int[] tags, int[] data) {
            for (int i = 0; i < n; i++) {
                int tag = info[pos++] & 0xff;
                tags[i] = tag;
                if (tag == OBJECT || tag == UNINIT) {
                    data[i] = ByteArray.readU16bit(info, pos);
                    pos += 2;
                }
            }

            return pos;
        }
    }

    /**
     * A writer of stack map tables.
     */
    static class Writer {
        ByteArrayOutputStream output;
        int numOfEntries;

        /**
         * Constructs a writer.
         * @param size      the initial buffer size.
         */
        public Writer(int size) {
            output = new ByteArrayOutputStream(size);
            numOfEntries = 0;
            output.write(0);        // u2 number_of_entries
            output.write(0);
        }

        /**
         * Returns the stack map table written out.
         */
        public byte[] toByteArray() {
            byte[] b = output.toByteArray();
            ByteArray.write16bit(numOfEntries, b, 0);
            return b;
        }

        /**
         * Writes a <code>same_frame</code> or a <code>same_frame_extended</code>.
         */
        public void sameFrame(int offsetDelta) {
            numOfEntries++;
            if (offsetDelta < 64)
                output.write(offsetDelta);
            else {
                output.write(251);  // SAME_FRAME_EXTENDED
                write16(offsetDelta);
            }
        }

        /**
         * Writes a <code>same_locals_1_stack_item</code>
         * or a <code>same_locals_1_stack_item_extended</code>.
         *
         * @param tag           <code>stack[0].tag</code>.
         * @param data          <code>stack[0].cpool_index</code>
         *                      if the tag is <code>OBJECT</code>,
         *                      or <cod>stack[0].offset</code>
         *                      if the tag is <code>UNINIT</code>.
         *                      Otherwise, this parameter is not used.
         */
        public void sameLocals(int offsetDelta, int tag, int data) {
            numOfEntries++;
            if (offsetDelta < 64)
                output.write(offsetDelta + 64);
            else {
                output.write(247);  // SAME_LOCALS_1_STACK_ITEM_EXTENDED
                write16(offsetDelta);
            }

            writeTypeInfo(tag, data);
        }

        /**
         * Writes a <code>chop_frame</code>.
         *
         * @param k                 the number of absent locals. 1, 2, or 3.
         */
        public void chopFrame(int offsetDelta, int k) {
            numOfEntries++;
            output.write(251 - k);
            write16(offsetDelta);
        }

        /**
         * Writes a <code>append_frame</code>.
         *
         * @param tag           <code>locals[].tag</code>.
         *                      The length of this array must be
         *                      either 1, 2, or 3.
         * @param data          <code>locals[].cpool_index</code>
         *                      if the tag is <code>OBJECT</code>,
         *                      or <cod>locals[].offset</code>
         *                      if the tag is <code>UNINIT</code>.
         *                      Otherwise, this parameter is not used.
         */
        public void appendFrame(int offsetDelta, int[] tags, int[] data) {
            numOfEntries++;
            int k = tags.length;    // k is 1, 2, or 3
            output.write(k + 251);
            write16(offsetDelta);
            for (int i = 0; i < k; i++)
                writeTypeInfo(tags[i], data[i]);
        }

        /**
         * Writes a <code>full_frame</code>.
         *
         * @param localTags     <code>locals[].tag</code>.
         * @param localData     <code>locals[].cpool_index</code>
         *                      if the tag is <code>OBJECT</code>,
         *                      or <cod>locals[].offset</code>
         *                      if the tag is <code>UNINIT</code>.
         *                      Otherwise, this parameter is not used.
         * @param stackTags     <code>stack[].tag</code>.
         * @param stackData     <code>stack[].cpool_index</code>
         *                      if the tag is <code>OBJECT</code>,
         *                      or <cod>stack[].offset</code>
         *                      if the tag is <code>UNINIT</code>.
         *                      Otherwise, this parameter is not used.
         */
        public void fullFrame(int offsetDelta, int[] localTags, int[] localData,
                              int[] stackTags, int[] stackData) {
            numOfEntries++;
            output.write(255);      // FULL_FRAME
            write16(offsetDelta);
            int n = localTags.length;
            write16(n);
            for (int i = 0; i < n; i++)
                writeTypeInfo(localTags[i], localData[i]);

            n = stackTags.length;
            for (int i = 0; i < n; i++)
                writeTypeInfo(stackTags[i], stackData[i]);
        }

        private void writeTypeInfo(int tag, int data) {
            output.write(tag);
            if (tag == OBJECT || tag == UNINIT)
                write16(data);
        }

        private void write16(int value) {
            output.write((value >>> 8) & 0xff);
            output.write(value & 0xff);
        }
    }
}
