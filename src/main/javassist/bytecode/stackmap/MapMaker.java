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

import javassist.ClassPool;
import javassist.bytecode.*;

/**
 * Stack map maker.
 */
public class MapMaker extends Tracer {
    private boolean moveon;     // used for returning another value from doOpcode().
    private boolean loopDetected;
    private int iteration;
    private BasicBlock[] blocks;

    public static void main(String[] args) throws Exception {
        boolean useMain2 = args[0].equals("0");
        if (useMain2 && args.length > 1) {
            main2(args);
            return;
        }

        for (int i = 0; i < args.length; i++)
            main1(args[i]);
    }

    public static void main1(String className) throws Exception {
        ClassPool cp = ClassPool.getDefault();
        //javassist.CtClass cc = cp.get(className);
        javassist.CtClass cc = cp.makeClass(new java.io.FileInputStream(className));
        System.out.println(className);
        ClassFile cf = cc.getClassFile();
        java.util.List minfos = cf.getMethods();
        for (int i = 0; i < minfos.size(); i++) {
            MethodInfo minfo = (MethodInfo)minfos.get(i);
            CodeAttribute ca = minfo.getCodeAttribute();
            if (ca != null)
                ca.setAttribute(MapMaker.getMap(cp, minfo)); 
        }

        cc.writeFile("tmp");
    }

    public static void main2(String[] args) throws Exception {
        ClassPool cp = ClassPool.getDefault();
        //javassist.CtClass cc = cp.get(args[1]);
        javassist.CtClass cc = cp.makeClass(new java.io.FileInputStream(args[1]));
        MethodInfo minfo;
        MapMaker mm;
        if (args[2].equals("_init_"))
            // minfo = cc.getDeclaredConstructors()[0].getMethodInfo();
            minfo = cc.getClassInitializer().getMethodInfo();
        else
            minfo = cc.getDeclaredMethod(args[2]).getMethodInfo();

        mm = makeMapMaker(cp, minfo);
        if (mm == null)
            System.out.println("single basic block");
        else {
            BasicBlock[] blocks = mm.getBlocks();
            for (int i = 0; i < blocks.length; i++)
                System.out.println(blocks[i]);

            StackMapTable smt = mm.toStackMap();
        }
    }

    /**
     * Computes the stack map table of the given method and returns it.
     * It returns null if the given method does not have to have a
     * stack map table.
     */
    public static StackMapTable getMap(ClassPool classes, MethodInfo minfo)
        throws BadBytecode
    {
        MapMaker mm = makeMapMaker(classes, minfo);
        if (mm == null)
            return null;
        else
            return mm.toStackMap();
    }

    /**
     * Makes basic blocks with stack maps.  If the number of the basic blocks
     * is one, this method returns null.  If the given method info does not
     * include a code attribute, this method also returns null.
     */
    public static MapMaker makeMapMaker(ClassPool classes, MethodInfo minfo)
        throws BadBytecode
    {
        CodeAttribute ca = minfo.getCodeAttribute();
        if (ca == null)
            return null;

        CodeIterator ci = ca.iterator();
        ConstPool pool = minfo.getConstPool();
        ExceptionTable et = ca.getExceptionTable();
        BasicBlock[] blocks = BasicBlock.makeBlocks(ci, 0, ci.getCodeLength(),
                                                    et, 0, pool);
        if (blocks.length < 2)
            if (blocks.length == 0 || blocks[0].inbound < 1) 
                return null;

        boolean isStatic = (minfo.getAccessFlags() & AccessFlag.STATIC) != 0;
        int maxStack = ca.getMaxStack();
        int maxLocals = ca.getMaxLocals();
        BasicBlock top = blocks[0];
        String desc = minfo.getDescriptor();
        top.initFirstBlock(maxStack, maxLocals, pool.getClassName(), desc,
                           isStatic, minfo.isConstructor());
        String retType = BasicBlock.getRetType(desc);
        MapMaker mm = new MapMaker(classes, pool, maxStack, maxLocals,
                                   blocks, retType, blocks[0], 0);
        mm.make(ca.getCode(), et);
        return mm;
    }

    /**
     * Constructs a tracer.
     */
    MapMaker(ClassPool classes, ConstPool cp,
             int maxStack, int maxLocals, BasicBlock[] bb,
             String retType, BasicBlock init, int iterate)
    {
        this(classes, cp, maxStack, maxLocals, bb, retType, iterate);
        TypeData[] srcTypes = init.localsTypes;
        copyFrom(srcTypes.length, srcTypes, this.localsTypes);
    }

    private MapMaker(ClassPool classes, ConstPool cp,
            int maxStack, int maxLocals, BasicBlock[] bb,
            String retType, int iterateNo)
    {
        super(classes, cp, maxStack, maxLocals, retType);
        blocks = bb;
        loopDetected = false;
        iteration = iterateNo;
    }

    public BasicBlock[] getBlocks() { return blocks; }

    /**
     * Runs an analyzer.
     */
    void make(byte[] code, ExceptionTable et) throws BadBytecode {
        blocks[0].version = iteration;
        make(code, blocks[0]);
        if (loopDetected) {
            blocks[0].version = ++iteration;
            make(code, blocks[0]);
        }

        int n = blocks.length;
        for (int i = 0; i < n; i++)
            evalExpected(blocks[i]);
    }

    // Phase 1: Code Tracing

    private void make(byte[] code, BasicBlock bb)
        throws BadBytecode
    {
        int pos = bb.position;
        int end = pos + bb.length;
        traceExceptions(code, bb.catchBlocks);
        moveon = true;
        while (moveon && pos < end)
            pos += doOpcode(pos, code);

        if (moveon && pos < code.length) {
            this.copyFrom(this);
            nextBlock(pos, code, 0);
        }
    }

    private void nextBlock(int pos, byte[] code, int offset) throws BadBytecode {
        BasicBlock bb = BasicBlock.find(blocks, pos + offset);
        if (bb.alreadySet(iteration)) {
            mergeMap(stackTypes, bb.stackTypes);
            mergeMap(localsTypes, bb.localsTypes);
            mergeUsage(bb);
        }
        else {
            recordStackMap(bb);
            bb.version = iteration;
            MapMaker maker = new MapMaker(classPool, cpool, stackTypes.length,
                                localsTypes.length, blocks, returnType, iteration);
            maker.copyFrom(this);
            maker.make(code, bb);
            recordUsage(bb, maker);
            if (maker.loopDetected)
                this.loopDetected = true;
        }
    }

    private void traceExceptions(byte[] code, BasicBlock.Branch branches)
        throws BadBytecode
    {
        while (branches != null) {
            int pos = branches.target;
            BasicBlock bb = BasicBlock.find(blocks, pos);
            if (bb.alreadySet(iteration)) {
                mergeMap(localsTypes, bb.localsTypes);
                mergeUsage(bb);
            }
            else {
                recordStackMap(bb, branches.typeIndex);
                bb.version = iteration;
                MapMaker maker = new MapMaker(classPool, cpool, stackTypes.length,
                                   localsTypes.length, blocks, returnType, iteration);

                /* the following code is equivalent to maker.copyFrom(this)
                 * except stackTypes are not copied.
                 */ 
                maker.stackTypes[0] = bb.stackTypes[0].getSelf();
                maker.stackTop = 1;
                TypeData[] srcTypes = this.localsTypes;
                copyFrom(srcTypes.length, srcTypes, maker.localsTypes);

                maker.make(code, bb);
                recordUsage(bb, maker);
                if (maker.loopDetected)
                    this.loopDetected = true;
            }

            branches = branches.next;
        }
    }

    private static void mergeMap(TypeData[] srcTypes, TypeData[] destTypes) {
        int n = srcTypes.length;
        for (int i = 0; i < n; i++) {
            TypeData s = srcTypes[i];
            TypeData d = destTypes[i];
            boolean sIsObj = false;
            boolean dIsObj = false;
            // s or b is null if it is TOP. 
            if (s != TOP && s.isObjectType())
                sIsObj = true;

            if (d != TOP && d.isObjectType())
                dIsObj = true;

            if (sIsObj && dIsObj)
                d.merge(s);
            else if (s != d)
                destTypes[i] = TOP;
        }
    }

    private void copyFrom(MapMaker src) {
        int sp = src.stackTop;
        this.stackTop = sp;
        copyFrom(sp, src.stackTypes, this.stackTypes);
        TypeData[] srcTypes = src.localsTypes;
        copyFrom(srcTypes.length, srcTypes, this.localsTypes);
    }

    private static int copyFrom(int n, TypeData[] srcTypes, TypeData[] destTypes) {
        int k = -1;
        for (int i = 0; i < n; i++) {
            TypeData t = srcTypes[i];
            destTypes[i] = t == null ? null : t.getSelf();
            if (t != TOP)
                if (t.is2WordType())
                    k = i + 1;
                else
                    k = i;
        }

        return k + 1;
    }

    private void recordStackMap(BasicBlock target)
        throws BadBytecode
    {
        int n = localsTypes.length;
        TypeData[] tLocalsTypes = new TypeData[n];
        int k = copyFrom(n, localsTypes, tLocalsTypes);

        n = stackTypes.length;
        TypeData[] tStackTypes = new TypeData[n];
        int st = stackTop;
        copyFrom(st, stackTypes, tStackTypes);

        target.setStackMap(st, tStackTypes, k, tLocalsTypes);
    }

    private void recordStackMap(BasicBlock target, int exceptionType)
        throws BadBytecode
    {
        int n = localsTypes.length;
        TypeData[] tLocalsTypes = new TypeData[n];
        int k = copyFrom(n, localsTypes, tLocalsTypes);

        String type;
        if (exceptionType == 0)
            type = "java.lang.Throwable";
        else
            type = cpool.getClassInfo(exceptionType);

        TypeData[] tStackTypes = new TypeData[stackTypes.length];
        tStackTypes[0] = new TypeData.ClassName(type);

        target.setStackMap(1, tStackTypes, k, tLocalsTypes);
    }

    private void recordUsage(BasicBlock target, MapMaker next) {
        int[] nextUsage = next.localsUsage;
        TypeData[] tData = target.localsTypes;
        int n = tData.length;
        for (int i = blocks[0].numLocals; i < n; i++)
            if (nextUsage[i] == READ)
                readLocal(i);
            else
                tData[i] = TOP;

        int[] usage = new int[nextUsage.length];
        n = usage.length;
        for (int i = 0; i < n; i++)
            usage[i] = nextUsage[i];

        target.localsUsage = usage;
    }

    private void mergeUsage(BasicBlock target) {
        int[] usage = target.localsUsage;
        if (usage == null) {
            // detected a loop.
            loopDetected = true;
        }
        else {
            int n = usage.length;
            for (int i = 0; i < n; i++)
                if (usage[i] == READ)
                    readLocal(i);
        }
    }

    // Phase 2

    void evalExpected(BasicBlock target) throws BadBytecode {
        ClassPool cp = classPool;
        evalExpected(cp, target.stackTop, target.stackTypes);
        TypeData[] types = target.localsTypes;
        if (types != null)  // unless this block is dead code
            evalExpected(cp, types.length, types);
    }

    private static void evalExpected(ClassPool cp, int n, TypeData[] types)
        throws BadBytecode
    {
        for (int i = 0; i < n; i++) {
            TypeData td = types[i];
            if (td != null)
                td.evalExpectedType(cp);
        }
    }

    // Phase 3

    public StackMapTable toStackMap() {
        BasicBlock[] blocks = this.blocks;
        StackMapTable.Writer writer = new StackMapTable.Writer(32);
        int n = blocks.length;
        BasicBlock prev = blocks[0];
        int offsetDelta = prev.length;
        if (prev.inbound > 0) {     // the first instruction is a branch target.
            writer.sameFrame(0);
            offsetDelta--;
        }

        for (int i = 1; i < n; i++) {
            BasicBlock bb = blocks[i];
            if (bb.inbound > 0) {
                bb.resetNumLocals();
                int diffL = stackMapDiff(prev.numLocals, prev.localsTypes,
                                         bb.numLocals, bb.localsTypes);
                toStackMapBody(writer, bb, diffL, offsetDelta, prev);
                offsetDelta = bb.length - 1;
                prev = bb;
            }
            else
                offsetDelta += bb.length;

        }

        return writer.toStackMapTable(cpool);
    }

    private void toStackMapBody(StackMapTable.Writer writer, BasicBlock bb,
                                int diffL, int offsetDelta, BasicBlock prev) {
        // if diffL is -100, two TypeData arrays do not share
        // any elements.

        int stackTop = bb.stackTop;
        if (stackTop == 0) {
            if (diffL == 0) {
                writer.sameFrame(offsetDelta);
                return;
            }
            else if (0 > diffL && diffL >= -3) {
                writer.chopFrame(offsetDelta, -diffL);
                return;
            }
            else if (0 < diffL && diffL <= 3) {
                int[] data = new int[diffL];
                int[] tags = fillStackMap(bb.numLocals - prev.numLocals,
                                          prev.numLocals, data,
                                          bb.localsTypes);
                writer.appendFrame(offsetDelta, tags, data);
                return;
            }
        }
        else if (stackTop == 1 && diffL == 0) {
            TypeData td = bb.stackTypes[0];
            if (td == TOP)
                writer.sameLocals(offsetDelta, StackMapTable.TOP, 0);
            else
                writer.sameLocals(offsetDelta, td.getTypeTag(),
                                  td.getTypeData(cpool));
            return;
        }
        else if (stackTop == 2 && diffL == 0) {
            TypeData td = bb.stackTypes[0];
            if (td != TOP && td.is2WordType()) {
                // bb.stackTypes[1] must be TOP.
                writer.sameLocals(offsetDelta, td.getTypeTag(),
                                  td.getTypeData(cpool));
                return;
            }
        }

        int[] sdata = new int[stackTop];
        int[] stags = fillStackMap(stackTop, 0, sdata, bb.stackTypes);
        int[] ldata = new int[bb.numLocals];
        int[] ltags = fillStackMap(bb.numLocals, 0, ldata, bb.localsTypes);
        writer.fullFrame(offsetDelta, ltags, ldata, stags, sdata);
    }

    private int[] fillStackMap(int num, int offset, int[] data, TypeData[] types) {
        int realNum = diffSize(types, offset, offset + num);
        ConstPool cp = cpool;
        int[] tags = new int[realNum];
        int j = 0;
        for (int i = 0; i < num; i++) {
            TypeData td = types[offset + i];
            if (td == TOP) {
                tags[j] = StackMapTable.TOP;
                data[j] = 0;
            }
            else {
                tags[j] = td.getTypeTag();
                data[j] = td.getTypeData(cp);
                if (td.is2WordType())
                    i++;
            }

            j++;
        }

        return tags;
    }

    private static int stackMapDiff(int oldTdLen, TypeData[] oldTd,
                                    int newTdLen, TypeData[] newTd)
    {
        int diff = newTdLen - oldTdLen;
        int len;
        if (diff > 0)
            len = oldTdLen;
        else
            len = newTdLen;

        if (stackMapEq(oldTd, newTd, len))
            if (diff > 0)
                return diffSize(newTd, len, newTdLen);
            else
                return -diffSize(oldTd, len, oldTdLen);
        else
            return -100;
    }

    private static boolean stackMapEq(TypeData[] oldTd, TypeData[] newTd, int len) {
        for (int i = 0; i < len; i++) {
            TypeData td = oldTd[i];
            if (td == TOP) {        // the next element to LONG/DOUBLE is TOP.
                if (newTd[i] != TOP)
                    return false;
            }
            else
                if (!oldTd[i].equals(newTd[i]))
                    return false;
        }

        return true;
    }

    private static int diffSize(TypeData[] types, int offset, int len) {
        int num = 0;
        while (offset < len) {
            TypeData td = types[offset++];
            num++;
            if (td != TOP && td.is2WordType())
                offset++;
        }

        return num;
    }

    // Branch actions

    protected void visitBranch(int pos, byte[] code, int offset) throws BadBytecode {
        nextBlock(pos, code, offset);
    }

    protected void visitGoto(int pos, byte[] code, int offset) throws BadBytecode {
        nextBlock(pos, code, offset);
        moveon = false;
    }

    protected void visitTableSwitch(int pos, byte[] code, int n, int offsetPos, int defaultOffset)
        throws BadBytecode
    {
        nextBlock(pos, code, defaultOffset);
        for (int i = 0; i < n; i++) {
            nextBlock(pos, code, ByteArray.read32bit(code, offsetPos));
            offsetPos += 4;
        }

        moveon = false;
    }

    protected void visitLookupSwitch(int pos, byte[] code, int n, int pairsPos, int defaultOffset)
        throws BadBytecode
    {
        nextBlock(pos, code, defaultOffset);
        pairsPos += 4;
        for (int i = 0; i < n; i++) {
            nextBlock(pos, code, ByteArray.read32bit(code, pairsPos));
            pairsPos += 8;
        }

        moveon = false;
    }

    protected void visitReturn(int pos, byte[] code) { moveon = false; }

    protected void visitThrow(int pos, byte[] code) { moveon = false; }
}
