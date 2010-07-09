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

import java.util.ArrayList;

/**
 * An iterator for editing a code attribute.
 *
 * <p>If there are multiple <code>CodeIterator</code>s referring to the
 * same <code>Code_attribute</code>, then inserting a gap by one
 * <code>CodeIterator</code> will break the other
 * <code>CodeIterator</code>.
 *
 * <p>This iterator does not provide <code>remove()</code>.
 * If a piece of code in a <code>Code_attribute</code> is unnecessary,
 * it should be overwritten with <code>NOP</code>.
 *
 * @see CodeAttribute#iterator()
 */
public class CodeIterator implements Opcode {
    protected CodeAttribute codeAttr;
    protected byte[] bytecode;
    protected int endPos;
    protected int currentPos;
    protected int mark;

    protected CodeIterator(CodeAttribute ca) {
        codeAttr = ca;
        bytecode = ca.getCode();
        begin();
    }

    /**
     * Moves to the first instruction.
     */
    public void begin() {
        currentPos = mark = 0;
        endPos = getCodeLength();
    }

    /**
     * Moves to the given index.
     *
     * <p>The index of the next instruction is set to the given index.
     * The successive call to <code>next()</code>
     * returns the index that has been given to <code>move()</code>.
     *
     * <p>Note that the index is into the byte array returned by
     * <code>get().getCode()</code>.
     *
     * @see CodeAttribute#getCode()
     */
    public void move(int index) {
        currentPos = index;
    }

    /**
     * Sets a mark to the bytecode at the given index.
     * The mark can be used to track the position of that bytecode
     * when code blocks are inserted.
     * If a code block is inclusively inserted at the position of the
     * bytecode, the mark is set to the inserted code block.
     *
     * @see #getMark()
     * @since 3.11
     */
    public void setMark(int index) {
        mark = index;
    }

    /**
     * Gets the index of the position of the mark set by
     * <code>setMark</code>.
     *
     * @return the index of the position.
     * @see #setMark(int)
     * @since 3.11
     */
    public int getMark() { return mark; }

    /**
     * Returns a Code attribute read with this iterator.
     */
    public CodeAttribute get() {
        return codeAttr;
    }

    /**
     * Returns <code>code_length</code> of <code>Code_attribute</code>.
     */
    public int getCodeLength() {
        return bytecode.length;
    }

    /**
     * Returns the unsigned 8bit value at the given index.
     */
    public int byteAt(int index) { return bytecode[index] & 0xff; }

    /**
     * Writes an 8bit value at the given index.
     */
    public void writeByte(int value, int index) {
        bytecode[index] = (byte)value;
    }

    /**
     * Returns the unsigned 16bit value at the given index.
     */
    public int u16bitAt(int index) {
        return ByteArray.readU16bit(bytecode, index);
    }

    /**
     * Returns the signed 16bit value at the given index.
     */
    public int s16bitAt(int index) {
        return ByteArray.readS16bit(bytecode, index);
    }

    /**
     * Writes a 16 bit integer at the index.
     */
    public void write16bit(int value, int index) {
        ByteArray.write16bit(value, bytecode, index);
    }

    /**
     * Returns the signed 32bit value at the given index.
     */
    public int s32bitAt(int index) {
        return ByteArray.read32bit(bytecode, index);
    }

    /**
     * Writes a 32bit integer at the index.
     */
    public void write32bit(int value, int index) {
        ByteArray.write32bit(value, bytecode, index);
    }

    /**
     * Writes a byte array at the index.
     *
     * @param code	may be a zero-length array.
     */
    public void write(byte[] code, int index) {
        int len = code.length;
        for (int j = 0; j < len; ++j)
            bytecode[index++] = code[j];
    }

    /**
     * Returns true if there is more instructions.
     */
    public boolean hasNext() { return currentPos < endPos; }

    /**
     * Returns the index of the next instruction
     * (not the operand following the current opcode).
     *
     * <p>Note that the index is into the byte array returned by
     * <code>get().getCode()</code>.
     *
     * @see CodeAttribute#getCode()
     * @see CodeIterator#byteAt(int)
     */
    public int next() throws BadBytecode {
        int pos = currentPos;
        currentPos = nextOpcode(bytecode, pos);
        return pos;
    }

    /**
     * Obtains the value that the next call
     * to <code>next()</code> will return.
     *
     * <p>This method is side-effects free.
     * Successive calls to <code>lookAhead()</code> return the
     * same value until <code>next()</code> is called.
     */
    public int lookAhead() {
        return currentPos;
    }

    /**
     * Moves to the instruction for
     * either <code>super()</code> or <code>this()</code>.
     *
     * <p>This method skips all the instructions for computing arguments
     * to <code>super()</code> or <code>this()</code>, which should be
     * placed at the beginning of a constructor body.
     *
     * <p>This method returns the index of INVOKESPECIAL instruction
     * executing <code>super()</code> or <code>this()</code>.
     * A successive call to <code>next()</code> returns the
     * index of the next instruction following that INVOKESPECIAL.
     *
     * <p>This method works only for a constructor.
     *
     * @return  the index of the INVOKESPECIAL instruction, or -1
     *          if a constructor invocation is not found.
     */
    public int skipConstructor() throws BadBytecode {
        return skipSuperConstructor0(-1);
    }

    /**
     * Moves to the instruction for <code>super()</code>.
     *
     * <p>This method skips all the instructions for computing arguments to
     * <code>super()</code>, which should be
     * placed at the beginning of a constructor body.
     *
     * <p>This method returns the index of INVOKESPECIAL instruction
     * executing <code>super()</code>.
     * A successive call to <code>next()</code> returns the
     * index of the next instruction following that INVOKESPECIAL.
     *
     * <p>This method works only for a constructor.
     *
     * @return  the index of the INVOKESPECIAL instruction, or -1
     *          if a super constructor invocation is not found
     *          but <code>this()</code> is found.
     */
    public int skipSuperConstructor() throws BadBytecode {
        return skipSuperConstructor0(0);
    }

    /**
     * Moves to the instruction for <code>this()</code>.
     *
     * <p>This method skips all the instructions for computing arguments to
     * <code>this()</code>, which should be
     * placed at the beginning of a constructor body.
     *
     * <p>This method returns the index of INVOKESPECIAL instruction
     * executing <code>this()</code>.
     * A successive call to <code>next()</code> returns the
     * index of the next instruction following that INVOKESPECIAL.
     *
     * <p>This method works only for a constructor.
     *
     * @return  the index of the INVOKESPECIAL instruction, or -1
     *          if a explicit constructor invocation is not found
     *          but <code>super()</code> is found.
     */
    public int skipThisConstructor() throws BadBytecode {
        return skipSuperConstructor0(1);
    }

    /* skipSuper        1: this(), 0: super(), -1: both.
     */
    private int skipSuperConstructor0(int skipThis) throws BadBytecode {
        begin();
        ConstPool cp = codeAttr.getConstPool();
        String thisClassName = codeAttr.getDeclaringClass();
        int nested = 0;
        while (hasNext()) {
            int index = next();
            int c = byteAt(index);
            if (c == NEW)
                ++nested;
            else if (c == INVOKESPECIAL) {
                int mref = ByteArray.readU16bit(bytecode, index + 1);
                if (cp.getMethodrefName(mref).equals(MethodInfo.nameInit))
                    if (--nested < 0) {
                        if (skipThis < 0)
                            return index;

                        String cname = cp.getMethodrefClassName(mref);
                        if (cname.equals(thisClassName) == (skipThis > 0))
                            return index;
                        else
                            break;
                    }
            }
        }

        begin();
        return -1;
    }

    /**
     * Inserts the given bytecode sequence
     * before the next instruction that would be returned by
     * <code>next()</code> (not before the instruction returned
     * by the last call to <code>next()</code>).
     * Branch offsets and the exception table are also updated.
     *
     * <p>If the next instruction is at the beginning of a block statement,
     * then the bytecode is inserted within that block.
     *
     * <p>An extra gap may be inserted at the end of the inserted
     * bytecode sequence for adjusting alignment if the code attribute
     * includes <code>LOOKUPSWITCH</code> or <code>TABLESWITCH</code>.
     *
     * @param code      inserted bytecode sequence.
     * @return          the index indicating the first byte of the
     *                  inserted byte sequence.
     */
    public int insert(byte[] code)
        throws BadBytecode
    {
        return insert0(currentPos, code, false);
    }

    /**
     * Inserts the given bytecode sequence
     * before the instruction at the given index <code>pos</code>.
     * Branch offsets and the exception table are also updated.
     *
     * <p>If the instruction at the given index is at the beginning
     * of a block statement,
     * then the bytecode is inserted within that block.
     *
     * <p>An extra gap may be inserted at the end of the inserted
     * bytecode sequence for adjusting alignment if the code attribute
     * includes <code>LOOKUPSWITCH</code> or <code>TABLESWITCH</code>.
     *
     * <p>The index at which the byte sequence is actually inserted
     * might be different from pos since some other bytes might be
     * inserted at other positions (e.g. to change <code>GOTO</code>
     * to <code>GOTO_W</code>).
     *
     * @param pos       the index at which a byte sequence is inserted.
     * @param code      inserted bytecode sequence.
     */
    public void insert(int pos, byte[] code) throws BadBytecode {
        insert0(pos, code, false);
    }

    /**
     * Inserts the given bytecode sequence
     * before the instruction at the given index <code>pos</code>.
     * Branch offsets and the exception table are also updated.
     *
     * <p>If the instruction at the given index is at the beginning
     * of a block statement,
     * then the bytecode is inserted within that block.
     *
     * <p>An extra gap may be inserted at the end of the inserted
     * bytecode sequence for adjusting alignment if the code attribute
     * includes <code>LOOKUPSWITCH</code> or <code>TABLESWITCH</code>.
     *
     * @param pos       the index at which a byte sequence is inserted.
     * @param code      inserted bytecode sequence.
     * @return          the index indicating the first byte of the
     *                  inserted byte sequence, which might be
     *                  different from pos.
     * @since 3.11
     */
    public int insertAt(int pos, byte[] code) throws BadBytecode {
        return insert0(pos, code, false);
    }

    /**
     * Inserts the given bytecode sequence exclusively
     * before the next instruction that would be returned by
     * <code>next()</code> (not before the instruction returned
     * by tha last call to <code>next()</code>).
     * Branch offsets and the exception table are also updated.
     *
     * <p>If the next instruction is at the beginning of a block statement,
     * then the bytecode is excluded from that block.
     *
     * <p>An extra gap may be inserted at the end of the inserted
     * bytecode sequence for adjusting alignment if the code attribute
     * includes <code>LOOKUPSWITCH</code> or <code>TABLESWITCH</code>.
     *
     * @param code      inserted bytecode sequence.
     * @return          the index indicating the first byte of the
     *                  inserted byte sequence.
     */
    public int insertEx(byte[] code)
        throws BadBytecode
    {
        return insert0(currentPos, code, true);
    }

    /**
     * Inserts the given bytecode sequence exclusively
     * before the instruction at the given index <code>pos</code>.
     * Branch offsets and the exception table are also updated.
     *
     * <p>If the instruction at the given index is at the beginning
     * of a block statement,
     * then the bytecode is excluded from that block.
     *
     * <p>An extra gap may be inserted at the end of the inserted
     * bytecode sequence for adjusting alignment if the code attribute
     * includes <code>LOOKUPSWITCH</code> or <code>TABLESWITCH</code>.
     *
     * <p>The index at which the byte sequence is actually inserted
     * might be different from pos since some other bytes might be
     * inserted at other positions (e.g. to change <code>GOTO</code>
     * to <code>GOTO_W</code>). 
     *
     * @param pos       the index at which a byte sequence is inserted.
     * @param code      inserted bytecode sequence.
     */
    public void insertEx(int pos, byte[] code) throws BadBytecode {
        insert0(pos, code, true);
    }

    /**
     * Inserts the given bytecode sequence exclusively
     * before the instruction at the given index <code>pos</code>.
     * Branch offsets and the exception table are also updated.
     *
     * <p>If the instruction at the given index is at the beginning
     * of a block statement,
     * then the bytecode is excluded from that block.
     *
     * <p>An extra gap may be inserted at the end of the inserted
     * bytecode sequence for adjusting alignment if the code attribute
     * includes <code>LOOKUPSWITCH</code> or <code>TABLESWITCH</code>.
     *
     * @param pos       the index at which a byte sequence is inserted.
     * @param code      inserted bytecode sequence.
     * @return          the index indicating the first byte of the
     *                  inserted byte sequence, which might be
     *                  different from pos.
     * @since 3.11
     */
    public int insertExAt(int pos, byte[] code) throws BadBytecode {
        return insert0(pos, code, true);
    }

    /**
     * @return          the index indicating the first byte of the
     *                  inserted byte sequence.
     */
    private int insert0(int pos, byte[] code, boolean exclusive)
        throws BadBytecode
    {
        int len = code.length;
        if (len <= 0)
            return pos;

        // currentPos will change.
        pos = insertGapAt(pos, len, exclusive).position;

        int p = pos;
        for (int j = 0; j < len; ++j)
            bytecode[p++] = code[j];

        return pos;
    }

    /**
     * Inserts a gap
     * before the next instruction that would be returned by
     * <code>next()</code> (not before the instruction returned
     * by the last call to <code>next()</code>).
     * Branch offsets and the exception table are also updated.
     * The inserted gap is filled with NOP.  The gap length may be
     * extended to a multiple of 4.
     *
     * <p>If the next instruction is at the beginning of a block statement,
     * then the gap is inserted within that block.
     *
     * @param length            gap length
     * @return  the index indicating the first byte of the inserted gap.
     */
    public int insertGap(int length) throws BadBytecode {
        return insertGapAt(currentPos, length, false).position;
    }

    /**
     * Inserts a gap in front of the instruction at the given
     * index <code>pos</code>.
     * Branch offsets and the exception table are also updated.
     * The inserted gap is filled with NOP.  The gap length may be
     * extended to a multiple of 4.
     *
     * <p>If the instruction at the given index is at the beginning
     * of a block statement,
     * then the gap is inserted within that block.
     *
     * @param pos               the index at which a gap is inserted.
     * @param length            gap length.
     * @return the length of the inserted gap.
     *          It might be bigger than <code>length</code>.
     */
    public int insertGap(int pos, int length) throws BadBytecode {
        return insertGapAt(pos, length, false).length;
    }

    /**
     * Inserts an exclusive gap
     * before the next instruction that would be returned by
     * <code>next()</code> (not before the instruction returned
     * by the last call to <code>next()</code>).
     * Branch offsets and the exception table are also updated.
     * The inserted gap is filled with NOP.  The gap length may be
     * extended to a multiple of 4.
     *
     * <p>If the next instruction is at the beginning of a block statement,
     * then the gap is excluded from that block.
     *
     * @param length            gap length
     * @return  the index indicating the first byte of the inserted gap.
     */
    public int insertExGap(int length) throws BadBytecode {
        return insertGapAt(currentPos, length, true).position;
    }

    /**
     * Inserts an exclusive gap in front of the instruction at the given
     * index <code>pos</code>.
     * Branch offsets and the exception table are also updated.
     * The inserted gap is filled with NOP.  The gap length may be
     * extended to a multiple of 4.
     *
     * <p>If the instruction at the given index is at the beginning
     * of a block statement,
     * then the gap is excluded from that block.
     *
     * @param pos               the index at which a gap is inserted.
     * @param length            gap length.
     * @return the length of the inserted gap.
     *          It might be bigger than <code>length</code>.
     */
    public int insertExGap(int pos, int length) throws BadBytecode {
        return insertGapAt(pos, length, true).length;
    }

    /**
     * An inserted gap.
     *
     * @since 3.11
     */
    public static class Gap {
        /**
         * The position of the gap.
         */
        public int position;

        /**
         * The length of the gap.
         */
        public int length;
    }

    /**
     * Inserts an inclusive or exclusive gap in front of the instruction
     * at the given index <code>pos</code>.
     * Branch offsets and the exception table in the method body
     * are also updated.  The inserted gap is filled with NOP.
     * The gap length may be extended to a multiple of 4.
     *
     * <p>Suppose that the instruction at the given index is at the
     * beginning of a block statement.  If the gap is inclusive,
     * then it is included within that block.  If the gap is exclusive,
     * then it is excluded from that block.
     *
     * <p>The index at which the gap is actually inserted
     * might be different from pos since some other bytes might be
     * inserted at other positions (e.g. to change <code>GOTO</code>
     * to <code>GOTO_W</code>).  The index is available from the <code>Gap</code>
     * object returned by this method.
     *
     * <p>Suppose that the gap is inserted at the position of
     * the next instruction that would be returned by
     * <code>next()</code> (not the last instruction returned
     * by the last call to <code>next()</code>).  The next
     * instruction returned by <code>next()</code> after the gap is
     * inserted is still the same instruction.  It is not <code>NOP</code>
     * at the first byte of the inserted gap.
     *
     * @param pos               the index at which a gap is inserted.
     * @param length            gap length.
     * @param exclusive         true if exclusive, otherwise false.
     * @return the position and the length of the inserted gap.
     * @since 3.11
     */
    public Gap insertGapAt(int pos, int length, boolean exclusive)
        throws BadBytecode
    {
        /**
         * cursorPos indicates the next bytecode whichever exclusive is
         * true or false.
         */
        Gap gap = new Gap();
        if (length <= 0) {
            gap.position = pos;
            gap.length = 0;
            return gap;
        }

        byte[] c;
        int length2;
        if (bytecode.length + length > Short.MAX_VALUE) {
            // currentPos might change after calling insertGapCore0w().
            c = insertGapCore0w(bytecode, pos, length, exclusive,
                                get().getExceptionTable(), codeAttr, gap);
            pos = gap.position;
            length2 = length; // == gap.length
        }
        else {
            int cur = currentPos;
            c = insertGapCore0(bytecode, pos, length, exclusive,
                                      get().getExceptionTable(), codeAttr);
            // insertGapCore0() never changes pos.
            length2 = c.length - bytecode.length;
            gap.position = pos;
            gap.length = length2;
            if (cur >= pos)
                currentPos = cur + length2;

            if (mark > pos || (mark == pos && exclusive))
                mark += length2;
        }

        codeAttr.setCode(c);
        bytecode = c;
        endPos = getCodeLength();
        updateCursors(pos, length2);
        return gap;
    }

    /**
     * Is called when a gap is inserted.  The default implementation is empty.
     * A subclass can override this method so that cursors will be updated.
     *
     * @param pos           the position where a gap is inserted.
     * @param length        the length of the gap.
     */
    protected void updateCursors(int pos, int length) {
        // empty
    }

    /**
     * Copies and inserts the entries in the given exception table
     * at the beginning of the exception table in the code attribute
     * edited by this object.
     *
     * @param offset    the value added to the code positions included
     *                          in the entries.
     */
    public void insert(ExceptionTable et, int offset) {
        codeAttr.getExceptionTable().add(0, et, offset);
    }

    /**
     * Appends the given bytecode sequence at the end.
     *
     * @param code      the bytecode appended.
     * @return  the position of the first byte of the appended bytecode.
     */
    public int append(byte[] code) {
        int size = getCodeLength();
        int len = code.length;
        if (len <= 0)
            return size;

        appendGap(len);
        byte[] dest = bytecode;
        for (int i = 0; i < len; ++i)
            dest[i + size] = code[i];

        return size;
    }

    /**
     * Appends a gap at the end of the bytecode sequence.
     *
     * @param gapLength            gap length
     */
    public void appendGap(int gapLength) {
        byte[] code = bytecode;
        int codeLength = code.length;
        byte[] newcode = new byte[codeLength + gapLength];

        int i;
        for (i = 0; i < codeLength; ++i)
            newcode[i] = code[i];

        for (i = codeLength; i < codeLength + gapLength; ++i)
            newcode[i] = NOP;

        codeAttr.setCode(newcode);
        bytecode = newcode;
        endPos = getCodeLength();
    }

    /**
     * Copies and appends the entries in the given exception table
     * at the end of the exception table in the code attribute
     * edited by this object.
     *
     * @param offset    the value added to the code positions included
     *                          in the entries.
     */
    public void append(ExceptionTable et, int offset) {
        ExceptionTable table = codeAttr.getExceptionTable();
        table.add(table.size(), et, offset);
    }

    /* opcodeLegth is used for implementing nextOpcode().
     */
    private static final int opcodeLength[] = {
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 3, 2, 3,
        3, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3,
        3, 3, 3, 3, 3, 3, 3, 3, 3, 2, 0, 0, 1, 1, 1, 1, 1, 1, 3, 3,
        3, 3, 3, 3, 3, 5, 0, 3, 2, 3, 1, 1, 3, 3, 1, 1, 0, 4, 3, 3,
        5, 5
    };
    // 0 .. UNUSED (186), LOOKUPSWITCH, TABLESWITCH, WIDE

    /**
     * Calculates the index of the next opcode.
     */
    static int nextOpcode(byte[] code, int index)
        throws BadBytecode
    {
        int opcode;
        try {
            opcode = code[index] & 0xff;
        }
        catch (IndexOutOfBoundsException e) {
            throw new BadBytecode("invalid opcode address");
        }

        try {
            int len = opcodeLength[opcode];
            if (len > 0)
                return index + len;
            else if (opcode == WIDE)
                if (code[index + 1] == (byte)IINC)      // WIDE IINC
                    return index + 6;
                else
                    return index + 4;           // WIDE ...
            else {
                int index2 = (index & ~3) + 8;
                if (opcode == LOOKUPSWITCH) {
                    int npairs = ByteArray.read32bit(code, index2);
                    return index2 + npairs * 8 + 4;
                }
                else if (opcode == TABLESWITCH) {
                    int low = ByteArray.read32bit(code, index2);
                    int high = ByteArray.read32bit(code, index2 + 4);
                    return index2 + (high - low + 1) * 4 + 8;
                }
                // else
                //     throw new BadBytecode(opcode);
            }
        }
        catch (IndexOutOfBoundsException e) {
        }

        // opcode is UNUSED or an IndexOutOfBoundsException was thrown.
        throw new BadBytecode(opcode);
    }

    // methods for implementing insertGap().

    static class AlignmentException extends Exception {}

    /**
     * insertGapCore0() inserts a gap (some NOPs).
     * It cannot handle a long code sequence more than 32K.  All branch offsets must be
     * signed 16bits. 
     *
     * If "where" is the beginning of a block statement and exclusive is false,
     * then the inserted gap is also included in the block statement.
     * "where" must indicate the first byte of an opcode.
     * The inserted gap is filled with NOP.  gapLength may be extended to
     * a multiple of 4.
     *
     * This method was also called from CodeAttribute.LdcEntry.doit().
     *
     * @param where       It must indicate the first byte of an opcode.
     */
    static byte[] insertGapCore0(byte[] code, int where, int gapLength,
                                 boolean exclusive, ExceptionTable etable, CodeAttribute ca)
        throws BadBytecode
    {
        if (gapLength <= 0)
            return code;

        try {
            return insertGapCore1(code, where, gapLength, exclusive, etable, ca);
        }
        catch (AlignmentException e) {
            try {
                return insertGapCore1(code, where, (gapLength + 3) & ~3,
                                  exclusive, etable, ca);
            }
            catch (AlignmentException e2) {
                throw new RuntimeException("fatal error?");
            }
        }
    }

    private static byte[] insertGapCore1(byte[] code, int where, int gapLength,
                                         boolean exclusive, ExceptionTable etable,
                                         CodeAttribute ca)
        throws BadBytecode, AlignmentException
    {
        int codeLength = code.length;
        byte[] newcode = new byte[codeLength + gapLength];
        insertGap2(code, where, gapLength, codeLength, newcode, exclusive);
        etable.shiftPc(where, gapLength, exclusive);
        LineNumberAttribute na
            = (LineNumberAttribute)ca.getAttribute(LineNumberAttribute.tag);
        if (na != null)
            na.shiftPc(where, gapLength, exclusive);

        LocalVariableAttribute va = (LocalVariableAttribute)ca.getAttribute(
                                                LocalVariableAttribute.tag);
        if (va != null)
            va.shiftPc(where, gapLength, exclusive);

        LocalVariableAttribute vta
            = (LocalVariableAttribute)ca.getAttribute(
                                              LocalVariableAttribute.typeTag);
        if (vta != null)
            vta.shiftPc(where, gapLength, exclusive);

        StackMapTable smt = (StackMapTable)ca.getAttribute(StackMapTable.tag);
        if (smt != null)
            smt.shiftPc(where, gapLength, exclusive);

        StackMap sm = (StackMap)ca.getAttribute(StackMap.tag);
        if (sm != null)
            sm.shiftPc(where, gapLength, exclusive);

        return newcode;
    }

    private static void insertGap2(byte[] code, int where, int gapLength,
                        int endPos, byte[] newcode, boolean exclusive)
        throws BadBytecode, AlignmentException
    {
        int nextPos;
        int i = 0;
        int j = 0;
        for (; i < endPos; i = nextPos) {
            if (i == where) {
                int j2 = j + gapLength;
                while (j < j2)
                    newcode[j++] = NOP;
            }

            nextPos = nextOpcode(code, i);
            int inst = code[i] & 0xff;
            // if<cond>, if_icmp<cond>, if_acmp<cond>, goto, jsr
            if ((153 <= inst && inst <= 168)
                || inst == IFNULL || inst == IFNONNULL) {
                /* 2bytes *signed* offset */
                int offset = (code[i + 1] << 8) | (code[i + 2] & 0xff);
                offset = newOffset(i, offset, where, gapLength, exclusive);
                newcode[j] = code[i];
                ByteArray.write16bit(offset, newcode, j + 1);
                j += 3;
            }
            else if (inst == GOTO_W || inst == JSR_W) {
                /* 4bytes offset */
                int offset = ByteArray.read32bit(code, i + 1);
                offset = newOffset(i, offset, where, gapLength, exclusive);
                newcode[j++] = code[i];
                ByteArray.write32bit(offset, newcode, j);
                j += 4;
            }
            else if (inst == TABLESWITCH) {
                if (i != j && (gapLength & 3) != 0)
                    throw new AlignmentException();

                int i2 = (i & ~3) + 4;  // 0-3 byte padding
                // IBM JVM 1.4.2 cannot run the following code:
                // int i0 = i;
                // while (i0 < i2)
                //    newcode[j++] = code[i0++];
                // So extracting this code into an external method.
                // see JIRA JASSIST-74.
                j = copyGapBytes(newcode, j, code, i, i2);

                int defaultbyte = newOffset(i, ByteArray.read32bit(code, i2),
                                            where, gapLength, exclusive);
                ByteArray.write32bit(defaultbyte, newcode, j);
                int lowbyte = ByteArray.read32bit(code, i2 + 4);
                ByteArray.write32bit(lowbyte, newcode, j + 4);
                int highbyte = ByteArray.read32bit(code, i2 + 8);
                ByteArray.write32bit(highbyte, newcode, j + 8);
                j += 12;
                int i0 = i2 + 12;
                i2 = i0 + (highbyte - lowbyte + 1) * 4;
                while (i0 < i2) {
                    int offset = newOffset(i, ByteArray.read32bit(code, i0),
                                           where, gapLength, exclusive);
                    ByteArray.write32bit(offset, newcode, j);
                    j += 4;
                    i0 += 4;
                }
            }
            else if (inst == LOOKUPSWITCH) {
                if (i != j && (gapLength & 3) != 0)
                    throw new AlignmentException();

                int i2 = (i & ~3) + 4;  // 0-3 byte padding

                // IBM JVM 1.4.2 cannot run the following code:
                // int i0 = i;
                // while (i0 < i2)
                //    newcode[j++] = code[i0++];
                // So extracting this code into an external method.
                // see JIRA JASSIST-74.
                j = copyGapBytes(newcode, j, code, i, i2);

                int defaultbyte = newOffset(i, ByteArray.read32bit(code, i2),
                                            where, gapLength, exclusive);
                ByteArray.write32bit(defaultbyte, newcode, j);
                int npairs = ByteArray.read32bit(code, i2 + 4);
                ByteArray.write32bit(npairs, newcode, j + 4);
                j += 8;
                int i0 = i2 + 8;
                i2 = i0 + npairs * 8;
                while (i0 < i2) {
                    ByteArray.copy32bit(code, i0, newcode, j);
                    int offset = newOffset(i,
                                        ByteArray.read32bit(code, i0 + 4),
                                        where, gapLength, exclusive);
                    ByteArray.write32bit(offset, newcode, j + 4);
                    j += 8;
                    i0 += 8;
                }
            }
            else
                while (i < nextPos)
                    newcode[j++] = code[i++];
            }
    }


    private static int copyGapBytes(byte[] newcode, int j, byte[] code, int i, int iEnd) {
        switch (iEnd - i) {
        case 4:
            newcode[j++] = code[i++];
        case 3:
            newcode[j++] = code[i++];
        case 2:
            newcode[j++] = code[i++];
        case 1:
            newcode[j++] = code[i++];
        default:
        }

        return j;
    }

    private static int newOffset(int i, int offset, int where,
                                 int gapLength, boolean exclusive) {
        int target = i + offset;
        if (i < where) {
            if (where < target || (exclusive && where == target))
                offset += gapLength;
        }
        else if (i == where) {
            // This code is different from the code in Branch#shiftOffset().
            // see JASSIST-124.
            if (target < where)
                offset -= gapLength;
        }
        else
            if (target < where || (!exclusive && where == target))
                offset -= gapLength;

        return offset;
    }

    static class Pointers {
        int cursor;
        int mark0, mark;
        ExceptionTable etable;
        LineNumberAttribute line;
        LocalVariableAttribute vars, types;
        StackMapTable stack;
        StackMap stack2;

        Pointers(int cur, int m, int m0, ExceptionTable et, CodeAttribute ca) {
            cursor = cur;
            mark = m;
            mark0 = m0;
            etable = et;    // non null
            line = (LineNumberAttribute)ca.getAttribute(LineNumberAttribute.tag);
            vars = (LocalVariableAttribute)ca.getAttribute(LocalVariableAttribute.tag);
            types = (LocalVariableAttribute)ca.getAttribute(LocalVariableAttribute.typeTag);
            stack = (StackMapTable)ca.getAttribute(StackMapTable.tag);
            stack2 = (StackMap)ca.getAttribute(StackMap.tag);
        }

        void shiftPc(int where, int gapLength, boolean exclusive) throws BadBytecode {
            if (where < cursor || (where == cursor && exclusive))
                cursor += gapLength;

            if (where < mark || (where == mark && exclusive))
                mark += gapLength;

            if (where < mark0 || (where == mark0 && exclusive))
                mark0 += gapLength;

            etable.shiftPc(where, gapLength, exclusive);
            if (line != null)
                line.shiftPc(where, gapLength, exclusive);

            if (vars != null)
                vars.shiftPc(where, gapLength, exclusive);

            if (types != null)
                types.shiftPc(where, gapLength, exclusive);

            if (stack != null)
                stack.shiftPc(where, gapLength, exclusive);

            if (stack2 != null)
                stack2.shiftPc(where, gapLength, exclusive);
        }
    }

    /*
     * This method is called from CodeAttribute.LdcEntry.doit().
     */
    static byte[] changeLdcToLdcW(byte[] code, ExceptionTable etable,
                                  CodeAttribute ca, CodeAttribute.LdcEntry ldcs)
        throws BadBytecode
    {
        ArrayList jumps = makeJumpList(code, code.length);
        while (ldcs != null) {
            addLdcW(ldcs, jumps);
            ldcs = ldcs.next;
        }

        Pointers pointers = new Pointers(0, 0, 0, etable, ca);
        byte[] r = insertGap2w(code, 0, 0, false, jumps, pointers);
        return r;
    }

    private static void addLdcW(CodeAttribute.LdcEntry ldcs, ArrayList jumps) {
        int where = ldcs.where;
        LdcW ldcw = new LdcW(where, ldcs.index);
        int s = jumps.size();
        for (int i = 0; i < s; i++)
            if (where < ((Branch)jumps.get(i)).orgPos) {
                jumps.add(i, ldcw);
                return;
            }

        jumps.add(ldcw);
    }

    /*
     * insertGapCore0w() can handle a long code sequence more than 32K. 
     * It guarantees that the length of the inserted gap (NOPs) is equal to
     * gapLength.  No other NOPs except some NOPs following TABLESWITCH or
     * LOOKUPSWITCH will not be inserted. 
     * 
     * Note: currentPos might be moved.
     *
     * @param where       It must indicate the first byte of an opcode.
     * @param newWhere    It contains the updated index of the position where a gap
     *                    is inserted and the length of the gap.
     *                    It must not be null.
     */
    private byte[] insertGapCore0w(byte[] code, int where, int gapLength, boolean exclusive,
                                   ExceptionTable etable, CodeAttribute ca, Gap newWhere)
        throws BadBytecode
    {
        if (gapLength <= 0)
            return code;

        ArrayList jumps = makeJumpList(code, code.length);
        Pointers pointers = new Pointers(currentPos, mark, where, etable, ca);
        byte[] r = insertGap2w(code, where, gapLength, exclusive, jumps, pointers);
        currentPos = pointers.cursor;
        mark = pointers.mark;
        int where2 = pointers.mark0;
        if (where2 == currentPos && !exclusive)
            currentPos += gapLength;

        if (exclusive)
            where2 -= gapLength;

        newWhere.position = where2;
        newWhere.length = gapLength;
        return r;
    }

    private static byte[] insertGap2w(byte[] code, int where, int gapLength,
                                      boolean exclusive, ArrayList jumps, Pointers ptrs)
        throws BadBytecode
    {
        int n = jumps.size();
        if (gapLength > 0) {
            ptrs.shiftPc(where, gapLength, exclusive);
            for (int i = 0; i < n; i++)
                ((Branch)jumps.get(i)).shift(where, gapLength, exclusive);
        }

        boolean unstable = true;
        do {
            while (unstable) {
                unstable = false;
                for (int i = 0; i < n; i++) {
                    Branch b = (Branch)jumps.get(i);
                    if (b.expanded()) {
                        unstable = true;
                        int p = b.pos;
                        int delta = b.deltaSize();
                        ptrs.shiftPc(p, delta, false);
                        for (int j = 0; j < n; j++)
                            ((Branch)jumps.get(j)).shift(p, delta, false);
                    }
                }
            }

            for (int i = 0; i < n; i++) {
                Branch b = (Branch)jumps.get(i);
                int diff = b.gapChanged();
                if (diff > 0) {
                    unstable = true;
                    int p = b.pos;
                    ptrs.shiftPc(p, diff, false);
                    for (int j = 0; j < n; j++)
                        ((Branch)jumps.get(j)).shift(p, diff, false);
                }
            }
        } while (unstable);

        return makeExapndedCode(code, jumps, where, gapLength);
    }

    private static ArrayList makeJumpList(byte[] code, int endPos)
        throws BadBytecode
    {
        ArrayList jumps = new ArrayList();
        int nextPos;
        for (int i = 0; i < endPos; i = nextPos) {
            nextPos = nextOpcode(code, i);
            int inst = code[i] & 0xff;
            // if<cond>, if_icmp<cond>, if_acmp<cond>, goto, jsr
            if ((153 <= inst && inst <= 168)
                    || inst == IFNULL || inst == IFNONNULL) {
                /* 2bytes *signed* offset */
                int offset = (code[i + 1] << 8) | (code[i + 2] & 0xff);
                Branch b;
                if (inst == GOTO || inst == JSR)
                    b = new Jump16(i, offset);
                else
                    b = new If16(i, offset);

                jumps.add(b);
            }
            else if (inst == GOTO_W || inst == JSR_W) {
                /* 4bytes offset */
                int offset = ByteArray.read32bit(code, i + 1);
                jumps.add(new Jump32(i, offset));
            }
            else if (inst == TABLESWITCH) {
                int i2 = (i & ~3) + 4;  // 0-3 byte padding
                int defaultbyte = ByteArray.read32bit(code, i2);
                int lowbyte = ByteArray.read32bit(code, i2 + 4);
                int highbyte = ByteArray.read32bit(code, i2 + 8);
                int i0 = i2 + 12;
                int size = highbyte - lowbyte + 1;
                int[] offsets = new int[size];
                for (int j = 0; j < size; j++) {
                    offsets[j] = ByteArray.read32bit(code, i0);
                    i0 += 4;
                }

                jumps.add(new Table(i, defaultbyte, lowbyte, highbyte, offsets));
            }
            else if (inst == LOOKUPSWITCH) {
                int i2 = (i & ~3) + 4;  // 0-3 byte padding
                int defaultbyte = ByteArray.read32bit(code, i2);
                int npairs = ByteArray.read32bit(code, i2 + 4);
                int i0 = i2 + 8;
                int[] matches = new int[npairs];
                int[] offsets = new int[npairs];
                for (int j = 0; j < npairs; j++) {
                    matches[j] = ByteArray.read32bit(code, i0);
                    offsets[j] = ByteArray.read32bit(code, i0 + 4);
                    i0 += 8;
                }

                jumps.add(new Lookup(i, defaultbyte, matches, offsets));
            }
        }

        return jumps;
    }

    private static byte[] makeExapndedCode(byte[] code, ArrayList jumps,
                                           int where, int gapLength)
        throws BadBytecode
    {
        int n = jumps.size();
        int size = code.length + gapLength;
        for (int i = 0; i < n; i++) {
            Branch b = (Branch)jumps.get(i);
            size += b.deltaSize();
        }

        byte[] newcode = new byte[size];
        int src = 0, dest = 0, bindex = 0;
        int len = code.length;
        Branch b;
        int bpos;
        if (0 < n) {
            b = (Branch)jumps.get(0);
            bpos = b.orgPos;
        }
        else {
            b = null;
            bpos = len;  // src will be never equal to bpos 
        }

        while (src < len) {
            if (src == where) {
                int pos2 = dest + gapLength;
                while (dest < pos2)
                    newcode[dest++] = NOP;
            }

            if (src != bpos)
                newcode[dest++] = code[src++];
            else {
                int s = b.write(src, code, dest, newcode);
                src += s;
                dest += s + b.deltaSize();
                if (++bindex < n) {
                    b = (Branch)jumps.get(bindex);
                    bpos = b.orgPos;
                }
                else  {
                    b = null;
                    bpos = len;
                }
            }
        }

        return newcode;
    }

    static abstract class Branch {
        int pos, orgPos;
        Branch(int p) { pos = orgPos = p; }
        void shift(int where, int gapLength, boolean exclusive) {
            if (where < pos || (where == pos && exclusive))
                pos += gapLength;
        }

        static int shiftOffset(int i, int offset, int where,
                               int gapLength, boolean exclusive) {
            int target = i + offset;
            if (i < where) {
                if (where < target || (exclusive && where == target))
                    offset += gapLength;
            }
            else if (i == where) {
                // This code is different from the code in CodeIterator#newOffset().
                // see JASSIST-124.
                if (target < where && exclusive)
                    offset -= gapLength;
                else if (where < target && !exclusive)
                    offset += gapLength;
            }
            else
                if (target < where || (!exclusive && where == target))
                    offset -= gapLength;

            return offset;
        }

        boolean expanded() { return false; }
        int gapChanged() { return 0; }
        int deltaSize() { return 0; }   // newSize - oldSize

        // This returns the original instruction size.
        abstract int write(int srcPos, byte[] code, int destPos, byte[] newcode);
    }

    /* used by changeLdcToLdcW() and CodeAttribute.LdcEntry.
     */
    static class LdcW extends Branch {
        int index;
        boolean state;
        LdcW(int p, int i) {
            super(p);
            index = i;
            state = true;
        }

        boolean expanded() {
            if (state) {
                state = false;
                return true;
            }
            else
                return false;
        }

        int deltaSize() { return 1; }

        int write(int srcPos, byte[] code, int destPos, byte[] newcode) {
            newcode[destPos] = LDC_W;
            ByteArray.write16bit(index, newcode, destPos + 1);
            return 2;
        }
    }

    static abstract class Branch16 extends Branch {
        int offset;
        int state;
        static final int BIT16 = 0;
        static final int EXPAND = 1;
        static final int BIT32 = 2;

        Branch16(int p, int off) {
            super(p);
            offset = off;
            state = BIT16;
        }

        void shift(int where, int gapLength, boolean exclusive) {
            offset = shiftOffset(pos, offset, where, gapLength, exclusive);
            super.shift(where, gapLength, exclusive);
            if (state == BIT16)
                if (offset < Short.MIN_VALUE || Short.MAX_VALUE < offset)
                    state = EXPAND;
        }

        boolean expanded() {
            if (state == EXPAND) {
                state = BIT32;
                return true;
            }
            else
                return false;
        }

        abstract int deltaSize();
        abstract void write32(int src, byte[] code, int dest, byte[] newcode);

        int write(int src, byte[] code, int dest, byte[] newcode) {
            if (state == BIT32)
                write32(src, code, dest, newcode);
            else {
                newcode[dest] = code[src];
                ByteArray.write16bit(offset, newcode, dest + 1);
            }

            return 3;
        }
    }

    // GOTO or JSR
    static class Jump16 extends Branch16 {
        Jump16(int p, int off) {
            super(p, off);
        }

        int deltaSize() {
            return state == BIT32 ? 2 : 0;
        }

        void write32(int src, byte[] code, int dest, byte[] newcode) {
            newcode[dest] = (byte)(((code[src] & 0xff) == GOTO) ? GOTO_W : JSR_W);
            ByteArray.write32bit(offset, newcode, dest + 1);
        }
    }

    // if<cond>, if_icmp<cond>, or if_acmp<cond>
    static class If16 extends Branch16 {
        If16(int p, int off) {
            super(p, off);
        }

        int deltaSize() {
            return state == BIT32 ? 5 : 0;
        }

        void write32(int src, byte[] code, int dest, byte[] newcode) {
            newcode[dest] = (byte)opcode(code[src] & 0xff);
            newcode[dest + 1] = 0;
            newcode[dest + 2] = 8;  // branch_offset = 8
            newcode[dest + 3] = (byte)GOTO_W;
            ByteArray.write32bit(offset - 3, newcode, dest + 4);
        }

        int opcode(int op) {
            if (op == IFNULL)
                return IFNONNULL;
            else if (op == IFNONNULL)
                return IFNULL;
            else {
                if (((op - IFEQ) & 1) == 0)
                    return op + 1;
                else
                    return op - 1;
            }
        }
    }

    static class Jump32 extends Branch {
        int offset;

        Jump32(int p, int off) {
            super(p);
            offset = off;
        }

        void shift(int where, int gapLength, boolean exclusive) {
            offset = shiftOffset(pos, offset, where, gapLength, exclusive);
            super.shift(where, gapLength, exclusive);
        }

        int write(int src, byte[] code, int dest, byte[] newcode) {
            newcode[dest] = code[src];
            ByteArray.write32bit(offset, newcode, dest + 1);
            return 5;
        }
    }

    static abstract class Switcher extends Branch {
        int gap, defaultByte;
        int[] offsets;

        Switcher(int pos, int defaultByte, int[] offsets) {
            super(pos);
            this.gap = 3 - (pos & 3);
            this.defaultByte = defaultByte;
            this.offsets = offsets;
        }

        void shift(int where, int gapLength, boolean exclusive) {
            int p = pos;
            defaultByte = shiftOffset(p, defaultByte, where, gapLength, exclusive);
            int num = offsets.length;
            for (int i = 0; i < num; i++)
                offsets[i] = shiftOffset(p, offsets[i], where, gapLength, exclusive);

            super.shift(where, gapLength, exclusive);
        }

        int gapChanged() {
            int newGap = 3 - (pos & 3);
            if (newGap > gap) {
                int diff = newGap - gap;
                gap = newGap;
                return diff;
            }

            return 0;
        }

        int deltaSize() {
            return gap - (3 - (orgPos & 3));
        }

        int write(int src, byte[] code, int dest, byte[] newcode) {
            int padding = 3 - (pos & 3);
            int nops = gap - padding;
            int bytecodeSize = 5 + (3 - (orgPos & 3)) + tableSize();
            adjustOffsets(bytecodeSize, nops);
            newcode[dest++] = code[src];
            while (padding-- > 0)
                newcode[dest++] = 0;

            ByteArray.write32bit(defaultByte, newcode, dest);
            int size = write2(dest + 4, newcode);
            dest += size + 4;
            while (nops-- > 0)
                newcode[dest++] = NOP;

            return 5 + (3 - (orgPos & 3)) + size;
        }

        abstract int write2(int dest, byte[] newcode);
        abstract int tableSize();

        /* If the new bytecode size is shorter than the original, some NOPs
         * are appended after this branch instruction (tableswitch or
         * lookupswitch) to fill the gap.
         * This method changes a branch offset to point to the first NOP
         * if the offset originally points to the bytecode next to this
         * branch instruction.  Otherwise, the bytecode would contain
         * dead code.  It complicates the generation of StackMap and
         * StackMapTable.
         */
        void adjustOffsets(int size, int nops) {
            if (defaultByte == size)
                defaultByte -= nops;

            for (int i = 0; i < offsets.length; i++)
                if (offsets[i] == size)
                    offsets[i] -= nops;
        }
    }

    static class Table extends Switcher {
        int low, high;

        Table(int pos, int defaultByte, int low, int high, int[] offsets) {
            super(pos, defaultByte, offsets);
            this.low = low;
            this.high = high;
        }

        int write2(int dest, byte[] newcode) {
            ByteArray.write32bit(low, newcode, dest);
            ByteArray.write32bit(high, newcode, dest + 4);
            int n = offsets.length;
            dest += 8;
            for (int i = 0; i < n; i++) {
                ByteArray.write32bit(offsets[i], newcode, dest);
                dest += 4;
            }

            return 8 + 4 * n;
        }

        int tableSize() { return 8 + 4 * offsets.length; }
    }

    static class Lookup extends Switcher {
        int[] matches;

        Lookup(int pos, int defaultByte, int[] matches, int[] offsets) {
            super(pos, defaultByte, offsets);
            this.matches = matches;
        }

        int write2(int dest, byte[] newcode) {
            int n = matches.length;
            ByteArray.write32bit(n, newcode, dest);
            dest += 4;
            for (int i = 0; i < n; i++) {
                ByteArray.write32bit(matches[i], newcode, dest);
                ByteArray.write32bit(offsets[i], newcode, dest + 4);
                dest += 8;
            }

            return 4 + 8 * n;
        }

        int tableSize() { return 4 + 8 * matches.length; }
    }
}
