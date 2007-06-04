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

 package javassist.bytecode.stackmap;

import javassist.bytecode.*;

public class Liveness {
    protected static final byte UNKNOWN = 0;
    protected static final byte READ = 1;
    protected static final byte UPDATED = 2;
    protected byte[] localsUsage;

    /**
     * If true, all the arguments become alive within the whole method body.
     *
     * To correctly compute a stack map table, all the arguments must
     * be alive (localsUsage[?] must be READ) at least in the first block.
     */
    public static boolean useArgs = true;

    public void compute(CodeIterator ci, TypedBlock[] blocks, int maxLocals,
                        TypeData[] args)
        throws BadBytecode
    {
        computeUsage(ci, blocks, maxLocals);
        if (useArgs)
            useAllArgs(blocks, args);

        computeLiveness1(blocks[0]);
        while (hasChanged(blocks))
            computeLiveness2(blocks[0]);
    }

    private void useAllArgs(TypedBlock[] blocks, TypeData[] args) {
        for (int k = 0; k < blocks.length; k++) {
            byte[] usage = blocks[k].localsUsage;
            for (int i = 0; i < args.length; i++)
                if (args[i] != TypeTag.TOP)
                    usage[i] = READ;
        }
    }

    static final int NOT_YET = 0;
    static final int CHANGED_LAST = 1;
    static final int DONE = 2;
    static final int CHANGED_NOW = 3;

    private void computeLiveness1(TypedBlock tb) {
        if (tb.updating) {
            // a loop was detected.
            computeLiveness1u(tb);
            return;
        }

        if (tb.inputs != null)
            return;

        tb.updating = true;
        byte[] usage = tb.localsUsage;
        int n = usage.length;
        boolean[] in = new boolean[n];
        for (int i = 0; i < n; i++)
            in[i] = usage[i] == READ;

        BasicBlock.Catch handlers = tb.toCatch;
        while (handlers != null) {
            TypedBlock h = (TypedBlock)handlers.body;
            computeLiveness1(h);
            for (int k = 0; k < n; k++)
                if (h.inputs[k])
                    in[k] = true;

            handlers = handlers.next;
        }

        if (tb.exit != null) {
            for (int i = 0; i < tb.exit.length; i++) {
                TypedBlock e = (TypedBlock)tb.exit[i];
                computeLiveness1(e);
                for (int k = 0; k < n; k++)
                    if (!in[k])
                        in[k] = usage[k] == UNKNOWN && e.inputs[k];
            }
        }

        tb.updating = false;
        if (tb.inputs == null) {
            tb.inputs = in;
            tb.status = DONE;
        }
        else {
            for (int i = 0; i < n; i++)
                if (in[i] && !tb.inputs[i]) {
                    tb.inputs[i] = true; 
                    tb.status = CHANGED_NOW;
                }
        }
    }

    private void computeLiveness1u(TypedBlock tb) {
        if (tb.inputs == null) {
            byte[] usage = tb.localsUsage;
            int n = usage.length;
            boolean[] in = new boolean[n];
            for (int i = 0; i < n; i++)
                in[i] = usage[i] == READ;

            tb.inputs = in;
            tb.status = DONE;
        }
    }

    private void computeLiveness2(TypedBlock tb) {
        if (tb.updating || tb.status >= DONE)
            return;

        tb.updating = true;
        if (tb.exit == null)
            tb.status = DONE;
        else {
            boolean changed = false;
            for (int i = 0; i < tb.exit.length; i++) {
                TypedBlock e = (TypedBlock)tb.exit[i];
                computeLiveness2(e);
                if (e.status != DONE)
                    changed = true;
            }

            if (changed) {
                changed = false;
                byte[] usage = tb.localsUsage;
                int n = usage.length;
                for (int i = 0; i < tb.exit.length; i++) {
                    TypedBlock e = (TypedBlock)tb.exit[i];
                    if (e.status != DONE)
                        for (int k = 0; k < n; k++)
                            if (!tb.inputs[k]) {
                                if (usage[k] == UNKNOWN && e.inputs[k]) {
                                    tb.inputs[k] = true;
                                    changed = true;
                                }
                            }
                }

                tb.status = changed ? CHANGED_NOW : DONE;
            }
            else
                tb.status = DONE;
        }

        if (computeLiveness2except(tb))
            tb.status = CHANGED_NOW;

        tb.updating = false;
    }

    private boolean computeLiveness2except(TypedBlock tb) {
        BasicBlock.Catch handlers = tb.toCatch;
        boolean changed = false;
        while (handlers != null) {
            TypedBlock h = (TypedBlock)handlers.body;
            computeLiveness2(h);
            if (h.status != DONE) {
                boolean[] in = tb.inputs;
                int n = in.length;
                for (int k = 0; k < n; k++)
                    if (!in[k] && h.inputs[k]) {
                        in[k] = true;
                        changed = true;
                    }
            }

            handlers = handlers.next;
        }

        return changed;
    }

    private boolean hasChanged(TypedBlock[] blocks) {
        int n = blocks.length;
        boolean changed = false;
        for (int i = 0; i < n; i++) {
            TypedBlock tb = blocks[i];
            if (tb.status == CHANGED_NOW) {
                tb.status = CHANGED_LAST;
                changed = true;
            }
            else
                tb.status = NOT_YET;
        }

        return changed;
    }

    private void computeUsage(CodeIterator ci, TypedBlock[] blocks, int maxLocals)
        throws BadBytecode
    {
        int n = blocks.length;
        for (int i = 0; i < n; i++) {
            TypedBlock tb = blocks[i];
            localsUsage = tb.localsUsage = new byte[maxLocals];
            int pos = tb.position;
            analyze(ci, pos, pos + tb.length);
            localsUsage = null;
        }
    }

    protected final void readLocal(int reg) {
        if (localsUsage[reg] == UNKNOWN)
            localsUsage[reg] = READ;
    }

    protected final void writeLocal(int reg) {
        if (localsUsage[reg] == UNKNOWN)
            localsUsage[reg] = UPDATED;
    }

    protected void analyze(CodeIterator ci, int begin, int end)
        throws BadBytecode
    {
        ci.begin();
        ci.move(begin);
        while (ci.hasNext()) {
            int index = ci.next();
            if (index >= end)
                break;

            int op = ci.byteAt(index);
            if (op < 96)
                if (op < 54)
                    doOpcode0_53(ci, index, op);
                else
                    doOpcode54_95(ci, index, op);
            else
                if (op == Opcode.IINC) {
                    // this does not call writeLocal().
                    readLocal(ci.byteAt(index + 1));
                }
                else if (op == Opcode.WIDE)
                    doWIDE(ci, index);
        }
    }

    private void doOpcode0_53(CodeIterator ci, int pos, int op) {
        switch (op) {
        case Opcode.ILOAD :
        case Opcode.LLOAD :
        case Opcode.FLOAD :
        case Opcode.DLOAD :
        case Opcode.ALOAD :
            readLocal(ci.byteAt(pos + 1));
            break;
        case Opcode.ILOAD_0 :
        case Opcode.ILOAD_1 :
        case Opcode.ILOAD_2 :
        case Opcode.ILOAD_3 :
            readLocal(op - Opcode.ILOAD_0);
            break;
        case Opcode.LLOAD_0 :
        case Opcode.LLOAD_1 :
        case Opcode.LLOAD_2 :
        case Opcode.LLOAD_3 :
            readLocal(op - Opcode.LLOAD_0);
            break;
        case Opcode.FLOAD_0 :
        case Opcode.FLOAD_1 :
        case Opcode.FLOAD_2 :
        case Opcode.FLOAD_3 :
            readLocal(op - Opcode.FLOAD_0);
            break;
        case Opcode.DLOAD_0 :
        case Opcode.DLOAD_1 :
        case Opcode.DLOAD_2 :
        case Opcode.DLOAD_3 :
            readLocal(op - Opcode.DLOAD_0);
            break;
        case Opcode.ALOAD_0 :
        case Opcode.ALOAD_1 :
        case Opcode.ALOAD_2 :
        case Opcode.ALOAD_3 :
            readLocal(op - Opcode.ALOAD_0);
            break;
        }
    }

    private void doOpcode54_95(CodeIterator ci, int pos, int op) {
        switch (op) {
        case Opcode.ISTORE :
        case Opcode.LSTORE :
        case Opcode.FSTORE :
        case Opcode.DSTORE :
        case Opcode.ASTORE :
            writeLocal(ci.byteAt(pos + 1));
            break;
        case Opcode.ISTORE_0 :
        case Opcode.ISTORE_1 :
        case Opcode.ISTORE_2 :
        case Opcode.ISTORE_3 :
            writeLocal(op - Opcode.ISTORE_0);
            break;
        case Opcode.LSTORE_0 :
        case Opcode.LSTORE_1 :
        case Opcode.LSTORE_2 :
        case Opcode.LSTORE_3 :
            writeLocal(op - Opcode.LSTORE_0);
            break;
        case Opcode.FSTORE_0 :
        case Opcode.FSTORE_1 :
        case Opcode.FSTORE_2 :
        case Opcode.FSTORE_3 :
            writeLocal(op - Opcode.FSTORE_0);
            break;
        case Opcode.DSTORE_0 :
        case Opcode.DSTORE_1 :
        case Opcode.DSTORE_2 :
        case Opcode.DSTORE_3 :
            writeLocal(op - Opcode.DSTORE_0);
            break;
        case Opcode.ASTORE_0 :
        case Opcode.ASTORE_1 :
        case Opcode.ASTORE_2 :
        case Opcode.ASTORE_3 :
            writeLocal(op - Opcode.ASTORE_0);
            break;
        }
    }

    private void doWIDE(CodeIterator ci, int pos) throws BadBytecode {
        int op = ci.byteAt(pos + 1);
        int var = ci.u16bitAt(pos + 2);
        switch (op) {
        case Opcode.ILOAD :
        case Opcode.LLOAD :
        case Opcode.FLOAD :
        case Opcode.DLOAD :
        case Opcode.ALOAD :
            readLocal(var);
            break;
        case Opcode.ISTORE :
        case Opcode.LSTORE :
        case Opcode.FSTORE :
        case Opcode.DSTORE :
        case Opcode.ASTORE :
            writeLocal(var);
            break;
        case Opcode.IINC :
            readLocal(var);
            // this does not call writeLocal().
            break;
        }
    }
}
