/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2005 Shigeru Chiba. All Rights Reserved.
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

/**
 * An iterator for editing a code attribute.
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

    CodeIterator(CodeAttribute ca) {
        codeAttr = ca;
        bytecode = ca.getCode();
        begin();
    }

    /**
     * Moves to the first instruction.
     */
    public void begin() {
        currentPos = 0;
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
     * Moves to the first instruction following
     * constructor invocation <code>super()</code> or <code>this()</code>.
     *
     * <p>This method skips all the instructions for executing
     * <code>super()</code> or <code>this()</code>, which should be
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
     * Moves to the first instruction following super
     * constructor invocation <code>super()</code>.
     *
     * <p>This method skips all the instructions for executing
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
     * Moves to the first instruction following explicit
     * constructor invocation <code>this()</code>.
     *
     * <p>This method skips all the instructions for executing
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
     * by tha last call to <code>next()</code>).
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
        int pos = currentPos;
        insert0(currentPos, code, false);
        return pos;
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
     */
    public void insert(int pos, byte[] code) throws BadBytecode {
        insert0(pos, code, false);
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
        int pos = currentPos;
        insert0(currentPos, code, true);
        return pos;
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
     */
    public void insertEx(int pos, byte[] code) throws BadBytecode {
        insert0(pos, code, true);
    }

    private void insert0(int pos, byte[] code, boolean exclusive)
        throws BadBytecode
    {
        int len = code.length;
        if (len <= 0)
            return;

        insertGapCore(pos, len, exclusive);     // currentPos will change.
        for (int j = 0; j < len; ++j)
            bytecode[pos++] = code[j];
    }

    /**
     * Inserts a gap
     * before the next instruction that would be returned by
     * <code>next()</code> (not before the instruction returned
     * by tha last call to <code>next()</code>).
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
        int pos = currentPos;
        insertGapCore(currentPos, length, false);
        return pos;
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
        return insertGapCore(pos, length, false);
    }

    /**
     * Inserts an exclusive gap
     * before the next instruction that would be returned by
     * <code>next()</code> (not before the instruction returned
     * by tha last call to <code>next()</code>).
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
        int pos = currentPos;
        insertGapCore(currentPos, length, true);
        return pos;
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
        return insertGapCore(pos, length, true);
    }

    /**
     * @return the length of the really inserted gap.
     */
    private int insertGapCore(int pos, int length, boolean exclusive)
        throws BadBytecode
    {
        if (length <= 0)
            return 0;

        int cur = currentPos;
        byte[] c = insertGap(bytecode, pos, length, exclusive,
                             get().getExceptionTable(), codeAttr);
        int length2 = c.length - bytecode.length;
        if (cur >= pos)
            currentPos = cur + length2;

        codeAttr.setCode(c);
        bytecode = c;
        endPos = getCodeLength();
        return length2;
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

    /* If "where" is the beginning of a block statement, then the inserted
     * gap is also included in the block statement.
     * "where" must indicate the first byte of an opcode.
     * The inserted gap is filled with NOP.  gapLength may be extended to
     * a multiple of 4.
     */
    static byte[] insertGap(byte[] code, int where, int gapLength,
                boolean exclusive, ExceptionTable etable, CodeAttribute ca)
        throws BadBytecode
    {
        if (gapLength <= 0)
            return code;

        try {
            return insertGap0(code, where, gapLength, exclusive, etable, ca);
        }
        catch (AlignmentException e) {
            try {
                return insertGap0(code, where, (gapLength + 3) & ~3,
                                  exclusive, etable, ca);
            }
            catch (AlignmentException e2) {
                throw new RuntimeException("fatal error?");
            }
        }
    }

    private static byte[] insertGap0(byte[] code, int where, int gapLength,
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

                int i0 = i;
                int i2 = (i & ~3) + 4;  // 0-3 byte padding
                while (i0 < i2)
                    newcode[j++] = code[i0++];

                int defaultbyte = newOffset(i, ByteArray.read32bit(code, i2),
                                            where, gapLength, exclusive);
                ByteArray.write32bit(defaultbyte, newcode, j);
                int lowbyte = ByteArray.read32bit(code, i2 + 4);
                ByteArray.write32bit(lowbyte, newcode, j + 4);
                int highbyte = ByteArray.read32bit(code, i2 + 8);
                ByteArray.write32bit(highbyte, newcode, j + 8);
                j += 12;
                i0 = i2 + 12;
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

                int i0 = i;
                int i2 = (i & ~3) + 4;  // 0-3 byte padding
                while (i0 < i2)
                    newcode[j++] = code[i0++];

                int defaultbyte = newOffset(i, ByteArray.read32bit(code, i2),
                                            where, gapLength, exclusive);
                ByteArray.write32bit(defaultbyte, newcode, j);
                int npairs = ByteArray.read32bit(code, i2 + 4);
                ByteArray.write32bit(npairs, newcode, j + 4);
                j += 8;
                i0 = i2 + 8;
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

    private static int newOffset(int i, int offset, int where,
                                 int gapLength, boolean exclusive) {
        int target = i + offset;
        if (i < where) {
            if (where < target || (exclusive && where == target))
                offset += gapLength;
        }
        else
            if (target < where || (!exclusive && where == target))
                offset -= gapLength;

        return offset;
    }
}


class AlignmentException extends Exception {
}
