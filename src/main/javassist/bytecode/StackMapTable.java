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
import java.io.DataOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Map;
import javassist.CannotCompileException;

/**
 * <code>stack_map</code> attribute.
 *
 * <p>This is an entry in the attributes table of a Code attribute.
 * It was introduced by J2SE 6 for the process of verification by
 * typechecking.
 *
 * @see StackMap
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
    StackMapTable(ConstPool cp, byte[] newInfo) {
        super(cp, tag, newInfo);
    }

    StackMapTable(ConstPool cp, int name_id, DataInputStream in)
        throws IOException
    {
        super(cp, name_id, in);
    }

    /**
     * Makes a copy.
     *
     * @exception RuntimeCopyException  if a <code>BadBytecode</code>
     *                          exception is thrown while copying,
     *                          it is converted into
     *                          <code>RuntimeCopyException</code>.
     *
     */
    public AttributeInfo copy(ConstPool newCp, Map classnames)
        throws RuntimeCopyException
    {
        try {
            return new StackMapTable(newCp,
                            new Copier(this.constPool, info, newCp).doit());
        }
        catch (BadBytecode e) {
            throw new RuntimeCopyException("bad bytecode. fatal?"); 
        }
    }

    /**
     * An exception that may be thrown by <code>copy()</code>
     * in <code>StackMapTable</code>.
     */
    public static class RuntimeCopyException extends RuntimeException {
        /**
         * Constructs an exception.
         */
        public RuntimeCopyException(String s) {
            super(s);
        }
    }

    void write(DataOutputStream out) throws IOException {
        super.write(out);
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
     * A code walker for a StackMapTable attribute.
     */
    public static class Walker {
        byte[] info;
        int numOfEntries;

        /**
         * Constructs a walker.
         *
         * @param smt       the StackMapTable that this walker
         *                  walks around.
         */
        public Walker(StackMapTable smt) {
            this(smt.get());
        }

        /**
         * Constructs a walker.
         *
         * @param data      the <code>info</code> field of the
         *                  <code>attribute_info</code> structure.
         *                  It can be obtained by <code>get()</code>
         *                  in the <code>AttributeInfo</code> class.
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
        public void parse() throws BadBytecode {
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
        public void sameFrame(int pos, int offsetDelta) throws BadBytecode {}

        private int sameLocals(int pos, int type) throws BadBytecode {
            int top = pos;
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

            sameLocals(top, offset, tag, data);
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
        public void sameLocals(int pos, int offsetDelta, int stackTag, int stackData)
            throws BadBytecode {}

        /**
         * Invoked if the visited frame is a <code>chop_frame</code>.
         * 
         * @param pos               the position.
         * @param offsetDelta
         * @param k                 the <cod>k</code> last locals are absent. 
         */
        public void chopFrame(int pos, int offsetDelta, int k) throws BadBytecode {}

        private int appendFrame(int pos, int type) throws BadBytecode {
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
        public void appendFrame(int pos, int offsetDelta, int[] tags, int[] data)
            throws BadBytecode {} 

        private int fullFrame(int pos) throws BadBytecode {
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
        public void fullFrame(int pos, int offsetDelta, int[] localTags, int[] localData,
                              int[] stackTags, int[] stackData)
            throws BadBytecode {}

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

    static class SimpleCopy extends Walker {
        private Writer writer;

        public SimpleCopy(byte[] data) {
            super(data);
            writer = new Writer(data.length);
        }

        public byte[] doit() throws BadBytecode {
            parse();
            return writer.toByteArray();
        }

        public void sameFrame(int pos, int offsetDelta) {
            writer.sameFrame(offsetDelta);
        }

        public void sameLocals(int pos, int offsetDelta, int stackTag, int stackData) {
            writer.sameLocals(offsetDelta, stackTag, copyData(stackTag, stackData));
        }

        public void chopFrame(int pos, int offsetDelta, int k) {
            writer.chopFrame(offsetDelta, k);
        }

        public void appendFrame(int pos, int offsetDelta, int[] tags, int[] data) {
            writer.appendFrame(offsetDelta, tags, copyData(tags, data));
        }

        public void fullFrame(int pos, int offsetDelta, int[] localTags, int[] localData,
                              int[] stackTags, int[] stackData) {
            writer.fullFrame(offsetDelta, localTags, copyData(localTags, localData),
                             stackTags, copyData(stackTags, stackData));
        }

        protected int copyData(int tag, int data) {
            return data;
        }

        protected int[] copyData(int[] tags, int[] data) {
            return data;
        }
    }

    static class Copier extends SimpleCopy {
        private ConstPool srcPool, destPool;

        public Copier(ConstPool src, byte[] data, ConstPool dest) {
            super(data);
            srcPool = src;
            destPool = dest;
        }

        protected int copyData(int tag, int data) {
            if (tag == OBJECT)
                return srcPool.copy(data, destPool, null); 
            else
                return data;
        }

        protected int[] copyData(int[] tags, int[] data) {
            int[] newData = new int[data.length];
            for (int i = 0; i < data.length; i++)
                if (tags[i] == OBJECT)
                    newData[i] = srcPool.copy(data[i], destPool, null);
                else
                    newData[i] = data[i];

            return newData;
        }
    }

    /**
     * Updates this stack map table when a new local variable is inserted
     * for a new parameter.
     *
     * @param index          the index of the added local variable.
     * @param tag            the type tag of that local variable. 
     * @param classInfo      the index of the <code>CONSTANT_Class_info</code> structure
     *                       in a constant pool table.  This should be zero unless the tag
     *                       is <code>ITEM_Object</code>.
     *
     * @see javassist.CtBehavior#addParameter(javassist.CtClass)
     * @see #typeTagOf(char)
     * @see ConstPool
     */
    public void insertLocal(int index, int tag, int classInfo)
        throws BadBytecode
    {
        byte[] data = new InsertLocal(this.get(), index, tag, classInfo).doit();
        this.set(data);
    }

    /**
     * Returns the tag of the type specified by the
     * descriptor.  This method returns <code>INTEGER</code>
     * unless the descriptor is either D (double), F (float),
     * J (long), L (class type), or [ (array).
     *
     * @param descriptor        the type descriptor.
     * @see Descriptor
     */
    public static int typeTagOf(char descriptor) {
        switch (descriptor) {
        case 'D' :
            return DOUBLE;
        case 'F' :
            return FLOAT;
        case 'J' :
            return LONG;
        case 'L' :
        case '[' :
            return OBJECT;
        // case 'V' :
        default :
            return INTEGER;
        }
    }

    /* This implementation assumes that a local variable initially
     * holding a parameter value is never changed to be a different
     * type.
     * 
     */
    static class InsertLocal extends SimpleCopy {
        private int varIndex;
        private int varTag, varData;

        public InsertLocal(byte[] data, int varIndex, int varTag, int varData) {
            super(data);
            this.varIndex = varIndex;
            this.varTag = varTag;
            this.varData = varData;
        }

        public void fullFrame(int pos, int offsetDelta, int[] localTags, int[] localData,
                              int[] stackTags, int[] stackData) {
            int len = localTags.length;
            if (len < varIndex) {
                super.fullFrame(pos, offsetDelta, localTags, localData, stackTags, stackData);
                return;
            }

            int typeSize = (varTag == LONG || varTag == DOUBLE) ? 2 : 1;
            int[] localTags2 = new int[len + typeSize];
            int[] localData2 = new int[len + typeSize];
            int index = varIndex;
            int j = 0;
            for (int i = 0; i < len; i++) {
                if (j == index)
                    j += typeSize;

                localTags2[j] = localTags[i];
                localData2[j++] = localData[i];
            }

            localTags2[index] = varTag;
            localData2[index] = varData;
            if (typeSize > 1) {
                localTags2[index + 1] = TOP;
                localData2[index + 1] = 0;
            }

            super.fullFrame(pos, offsetDelta, localTags2, localData2, stackTags, stackData);
        }
    }

    /**
     * A writer of stack map tables.
     */
    public static class Writer {
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
         * Constructs and a return a stack map table containing
         * the written stack map entries.
         *
         * @param cp        the constant pool used to write
         *                  the stack map entries.
         */
        public StackMapTable toStackMapTable(ConstPool cp) {
            return new StackMapTable(cp, toByteArray());
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
         * Writes a <code>append_frame</code>.  The number of the appended
         * locals is specified by the length of <code>tags</code>.
         *
         * @param tags           <code>locals[].tag</code>.
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
         * <code>number_of_locals</code> and <code>number_of_stack_items</code>
         * are specified by the the length of <code>localTags</code> and
         * <code>stackTags</code>.
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
            write16(n);
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

    /**
     * Prints the stack table map.
     */
    public void println(PrintWriter w) {
        Printer.print(this, w);
    }

    /**
     * Prints the stack table map.
     *
     * @param ps    a print stream such as <code>System.out</code>.
     */
    public void println(java.io.PrintStream ps) {
        Printer.print(this, new java.io.PrintWriter(ps, true));
    }

    static class Printer extends Walker {
        private PrintWriter writer;
        private int offset;

        /**
         * Prints the stack table map.
         */
        public static void print(StackMapTable smt, PrintWriter writer) {
            try {
                new Printer(smt.get(), writer).parse();
            }
            catch (BadBytecode e) {
                writer.println(e.getMessage());
            }
        }

        Printer(byte[] data, PrintWriter pw) {
            super(data);
            writer = pw;
            offset = -1;
        }

        public void sameFrame(int pos, int offsetDelta) {
            offset += offsetDelta + 1;
            writer.println(offset + " same frame: " + offsetDelta);
        }

        public void sameLocals(int pos, int offsetDelta, int stackTag, int stackData) {
            offset += offsetDelta + 1;
            writer.println(offset + " same locals: " + offsetDelta);
            printTypeInfo(stackTag, stackData);
        }

        public void chopFrame(int pos, int offsetDelta, int k) {
            offset += offsetDelta + 1;
            writer.println(offset + " chop frame: " + offsetDelta + ",    " + k + " last locals");
        }

        public void appendFrame(int pos, int offsetDelta, int[] tags, int[] data) {
            offset += offsetDelta + 1;
            writer.println(offset + " append frame: " + offsetDelta);
            for (int i = 0; i < tags.length; i++)
                printTypeInfo(tags[i], data[i]);
        }

        public void fullFrame(int pos, int offsetDelta, int[] localTags, int[] localData,
                              int[] stackTags, int[] stackData) {
            offset += offsetDelta + 1;
            writer.println(offset + " full frame: " + offsetDelta);
            writer.println("[locals]");
            for (int i = 0; i < localTags.length; i++)
                printTypeInfo(localTags[i], localData[i]);

            writer.println("[stack]");
            for (int i = 0; i < stackTags.length; i++)
                printTypeInfo(stackTags[i], stackData[i]);
        }

        private void printTypeInfo(int tag, int data) {
            String msg = null;
            switch (tag) {
            case TOP :
                msg = "top";
                break;
            case INTEGER :
                msg = "integer";
                break;
            case FLOAT :
                msg = "float";
                break;
            case DOUBLE :
                msg = "double";
                break;
            case LONG :
                msg = "long";
                break;
            case NULL :
                msg = "null";
                break;
            case THIS :
                msg = "this";
                break;
            case OBJECT :
                msg = "object (cpool_index " + data + ")";
                break;
            case UNINIT :
                msg = "uninitialized (offset " + data + ")";
                break;
            }

            writer.print("    ");
            writer.println(msg);
        }
    }

    void shiftPc(int where, int gapSize, boolean exclusive)
        throws BadBytecode
    {
        new Shifter(this, where, gapSize, exclusive).doit();
    }

    static class Shifter extends Walker {
        private StackMapTable stackMap;
        private int where, gap;
        private int position;
        private byte[] updatedInfo;
        private boolean exclusive;

        public Shifter(StackMapTable smt, int where, int gap, boolean exclusive) {
            super(smt);
            stackMap = smt;
            this.where = where;
            this.gap = gap;
            this.position = 0;
            this.updatedInfo = null;
            this.exclusive = exclusive;
        }

        public void doit() throws BadBytecode {
            parse();
            if (updatedInfo != null)
                stackMap.set(updatedInfo);
        }

        public void sameFrame(int pos, int offsetDelta) {
            update(pos, offsetDelta, 0, 251);
        }

        public void sameLocals(int pos, int offsetDelta, int stackTag, int stackData) {
            update(pos, offsetDelta, 64, 247);
        }

        private void update(int pos, int offsetDelta, int base, int entry) {
            int oldPos = position;
            position = oldPos + offsetDelta + (oldPos == 0 ? 0 : 1);
            boolean match;
            if (exclusive)
                match = oldPos < where  && where <= position;
            else
                match = oldPos <= where  && where < position;

            if (match) {
                int newDelta = offsetDelta + gap;
                position += gap;
                if (newDelta < 64)
                    info[pos] = (byte)(newDelta + base);
                else if (offsetDelta < 64) {
                    byte[] newinfo = insertGap(info, pos, 2);
                    newinfo[pos] = (byte)entry;
                    ByteArray.write16bit(newDelta, newinfo, pos + 1);
                    updatedInfo = newinfo;
                }
                else
                    ByteArray.write16bit(newDelta, info, pos + 1);
            }
        }

        private static byte[] insertGap(byte[] info, int where, int gap) {
            int len = info.length;
            byte[] newinfo = new byte[len + gap];
            for (int i = 0; i < len; i++)
                newinfo[i + (i < where ? 0 : gap)] = info[i];

            return newinfo;
        }

        public void chopFrame(int pos, int offsetDelta, int k) {
            update(pos, offsetDelta);
        }

        public void appendFrame(int pos, int offsetDelta, int[] tags, int[] data) {
            update(pos, offsetDelta);
        }

        public void fullFrame(int pos, int offsetDelta, int[] localTags, int[] localData,
                              int[] stackTags, int[] stackData) {
            update(pos, offsetDelta);
        }

        private void update(int pos, int offsetDelta) {
            int oldPos = position;
            position = oldPos + offsetDelta + (oldPos == 0 ? 0 : 1);
            boolean match;
            if (exclusive)
                match = oldPos < where  && where <= position;
            else
                match = oldPos <= where  && where < position;

            if (match) {
                int newDelta = offsetDelta + gap;
                ByteArray.write16bit(newDelta, info, pos + 1);
                position += gap;
            }
        }
    }

    /**
     * Undocumented method.  Do not use; internal-use only.
     *
     * <p>This method is for javassist.convert.TransformNew.
     * It is called to update the stack map table when
     * the NEW opcode (and the following DUP) is removed. 
     *
     * @param where     the position of the removed NEW opcode.
     */
     public void removeNew(int where) throws CannotCompileException {
        try {
            byte[] data = new NewRemover(this.get(), where).doit();
            this.set(data);
        }
        catch (BadBytecode e) {
            throw new CannotCompileException("bad stack map table", e);
        }
    }

    static class NewRemover extends SimpleCopy {
        int posOfNew;

        public NewRemover(byte[] data, int pos) {
            super(data);
            posOfNew = pos;
        }

        public void sameLocals(int pos, int offsetDelta, int stackTag, int stackData) {
            if (stackTag == UNINIT && stackData == posOfNew)
                super.sameFrame(pos, offsetDelta);
            else
                super.sameLocals(pos, offsetDelta, stackTag, stackData);
        }

        public void fullFrame(int pos, int offsetDelta, int[] localTags, int[] localData,
                              int[] stackTags, int[] stackData) {
            int n = stackTags.length - 1;
            for (int i = 0; i < n; i++)
                if (stackTags[i] == UNINIT && stackData[i] == posOfNew
                    && stackTags[i + 1] == UNINIT && stackData[i + 1] == posOfNew) {
                    n++;
                    int[] stackTags2 = new int[n - 2];
                    int[] stackData2 = new int[n - 2];
                    int k = 0;
                    for (int j = 0; j < n; j++)
                        if (j == i)
                            j++;
                        else {
                            stackTags2[k] = stackTags[j];
                            stackData2[k++] = stackData[j];
                        }

                    stackTags = stackTags2;
                    stackData = stackData2;
                    break;
                }

            super.fullFrame(pos, offsetDelta, localTags, localData, stackTags, stackData);
        }
    }
}
