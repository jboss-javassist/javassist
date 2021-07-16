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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ExceptionTable;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;

/**
 * A basic block is a sequence of bytecode that does not contain jump/branch
 * instructions except at the last bytecode.
 * Since Java7 or later does not allow JSR, this class throws an exception when
 * it finds JSR.
 */
public class BasicBlock {
    static class JsrBytecode extends BadBytecode {
        /** default serialVersionUID */
        private static final long serialVersionUID = 1L;

        JsrBytecode() { super("JSR"); }
    }

    protected int position, length;
    protected int incoming;        // the number of incoming branches.
    protected BasicBlock[] exit;   // null if the block is a leaf.
    protected boolean stop;        // true if the block ends with an unconditional jump. 
    protected Catch toCatch;

    protected BasicBlock(int pos) {
        position = pos;
        length = 0;
        incoming = 0;
    }

    public static BasicBlock find(BasicBlock[] blocks, int pos)
        throws BadBytecode
    {
        for (BasicBlock b:blocks)
            if (b.position <= pos && pos < b.position + b.length)
                return b;

        throw new BadBytecode("no basic block at " + pos);
    }

    public static class Catch {
        public Catch next;
        public BasicBlock body;
        public int typeIndex;
        Catch(BasicBlock b, int i, Catch c) {
            body = b;
            typeIndex = i;
            next = c;
        }
    }

    @Override
    public String toString() {
        StringBuilder sbuf = new StringBuilder();
        String cname = this.getClass().getName();
        int i = cname.lastIndexOf('.');
        sbuf.append(i < 0 ? cname : cname.substring(i + 1));
        sbuf.append('[');
        toString2(sbuf);
        sbuf.append(']');
        return sbuf.toString();
    }

    protected void toString2(StringBuilder sbuf) {
        sbuf.append("pos=").append(position).append(", len=")
            .append(length).append(", in=").append(incoming)
            .append(", exit{");
        if (exit != null)
            for (BasicBlock b:exit)
                sbuf.append(b.position).append(',');

        sbuf.append("}, {");
        Catch th = toCatch;
        while (th != null) {
            sbuf.append('(').append(th.body.position).append(", ")
                .append(th.typeIndex).append("), ");
            th = th.next;
        }

        sbuf.append('}');
    }

    /**
     * A Mark indicates the position of a branch instruction
     * or a branch target.
     */
    static class Mark implements Comparable<Mark> {
        int position;
        BasicBlock block;
        BasicBlock[] jump;
        boolean alwaysJmp;     // true if an unconditional branch.
        int size;       // 0 unless the mark indicates RETURN etc. 
        Catch catcher;

        Mark(int p) {
            position = p;
            block = null;
            jump = null;
            alwaysJmp = false;
            size = 0;
            catcher = null;
        }

        @Override
        public int compareTo(Mark obj) {
            if (null == obj)
                return -1;
            return position - obj.position;
        }

        void setJump(BasicBlock[] bb, int s, boolean always) {
            jump = bb;
            size = s;
            alwaysJmp = always;
        }
    }

    public static class Maker {
        /* Override these two methods if a subclass of BasicBlock must be
         * instantiated.
         */
        protected BasicBlock makeBlock(int pos) {
            return new BasicBlock(pos);
        }

        protected BasicBlock[] makeArray(int size) {
            return new BasicBlock[size];
        }

        private BasicBlock[] makeArray(BasicBlock b) {
            BasicBlock[] array = makeArray(1);
            array[0] = b;
            return array;
        }

        private BasicBlock[] makeArray(BasicBlock b1, BasicBlock b2) {
            BasicBlock[] array = makeArray(2);
            array[0] = b1;
            array[1] = b2;
            return array;
        }

        public BasicBlock[] make(MethodInfo minfo) throws BadBytecode {
            CodeAttribute ca = minfo.getCodeAttribute();
            if (ca == null)
                return null;

            CodeIterator ci = ca.iterator();
            return make(ci, 0, ci.getCodeLength(), ca.getExceptionTable());
        }

        public BasicBlock[] make(CodeIterator ci, int begin, int end,
                                 ExceptionTable et)
            throws BadBytecode
        {
            Map<Integer,Mark> marks = makeMarks(ci, begin, end, et);
            BasicBlock[] bb = makeBlocks(marks);
            addCatchers(bb, et);
            return bb;
        }

        /* Branch target
         */
        private Mark makeMark(Map<Integer,Mark> table, int pos) {
            return makeMark0(table, pos, true, true);
        }

        /* Branch instruction.
         * size > 0
         */
        private Mark makeMark(Map<Integer,Mark> table, int pos, BasicBlock[] jump,
                              int size, boolean always) {
            Mark m = makeMark0(table, pos, false, false);
            m.setJump(jump, size, always);
            return m;
        }

        private Mark makeMark0(Map<Integer,Mark> table, int pos,
                               boolean isBlockBegin, boolean isTarget) {
            Integer p = pos;
            Mark m = table.get(p);
            if (m == null) {
                m = new Mark(pos);
                table.put(p, m);
            }

            if (isBlockBegin) {
                if (m.block == null)
                    m.block = makeBlock(pos);

                if (isTarget)
                    m.block.incoming++;
            }

            return m;
        }

        private Map<Integer,Mark> makeMarks(CodeIterator ci, int begin, int end,
                                  ExceptionTable et)
            throws BadBytecode
        {
            ci.begin();
            ci.move(begin);
            Map<Integer,Mark> marks = new HashMap<Integer,Mark>();
            while (ci.hasNext()) {
                int index = ci.next();
                if (index >= end)
                    break;

                int op = ci.byteAt(index);
                if ((Opcode.IFEQ <= op && op <= Opcode.IF_ACMPNE)
                        || op == Opcode.IFNULL || op == Opcode.IFNONNULL) {
                    Mark to = makeMark(marks, index + ci.s16bitAt(index + 1));
                    Mark next = makeMark(marks, index + 3);
                    makeMark(marks, index, makeArray(to.block, next.block), 3, false);
                }
                else if (Opcode.GOTO <= op && op <= Opcode.LOOKUPSWITCH)
                    switch (op) {
                    case Opcode.GOTO :
                        makeGoto(marks, index, index + ci.s16bitAt(index + 1), 3);
                        break;
                    case Opcode.JSR :
                        makeJsr(marks, index, index + ci.s16bitAt(index + 1), 3);
                        break;
                    case Opcode.RET :
                        makeMark(marks, index, null, 2, true);
                        break;
                    case Opcode.TABLESWITCH : {
                        int pos = (index & ~3) + 4;
                        int low = ci.s32bitAt(pos + 4);
                        int high = ci.s32bitAt(pos + 8);
                        int ncases = high - low + 1;
                        BasicBlock[] to = makeArray(ncases + 1);
                        to[0] = makeMark(marks, index + ci.s32bitAt(pos)).block;   // default branch target
                        int p = pos + 12;
                        int n = p + ncases * 4;
                        int k = 1;
                        while (p < n) {
                            to[k++] = makeMark(marks, index + ci.s32bitAt(p)).block;
                            p += 4;
                        }
                        makeMark(marks, index, to, n - index, true);
                        break; }
                    case Opcode.LOOKUPSWITCH : {
                        int pos = (index & ~3) + 4;
                        int ncases = ci.s32bitAt(pos + 4);
                        BasicBlock[] to = makeArray(ncases + 1);
                        to[0] = makeMark(marks, index + ci.s32bitAt(pos)).block;   // default branch target
                        int p = pos + 8 + 4;
                        int n = p + ncases * 8 - 4;
                        int k = 1;
                        while (p < n) {
                            to[k++] = makeMark(marks, index + ci.s32bitAt(p)).block;
                            p += 8;
                        }
                        makeMark(marks, index, to, n - index, true);
                        break; }
                    }
                else if ((Opcode.IRETURN <= op && op <= Opcode.RETURN) || op == Opcode.ATHROW)
                    makeMark(marks, index, null, 1, true);
                else if (op == Opcode.GOTO_W)
                    makeGoto(marks, index, index + ci.s32bitAt(index + 1), 5);
                else if (op == Opcode.JSR_W)
                    makeJsr(marks, index, index + ci.s32bitAt(index + 1), 5);
                else if (op == Opcode.WIDE && ci.byteAt(index + 1) == Opcode.RET)
                    makeMark(marks, index, null, 4, true);
            }

            if (et != null) {
                int i = et.size();
                while (--i >= 0) {
                    makeMark0(marks, et.startPc(i), true, false);
                    makeMark(marks, et.handlerPc(i));
                }
            }

            return marks;
        }

        private void makeGoto(Map<Integer,Mark> marks, int pos, int target, int size) {
            Mark to = makeMark(marks, target);
            BasicBlock[] jumps = makeArray(to.block);
            makeMark(marks, pos, jumps, size, true);
        }

        /*
         * We could ignore JSR since Java 7 or later does not allow it.
         * See The JVM Spec. Sec. 4.10.2.5.
         */
        protected void makeJsr(Map<Integer,Mark> marks, int pos, int target, int size) throws BadBytecode {
            /*
            Mark to = makeMark(marks, target);
            Mark next = makeMark(marks, pos + size);
            BasicBlock[] jumps = makeArray(to.block, next.block);
            makeMark(marks, pos, jumps, size, false);
            */
            throw new JsrBytecode();
        }

        private BasicBlock[] makeBlocks(Map<Integer,Mark> markTable) {
            Mark[] marks = markTable.values().toArray(new Mark[markTable.size()]);
            Arrays.sort(marks);
            List<BasicBlock> blocks = new ArrayList<BasicBlock>();
            int i = 0;
            BasicBlock prev;
            if (marks.length > 0 && marks[0].position == 0 && marks[0].block != null)
                prev = getBBlock(marks[i++]);
            else
                prev = makeBlock(0);

            blocks.add(prev);
            while (i < marks.length) {
                Mark m = marks[i++];
                BasicBlock bb = getBBlock(m);
                if (bb == null) {
                    // the mark indicates a branch instruction
                    if (prev.length > 0) {
                        // the previous mark already has exits.
                        prev = makeBlock(prev.position + prev.length);
                        blocks.add(prev);
                    }

                    prev.length = m.position + m.size - prev.position;
                    prev.exit = m.jump;
                    prev.stop = m.alwaysJmp;
                }
                else {
                    // the mark indicates a branch target
                    if (prev.length == 0) {
                        prev.length = m.position - prev.position;
                        bb.incoming++;
                        prev.exit = makeArray(bb);
                    }
                    else {
                        // the previous mark already has exits.
                        if (prev.position + prev.length < m.position) {
                            // dead code is found.
                            prev = makeBlock(prev.position + prev.length);
                            blocks.add(prev);
                            prev.length = m.position - prev.position;
                            // the incoming flow from dead code is not counted
                            // bb.incoming++;
                            prev.stop = true;   // because the incoming flow is not counted.
                            prev.exit = makeArray(bb);
                        }
                    }

                    blocks.add(bb);
                    prev = bb;
                }
            }

            return blocks.toArray(makeArray(blocks.size()));
        }

        private static BasicBlock getBBlock(Mark m) {
            BasicBlock b = m.block;
            if (b != null && m.size > 0) {
                b.exit = m.jump;
                b.length = m.size;
                b.stop = m.alwaysJmp;
            }

            return b;
        }

        private void addCatchers(BasicBlock[] blocks, ExceptionTable et)
            throws BadBytecode
        {
            if (et == null)
                return;

            int i = et.size();
            while (--i >= 0) {
                BasicBlock handler = find(blocks, et.handlerPc(i));
                int start = et.startPc(i);
                int end = et.endPc(i);
                int type = et.catchType(i);
                handler.incoming--;
                for (int k = 0; k < blocks.length; k++) {
                    BasicBlock bb = blocks[k];
                    int iPos = bb.position;
                    if (start <= iPos && iPos < end) {
                        bb.toCatch = new Catch(handler, type, bb.toCatch);
                        handler.incoming++;
                    }
                }
            }
        }
    }
}
