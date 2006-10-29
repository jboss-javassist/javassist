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
import java.io.IOException;
import java.util.Map;

/**
 * <code>stack_map</code> attribute.
 *
 * <p>This is an entry in the attributes table of a Code attribute.
 * It was introduced by J2SE 6 for the process of verification by
 * typechecking.
 */
public class StackMapTable extends AttributeInfo {
    /**
     * The name of this attribute <code>"StackMapTable"</code>.
     */
    public static final String tag = "StackMapTable";

    private StackMapFrame[] entries;    // may be null

    /**
     * Constructs a <code>stack_map</code> attribute.
     */
    public StackMapTable(ConstPool cp) {
        this(cp, (byte[])null);
    }

    private StackMapTable(ConstPool cp, byte[] newInfo) {
        super(cp, tag, newInfo);
        entries = null;
    }

    StackMapTable(ConstPool cp, int name_id, DataInputStream in)
        throws IOException
    {
        super(cp, name_id, in);
        entries = null;
    }

    /**
     * Makes a copy.
     */
    public AttributeInfo copy(ConstPool newCp, Map classnames) {
        toByteArray(false);
        int s = info.length;
        byte[] newInfo = new byte[s];
        System.arraycopy(info, 0, newInfo, 0, s);
        return new StackMapTable(newCp, newInfo);
    }

    void write(DataOutputStream out) throws IOException {
        toByteArray(true);
        super.write(out);
    }

    private void parseMap() {
        byte[] data = info;
        int n = ByteArray.readU16bit(data, 0);
        entries = new StackMapFrame[n];
    }

    private void toByteArray(boolean clear) {
        if (entries != null)
            ; // unparse 
    }

    /**
     * <code>union stack_map_frame</code>
     */
    public static class StackMapFrame {} 

    /*
     * verification_type_info is represented by a pair of
     * 2 int variables (tag and cpool_index/offset). 
     */

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
     * <code>same_frame</code>.
     * The frame has exactly the same locals as the previous
     * stack map frame.
     */
    public static class SameFrame extends StackMapFrame {
        /**
         * The maximum value of <code>SAME</code>.
         */
        public static final int FRAME_TYPE_MAX = 63;

        /**
         * <code>u1 frame_type</code>.
         */
        public int frameType;
    }

    /**
     * <code>same_locals_1_stack_item_frame</code> or 
     * <code>same_locals_1_stack_item_frame_extended</code>. 
     */
    public static class SameLocals extends StackMapFrame {
        /**
         * The minimum value of <code>SAME_LOCALS_1_STACK_ITEM</code>.
         */
        public static final int FRAME_TYPE = 64;

        /**
         * The maximum value of <code>SAME_LOCALS_1_STACK_ITEM</code>.
         */
        public static final int FRAME_TYPE_MAX = 127;

        /**
         * <code>SAME_LOCALS_1_STACK_ITEM_EXTENDED</code>.
         */
        public static final int FRAME_TYPE_EXTENDED = 247;

        /*
         * <code>frame_type</code> is computed by offsetDelta.
         */

        /**
         * <code>u2 offset_delta</code>.
         */
        public int offsetDelta;

        /**
         * <code>stack[0].tag</code>.
         */
        public int typeTag;

        /**
         * <code>stack[0].cpool_index</code> or <code>stack[0].offset</code>.
         */
        public int typeValue;
    }

    /**
     * <code>chop_frame</code>.
     */
    public static class ChopFrame extends StackMapFrame {
        /**
         * The minimum value of <code>CHOP</code>.
         */
        public static final int FRAME_TYPE = 248;

        /**
         * The maximum value of <code>CHOP</code>.
         */
        public static final int FRAME_TYPE_MAX = 250;

        /**
         * <code>u1 frame_type</code>.
         */
        public int frameType;

        /**
         * <code>u2 offset_delta</code>.
         */
        public int offsetDelta;
    }

    /**
     * <code>same_frame_extended</code>.
     */
    public static class SameFrameExtended extends StackMapFrame {
        /**
         * <code>SAME_FRAME_EXTENDED</code>.
         */
        public static final int FRAME_TYPE = 251;

        /**
         * <code>u2 offset_delta</code>.
         */
        public int offsetDelta;
    }

    /**
     * <code>append_frame</code>.
     */
    public static class AppendFrame extends StackMapFrame {
        /**
         * The minimum value of <code>APPEND</code>.
         */
        public static final int FRAME_TYPE = 252;

        /**
         * The maximum value of <code>APPEND</code>.
         */
        public static final int FRAME_TYPE_MAX = 254;

        /**
         * <code>u1 frame_type</code>.
         */
        public int frameType;

        /**
         * <code>u2 offset_delta</code>.
         */
        public int offsetDelta;

        /**
         * <code>locals[?].tag</code>.
         */
        public int[] typeTags;

        /**
         * <code>locals[?].cpool_index</code> or <code>locals[?].offset</code>.
         */
        public int[] typeValues;
    }

    /**
     * <code>ful_frame</code>.
     */
    public static class FullFrame extends StackMapFrame {
        /**
         * <code>FULL_FRAME</code>.
         */
        public static final int FRAME_TYPE = 255;

        /**
         * <code>u2 offset_delta</code>.
         */
        public int offsetDelta;

        /**
         * <code>u2 number_of_locals</code>.
         */
        public int numOfLocals;

        /**
         * <code>locals[?].tag</code>.
         */
        public int[] typeTags;

        /**
         * <code>locals[?].cpool_index</code> or <code>locals[?].offset</code>.
         */
        public int[] typeValues;

        /**
         * <code>u2 number_of_stack_items</code>.
         */
        public int numOfStackItems;

        /**
         * <code>stack[?].tag</code>.
         */
        public int[] stackTypeTags;

        /**
         * <code>stack[?].cpool_index</code> or <code>locals[?].offset</code>.
         */
        public int[] stackTypeValues;
    }
}
