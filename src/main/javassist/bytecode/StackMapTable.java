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
import java.util.ArrayList;

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

    private ArrayList entries;    // ArrayList<StackMapFrame>.  may be null.

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
        entries = new ArrayList(n);
        int offset = 2;
        while (n-- > 0) {
            int frameType;
        }

    }

    private void toByteArray(boolean clear) {
        if (entries != null)
            ; // unparse 
    }

    /**
     * <code>union stack_map_frame</code>.
     * <p><code>verification_type_info</code> is represented
     * by a pair of two <code>int</code> values.  No class
     * for represening <code>verification_type_ 
     */
    public static class StackMapFrame {
        /**
         * <code>u2 offset_delta</code>.
         * If <code>offset_delta</code> is not included
         * (i.e. <code>same_frame</code>), the value of
         * this field is computed from other members such
         * as <code>frame_type</code>. 
         */
        public int offsetDelta;
    } 

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
     * <code>frame_type</code> is not included.
     * It is computed by <code>offsetDelta</code>. 
     */
    public static class SameFrame extends StackMapFrame {
        /**
         * The maximum value of <code>SAME</code>.
         */
        public static final int FRAME_TYPE_MAX = 63;
    }

    /**
     * <code>same_locals_1_stack_item_frame</code> or 
     * <code>same_locals_1_stack_item_frame_extended</code>.
     *
     * <p><code>frame_type</code> is not included.
     * It is computed by <code>offsetDelta</code>. 
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
    }

    /**
     * <code>same_frame_extended</code>.
     */
    public static class SameFrameExtended extends StackMapFrame {
        /**
         * <code>SAME_FRAME_EXTENDED</code>.
         */
        public static final int FRAME_TYPE = 251;
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
         * <code>locals[?].tag</code>.
         * <code>typeTags.length</code> and <code>typeValues.length</code>
         * are equal to <code>number_of_locals</code>.
         */
        public int[] typeTags;

        /**
         * <code>locals[?].cpool_index</code> or <code>locals[?].offset</code>.
         * <code>typeTags.length</code> and <code>typeValues.length</code>
         * are equal to <code>number_of_locals</code>.
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
         * <code>locals[?].tag</code>.
         * <code>typeTags.length</code> and <code>typeValues.length</code>
         * are equal to <code>number_of_locals</code>.
         */
        public int[] typeTags;

        /**
         * <code>locals[?].cpool_index</code> or <code>locals[?].offset</code>.
         * <code>typeTags.length</code> and <code>typeValues.length</code>
         * are equal to <code>number_of_locals</code>.
         */
        public int[] typeValues;

        /**
         * <code>stack[?].tag</code>.
         * <code>stackTypeTags.length</code> and <code>stackTypeValues.length</code>
         * are equal to <code>number_of_stack_items</code>.
         */
        public int[] stackTypeTags;

        /**
         * <code>stack[?].cpool_index</code> or <code>locals[?].offset</code>.
         * <code>stackTypeTags.length</code> and <code>stackTypeValues.length</code>
         * are equal to <code>number_of_stack_items</code>.
         */
        public int[] stackTypeValues;
    }
}
