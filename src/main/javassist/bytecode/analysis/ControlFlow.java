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

package javassist.bytecode.analysis;

import java.util.ArrayList;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.stackmap.BasicBlock;

/**
 * Represents the control flow graph of a given method.
 *
 * <p>To obtain the control flow graph, do the following:</p>
 *
 * <pre>CtMethod m = ...
 * ControlFlow cf = new ControlFlow(m);
 * Block[] blocks = cf.basicBlocks();
 * </pre>
 *
 * <p><code>blocks</code> is an array of basic blocks in
 * that method body.</p>
 *
 * @see javassist.CtMethod
 * @see Block
 * @see Frame
 * @see Analyzer
 * @author Shigeru Chiba
 */
public class ControlFlow {
    private CtClass clazz;
    private MethodInfo methodInfo;
    private Block[] basicBlocks;
    private Frame[] frames;

    /**
     * Constructs a control-flow analyzer for the given method.
     */
    public ControlFlow(CtMethod method) throws BadBytecode {
        this(method.getDeclaringClass(), method.getMethodInfo2());
    }

    /**
     * Constructs a control-flow analyzer.
     */
    public ControlFlow(CtClass ctclazz, MethodInfo minfo) throws BadBytecode {
        clazz = ctclazz;
        methodInfo = minfo;
        frames = null;
        basicBlocks = (Block[])new BasicBlock.Maker() {
            protected BasicBlock makeBlock(int pos) {
                return new Block(pos, methodInfo);
            }
            protected BasicBlock[] makeArray(int size) {
                return new Block[size];
            }
        }.make(minfo);
        int size = basicBlocks.length;
        int[] counters = new int[size];
        for (int i = 0; i < size; i++) {
            Block b = basicBlocks[i];
            b.index = i;
            b.entrances = new Block[b.incomings()];
            counters[i] = 0;
        }

        for (int i = 0; i < size; i++) {
            Block b = basicBlocks[i];
            for (int k = 0; k < b.exits(); k++) {
                Block e = b.exit(k);
                e.entrances[counters[e.index]++] = b;
            }
        }
    }

    /**
     * Returns all the basic blocks in the method body.
     * The first element is the root block of the dominator tree.
     */
    public Block[] basicBlocks() {
        dominatorTree();
        return basicBlocks;
    }

    /**
     * Returns the types of the local variables and stack frame entries
     * available at the given position.  If the byte at the position is
     * not the first byte of an instruction, then this method returns
     * null.
     *
     * @param pos       the position.
     */
    public Frame frameAt(int pos) throws BadBytecode {
        if (frames == null)
            frames = new Analyzer().analyze(clazz, methodInfo);

        return frames[pos];
    }

    /**
     * Returns a dominator tree.
     *
     * @return the root node or null if the method is abstract.
     */
    public Block dominatorTree() {
        int size = basicBlocks.length;
        if (size == 0)
            return null;

        if (basicBlocks[0].parent == null) {
            // a dominator tree has not been constructed.
            boolean[] visited = new boolean[size];
            int[] distance = new int[size];
            for (int i = 0; i < size; i++)
                visited[i] = false;

            basicBlocks[0].makeDepth1stTree(null, visited, 0, distance);
            for (int i = 0; i < size; i++)
                visited[i] = false;

            while (basicBlocks[0].makeDominatorTree(visited, distance))
                ;

            setChildren(size);
        }

        return basicBlocks[0];
    }

    private void setChildren(int size) {
        int[] nchildren = new int[size];
        for (int i = 0; i < size; i++)
            nchildren[i] = 0;

        for (int i = 0; i < size; i++) {
            Block p = basicBlocks[i].parent;
            if (p != null)
                nchildren[p.index]++;
        }

        for (int i = 0; i < size; i++)
            basicBlocks[i].children = new Block[nchildren[i]];

        for (int i = 0; i < size; i++)
            nchildren[i] = 0;

        for (int i = 0; i < size; i++) {
            Block b = basicBlocks[i];
            Block p = b.parent;
            if (p != null)
                p.children[nchildren[p.index]++] = b;            
        }
    }

    /**
     * Basic block.
     * It is a sequence of contiguous instructions that do not contain
     * jump/branch instructions except the last one.
     * Since Java6 or later does not allow <code>JSR</code>,
     * we deal with <code>JSR</code> as a non-branch instruction.
     */
    public static class Block extends BasicBlock {
        /**
         * A field that can be freely used for storing extra data.
         * A client program of this control-flow analyzer can append
         * an additional attribute to a <code>Block</code> object.
         */
        public Object clientData = null;

        int index;
        MethodInfo method;
        Block[] entrances;
        Block parent;           // for a dominator tree
        Block[] children;       // for a dominator tree

        Block(int pos, MethodInfo minfo) {
            super(pos);
            parent = null;
            method = minfo;
        }

        protected void toString2(StringBuffer sbuf) {
            super.toString2(sbuf);
            sbuf.append(", incomping{");
            for (int i = 0; i < entrances.length; i++)
                sbuf.append(entrances[i].position).append(", ");

            sbuf.append("}, dominator parent{");
            if (parent != null)
                sbuf.append(parent.position);

            sbuf.append("}, children{");
            for (int i = 0; i < children.length; i++)
                sbuf.append(children[i].position).append(", ");
            sbuf.append("}");
        }

        /**
         * Returns the position of this block in the array of
         * basic blocks that the <code>basicBlocks</code> method
         * returns.
         *
         * @see #basicBlocks()
         */
        public int index() { return index; }

        /**
         * Returns the position of the first instruction
         * in this block.
         */
        public int position() { return position; }

        /**
         * Returns the length of this block.
         * @return
         */
        public int length() { return length; }

        /**
         * Returns the number of the control paths entering this block.
         */
        int incomings() { return incoming; }

        /**
         * Returns the blocks that the control may jump into this block from.
         */
        public Block incoming(int n) {
            return entrances[n];
        }

        /**
         * Return the number of the blocks that may be executed
         * after this block.
         */
        public int exits() { return exit == null ? 0 : exit.length; }

        /**
         * Returns the n-th block that may be executed after this
         * block.
         *
         * @param n     an index in the array of exit blocks.
         */
        public Block exit(int n) { return (Block)exit[n]; }

        /**
         * Returns the number of the children of this block
         * in the dominator tree.
         */
        public int children() { return children.length; }

        /**
         * Returns the n-th child of this block in the
         * dominator tree.
         *  
         * @param n     an index in the array of children.
         */
        public Block child(int n) { return children[n]; }

        /**
         * Returns catch clauses that will catch an exception thrown
         * in this block. 
         */
        public Catcher[] catchers() {
            ArrayList catchers = new ArrayList();
            BasicBlock.Catch c = toCatch;
            while (c != null) {
                catchers.add(new Catcher(c));
                c = c.next;
            }

            return (Catcher[])catchers.toArray(new Catcher[catchers.size()]);
        }

        /*
         * After executing this method, distance[] represents the post order of the tree nodes.
         * It also represents distances from the root; a bigger number represents a shorter
         * distance.  parent is set to its parent in the depth first spanning tree.
         */
        int makeDepth1stTree(Block caller, boolean[] visited, int counter, int[] distance) {
            if (visited[index])
                return counter;

            visited[index] = true;
            parent = caller;
            if (exit == null) {
                distance[index] = counter++;
                return counter;
            }
            else
                for (int i = 0; i < exit.length; i++) {
                    Block b = (Block)exit[i];
                    counter = b.makeDepth1stTree(this, visited, counter, distance);
                }

            distance[index] = counter++;
            return counter;
        }

        boolean makeDominatorTree(boolean[] visited, int[] distance) {
            if (visited[index])
                return false;

            visited[index] = true;
            boolean changed = false;
            if (exit != null)
                for (int i = 0; i < exit.length; i++) {
                    Block b = (Block)exit[i];
                    if (b.makeDominatorTree(visited, distance))
                        changed = true;
                }

            for (int i = 0; i < entrances.length; i++) {
                if (parent != null) {
                    Block a = getAncestor(parent, entrances[i], distance);
                    if (a != parent) {
                        parent = a;
                        changed = true;
                    }
                }
            }

            return changed;
        }

        private Block getAncestor(Block b1, Block b2, int[] distance) {
            while (b1 != b2)
                if (distance[b1.index] < distance[b2.index])
                    b1 = b1.parent;
                else
                    b2 = b2.parent;

            return b1;
        }
    }

    /**
     * Represents a catch clause.
     */
    public static class Catcher {
        private Block node;
        private int typeIndex;

        Catcher(BasicBlock.Catch c) {
            node = (Block)c.body;
            typeIndex = c.typeIndex;
        }

        /**
         * Returns the first block of the catch clause. 
         */
        public Block block() { return node; }

        /**
         * Returns the name of the exception type that
         * this catch clause catches.
         */
        public String type() {
            if (typeIndex == 0)
                return "java.lang.Throwable";
            else
                return node.method.getConstPool().getClassInfo(typeIndex);
        }
    }
}
