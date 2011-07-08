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

import javassist.ClassPool;
import javassist.bytecode.*;

/**
 * Stack map maker.
 */
public class MapMaker extends Tracer {
    /*
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
                ca.setAttribute(make(cp, minfo));
        }

        cc.writeFile("tmp");
    }

    public static void main2(String[] args) throws Exception {
        ClassPool cp = ClassPool.getDefault();
        //javassist.CtClass cc = cp.get(args[1]);
        javassist.CtClass cc = cp.makeClass(new java.io.FileInputStream(args[1]));
        MethodInfo minfo;
        if (args[2].equals("_init_"))
            minfo = cc.getDeclaredConstructors()[0].getMethodInfo();
            // minfo = cc.getClassInitializer().getMethodInfo();
        else
            minfo = cc.getDeclaredMethod(args[2]).getMethodInfo();

        CodeAttribute ca = minfo.getCodeAttribute();
        if (ca == null) {
            System.out.println("abstarct method");
            return;
        }

        TypedBlock[] blocks = TypedBlock.makeBlocks(minfo, ca, false);
        MapMaker mm = new MapMaker(cp, minfo, ca);
        mm.make(blocks, ca.getCode());
        for (int i = 0; i < blocks.length; i++)
            System.out.println(blocks[i]);
    }
    */

    /**
     * Computes the stack map table of the given method and returns it.
     * It returns null if the given method does not have to have a
     * stack map table.
     */
    public static StackMapTable make(ClassPool classes, MethodInfo minfo)
        throws BadBytecode
    {
        CodeAttribute ca = minfo.getCodeAttribute();
        if (ca == null)
            return null;

        TypedBlock[] blocks = TypedBlock.makeBlocks(minfo, ca, true);
        if (blocks == null)
            return null;

        MapMaker mm = new MapMaker(classes, minfo, ca);
        mm.make(blocks, ca.getCode());
        return mm.toStackMap(blocks);
    }

    /**
     * Computes the stack map table for J2ME.
     * It returns null if the given method does not have to have a
     * stack map table.
     */
    public static StackMap make2(ClassPool classes, MethodInfo minfo)
        throws BadBytecode
    {
        CodeAttribute ca = minfo.getCodeAttribute();
        if (ca == null)
            return null;

        TypedBlock[] blocks = TypedBlock.makeBlocks(minfo, ca, true);
        if (blocks == null)
            return null;

        MapMaker mm = new MapMaker(classes, minfo, ca);
        mm.make(blocks, ca.getCode());
        return mm.toStackMap2(minfo.getConstPool(), blocks);
    }

    public MapMaker(ClassPool classes, MethodInfo minfo, CodeAttribute ca) {
        super(classes, minfo.getConstPool(),
              ca.getMaxStack(), ca.getMaxLocals(),
              TypedBlock.getRetType(minfo.getDescriptor()));
    }

    protected MapMaker(MapMaker old, boolean copyStack) {
        super(old, copyStack);
    }

    /**
     * Runs an analyzer (Phase 1 and 2).
     */
    void make(TypedBlock[] blocks, byte[] code)
        throws BadBytecode
    {
        TypedBlock first = blocks[0];
        fixParamTypes(first);
        TypeData[] srcTypes = first.localsTypes;
        copyFrom(srcTypes.length, srcTypes, this.localsTypes);
        make(code, first);

        int n = blocks.length;
        for (int i = 0; i < n; i++)
            evalExpected(blocks[i]);
    }

    /*
     * If a parameter type is String but it is used only as Object
     * within the method body, this MapMaker class will report its type
     * is Object.  To avoid this, fixParamTypes calls TypeData.setType()
     * on each parameter type.
     */
    private void fixParamTypes(TypedBlock first) throws BadBytecode {
        TypeData[] types = first.localsTypes;
        int n = types.length;
        for (int i = 0; i < n; i++) {
            TypeData t = types[i];
            if (t instanceof TypeData.ClassName) {
                /* Skip the following statement if t.isNullType() is true
                 * although a parameter type is never null type.
                 */
                TypeData.setType(t, t.getName(), classPool);
            }
        }
    }

    // Phase 1

    private void make(byte[] code, TypedBlock tb)
        throws BadBytecode
    {
        BasicBlock.Catch handlers = tb.toCatch;
        while (handlers != null) {
            traceException(code, handlers);
            handlers = handlers.next;
        }

        int pos = tb.position;
        int end = pos + tb.length;
        while (pos < end)
            pos += doOpcode(pos, code);

        if (tb.exit != null) {
            for (int i = 0; i < tb.exit.length; i++) {
                TypedBlock e = (TypedBlock)tb.exit[i];
                if (e.alreadySet())
                    mergeMap(e, true);
                else {
                    recordStackMap(e);
                    MapMaker maker = new MapMaker(this, true);
                    maker.make(code, e);
                }
            }
        }
    }

    private void traceException(byte[] code, TypedBlock.Catch handler)
        throws BadBytecode
    {
        TypedBlock tb = (TypedBlock)handler.body;
        if (tb.alreadySet())
            mergeMap(tb, false);
        else {
            recordStackMap(tb, handler.typeIndex);
            MapMaker maker = new MapMaker(this, false);

            /* the following code is equivalent to maker.copyFrom(this)
             * except stackTypes are not copied.
             */ 
            maker.stackTypes[0] = tb.stackTypes[0].getSelf();
            maker.stackTop = 1;
            maker.make(code, tb);
        }
    }

    private void mergeMap(TypedBlock dest, boolean mergeStack) {
        boolean[] inputs = dest.inputs;
        int n = inputs.length;
        for (int i = 0; i < n; i++)
            if (inputs[i])
                merge(localsTypes[i], dest.localsTypes[i]); 

        if (mergeStack) {
            n = stackTop;
            for (int i = 0; i < n; i++)
                merge(stackTypes[i], dest.stackTypes[i]);
        }
    }

    private void merge(TypeData td, TypeData target) {
        boolean tdIsObj = false;
        boolean targetIsObj = false;
        // td or target is null if it is TOP. 
        if (td != TOP && td.isObjectType())
            tdIsObj = true;

        if (target != TOP && target.isObjectType())
            targetIsObj = true;

        if (tdIsObj && targetIsObj)
            target.merge(td);
    }

    private void recordStackMap(TypedBlock target)
        throws BadBytecode
    {
        TypeData[] tStackTypes = new TypeData[stackTypes.length];
        int st = stackTop;
        copyFrom(st, stackTypes, tStackTypes);
        recordStackMap0(target, st, tStackTypes);
    }

    private void recordStackMap(TypedBlock target, int exceptionType)
        throws BadBytecode
    {
        String type;
        if (exceptionType == 0)
            type = "java.lang.Throwable";
        else
            type = cpool.getClassInfo(exceptionType);

        TypeData[] tStackTypes = new TypeData[stackTypes.length];
        tStackTypes[0] = new TypeData.ClassName(type);

        recordStackMap0(target, 1, tStackTypes);
    }

    private void recordStackMap0(TypedBlock target, int st, TypeData[] tStackTypes)
        throws BadBytecode
    {
        int n = localsTypes.length;
        TypeData[] tLocalsTypes = new TypeData[n];
        int k = copyFrom(n, localsTypes, tLocalsTypes);

        boolean[] inputs = target.inputs;
        for (int i = 0; i < n; i++)
            if (!inputs[i])
                tLocalsTypes[i] = TOP;

        target.setStackMap(st, tStackTypes, k, tLocalsTypes);
    }

    // Phase 2

    void evalExpected(TypedBlock target) throws BadBytecode {
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

    public StackMapTable toStackMap(TypedBlock[] blocks) {
        StackMapTable.Writer writer = new StackMapTable.Writer(32);
        int n = blocks.length;
        TypedBlock prev = blocks[0];
        int offsetDelta = prev.length;
        if (prev.incoming > 0) {     // the first instruction is a branch target.
            writer.sameFrame(0);
            offsetDelta--;
        }

        for (int i = 1; i < n; i++) {
            TypedBlock bb = blocks[i];
            if (isTarget(bb, blocks[i - 1])) {
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

    /**
     * Returns true if cur is a branch target.
     */
    private boolean isTarget(TypedBlock cur, TypedBlock prev) {
        int in = cur.incoming;
        if (in > 1)
            return true;
        else if (in < 1)
            return false;

        return prev.stop;
    }

    private void toStackMapBody(StackMapTable.Writer writer, TypedBlock bb,
                                int diffL, int offsetDelta, TypedBlock prev) {
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

    // Phase 3 for J2ME

    public StackMap toStackMap2(ConstPool cp, TypedBlock[] blocks) {
        StackMap.Writer writer = new StackMap.Writer();
        int n = blocks.length;      // should be > 0
        boolean[] effective = new boolean[n];
        TypedBlock prev = blocks[0];

        // Is the first instruction a branch target?
        effective[0] = prev.incoming > 0;

        int num = effective[0] ? 1 : 0;
        for (int i = 1; i < n; i++) {
            TypedBlock bb = blocks[i];
            if (effective[i] = isTarget(bb, blocks[i - 1])) {
                bb.resetNumLocals();
                prev = bb;
                num++;
            }
        }

        if (num == 0)
            return null;

        writer.write16bit(num);
        for (int i = 0; i < n; i++)
            if (effective[i])
                writeStackFrame(writer, cp, blocks[i].position, blocks[i]);

        return writer.toStackMap(cp);
    }

    private void writeStackFrame(StackMap.Writer writer, ConstPool cp, int offset, TypedBlock tb) {
        writer.write16bit(offset);
        writeVerifyTypeInfo(writer, cp, tb.localsTypes, tb.numLocals);
        writeVerifyTypeInfo(writer, cp, tb.stackTypes, tb.stackTop);
    }

    private void writeVerifyTypeInfo(StackMap.Writer writer, ConstPool cp, TypeData[] types, int num) {
        int numDWord = 0;
        for (int i = 0; i < num; i++) {
            TypeData td = types[i];
            if (td != null && td.is2WordType()) {
                numDWord++;
                i++;
            }
        }

        writer.write16bit(num - numDWord);
        for (int i = 0; i < num; i++) {
            TypeData td = types[i];
            if (td == TOP)
                writer.writeVerifyTypeInfo(StackMap.TOP, 0);
            else {
                writer.writeVerifyTypeInfo(td.getTypeTag(), td.getTypeData(cp));
                if (td.is2WordType())
                    i++;
            }
        }
    }
}
