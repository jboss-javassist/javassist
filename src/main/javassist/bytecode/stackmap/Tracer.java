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

package javassist.bytecode.stackmap;

import javassist.bytecode.ByteArray;
import javassist.bytecode.Opcode;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.BadBytecode;
import javassist.ClassPool;

/*
 * A class for performing abstract interpretation.
 * See also MapMaker class. 
 */

public abstract class Tracer implements TypeTag {
    protected ClassPool classPool;
    protected ConstPool cpool;
    protected String returnType;

    protected int stackTop;
    protected TypeData[] stackTypes;
    protected TypeData[] localsTypes;

    public Tracer(ClassPool classes, ConstPool cp, int maxStack, int maxLocals,
                  String retType) {
        classPool = classes;
        cpool = cp;
        returnType = retType;
        stackTop = 0;
        stackTypes = new TypeData[maxStack];
        localsTypes = new TypeData[maxLocals];
    }

    public Tracer(Tracer t, boolean copyStack) {
        classPool = t.classPool;
        cpool = t.cpool;
        returnType = t.returnType;

        stackTop = t.stackTop;
        int size = t.stackTypes.length;
        stackTypes = new TypeData[size];
        if (copyStack)
            copyFrom(t.stackTop, t.stackTypes, stackTypes);

        int size2 = t.localsTypes.length;
        localsTypes = new TypeData[size2];
        copyFrom(size2, t.localsTypes, localsTypes);
    }

    protected static int copyFrom(int n, TypeData[] srcTypes, TypeData[] destTypes) {
        int k = -1;
        for (int i = 0; i < n; i++) {
            TypeData t = srcTypes[i];
            destTypes[i] = t == TOP ? TOP : t.getSelf();
            if (t != TOP)
                if (t.is2WordType())
                    k = i + 1;
                else
                    k = i;
        }

        return k + 1;
    }

    /**
     * Does abstract interpretation on the given bytecode instruction.
     * It records whether or not a local variable (i.e. register) is accessed.
     * If the instruction requires that a local variable or
     * a stack element has a more specific type, this method updates the
     * type of it.
     *
     * @param pos         the position of the instruction.
     * @return      the size of the instruction at POS.
     */
    protected int doOpcode(int pos, byte[] code) throws BadBytecode {
        try {
            int op = code[pos] & 0xff;
            if (op < 96)
                if (op < 54)
                    return doOpcode0_53(pos, code, op);
                else
                    return doOpcode54_95(pos, code, op);
            else
                if (op < 148)
                    return doOpcode96_147(pos, code, op);
                else
                    return doOpcode148_201(pos, code, op);
        }
        catch (ArrayIndexOutOfBoundsException e) {
            throw new BadBytecode("inconsistent stack height " + e.getMessage());
        }
    }

    protected void visitBranch(int pos, byte[] code, int offset) throws BadBytecode {}
    protected void visitGoto(int pos, byte[] code, int offset) throws BadBytecode {}
    protected void visitReturn(int pos, byte[] code) throws BadBytecode {}
    protected void visitThrow(int pos, byte[] code) throws BadBytecode {}

    /**
     * @param pos           the position of TABLESWITCH
     * @param code          bytecode
     * @param n             the number of case labels
     * @param offsetPos     the position of the branch-target table.
     * @param defaultOffset     the offset to the default branch target.
     */
    protected void visitTableSwitch(int pos, byte[] code, int n,
                int offsetPos, int defaultOffset) throws BadBytecode {}

    /**
     * @param pos           the position of LOOKUPSWITCH
     * @param code          bytecode
     * @param n             the number of case labels
     * @param offsetPos     the position of the table of pairs of a value and a branch target.
     * @param defaultOffset     the offset to the default branch target.
     */
    protected void visitLookupSwitch(int pos, byte[] code, int n,
                int pairsPos, int defaultOffset) throws BadBytecode {}

    /**
     * Invoked when the visited instruction is jsr.
     * Java6 or later does not allow using RET.
     */
    protected void visitJSR(int pos, byte[] code) throws BadBytecode {
        /* Since JSR pushes a return address onto the operand stack,
         * the stack map at the entry point of a subroutine is
         * stackTypes resulting after executing the following code:
         *
         *     stackTypes[stackTop++] = TOP;
         */
    }

    /**
     * Invoked when the visited instruction is ret or wide ret.
     * Java6 or later does not allow using RET.
     */
    protected void visitRET(int pos, byte[] code) throws BadBytecode {}

    private int doOpcode0_53(int pos, byte[] code, int op) throws BadBytecode {
        int reg;
        TypeData[] stackTypes = this.stackTypes;
        switch (op) {
        case Opcode.NOP :
            break;
        case Opcode.ACONST_NULL :
            stackTypes[stackTop++] = new TypeData.NullType();
            break;
        case Opcode.ICONST_M1 :
        case Opcode.ICONST_0 :
        case Opcode.ICONST_1 :
        case Opcode.ICONST_2 :
        case Opcode.ICONST_3 :
        case Opcode.ICONST_4 :
        case Opcode.ICONST_5 :
            stackTypes[stackTop++] = INTEGER;
            break;
        case Opcode.LCONST_0 :
        case Opcode.LCONST_1 :
            stackTypes[stackTop++] = LONG;
            stackTypes[stackTop++] = TOP;
            break;
        case Opcode.FCONST_0 :
        case Opcode.FCONST_1 :
        case Opcode.FCONST_2 :
            stackTypes[stackTop++] = FLOAT;
            break;
        case Opcode.DCONST_0 :
        case Opcode.DCONST_1 :
            stackTypes[stackTop++] = DOUBLE;
            stackTypes[stackTop++] = TOP;
            break;
        case Opcode.BIPUSH :
        case Opcode.SIPUSH :
            stackTypes[stackTop++] = INTEGER;
            return op == Opcode.SIPUSH ? 3 : 2;
        case Opcode.LDC :
            doLDC(code[pos + 1] & 0xff);
            return 2;
        case Opcode.LDC_W :
        case Opcode.LDC2_W :
            doLDC(ByteArray.readU16bit(code, pos + 1));
            return 3;
        case Opcode.ILOAD :
            return doXLOAD(INTEGER, code, pos);
        case Opcode.LLOAD :
            return doXLOAD(LONG, code, pos);
        case Opcode.FLOAD :
            return doXLOAD(FLOAT, code, pos);
        case Opcode.DLOAD :
            return doXLOAD(DOUBLE, code, pos);
        case Opcode.ALOAD :
            return doALOAD(code[pos + 1] & 0xff);
        case Opcode.ILOAD_0 :
        case Opcode.ILOAD_1 :
        case Opcode.ILOAD_2 :
        case Opcode.ILOAD_3 :
            stackTypes[stackTop++] = INTEGER;
            break;
        case Opcode.LLOAD_0 :
        case Opcode.LLOAD_1 :
        case Opcode.LLOAD_2 :
        case Opcode.LLOAD_3 :
            stackTypes[stackTop++] = LONG;
            stackTypes[stackTop++] = TOP;
            break;
        case Opcode.FLOAD_0 :
        case Opcode.FLOAD_1 :
        case Opcode.FLOAD_2 :
        case Opcode.FLOAD_3 :
            stackTypes[stackTop++] = FLOAT;
            break;
        case Opcode.DLOAD_0 :
        case Opcode.DLOAD_1 :
        case Opcode.DLOAD_2 :
        case Opcode.DLOAD_3 :
            stackTypes[stackTop++] = DOUBLE;
            stackTypes[stackTop++] = TOP;
            break;
        case Opcode.ALOAD_0 :
        case Opcode.ALOAD_1 :
        case Opcode.ALOAD_2 :
        case Opcode.ALOAD_3 :
            reg = op - Opcode.ALOAD_0;
            stackTypes[stackTop++] = localsTypes[reg];
            break;
        case Opcode.IALOAD :
            stackTypes[--stackTop - 1] = INTEGER;
            break;
        case Opcode.LALOAD :
            stackTypes[stackTop - 2] = LONG;
            stackTypes[stackTop - 1] = TOP;
            break;
        case Opcode.FALOAD :
            stackTypes[--stackTop - 1] = FLOAT;
            break;
        case Opcode.DALOAD :
            stackTypes[stackTop - 2] = DOUBLE;
            stackTypes[stackTop - 1] = TOP;
            break;
        case Opcode.AALOAD : {
            int s = --stackTop - 1;
            TypeData data = stackTypes[s];
            if (data == null || !data.isObjectType())
                throw new BadBytecode("bad AALOAD");
            else
                stackTypes[s] = new TypeData.ArrayElement(data);

            break; }
        case Opcode.BALOAD :
        case Opcode.CALOAD :
        case Opcode.SALOAD :
            stackTypes[--stackTop - 1] = INTEGER;
            break;
        default :
            throw new RuntimeException("fatal");
        }

        return 1;
    }

    private void doLDC(int index) {
        TypeData[] stackTypes = this.stackTypes;
        int tag = cpool.getTag(index);
        if (tag == ConstPool.CONST_String)
            stackTypes[stackTop++] = new TypeData.ClassName("java.lang.String");
        else if (tag == ConstPool.CONST_Integer)
            stackTypes[stackTop++] = INTEGER;
        else if (tag == ConstPool.CONST_Float)
            stackTypes[stackTop++] = FLOAT;
        else if (tag == ConstPool.CONST_Long) {
            stackTypes[stackTop++] = LONG;
            stackTypes[stackTop++] = TOP;
        }
        else if (tag == ConstPool.CONST_Double) {
            stackTypes[stackTop++] = DOUBLE;
            stackTypes[stackTop++] = TOP;
        }
        else if (tag == ConstPool.CONST_Class)
            stackTypes[stackTop++] = new TypeData.ClassName("java.lang.Class");
        else
            throw new RuntimeException("bad LDC: " + tag);
    }

    private int doXLOAD(TypeData type, byte[] code, int pos) {
        int localVar = code[pos + 1] & 0xff;
        return doXLOAD(localVar, type);
    }

    private int doXLOAD(int localVar, TypeData type) {
        stackTypes[stackTop++] = type;
        if (type.is2WordType())
            stackTypes[stackTop++] = TOP;

        return 2;
    }

    private int doALOAD(int localVar) { // int localVar, TypeData type) {
        stackTypes[stackTop++] = localsTypes[localVar];
        return 2;
    }

    private int doOpcode54_95(int pos, byte[] code, int op) throws BadBytecode {
        TypeData[] localsTypes = this.localsTypes;
        TypeData[] stackTypes = this.stackTypes;
        switch (op) {
        case Opcode.ISTORE :
            return doXSTORE(pos, code, INTEGER);
        case Opcode.LSTORE :
            return doXSTORE(pos, code, LONG);
        case Opcode.FSTORE :
            return doXSTORE(pos, code, FLOAT);
        case Opcode.DSTORE :
            return doXSTORE(pos, code, DOUBLE);
        case Opcode.ASTORE :
            return doASTORE(code[pos + 1] & 0xff);
        case Opcode.ISTORE_0 :
        case Opcode.ISTORE_1 :
        case Opcode.ISTORE_2 :
        case Opcode.ISTORE_3 :
          { int var = op - Opcode.ISTORE_0;
            localsTypes[var] = INTEGER;
            stackTop--; }
            break;
        case Opcode.LSTORE_0 :
        case Opcode.LSTORE_1 :
        case Opcode.LSTORE_2 :
        case Opcode.LSTORE_3 :
          { int var = op - Opcode.LSTORE_0;
            localsTypes[var] = LONG;
            localsTypes[var + 1] = TOP;
            stackTop -= 2; }
            break;
        case Opcode.FSTORE_0 :
        case Opcode.FSTORE_1 :
        case Opcode.FSTORE_2 :
        case Opcode.FSTORE_3 :
          { int var = op - Opcode.FSTORE_0;
            localsTypes[var] = FLOAT;
            stackTop--; }
            break;
        case Opcode.DSTORE_0 :
        case Opcode.DSTORE_1 :
        case Opcode.DSTORE_2 :
        case Opcode.DSTORE_3 :
          { int var = op - Opcode.DSTORE_0;
            localsTypes[var] = DOUBLE;
            localsTypes[var + 1] = TOP;
            stackTop -= 2; }
            break;
        case Opcode.ASTORE_0 :
        case Opcode.ASTORE_1 :
        case Opcode.ASTORE_2 :
        case Opcode.ASTORE_3 :
          { int var = op - Opcode.ASTORE_0;
            doASTORE(var);
            break; }
        case Opcode.IASTORE :
        case Opcode.LASTORE :
        case Opcode.FASTORE :
        case Opcode.DASTORE :
            stackTop -= (op == Opcode.LASTORE || op == Opcode.DASTORE) ? 4 : 3;
            break;
        case Opcode.AASTORE :
            TypeData.setType(stackTypes[stackTop - 1],
                             TypeData.ArrayElement.getElementType(stackTypes[stackTop - 3].getName()),
                             classPool);
            stackTop -= 3;
            break;
        case Opcode.BASTORE :
        case Opcode.CASTORE :
        case Opcode.SASTORE :
            stackTop -= 3;
            break;
        case Opcode.POP :
            stackTop--;
            break;
        case Opcode.POP2 :
            stackTop -= 2;
            break;
        case Opcode.DUP : {
            int sp = stackTop;
            stackTypes[sp] = stackTypes[sp - 1];
            stackTop = sp + 1;
            break; }
        case Opcode.DUP_X1 :
        case Opcode.DUP_X2 : {
            int len = op - Opcode.DUP_X1 + 2;
            doDUP_XX(1, len);
            int sp = stackTop;
            stackTypes[sp - len] = stackTypes[sp];
            stackTop = sp + 1;
            break; }
        case Opcode.DUP2 :
            doDUP_XX(2, 2);
            stackTop += 2;
            break;
        case Opcode.DUP2_X1 :
        case Opcode.DUP2_X2 : {
            int len = op - Opcode.DUP2_X1 + 3;
            doDUP_XX(2, len);
            int sp = stackTop;
            stackTypes[sp - len] = stackTypes[sp];
            stackTypes[sp - len + 1] = stackTypes[sp + 1];
            stackTop = sp + 2; 
            break; }
        case Opcode.SWAP : {
            int sp = stackTop - 1;
            TypeData t = stackTypes[sp];
            stackTypes[sp] = stackTypes[sp - 1];
            stackTypes[sp - 1] = t;
            break; }
        default :
            throw new RuntimeException("fatal");
        }

        return 1;
    }

    private int doXSTORE(int pos, byte[] code, TypeData type) {
        int index = code[pos + 1] & 0xff;
        return doXSTORE(index, type);
    }

    private int doXSTORE(int index, TypeData type) {
        stackTop--;
        localsTypes[index] = type;
        if (type.is2WordType()) {
            stackTop--;
            localsTypes[index + 1] = TOP;
        }

        return 2;
    }

    private int doASTORE(int index) {
        stackTop--;
        // implicit upcast might be done.
        localsTypes[index] = stackTypes[stackTop].copy();
        return 2;
    }

    private void doDUP_XX(int delta, int len) {
        TypeData types[] = stackTypes;
        int sp = stackTop - 1;
        int end = sp - len;
        while (sp > end) {
            types[sp + delta] = types[sp];
            sp--;
        }
    }

    private int doOpcode96_147(int pos, byte[] code, int op) {
        if (op <= Opcode.LXOR) {    // IADD...LXOR
            stackTop += Opcode.STACK_GROW[op];
            return 1;
        }

        switch (op) {
        case Opcode.IINC :
            // this does not call writeLocal().
            return 3;
        case Opcode.I2L :
            stackTypes[stackTop] = LONG;
            stackTypes[stackTop - 1] = TOP;
            stackTop++;
            break;
        case Opcode.I2F :
            stackTypes[stackTop - 1] = FLOAT;
            break;
        case Opcode.I2D :
            stackTypes[stackTop] = DOUBLE;
            stackTypes[stackTop - 1] = TOP;
            stackTop++;
            break;
        case Opcode.L2I :
            stackTypes[--stackTop - 1] = INTEGER;
            break;
        case Opcode.L2F :
            stackTypes[--stackTop - 1] = FLOAT;
            break;
        case Opcode.L2D :
            stackTypes[stackTop - 1] = DOUBLE;
            break;
        case Opcode.F2I :
            stackTypes[stackTop - 1] = INTEGER;
            break;
        case Opcode.F2L :
            stackTypes[stackTop - 1] = TOP;
            stackTypes[stackTop++] = LONG;
            break;
        case Opcode.F2D :
            stackTypes[stackTop - 1] = TOP;
            stackTypes[stackTop++] = DOUBLE;
            break;
        case Opcode.D2I :
            stackTypes[--stackTop - 1] = INTEGER;
            break;
        case Opcode.D2L :
            stackTypes[stackTop - 1] = LONG;
            break;
        case Opcode.D2F :
            stackTypes[--stackTop - 1] = FLOAT;
            break;
        case Opcode.I2B :
        case Opcode.I2C :
        case Opcode.I2S :
            break;
        default :
            throw new RuntimeException("fatal");
        }

        return 1;
    }

    private int doOpcode148_201(int pos, byte[] code, int op) throws BadBytecode {
        switch (op) {
        case Opcode.LCMP :
            stackTypes[stackTop - 4] = INTEGER;
            stackTop -= 3;
            break;
        case Opcode.FCMPL :
        case Opcode.FCMPG :
            stackTypes[--stackTop - 1] = INTEGER;
            break;
        case Opcode.DCMPL :
        case Opcode.DCMPG :
            stackTypes[stackTop - 4] = INTEGER;
            stackTop -= 3;
            break;
        case Opcode.IFEQ :
        case Opcode.IFNE :
        case Opcode.IFLT :
        case Opcode.IFGE :
        case Opcode.IFGT :
        case Opcode.IFLE :
            stackTop--;     // branch
            visitBranch(pos, code, ByteArray.readS16bit(code, pos + 1));
            return 3;
        case Opcode.IF_ICMPEQ :
        case Opcode.IF_ICMPNE :
        case Opcode.IF_ICMPLT :
        case Opcode.IF_ICMPGE :
        case Opcode.IF_ICMPGT :
        case Opcode.IF_ICMPLE :
        case Opcode.IF_ACMPEQ :
        case Opcode.IF_ACMPNE :
            stackTop -= 2;  // branch
            visitBranch(pos, code, ByteArray.readS16bit(code, pos + 1));
            return 3;
        case Opcode.GOTO :
            visitGoto(pos, code, ByteArray.readS16bit(code, pos + 1));
            return 3;       // branch
        case Opcode.JSR :
            visitJSR(pos, code);
            return 3;       // branch
        case Opcode.RET :
            visitRET(pos, code);
            return 2;
        case Opcode.TABLESWITCH : {
            stackTop--;     // branch
            int pos2 = (pos & ~3) + 8;
            int low = ByteArray.read32bit(code, pos2);
            int high = ByteArray.read32bit(code, pos2 + 4);
            int n = high - low + 1;
            visitTableSwitch(pos, code, n, pos2 + 8, ByteArray.read32bit(code, pos2 - 4));
            return n * 4 + 16 - (pos & 3); }
        case Opcode.LOOKUPSWITCH : {
            stackTop--;     // branch
            int pos2 = (pos & ~3) + 8;
            int n = ByteArray.read32bit(code, pos2);
            visitLookupSwitch(pos, code, n, pos2 + 4, ByteArray.read32bit(code, pos2 - 4));
            return n * 8 + 12 - (pos & 3); }
        case Opcode.IRETURN :
            stackTop--;
            visitReturn(pos, code);
            break;
        case Opcode.LRETURN :
            stackTop -= 2;
            visitReturn(pos, code);
            break;
        case Opcode.FRETURN :
            stackTop--;
            visitReturn(pos, code);
            break;
        case Opcode.DRETURN :
            stackTop -= 2;
            visitReturn(pos, code);
            break;
        case Opcode.ARETURN :
            TypeData.setType(stackTypes[--stackTop], returnType, classPool);
            visitReturn(pos, code);
            break;
        case Opcode.RETURN :
            visitReturn(pos, code);
            break;
        case Opcode.GETSTATIC :
            return doGetField(pos, code, false);
        case Opcode.PUTSTATIC :
            return doPutField(pos, code, false);
        case Opcode.GETFIELD :
            return doGetField(pos, code, true);
        case Opcode.PUTFIELD :
            return doPutField(pos, code, true);
        case Opcode.INVOKEVIRTUAL :
        case Opcode.INVOKESPECIAL :
            return doInvokeMethod(pos, code, true);
        case Opcode.INVOKESTATIC :
            return doInvokeMethod(pos, code, false);
        case Opcode.INVOKEINTERFACE :
            return doInvokeIntfMethod(pos, code);
        case 186 :
            throw new RuntimeException("bad opcode 186");
        case Opcode.NEW : {
            int i = ByteArray.readU16bit(code, pos + 1);
            stackTypes[stackTop++]
                      = new TypeData.UninitData(pos, cpool.getClassInfo(i));
            return 3; }
        case Opcode.NEWARRAY :
            return doNEWARRAY(pos, code);
        case Opcode.ANEWARRAY : {
            int i = ByteArray.readU16bit(code, pos + 1);
            String type = cpool.getClassInfo(i).replace('.', '/');
            if (type.charAt(0) == '[')
                type = "[" + type;
            else
                type = "[L" + type + ";";

            stackTypes[stackTop - 1]
                    = new TypeData.ClassName(type);
            return 3; }
        case Opcode.ARRAYLENGTH :
            TypeData.setType(stackTypes[stackTop - 1], "[Ljava.lang.Object;", classPool);
            stackTypes[stackTop - 1] = INTEGER;
            break;
        case Opcode.ATHROW :
            TypeData.setType(stackTypes[--stackTop], "java.lang.Throwable", classPool);
            visitThrow(pos, code);
            break;
        case Opcode.CHECKCAST : {
            // TypeData.setType(stackTypes[stackTop - 1], "java.lang.Object", classPool);
            int i = ByteArray.readU16bit(code, pos + 1);
            stackTypes[stackTop - 1] = new TypeData.ClassName(cpool.getClassInfo(i));
            return 3; }
        case Opcode.INSTANCEOF :
            // TypeData.setType(stackTypes[stackTop - 1], "java.lang.Object", classPool);
            stackTypes[stackTop - 1] = INTEGER;
            return 3;
        case Opcode.MONITORENTER :
        case Opcode.MONITOREXIT :
            stackTop--;
            // TypeData.setType(stackTypes[stackTop], "java.lang.Object", classPool);
            break;
        case Opcode.WIDE :
            return doWIDE(pos, code);
        case Opcode.MULTIANEWARRAY :
            return doMultiANewArray(pos, code);
        case Opcode.IFNULL :
        case Opcode.IFNONNULL :
            stackTop--;         // branch
            visitBranch(pos, code, ByteArray.readS16bit(code, pos + 1));
            return 3;
        case Opcode.GOTO_W :
            visitGoto(pos, code, ByteArray.read32bit(code, pos + 1));
            return 5;           // branch
        case Opcode.JSR_W :
            visitJSR(pos, code);
            return 5;
        }
        return 1;
    }

    private int doWIDE(int pos, byte[] code) throws BadBytecode {
        int op = code[pos + 1] & 0xff;
        switch (op) {
        case Opcode.ILOAD :
            doWIDE_XLOAD(pos, code, INTEGER);
            break;
        case Opcode.LLOAD :
            doWIDE_XLOAD(pos, code, LONG);
            break;
        case Opcode.FLOAD :
            doWIDE_XLOAD(pos, code, FLOAT);
            break;
        case Opcode.DLOAD :
            doWIDE_XLOAD(pos, code, DOUBLE);
            break;
        case Opcode.ALOAD : {
            int index = ByteArray.readU16bit(code, pos + 2);
            doALOAD(index);
            break; }
        case Opcode.ISTORE :
            doWIDE_STORE(pos, code, INTEGER);
            break;
        case Opcode.LSTORE :
            doWIDE_STORE(pos, code, LONG);
            break;
        case Opcode.FSTORE :
            doWIDE_STORE(pos, code, FLOAT);
            break;
        case Opcode.DSTORE :
            doWIDE_STORE(pos, code, DOUBLE);
            break;
        case Opcode.ASTORE : {
            int index = ByteArray.readU16bit(code, pos + 2);
            doASTORE(index);
            break; }
        case Opcode.IINC :
            // this does not call writeLocal().
            return 6;
        case Opcode.RET :
            visitRET(pos, code);
            break;
        default :
            throw new RuntimeException("bad WIDE instruction: " + op);
        }

        return 4;
    }

    private void doWIDE_XLOAD(int pos, byte[] code, TypeData type) {
        int index = ByteArray.readU16bit(code, pos + 2);
        doXLOAD(index, type);
    }

    private void doWIDE_STORE(int pos, byte[] code, TypeData type) {
        int index = ByteArray.readU16bit(code, pos + 2);
        doXSTORE(index, type);
    }

    private int doPutField(int pos, byte[] code, boolean notStatic) throws BadBytecode {
        int index = ByteArray.readU16bit(code, pos + 1);
        String desc = cpool.getFieldrefType(index);
        stackTop -= Descriptor.dataSize(desc);
        char c = desc.charAt(0);
        if (c == 'L')
            TypeData.setType(stackTypes[stackTop], getFieldClassName(desc, 0), classPool);
        else if (c == '[')
            TypeData.setType(stackTypes[stackTop], desc, classPool);

        setFieldTarget(notStatic, index);
        return 3;
    }

    private int doGetField(int pos, byte[] code, boolean notStatic) throws BadBytecode {
        int index = ByteArray.readU16bit(code, pos + 1);
        setFieldTarget(notStatic, index);
        String desc = cpool.getFieldrefType(index);
        pushMemberType(desc);
        return 3;
    }

    private void setFieldTarget(boolean notStatic, int index) throws BadBytecode {
        if (notStatic) {
            String className = cpool.getFieldrefClassName(index);
            TypeData.setType(stackTypes[--stackTop], className, classPool);
        }
    }

    private int doNEWARRAY(int pos, byte[] code) {
        int s = stackTop - 1;
        String type;
        switch (code[pos + 1] & 0xff) {
        case Opcode.T_BOOLEAN :
            type = "[Z";
            break;
        case Opcode.T_CHAR :
            type = "[C";
            break;
        case Opcode.T_FLOAT :
            type = "[F";
            break;
        case Opcode.T_DOUBLE :
            type = "[D";
            break;
        case Opcode.T_BYTE :
            type = "[B";
            break;
        case Opcode.T_SHORT :
            type = "[S";
            break;
        case Opcode.T_INT :
            type = "[I";
            break;
        case Opcode.T_LONG :
            type = "[J";
            break;
        default :
            throw new RuntimeException("bad newarray");
        }

        stackTypes[s] = new TypeData.ClassName(type);
        return 2;
    }

    private int doMultiANewArray(int pos, byte[] code) {
        int i = ByteArray.readU16bit(code, pos + 1);
        int dim = code[pos + 3] & 0xff;
        stackTop -= dim - 1;

        String type = cpool.getClassInfo(i).replace('.', '/');
        stackTypes[stackTop - 1] = new TypeData.ClassName(type);
        return 4;
    }

    private int doInvokeMethod(int pos, byte[] code, boolean notStatic) throws BadBytecode {
        int i = ByteArray.readU16bit(code, pos + 1);
        String desc = cpool.getMethodrefType(i);
        checkParamTypes(desc, 1);
        if (notStatic) {
            String className = cpool.getMethodrefClassName(i);
            TypeData.setType(stackTypes[--stackTop], className, classPool);
        }

        pushMemberType(desc);
        return 3;
    }

    private int doInvokeIntfMethod(int pos, byte[] code) throws BadBytecode {
        int i = ByteArray.readU16bit(code, pos + 1);
        String desc = cpool.getInterfaceMethodrefType(i);
        checkParamTypes(desc, 1);
        String className = cpool.getInterfaceMethodrefClassName(i);
        TypeData.setType(stackTypes[--stackTop], className, classPool);
        pushMemberType(desc);
        return 5;
    }

    private void pushMemberType(String descriptor) {
        int top = 0;
        if (descriptor.charAt(0) == '(') {
            top = descriptor.indexOf(')') + 1;
            if (top < 1)
                throw new IndexOutOfBoundsException("bad descriptor: "
                                                    + descriptor);
        }

        TypeData[] types = stackTypes;
        int index = stackTop;
        switch (descriptor.charAt(top)) {
        case '[' :
            types[index] = new TypeData.ClassName(descriptor.substring(top));
            break;
        case 'L' :
            types[index] = new TypeData.ClassName(getFieldClassName(descriptor, top));
            break;
        case 'J' :
            types[index] = LONG;
            types[index + 1] = TOP;
            stackTop += 2;
            return;
        case 'F' :
            types[index] = FLOAT;
            break;
        case 'D' :
            types[index] = DOUBLE;
            types[index + 1] = TOP;
            stackTop += 2;
            return;
        case 'V' :
            return;
        default : // C, B, S, I, Z
            types[index] = INTEGER;
            break;
        }

        stackTop++;
    }

    private static String getFieldClassName(String desc, int index) {
        return desc.substring(index + 1, desc.length() - 1).replace('/', '.');
    }

    private void checkParamTypes(String desc, int i) throws BadBytecode {
        char c = desc.charAt(i);
        if (c == ')')
            return;

        int k = i;
        boolean array = false;
        while (c == '[') {
            array = true;
            c = desc.charAt(++k);
        }

        if (c == 'L') {
            k = desc.indexOf(';', k) + 1;
            if (k <= 0)
                throw new IndexOutOfBoundsException("bad descriptor");
        }
        else
            k++;

        checkParamTypes(desc, k);
        if (!array && (c == 'J' || c == 'D'))
            stackTop -= 2;
        else
            stackTop--;

        if (array)
            TypeData.setType(stackTypes[stackTop],
                             desc.substring(i, k), classPool);
        else if (c == 'L')
            TypeData.setType(stackTypes[stackTop],
                             desc.substring(i + 1, k - 1).replace('/', '.'), classPool);
    }
}
