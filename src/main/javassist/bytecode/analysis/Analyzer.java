/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2007 Shigeru Chiba, and others. All Rights Reserved.
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
package javassist.bytecode.analysis;

import java.util.Iterator;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.ExceptionTable;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;

/**
 * A data-flow analyzer that determines the type state of the stack and local
 * variable table at every reachable instruction in a method. During analysis,
 * bytecode verification is performed in a similar manner to that described
 * in the JVM specification.
 *
 * <p>Example:</p>
 *
 * <pre>
 * // Method to analyze
 * public Object doSomething(int x) {
 *     Number n;
 *     if (x < 5) {
 *        n = new Double(0);
 *     } else {
 *        n = new Long(0);
 *     }
 *
 *     return n;
 * }
 *
 * // Which compiles to:
 * // 0:   iload_1
 * // 1:   iconst_5
 * // 2:   if_icmpge   17
 * // 5:   new #18; //class java/lang/Double
 * // 8:   dup
 * // 9:   dconst_0
 * // 10:  invokespecial   #44; //Method java/lang/Double."<init>":(D)V
 * // 13:  astore_2
 * // 14:  goto    26
 * // 17:  new #16; //class java/lang/Long
 * // 20:  dup
 * // 21:  lconst_1
 * // 22:  invokespecial   #47; //Method java/lang/Long."<init>":(J)V
 * // 25:  astore_2
 * // 26:  aload_2
 * // 27:  areturn
 *
 * public void analyzeIt(CtClass clazz, MethodInfo method) {
 *     Analyzer analyzer = new Analyzer();
 *     Frame[] frames = analyzer.analyze(clazz, method);
 *     frames[0].getLocal(0).getCtClass(); // returns clazz;
 *     frames[0].getLocal(1).getCtClass(); // returns java.lang.String
 *     frames[1].peek(); // returns Type.INTEGER
 *     frames[27].peek().getCtClass(); // returns java.lang.Number
 * }
 * </pre>
 *
 * @see FramePrinter
 * @author Jason T. Greene
 */
public class Analyzer implements Opcode {
    private final SubroutineScanner scanner = new SubroutineScanner();
    private CtClass clazz;
    private ExceptionInfo[] exceptions;
    private Frame[] frames;
    private Subroutine[] subroutines;

    private static class ExceptionInfo {
        private int end;
        private int handler;
        private int start;
        private Type type;

        private ExceptionInfo(int start, int end, int handler, Type type) {
            this.start = start;
            this.end = end;
            this.handler = handler;
            this.type = type;
        }
    }

    /**
     * Performs data-flow analysis on a method and returns an array, indexed by
     * instruction position, containing the starting frame state of all reachable
     * instructions. Non-reachable code, and illegal code offsets are represented
     * as a null in the frame state array. This can be used to detect dead code.
     *
     * If the method does not contain code (it is either native or abstract), null
     * is returned.
     *
     * @param clazz the declaring class of the method
     * @param method the method to analyze
     * @return an array, indexed by instruction position, of the starting frame state,
     *         or null if this method doesn't have code
     * @throws BadBytecode if the bytecode does not comply with the JVM specification
     */
    public Frame[] analyze(CtClass clazz, MethodInfo method) throws BadBytecode {
        this.clazz = clazz;
        CodeAttribute codeAttribute = method.getCodeAttribute();
        // Native or Abstract
        if (codeAttribute == null)
            return null;

        int maxLocals = codeAttribute.getMaxLocals();
        int maxStack = codeAttribute.getMaxStack();
        int codeLength = codeAttribute.getCodeLength();

        CodeIterator iter = codeAttribute.iterator();
        IntQueue queue = new IntQueue();

        exceptions = buildExceptionInfo(method);
        subroutines = scanner.scan(method);

        Executor executor = new Executor(clazz.getClassPool(), method.getConstPool());
        frames = new Frame[codeLength];
        frames[iter.lookAhead()] = firstFrame(method, maxLocals, maxStack);
        queue.add(iter.next());
        while (!queue.isEmpty()) {
            analyzeNextEntry(method, iter, queue, executor);
        }

        return frames;
    }

    /**
     * Performs data-flow analysis on a method and returns an array, indexed by
     * instruction position, containing the starting frame state of all reachable
     * instructions. Non-reachable code, and illegal code offsets are represented
     * as a null in the frame state array. This can be used to detect dead code.
     *
     * If the method does not contain code (it is either native or abstract), null
     * is returned.
     *
     * @param method the method to analyze
     * @return an array, indexed by instruction position, of the starting frame state,
     *         or null if this method doesn't have code
     * @throws BadBytecode if the bytecode does not comply with the JVM specification
     */
    public Frame[] analyze(CtMethod method) throws BadBytecode {
        return analyze(method.getDeclaringClass(), method.getMethodInfo2());
    }

    private void analyzeNextEntry(MethodInfo method, CodeIterator iter,
            IntQueue queue, Executor executor) throws BadBytecode {
        int pos = queue.take();
        iter.move(pos);
        iter.next();

        Frame frame = frames[pos].copy();
        Subroutine subroutine = subroutines[pos];

        try {
            executor.execute(method, pos, iter, frame, subroutine);
        } catch (RuntimeException e) {
            throw new BadBytecode(e.getMessage() + "[pos = " + pos + "]", e);
        }

        int opcode = iter.byteAt(pos);

        if (opcode == TABLESWITCH) {
            mergeTableSwitch(queue, pos, iter, frame);
        } else if (opcode == LOOKUPSWITCH) {
            mergeLookupSwitch(queue, pos, iter, frame);
        } else if (opcode == RET) {
            mergeRet(queue, iter, pos, frame, subroutine);
        } else if (Util.isJumpInstruction(opcode)) {
            int target = Util.getJumpTarget(pos, iter);

            if (Util.isJsr(opcode)) {
                // Merge the state before the jsr into the next instruction
                mergeJsr(queue, frames[pos], subroutines[target], pos, lookAhead(iter, pos));
            } else if (! Util.isGoto(opcode)) {
                merge(queue, frame, lookAhead(iter, pos));
            }

            merge(queue, frame, target);
        } else if (opcode != ATHROW && ! Util.isReturn(opcode)) {
            // Can advance to next instruction
            merge(queue, frame, lookAhead(iter, pos));
        }

        // Merge all exceptions that are reachable from this instruction.
        // The redundancy is intentional, since the state must be based
        // on the current instruction frame.
        mergeExceptionHandlers(queue, method, pos, frame);
    }

    private ExceptionInfo[] buildExceptionInfo(MethodInfo method) {
        ConstPool constPool = method.getConstPool();
        ClassPool classes = clazz.getClassPool();

        ExceptionTable table = method.getCodeAttribute().getExceptionTable();
        ExceptionInfo[] exceptions = new ExceptionInfo[table.size()];
        for (int i = 0; i < table.size(); i++) {
            int index = table.catchType(i);
            Type type;
            try {
                type = index == 0 ? Type.THROWABLE : Type.get(classes.get(constPool.getClassInfo(index)));
            } catch (NotFoundException e) {
                throw new IllegalStateException(e.getMessage());
            }

            exceptions[i] = new ExceptionInfo(table.startPc(i), table.endPc(i), table.handlerPc(i), type);
        }

        return exceptions;
    }

    private Frame firstFrame(MethodInfo method, int maxLocals, int maxStack) {
        int pos = 0;

        Frame first = new Frame(maxLocals, maxStack);
        if ((method.getAccessFlags() & AccessFlag.STATIC) == 0) {
            first.setLocal(pos++, Type.get(clazz));
        }

        CtClass[] parameters;
        try {
            parameters = Descriptor.getParameterTypes(method.getDescriptor(), clazz.getClassPool());
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }

        for (int i = 0; i < parameters.length; i++) {
            Type type = zeroExtend(Type.get(parameters[i]));
            first.setLocal(pos++, type);
            if (type.getSize() == 2)
                first.setLocal(pos++, Type.TOP);
        }

        return first;
    }

    private int getNext(CodeIterator iter, int of, int restore) throws BadBytecode {
        iter.move(of);
        iter.next();
        int next = iter.lookAhead();
        iter.move(restore);
        iter.next();

        return next;
    }

    private int lookAhead(CodeIterator iter, int pos) throws BadBytecode {
        if (! iter.hasNext())
            throw new BadBytecode("Execution falls off end! [pos = " + pos + "]");

        return iter.lookAhead();
    }


    private void merge(IntQueue queue, Frame frame, int target) {
        Frame old = frames[target];
        boolean changed;

        if (old == null) {
            frames[target] = frame.copy();
            changed = true;
        } else {
            changed = old.merge(frame);
        }

        if (changed) {
            queue.add(target);
        }
    }

    private void mergeExceptionHandlers(IntQueue queue, MethodInfo method, int pos, Frame frame) {
        for (int i = 0; i < exceptions.length; i++) {
            ExceptionInfo exception = exceptions[i];

            // Start is inclusive, while end is exclusive!
            if (pos >= exception.start && pos < exception.end) {
                Frame newFrame = frame.copy();
                newFrame.clearStack();
                newFrame.push(exception.type);
                merge(queue, newFrame, exception.handler);
            }
        }
    }

    private void mergeJsr(IntQueue queue, Frame frame, Subroutine sub, int pos, int next) throws BadBytecode {
        if (sub == null)
            throw new BadBytecode("No subroutine at jsr target! [pos = " + pos + "]");

        Frame old = frames[next];
        boolean changed = false;

        if (old == null) {
            old = frames[next] = frame.copy();
            changed = true;
        } else {
            for (int i = 0; i < frame.localsLength(); i++) {
                // Skip everything accessed by a subroutine, mergeRet must handle this
                if (!sub.isAccessed(i)) {
                    Type oldType = old.getLocal(i);
                    Type newType = frame.getLocal(i);
                    if (oldType == null) {
                        old.setLocal(i, newType);
                        changed = true;
                        continue;
                    }

                    newType = oldType.merge(newType);
                    // Always set the type, in case a multi-type switched to a standard type.
                    old.setLocal(i, newType);
                    if (!newType.equals(oldType) || newType.popChanged())
                        changed = true;
                }
            }
        }

        if (! old.isJsrMerged()) {
            old.setJsrMerged(true);
            changed = true;
        }

        if (changed && old.isRetMerged())
            queue.add(next);

    }

    private void mergeLookupSwitch(IntQueue queue, int pos, CodeIterator iter, Frame frame) throws BadBytecode {
        int index = (pos & ~3) + 4;
        // default
        merge(queue, frame, pos + iter.s32bitAt(index));
        int npairs = iter.s32bitAt(index += 4);
        int end = npairs * 8 + (index += 4);

        // skip "match"
        for (index += 4; index < end; index += 8) {
            int target = iter.s32bitAt(index) + pos;
            merge(queue, frame, target);
        }
    }

    private void mergeRet(IntQueue queue, CodeIterator iter, int pos, Frame frame, Subroutine subroutine) throws BadBytecode {
        if (subroutine == null)
            throw new BadBytecode("Ret on no subroutine! [pos = " + pos + "]");

        Iterator callerIter = subroutine.callers().iterator();
        while (callerIter.hasNext()) {
            int caller = ((Integer) callerIter.next()).intValue();
            int returnLoc = getNext(iter, caller, pos);
            boolean changed = false;

            Frame old = frames[returnLoc];
            if (old == null) {
                old = frames[returnLoc] = frame.copyStack();
                changed = true;
            } else {
                changed = old.mergeStack(frame);
            }

            for (Iterator i = subroutine.accessed().iterator(); i.hasNext(); ) {
                int index = ((Integer)i.next()).intValue();
                Type oldType = old.getLocal(index);
                Type newType = frame.getLocal(index);
                if (oldType != newType) {
                    old.setLocal(index, newType);
                    changed = true;
                }
            }

            if (! old.isRetMerged()) {
                old.setRetMerged(true);
                changed = true;
            }

            if (changed && old.isJsrMerged())
                queue.add(returnLoc);
        }
    }


    private void mergeTableSwitch(IntQueue queue, int pos, CodeIterator iter, Frame frame) throws BadBytecode {
        // Skip 4 byte alignment padding
        int index = (pos & ~3) + 4;
        // default
        merge(queue, frame, pos + iter.s32bitAt(index));
        int low = iter.s32bitAt(index += 4);
        int high = iter.s32bitAt(index += 4);
        int end = (high - low + 1) * 4 + (index += 4);

        // Offset table
        for (; index < end; index += 4) {
            int target = iter.s32bitAt(index) + pos;
            merge(queue, frame, target);
        }
    }

    private Type zeroExtend(Type type) {
        if (type == Type.SHORT || type == Type.BYTE || type == Type.CHAR || type == Type.BOOLEAN)
            return  Type.INTEGER;

        return type;
    }
}
