package javassist.bytecode.stackmap;

import javassist.bytecode.StackMapTable;
import javassist.bytecode.ByteArray;
import javassist.bytecode.Opcode;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.BadBytecode;

public class StackAnalyzer {
    private ConstPool cpool;
    private int stackTop;
    private int[] stackTypes;
    private Object[] stackData;     // String or Integer

    private int[] localsTypes;
    private boolean[] localsUpdated;
    private Object[] localsData;    // String or Integer

    static final int EMPTY = -1;
    static final int TOP = StackMapTable.TOP;
    static final int INTEGER = StackMapTable.INTEGER;
    static final int FLOAT = StackMapTable.FLOAT;
    static final int DOUBLE = StackMapTable.DOUBLE;
    static final int LONG = StackMapTable.LONG;
    static final int NULL = StackMapTable.NULL;
    static final int THIS = StackMapTable.THIS;
    static final int OBJECT = StackMapTable.OBJECT;
    static final int UNINIT = StackMapTable.UNINIT;

    public StackAnalyzer(ConstPool cp, int maxStack, int maxLocals) {
        cpool = cp;
        stackTop = 0;
        stackTypes = null;
        localsTypes = null;
        growStack(maxStack);
        growLocals(maxLocals);
    }

    public void clearUpdatedFlags() {
        boolean[] updated = localsUpdated;
        for (int i = 0; i < updated.length; i++)
            updated[i] = false;
    }

    public void growStack(int size) {
        int oldSize;
        int[] types = new int[size];
        Object[] data = new Object[size];
        if (stackTypes == null)
            oldSize = 0;
        else {
            oldSize = stackTypes.length;
            System.arraycopy(stackTypes, 0, types, 0, oldSize);
            System.arraycopy(stackData, 0, data, 0, oldSize);
        }

        stackTypes = types;
        stackData = data;
        for (int i = oldSize; i < size; i++)
            stackTypes[i] = EMPTY;
    }

    public void growLocals(int size) {
        int oldSize;
        int[] types = new int[size];
        boolean[] updated = new boolean[size];
        Object[] data = new Object[size];
        if (localsTypes == null)
            oldSize = 0;
        else {
            oldSize = localsTypes.length;
            System.arraycopy(localsTypes, 0, types, 0, oldSize);
            System.arraycopy(localsUpdated, 0, updated, 0, oldSize);
            System.arraycopy(localsData, 0, data, 0, oldSize);
        }

        localsTypes = types;
        localsUpdated = updated;
        localsData = data;
        for (int i = oldSize; i < size; i++)
            localsTypes[i] = EMPTY;
    }

    public void pushType(int type, Object data) {
        stackTypes[stackTop] = type;
        stackData[stackTop++] = data;
    }

    /**
     * @return      the size of the instruction at POS.
     */
    protected int doOpcode(int pos, byte[] code) throws BadBytecode {
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

    protected void visitBranch(int pos, byte[] code, int offset) {}
    protected void visitGoto(int pos, byte[] code, int offset) {}
    protected void visitTableSwitch(int pos, byte[] code, int n, int offsetPos, int defaultByte) {}
    protected void visitLookupSwitch(int pos, byte[] code, int n, int pairsPos, int defaultByte) {}
    protected void visitReturn(int pos, byte[] code) {}
    protected void visitThrow(int pos, byte[] code) {}

    /**
     * Invoked when the visited instruction is jsr.
     */
    protected void visitJSR(int pos, byte[] code) throws BadBytecode {
        throwBadBytecode(pos, "jsr");
    }

    /**
     * Invoked when the visited instruction is ret.
     */
    protected void visitRET(int pos, byte[] code) throws BadBytecode {
        throwBadBytecode(pos, "ret");
    }

    private void throwBadBytecode(int pos, String name) throws BadBytecode {
        throw new BadBytecode(name + " at " + pos);
    }

    private int doOpcode0_53(int pos, byte[] code, int op) throws BadBytecode {
        int[] stackTypes = this.stackTypes;
        switch (op) {
        case Opcode.NOP :
            break;
        case Opcode.ACONST_NULL :
            stackTypes[stackTop++] = NULL;
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
            return doXLOAD(INTEGER);
        case Opcode.LLOAD :
            return doXLOAD(LONG);
        case Opcode.FLOAD :
            return doXLOAD(FLOAT);
        case Opcode.DLOAD :
            return doXLOAD(DOUBLE);
        case Opcode.ALOAD :
            stackTypes[stackTop] = OBJECT;
            stackData[stackTop++] = localsData[code[pos + 1] & 0xff];
            return 2;
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
            stackTypes[stackTop] = OBJECT;
            stackData[stackTop++] = localsData[op - Opcode.ALOAD_0];
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
            stackTypes[s] = OBJECT;
            Object data = stackData[s];
            if (data != null && data instanceof String)
                stackData[s] = getDerefType((String)data);
            else
                throw new BadBytecode("bad AALOAD");

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
        int[] stackTypes = this.stackTypes;
        int tag = cpool.getTag(index);
        if (tag == ConstPool.CONST_String) {
            stackTypes[stackTop] = OBJECT;
            stackData[stackTop++] = "java/lang/String";
        }
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
        else if (tag == ConstPool.CONST_Class) {
            stackTypes[stackTop] = OBJECT;
            stackData[stackTop++] = "java/lang/Class";
        }
        else
            throw new RuntimeException("bad LDC: " + tag);
    }

    private int doXLOAD(int type) {
        int[] stackTypes = this.stackTypes;
        stackTypes[stackTop++] = type;
        if (type == LONG || type == DOUBLE)
            stackTypes[stackTop++] = TOP;

        return 2;
    }

    private String getDerefType(String type) throws BadBytecode {
        if (type.charAt(0) == '[') {
            String type2 = type.substring(1);
            if (type2.length() > 0) {
                char c = type2.charAt(0);
                if (c == '[')
                    return type2;
                else if (c == 'L')
                    return type2.substring(1, type.length() - 2);
                else
                    return type2;
            }
        }

        throw new BadBytecode("bad array type for AALOAD: " + type);
    }

    private int doOpcode54_95(int pos, byte[] code, int op) {
        int[] localsTypes = this.localsTypes;
        int[] stackTypes = this.stackTypes;
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
            return doXSTORE(pos, code, OBJECT);
        case Opcode.ISTORE_0 :
        case Opcode.ISTORE_1 :
        case Opcode.ISTORE_2 :
        case Opcode.ISTORE_3 :
          { int var = op - Opcode.ISTORE_0;
            localsTypes[var] = INTEGER;
            localsUpdated[var] = true; }
            stackTop--;
            break;
        case Opcode.LSTORE_0 :
        case Opcode.LSTORE_1 :
        case Opcode.LSTORE_2 :
        case Opcode.LSTORE_3 :
          { int var = op - Opcode.LSTORE_0;
            localsTypes[var] = LONG;
            localsTypes[var + 1] = TOP;
            localsUpdated[var] = true; }
            stackTop -= 2;
            break;
        case Opcode.FSTORE_0 :
        case Opcode.FSTORE_1 :
        case Opcode.FSTORE_2 :
        case Opcode.FSTORE_3 :
          { int var = op - Opcode.FSTORE_0;
            localsTypes[var] = FLOAT;
            localsUpdated[var] = true; }
            stackTop--;
            break;
        case Opcode.DSTORE_0 :
        case Opcode.DSTORE_1 :
        case Opcode.DSTORE_2 :
        case Opcode.DSTORE_3 :
          { int var = op - Opcode.DSTORE_0;
            localsTypes[var] = DOUBLE;
            localsTypes[var + 1] = TOP;
            localsUpdated[var] = true; }
            stackTop -= 2;
            break;
        case Opcode.ASTORE_0 :
        case Opcode.ASTORE_1 :
        case Opcode.ASTORE_2 :
        case Opcode.ASTORE_3 :
          { int var = op - Opcode.ASTORE_0;
            localsTypes[var] = OBJECT;
            localsUpdated[var] = true;
            localsData[var] = stackData[--stackTop]; }
            break;
        case Opcode.IASTORE :
        case Opcode.LASTORE :
        case Opcode.FASTORE :
        case Opcode.DASTORE :
        case Opcode.AASTORE :
        case Opcode.BASTORE :
        case Opcode.CASTORE :
        case Opcode.SASTORE :
            stackTop -= (op == Opcode.LASTORE || op == Opcode.DASTORE) ? 4 : 3;
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
            stackData[sp] = stackData[sp - 1];
            stackTop = sp + 1;
            break; }
        case Opcode.DUP_X1 :
        case Opcode.DUP_X2 : {
            int len = op - Opcode.DUP_X1 + 2;
            doDUP_XX(1, len);
            int sp = stackTop;
            stackTypes[sp - len] = stackTypes[sp];
            stackData[sp - len] = stackData[sp];
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
            Object[] stackData = this.stackData;
            int sp = stackTop;
            stackTypes[sp - len] = stackTypes[sp];
            stackData[sp - len] = stackData[sp];
            stackTypes[sp - len + 1] = stackTypes[sp + 1];
            stackData[sp - len + 1] = stackData[sp + 1];
            stackTop = sp + 2; 
            break; }
        case Opcode.SWAP : {
            Object[] stackData = this.stackData;
            int sp = stackTop - 1;
            int t = stackTypes[sp];
            Object d = stackData[sp];
            stackTypes[sp] = stackTypes[sp - 1];
            stackData[sp] = stackData[sp - 1];
            stackTypes[sp - 1] = t;
            stackData[sp - 1] = d;
            break; }
        default :
            throw new RuntimeException("fatal");
        }

        return 1;
    }

    private int doXSTORE(int pos, byte[] code, int type) {
        int index = code[pos + 1] & 0xff;
        return doXSTORE(index, type);
    }

    private int doXSTORE(int index, int type) {
        stackTop--;
        localsTypes[index] = type;
        localsUpdated[index] = true;
        if (type == LONG || type == DOUBLE) {
            stackTop--;
            localsTypes[index + 1] = TOP;
        }
        else if (type == OBJECT)
            localsData[index] = stackData[stackTop];

        return 2;
    }

    private void doDUP_XX(int delta, int len) {
        int types[] = stackTypes;
        Object data[] = stackData;
        int sp = stackTop;
        int end = sp - len;
        while (sp > end) {
            types[sp + delta] = types[sp];
            data[sp + delta] = data[sp];
            sp--;
        }
    }

    private int doOpcode96_147(int pos, byte[] code, int op) {
        if (op <= Opcode.LXOR) {    // IADD...LXOR
            stackTop -= Opcode.STACK_GROW[op];
            return 1;
        }

        switch (op) {
        case Opcode.IINC :
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
            stackTypes[stackTop++] = TOP;       // not allowed?
            visitJSR(pos, code);
            return 3;       // branch
        case Opcode.RET :
            visitRET(pos, code);
            return 2;                           // not allowed?
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
            stackTop--;
            visitReturn(pos, code);
            break;
        case Opcode.RETURN :
            visitReturn(pos, code);
            break;
        case Opcode.GETSTATIC :
            return doFieldAccess(pos, code, true);
        case Opcode.PUTSTATIC :
            return doFieldAccess(pos, code, false);
        case Opcode.GETFIELD :
            stackTop--;
            return doFieldAccess(pos, code, true);
        case Opcode.PUTFIELD :
            stackTop--;
            return doFieldAccess(pos, code, false);
        case Opcode.INVOKEVIRTUAL :
        case Opcode.INVOKESPECIAL :
            return doInvokeMethod(pos, code, 1);
        case Opcode.INVOKESTATIC :
            return doInvokeMethod(pos, code, 0);
        case Opcode.INVOKEINTERFACE :
            return doInvokeIntfMethod(pos, code, 1);
        case 186 :
            throw new RuntimeException("bad opcode 186");
        case Opcode.NEW : {
            int i = ByteArray.readU16bit(code, pos + 1);
            stackTypes[stackTop - 1] = UNINIT;
            stackData[stackTop - 1] = new Integer(pos);
            return 3; }
        case Opcode.NEWARRAY :
            return doNEWARRAY(pos, code);
        case Opcode.ANEWARRAY : {
            int i = ByteArray.readU16bit(code, pos + 1);
            stackTypes[stackTop - 1] = OBJECT;
            stackData[stackTop - 1]
                      = "[L" + cpool.getClassInfo(i).replace('.', '/') + ";";
            return 3; }
        case Opcode.ARRAYLENGTH :
            stackTypes[stackTop - 1] = INTEGER;
            break;
        case Opcode.ATHROW :
            stackTop--;         // branch?
            visitThrow(pos, code);
            break;
        case Opcode.CHECKCAST : {
            int i = ByteArray.readU16bit(code, pos + 1);
            stackData[stackTop - 1] = cpool.getClassInfo(i);
            return 3; }
        case Opcode.INSTANCEOF :
            stackTypes[stackTop - 1] = INTEGER;
            return 3;
        case Opcode.MONITORENTER :
        case Opcode.MONITOREXIT :
            stackTop--;
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
            stackTypes[stackTop++] = TOP;       // not allowed?
            visitJSR(pos, code);
            return 5;
        }
        return 1;
    }

    private int doWIDE(int pos, byte[] code) {
        int op = code[pos + 1] & 0xff;
        switch (op) {
        case Opcode.ILOAD :
            doXLOAD(INTEGER);
            break;
        case Opcode.LLOAD :
            doXLOAD(LONG);
            break;
        case Opcode.FLOAD :
            doXLOAD(FLOAT);
            break;
        case Opcode.DLOAD :
            doXLOAD(DOUBLE);
            break;
        case Opcode.ALOAD :
            stackTypes[stackTop] = OBJECT;
            stackData[stackTop++] = localsData[ByteArray.readU16bit(code, pos)];
            break;
        case Opcode.ISTORE :
            return doWIDE_STORE(pos, code, INTEGER);
        case Opcode.LSTORE :
            return doWIDE_STORE(pos, code, LONG);
        case Opcode.FSTORE :
            return doWIDE_STORE(pos, code, FLOAT);
        case Opcode.DSTORE :
            return doWIDE_STORE(pos, code, DOUBLE);
        case Opcode.ASTORE :
            return doWIDE_STORE(pos, code, OBJECT);
        case Opcode.IINC :
            return 6;
        case Opcode.RET :
            break;
        default :
            throw new RuntimeException("bad WIDE instruction: " + op);
        }

        return 4;
    }

    private int doWIDE_STORE(int pos, byte[] code, int type) {
        int index = ByteArray.readU16bit(code, pos);
        return doXSTORE(index, type);
    }

    private int doFieldAccess(int pos, byte[] code, boolean isGet) {
        int index = ByteArray.readU16bit(code, pos + 1);
        String desc = cpool.getFieldrefType(index);
        if (isGet)
            pushMemberType(desc);
        else
            stackTop -= Descriptor.dataSize(desc);

        return 3;
    }

    private int doNEWARRAY(int pos, byte[] code) {
        int s = stackTop - 1;
        stackTypes[s] = OBJECT;
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

        stackData[s] = type;
        return 2;
    }

    private int doMultiANewArray(int pos, byte[] code) {
        int i = ByteArray.readU16bit(code, pos + 1);
        int dim = code[pos + 3] & 0xff;
        stackTop -= dim - 1;
        String type = cpool.getClassInfo(i);
        StringBuffer sbuf = new StringBuffer();
        while (dim-- > 0)
            sbuf.append('[');

        sbuf.append('L').append(type.replace('.', '/')).append(';');
        stackTypes[stackTop - 1] = OBJECT;
        stackData[stackTop - 1] = sbuf.toString(); 
        return 4;
    }

    private int doInvokeMethod(int pos, byte[] code, int targetSize) {
        int i = ByteArray.readU16bit(code, pos + 1);
        String desc = cpool.getMethodrefType(i);
        stackTop -= Descriptor.paramSize(desc) + targetSize;
        pushMemberType(desc);
        return 3;
    }

    private int doInvokeIntfMethod(int pos, byte[] code, int targetSize) {
        int i = ByteArray.readU16bit(code, pos + 1);
        String desc = cpool.getInterfaceMethodrefType(i);
        stackTop -= Descriptor.paramSize(desc) + targetSize;
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

        int[] types = stackTypes;
        int index = stackTop;
        switch (descriptor.charAt(top)) {
        case '[' :
            types[index] = OBJECT;
            stackData[index] = descriptor.substring(top);
            break;
        case 'L' :
            types[index] = OBJECT;
            stackData[index] = descriptor.substring(top + 1,
                                        descriptor.indexOf(';', top));
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
}
