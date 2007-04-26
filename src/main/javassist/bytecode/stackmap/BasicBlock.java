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

package javassist.bytecode.stackmap;

import javassist.bytecode.*;
import java.util.ArrayList;

public class BasicBlock implements TypeTag, Comparable {

    public int position, length;
    public int stackTop, numLocals;
    public TypeData[] stackTypes, localsTypes;

    /* The version number of the values of numLocals and localsTypes.
     * These values are repeatedly updated while MapMaker#make()
     * is running.  This field represents when the values are recorded.
     */
    public int version;

    /* a flag used by MapMaker#recordUsage()
     */
    public int[] localsUsage;

    /* The number of the basic blocks from which a thread of control
     * may reach this basic block.  The number excludes the preceding
     * block.  Thus, if it is zero, a thread of control reaches
     * only from the preceding block.  Such a basic block represents
     * the boundary of a try block.
     */
    public int inbound;

    public static class Branch {
        public Branch next;
        public int target;
        public int typeIndex;   // exception type
        public Branch(Branch next, int target, int type) {
            this.next = next;
            this.target = target;
            this.typeIndex = type;
        }
    }

    /* A list of catch clauses that a thread may jump
     * from this block to.
     */
    public Branch catchBlocks;

    /* public static void main(String[] args) throws Exception {
        BasicBlock b = new BasicBlock(0);
        b.initFirstBlock(8, 1, args[0], args[1], args[2].equals("static"), args[2].equals("const"));
        System.out.println(b);
    }*/

    private BasicBlock(int pos) {
        position = pos;
        length = 0;
        stackTop = numLocals = 0;
        stackTypes = localsTypes = null;
        inbound = 1;
        localsUsage = null;
        catchBlocks = null;
    }

    public boolean alreadySet(int ver) {
        return stackTypes != null && ver == version;
    }

    /*
     * Computes the correct value of numLocals.
     * It assumes that:
     *     correct numLocals <= current numLocals 
     */
    public void resetNumLocals() {
        if (localsTypes != null) {
            int nl = numLocals;
            while (nl > 0 && localsTypes[nl - 1] == TypeTag.TOP) {
                if (nl > 1) {
                    TypeData td = localsTypes[nl - 2];
                    if (td == TypeTag.LONG || td == TypeTag.DOUBLE)
                        break;
                }

                --nl;
            }

            numLocals = nl;
        }
    }

    public void setStackMap(int st, TypeData[] stack,
                            int nl, TypeData[] locals)
        throws BadBytecode
    {
        stackTop = st;
        stackTypes = stack;
        numLocals = nl;
        localsTypes = locals;
    }

    private void updateLength(int nextPos) {
        length = nextPos - position;
    }

    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append("Block at ");
        sbuf.append(position);
        sbuf.append(" stack={");
        printTypes(sbuf, stackTop, stackTypes);
        sbuf.append("} locals={");
        printTypes(sbuf, numLocals, localsTypes);
        sbuf.append('}');
        return sbuf.toString();
    }

    private static void printTypes(StringBuffer sbuf, int size,
                                   TypeData[] types) {
        if (types == null)
            return;

        for (int i = 0; i < size; i++) {
            if (i > 0)
                sbuf.append(", ");

            TypeData td = types[i];
            sbuf.append(td == null ? "<>" : td.toString());
        }
    }

    /**
     * Finds the basic block including the given position.
     *
     * @param pos       the position.
     */
    public static BasicBlock find(BasicBlock[] blocks, int pos) throws BadBytecode {
        int n = blocks.length;
        for (int i = 0; i < n; i++)
            if (blocks[i].position == pos)
                return blocks[i];

        throw new BadBytecode("no basic block: " + pos);
    }

    /**
     * Divides the given code fragment into basic blocks.
     * It returns null if the given MethodInfo does not include
     * a CodeAttribute.
     */
    public static BasicBlock[] makeBlocks(MethodInfo minfo) throws BadBytecode {
        CodeAttribute ca = minfo.getCodeAttribute();
        if (ca == null)
            return null;

        CodeIterator ci = ca.iterator();
        ConstPool pool = minfo.getConstPool();
        BasicBlock[] blocks = makeBlocks(ci, 0, ci.getCodeLength(), ca.getExceptionTable(), 0, pool);
        boolean isStatic = (minfo.getAccessFlags() & AccessFlag.STATIC) != 0;
        blocks[0].initFirstBlock(ca.getMaxStack(), ca.getMaxLocals(),
                                 pool.getClassName(), minfo.getDescriptor(),
                                 isStatic, minfo.isConstructor());
        return blocks;
    }

    /**
     * Divides the given code fragment into basic blocks.
     *
     * @param begin         the position where the basic block analysis starts. 
     * @param end           exclusive.
     * @param et            the appended exception table entries.
     * @param etOffset      the offset added to the handlerPc entries in the exception table.
     * @param pool          the constant pool.
     */
    public static BasicBlock[] makeBlocks(CodeIterator ci, int begin, int end,
                                           ExceptionTable et, int etOffset, ConstPool pool)
        throws BadBytecode
    {
        ci.begin();
        ci.move(begin);
        ArrayList targets = new ArrayList();
        BasicBlock bb0 = new BasicBlock(begin);
        bb0.inbound = 0;    // the first block is not a branch target.
        targets.add(bb0);
        while (ci.hasNext()) {
            int index = ci.next();
            if (index >= end)
                break;

            int op = ci.byteAt(index);
            if ((Opcode.IFEQ <= op && op <= Opcode.IF_ACMPNE)
                || op == Opcode.IFNULL || op == Opcode.IFNONNULL)
                targets.add(new BasicBlock(index + ci.s16bitAt(index + 1)));
            else if (Opcode.GOTO <= op && op <= Opcode.LOOKUPSWITCH)
                switch (op) {
                case Opcode.GOTO :
                case Opcode.JSR :
                    targets.add(new BasicBlock(index + ci.s16bitAt(index + 1)));
                    break;
                // case Opcode.RET :
                //    throw new BadBytecode("ret at " + index);
                case Opcode.TABLESWITCH : {
                    int pos = (index & ~3) + 4;
                    targets.add(new BasicBlock(index + ci.s32bitAt(pos)));   // default branch target
                    int low = ci.s32bitAt(pos + 4);
                    int high = ci.s32bitAt(pos + 8);
                    int p = pos + 12;
                    int n = p + (high - low + 1) * 4;
                    while (p < n) {
                        targets.add(new BasicBlock(index + ci.s32bitAt(p)));
                        p += 4;
                    }
                    break; }
                case Opcode.LOOKUPSWITCH : {
                    int pos = (index & ~3) + 4;
                    targets.add(new BasicBlock(index + ci.s32bitAt(pos)));   // default branch target
                    int p = pos + 8 + 4;
                    int n = p + ci.s32bitAt(pos + 4) * 8;
                    while (p < n) {
                        targets.add(new BasicBlock(index + ci.s32bitAt(p)));
                        p += 8;
                    }
                    break; }
                }
            else if (op == Opcode.GOTO_W || op == Opcode.JSR_W)
                targets.add(new BasicBlock(index + ci.s32bitAt(index + 1)));
        }

        if (et != null) {
            int i = et.size();
            while (--i >= 0) {
                BasicBlock bb = new BasicBlock(et.startPc(i) + etOffset);
                bb.inbound = 0;
                targets.add(bb);
                targets.add(new BasicBlock(et.handlerPc(i) + etOffset));
            }
        }

        BasicBlock[] blocks = trimArray(targets, end);
        markCatch(et, etOffset, blocks);
        return blocks;
    }

    public int compareTo(Object obj) {
        if (obj instanceof BasicBlock) {
            int pos = ((BasicBlock)obj).position;
            return position - pos;
        }

        return -1;
    }

    /**
     * @param endPos        exclusive
     */
    private static BasicBlock[] trimArray(ArrayList targets, int endPos) {
        Object[] targetArray = targets.toArray();
        int size = targetArray.length;
        java.util.Arrays.sort(targetArray);
        int s = 0;
        int t0 = -1;
        for (int i = 0; i < size; i++) {
            int t = ((BasicBlock)targetArray[i]).position;
            if (t != t0) {
                s++;
                t0 = t;
            }
        }

        BasicBlock[] results = new BasicBlock[s];
        BasicBlock bb0 = (BasicBlock)targetArray[0];
        results[0] = bb0;
        t0 = bb0.position;
        int j = 1;
        for (int i = 1; i < size; i++) {
            BasicBlock bb = (BasicBlock)targetArray[i];
            int t = bb.position;
            if (t == t0)
                results[j - 1].inbound += bb.inbound;
            else {
                results[j - 1].updateLength(t);
                results[j++] = bb;
                t0 = t;
            }
        }

        results[j - 1].updateLength(endPos);
        return results;
    }

    private static void markCatch(ExceptionTable et, int etOffset,
            BasicBlock[] blocks)
    {
        if (et == null)
            return;

        int nblocks = blocks.length;
        int n = et.size();
        for (int i = 0; i < n; i++) {
            int start = et.startPc(i) + etOffset;
            int end = et.endPc(i) + etOffset;
            int handler = et.handlerPc(i) + etOffset;
            int type = et.catchType(i);
            for (int k = 0; k < nblocks; k++) {
                BasicBlock bb = blocks[k];
                int p = bb.position;
                if (start <= p && p < end)
                    bb.catchBlocks = new Branch(bb.catchBlocks, handler, type);
            }
        }
    }

    /**
     * Initializes the first block by the given method descriptor.
     *
     * @param block             the first basic block that this method initializes.
     * @param className         a dot-separated fully qualified class name.
     *                          For example, <code>javassist.bytecode.stackmap.BasicBlock</code>.
     * @param methodDesc        method descriptor.
     * @param isStatic          true if the method is a static method.
     * @param isConstructor     true if the method is a constructor.
     */
    void initFirstBlock(int maxStack, int maxLocals, String className,
                        String methodDesc, boolean isStatic, boolean isConstructor)
        throws BadBytecode
    {
        if (methodDesc.charAt(0) != '(')
            throw new BadBytecode("no method descriptor: " + methodDesc);

        stackTop = 0;
        stackTypes = new TypeData[maxStack];
        TypeData[] locals = new TypeData[maxLocals];
        if (isConstructor)
            locals[0] = new TypeData.UninitThis(className);
        else if (!isStatic)
            locals[0] = new TypeData.ClassName(className);

        int n = isStatic ? -1 : 0;
        int i = 1;
        try {
            while ((i = descToTag(methodDesc, i, ++n, locals)) > 0)
                if (locals[n].is2WordType())
                    locals[++n] = TOP;
        }
        catch (StringIndexOutOfBoundsException e) {
            throw new BadBytecode("bad method descriptor: "
                                  + methodDesc);
        }

        numLocals = n;
        localsTypes = locals;
    }

    private static int descToTag(String desc, int i,
                                 int n, TypeData[] types)
        throws BadBytecode
    {
        int i0 = i;
        int arrayDim = 0;
        char c = desc.charAt(i);
        if (c == ')')
            return 0;

        while (c == '[') {
            ++arrayDim;
            c = desc.charAt(++i);
        }

        if (c == 'L') {
            int i2 = desc.indexOf(';', ++i);
            if (arrayDim > 0)
                types[n] = new TypeData.ClassName(desc.substring(i0, ++i2));
            else
                types[n] = new TypeData.ClassName(desc.substring(i0 + 1, ++i2 - 1)
                                                      .replace('/', '.'));
            return i2;
        }
        else if (arrayDim > 0) {
            types[n] = new TypeData.ClassName(desc.substring(i0, ++i));
            return i;
        }
        else {
            TypeData t = toPrimitiveTag(c);
            if (t == null)
                throw new BadBytecode("bad method descriptor: " + desc);

            types[n] = t;
            return i + 1;
        }
    }

    private static TypeData toPrimitiveTag(char c) {
        switch (c) {
        case 'Z' :
        case 'C' :
        case 'B' :
        case 'S' :
        case 'I' :
            return INTEGER;
        case 'J' :
            return LONG;
        case 'F' :
            return FLOAT;
        case 'D' :
            return DOUBLE;
        case 'V' :
        default :
            return null;
        }
    }

    public static String getRetType(String desc) {
        int i = desc.indexOf(')');
        if (i < 0)
            return "java.lang.Object";

        char c = desc.charAt(i + 1);
        if (c == '[')
            return desc.substring(i + 1);
        else if (c == 'L')
            return desc.substring(i + 2, desc.length() - 1).replace('/', '.');
        else
            return "java.lang.Object";
    }
}
