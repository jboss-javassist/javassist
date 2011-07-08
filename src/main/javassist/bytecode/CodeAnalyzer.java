/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999- Shigeru Chiba. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later,
 * or the Apache License Version 2.0.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */

package javassist.bytecode;

/**
 * Utility for computing <code>max_stack</code>.
 */
class CodeAnalyzer implements Opcode {
    private ConstPool constPool;
    private CodeAttribute codeAttr;

    public CodeAnalyzer(CodeAttribute ca) {
        codeAttr = ca;
        constPool = ca.getConstPool();
    }

    public int computeMaxStack()
        throws BadBytecode
    {
        /* d = stack[i]
         * d == 0: not visited
         * d > 0: the depth is d - 1 after executing the bytecode at i.
         * d < 0: not visited. the initial depth (before execution) is 1 - d.
         */
        CodeIterator ci = codeAttr.iterator();
        int length = ci.getCodeLength();
        int[] stack = new int[length];
        constPool = codeAttr.getConstPool();
        initStack(stack, codeAttr);
        boolean repeat;
        do {
            repeat = false;
            for (int i = 0; i < length; ++i)
                if (stack[i] < 0) {
                    repeat = true;
                    visitBytecode(ci, stack, i);
                }
        } while (repeat);

        int maxStack = 1;
        for (int i = 0; i < length; ++i)
            if (stack[i] > maxStack)
                maxStack = stack[i];

        return maxStack - 1;    // the base is 1.
    }

    private void initStack(int[] stack, CodeAttribute ca) {
        stack[0] = -1;
        ExceptionTable et = ca.getExceptionTable();
        if (et != null) {
            int size = et.size();
            for (int i = 0; i < size; ++i)
                stack[et.handlerPc(i)] = -2;    // an exception is on stack
        }
    }

    private void visitBytecode(CodeIterator ci, int[] stack, int index)
        throws BadBytecode
    {
        int codeLength = stack.length;
        ci.move(index);
        int stackDepth = -stack[index];
        int[] jsrDepth = new int[1];
        jsrDepth[0] = -1;
        while (ci.hasNext()) {
            index = ci.next();
            stack[index] = stackDepth;
            int op = ci.byteAt(index);
            stackDepth = visitInst(op, ci, index, stackDepth);
            if (stackDepth < 1)
                throw new BadBytecode("stack underflow at " + index);

            if (processBranch(op, ci, index, codeLength, stack, stackDepth, jsrDepth))
                break;

            if (isEnd(op))     // return, ireturn, athrow, ...
                break;

            if (op == JSR || op == JSR_W)
                --stackDepth;
        }
    }

    private boolean processBranch(int opcode, CodeIterator ci, int index,
                                  int codeLength, int[] stack, int stackDepth, int[] jsrDepth)
        throws BadBytecode
    {
        if ((IFEQ <= opcode && opcode <= IF_ACMPNE)
                            || opcode == IFNULL || opcode == IFNONNULL) {
            int target = index + ci.s16bitAt(index + 1);
            checkTarget(index, target, codeLength, stack, stackDepth);
        }
        else {
            int target, index2;
            switch (opcode) {
            case GOTO :
                target = index + ci.s16bitAt(index + 1);
                checkTarget(index, target, codeLength, stack, stackDepth);
                return true;
            case GOTO_W :
                target = index + ci.s32bitAt(index + 1);
                checkTarget(index, target, codeLength, stack, stackDepth);
                return true;
            case JSR :
            case JSR_W :
                if (opcode == JSR)
                    target = index + ci.s16bitAt(index + 1);
                else
                    target = index + ci.s32bitAt(index + 1);

                checkTarget(index, target, codeLength, stack, stackDepth);
                /*
                 * It is unknown which RET comes back to this JSR.
                 * So we assume that if the stack depth at one JSR instruction
                 * is N, then it is also N at other JSRs and N - 1 at all RET
                 * instructions.  Note that STACK_GROW[JSR] is 1 since it pushes
                 * a return address on the operand stack.
                 */
                if (jsrDepth[0] < 0) {
                    jsrDepth[0] = stackDepth;
                    return false;
                }
                else if (stackDepth == jsrDepth[0])
                    return false;
                else
                    throw new BadBytecode(
                        "sorry, cannot compute this data flow due to JSR: "
                            + stackDepth + "," + jsrDepth[0]);
            case RET :
                if (jsrDepth[0] < 0) {
                    jsrDepth[0] = stackDepth + 1;
                    return false;
                }
                else if (stackDepth + 1 == jsrDepth[0])
                    return true;
                else
                    throw new BadBytecode(
                        "sorry, cannot compute this data flow due to RET: "
                            + stackDepth + "," + jsrDepth[0]);
            case LOOKUPSWITCH :
            case TABLESWITCH :
                index2 = (index & ~3) + 4;
                target = index + ci.s32bitAt(index2);
                checkTarget(index, target, codeLength, stack, stackDepth);
                if (opcode == LOOKUPSWITCH) {
                    int npairs = ci.s32bitAt(index2 + 4);
                    index2 += 12;
                    for (int i = 0; i < npairs; ++i) {
                        target = index + ci.s32bitAt(index2);
                        checkTarget(index, target, codeLength,
                                    stack, stackDepth);
                        index2 += 8;
                    }
                }
                else {
                    int low = ci.s32bitAt(index2 + 4);
                    int high = ci.s32bitAt(index2 + 8);
                    int n = high - low + 1;
                    index2 += 12;
                    for (int i = 0; i < n; ++i) {
                        target = index + ci.s32bitAt(index2);
                        checkTarget(index, target, codeLength,
                                    stack, stackDepth);
                        index2 += 4;
                    }
                }

                return true;    // always branch.
            }
        }

        return false;   // may not branch.
    }

    private void checkTarget(int opIndex, int target, int codeLength,
                             int[] stack, int stackDepth)
        throws BadBytecode
    {
        if (target < 0 || codeLength <= target)
            throw new BadBytecode("bad branch offset at " + opIndex);

        int d = stack[target];
        if (d == 0)
            stack[target] = -stackDepth;
        else if (d != stackDepth && d != -stackDepth)
            throw new BadBytecode("verification error (" + stackDepth +
                                  "," + d + ") at " + opIndex);
    }
                             
    private static boolean isEnd(int opcode) {
        return (IRETURN <= opcode && opcode <= RETURN) || opcode == ATHROW; 
    }

    /**
     * Visits an instruction.
     */
    private int visitInst(int op, CodeIterator ci, int index, int stack)
        throws BadBytecode
    {
        String desc;
        switch (op) {
        case GETFIELD :
            stack += getFieldSize(ci, index) - 1;
            break;
        case PUTFIELD :
            stack -= getFieldSize(ci, index) + 1;
            break;
        case GETSTATIC :
            stack += getFieldSize(ci, index);
            break;
        case PUTSTATIC :
            stack -= getFieldSize(ci, index);
            break;
        case INVOKEVIRTUAL :
        case INVOKESPECIAL :
            desc = constPool.getMethodrefType(ci.u16bitAt(index + 1));
            stack += Descriptor.dataSize(desc) - 1;
            break;
        case INVOKESTATIC :
            desc = constPool.getMethodrefType(ci.u16bitAt(index + 1));
            stack += Descriptor.dataSize(desc);
            break;
        case INVOKEINTERFACE :
            desc = constPool.getInterfaceMethodrefType(
                                            ci.u16bitAt(index + 1));
            stack += Descriptor.dataSize(desc) - 1;
            break;
        case ATHROW :
            stack = 1;      // the stack becomes empty (1 means no values).
            break;
        case MULTIANEWARRAY :
            stack += 1 - ci.byteAt(index + 3);
            break;
        case WIDE :
            op = ci.byteAt(index + 1);
            // don't break here.
        default :
            stack += STACK_GROW[op];
        }

        return stack;
    }

    private int getFieldSize(CodeIterator ci, int index) {
        String desc = constPool.getFieldrefType(ci.u16bitAt(index + 1));
        return Descriptor.dataSize(desc);
    }
}
