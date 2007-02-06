package javassist.bytecode.stackmap;

import javassist.bytecode.*;

public class BasicBlock {
    public int position;
    public int stackTop;
    public int[] stackTypes, localsTypes;
    public Object[] stackData, localsData;

    private BasicBlock(int pos) {
        position = pos;
    }

    public void set(int st, int[] stypes, Object[] sdata, int[] ltypes, Object[] ldata)
        throws BadBytecode
    {
        if (stackTypes == null) {
            stackTop = st;
            stackTypes = copy(stypes);
            stackData = copy(sdata);
            localsTypes = copy(ltypes);
            localsData = copy(ldata);
        }
        else {
            if (st != stackTop)
                throw new BadBytecode("verification failure");

            int n = ltypes.length;
            for (int i = 0; i < n; i++)
                if (ltypes[i] != localsTypes[i]) {
                    localsTypes[i] = StackAnalyzer.EMPTY;
                    localsData[i] = null;
                }
                else if (ltypes[i] == StackAnalyzer.OBJECT
                         && !ldata[i].equals(localsData[i]))
                    ; // localsData[i] = ??;
        }
    }

    private static int[] copy(int[] a) {
        int[] b = new int[a.length];
        System.arraycopy(a, 0, b, 0, a.length);
        return b;
    }

    private static Object[] copy(Object[] a) {
        Object[] b = new Object[a.length];
        System.arraycopy(a, 0, b, 0, a.length);
        return b;
    }

    public static BasicBlock find(BasicBlock[] blocks, int pos) throws BadBytecode {
        int n = blocks.length;
        for (int i = 0; i < n; i++)
            if (blocks[i].position == pos)
                return blocks[i];

        throw new BadBytecode("no basic block: " + pos);
    }

    public static BasicBlock[] makeBlocks(CodeIterator ci, ExceptionTable et)
        throws BadBytecode
    {
        ci.begin();
        int[] targets = new int[16];
        int size = 0;
        while (ci.hasNext()) {
            int index = ci.next();
            int op = ci.byteAt(index);
            if ((Opcode.IFEQ <= op && op <= Opcode.IF_ACMPNE)
                || op == Opcode.IFNULL || op == Opcode.IFNONNULL)
                targets = add(targets, size++, index + ci.s16bitAt(index + 1));
            else if (Opcode.GOTO <= op && op <= Opcode.LOOKUPSWITCH)
                switch (op) {
                case Opcode.GOTO :
                    targets = add(targets, size++, index + ci.s16bitAt(index + 1));
                    break;
                case Opcode.JSR :
                case Opcode.RET :
                    throw new BadBytecode("jsr/ret at " + index);
                case Opcode.TABLESWITCH : {
                    int pos = (index & ~3) + 4;
                    targets = add(targets, size++, index + ci.s32bitAt(pos));   // default offset
                    int low = ci.s32bitAt(pos + 4);
                    int high = ci.s32bitAt(pos + 8);
                    int p = pos + 12;
                    int n = p + (high - low + 1) * 4;
                    while (p < n) {
                        targets = add(targets, size++, index + ci.s32bitAt(p));
                        p += 4;
                    }
                    break; }
                case Opcode.LOOKUPSWITCH : {
                    int pos = (index & ~3) + 4;
                    targets = add(targets, size++, index + ci.s32bitAt(pos));   // default offset
                    int p = pos + 8 + 4;
                    int n = p + ci.s32bitAt(pos + 4) * 8;
                    while (p < n) {
                        targets = add(targets, size++, index + ci.s32bitAt(p));
                        p += 8;
                    }
                    break; }
                }
            else if (op == Opcode.GOTO_W)
                targets = add(targets, size++, index + ci.s32bitAt(index + 1));
            else if (op == Opcode.JSR_W)
                throw new BadBytecode("jsr_w at " + index);
        }

        if (et != null) {
            int i = et.size();
            while (--i >= 0) {
                targets = add(targets, size++, et.startPc(i));
                targets = add(targets, size++, et.handlerPc(i));
            }
        }

        return trimArray(targets);
    }

    private static int[] add(int[] targets, int size, int value) {
        if (targets.length >= size) {
            int[] a = new int[size << 1];
            System.arraycopy(targets, 0, a, 0, targets.length);
            targets = a;
        }

        targets[size++] = value;
        return targets;
    }

    private static BasicBlock[] trimArray(int[] targets) {
        int size = targets.length;
        java.util.Arrays.sort(targets);
        int s = 0;
        int t0 = 0;
        for (int i = 0; i < size; i++) {
            int t = targets[i];
            if (t != t0) {
                s++;
                t0 = t;
            }
        }

        BasicBlock[] results = new BasicBlock[s + 1];
        results[0] = new BasicBlock(0);
        t0 = 0;
        for (int i = 0, j = 1; i < size; i++) { 
            int t = targets[i];
            if (t != t0) {
                BasicBlock b = new BasicBlock(t);
                results[j++] = b;
                t0 = t;
            }
        }

        return results;
    }
}
