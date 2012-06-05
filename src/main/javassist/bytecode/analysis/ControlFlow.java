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
 * @since 3.16
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
     */
    public Block[] basicBlocks() {
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
     * Constructs a dominator tree.  This method returns an array of
     * the tree nodes.  The first element of the array is the root
     * of the tree.
     * 
     * <p> The order of the elements is the same as that
     * of the elements in the <code>Block</code> array returned
     * by the <code>basicBlocks</code>
     * method.  If a <code>Block</code> object is at the i-th position
     * in the <code>Block</code> array, then  
     * the <code>Node</code> object referring to that
     * <code>Block</code> object is at the i-th position in the
     * array returned by this method.
     * For every array element <code>node</code>, its index in the
     * array is equivalent to <code>node.block().index()</code>. 
     *
     * @return an array of the tree nodes, or null if the method is abstract.
     * @see Node#block()
     * @see Block#index()
     */
    public Node[] dominatorTree() {
        int size = basicBlocks.length;
        if (size == 0)
            return null;

        Node[] nodes = new Node[size];
        boolean[] visited = new boolean[size];
        int[] distance = new int[size];
        for (int i = 0; i < size; i++) {
            nodes[i] = new Node(basicBlocks[i]);
            visited[i] = false;
        }

        Access access = new Access(nodes) {
            BasicBlock[] exits(Node n) { return n.block.getExit(); }
            BasicBlock[] entrances(Node n) { return n.block.entrances; }
        };
        nodes[0].makeDepth1stTree(null, visited, 0, distance, access);
        do {
            for (int i = 0; i < size; i++)
                visited[i] = false;
        } while (nodes[0].makeDominatorTree(visited, distance, access));
        Node.setChildren(nodes);
        return nodes;
    }

    /**
     * Constructs a post dominator tree.  This method returns an array of
     * the tree nodes.  Note that the tree has multiple roots.
     * The parent of the root nodes is null.
     * 
     * <p> The order of the elements is the same as that
     * of the elements in the <code>Block</code> array returned
     * by the <code>basicBlocks</code>
     * method.  If a <code>Block</code> object is at the i-th position
     * in the <code>Block</code> array, then  
     * the <code>Node</code> object referring to that
     * <code>Block</code> object is at the i-th position in the
     * array returned by this method.
     * For every array element <code>node</code>, its index in the
     * array is equivalent to <code>node.block().index()</code>.
     *
     * @return an array of the tree nodes, or null if the method is abstract.
     * @see Node#block()
     * @see Block#index()
     */
    public Node[] postDominatorTree() {
        int size = basicBlocks.length;
        if (size == 0)
            return null;

        Node[] nodes = new Node[size];
        boolean[] visited = new boolean[size];
        int[] distance = new int[size];
        for (int i = 0; i < size; i++) {
            nodes[i] = new Node(basicBlocks[i]);
            visited[i] = false;
        }

        Access access = new Access(nodes) {
            BasicBlock[] exits(Node n) { return n.block.entrances; }
            BasicBlock[] entrances(Node n) { return n.block.getExit(); }
        };

        int counter = 0;
        for (int i = 0; i < size; i++)
            if (nodes[i].block.exits() == 0)
                counter = nodes[i].makeDepth1stTree(null, visited, counter, distance, access);

        boolean changed;
        do {
            for (int i = 0; i < size; i++)
                visited[i] = false;

            changed = false;
            for (int i = 0; i < size; i++)
                if (nodes[i].block.exits() == 0)
                    if (nodes[i].makeDominatorTree(visited, distance, access))
                        changed = true;
        } while (changed);

        Node.setChildren(nodes);
        return nodes;
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
         * The Javassist library never accesses this field.
         */
        public Object clientData = null;

        int index;
        MethodInfo method;
        Block[] entrances;

        Block(int pos, MethodInfo minfo) {
            super(pos);
            method = minfo;
        }

        protected void toString2(StringBuffer sbuf) {
            super.toString2(sbuf);
            sbuf.append(", incoming{");
            for (int i = 0; i < entrances.length; i++)
                sbuf.append(entrances[i].position).append(", ");

            sbuf.append("}");
        }

        BasicBlock[] getExit() { return exit; }

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
         */
        public int length() { return length; }

        /**
         * Returns the number of the control paths entering this block.
         */
        public int incomings() { return incoming; }

        /**
         * Returns the block that the control may jump into this block from.
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
    }

    static abstract class Access {
        Node[] all;
        Access(Node[] nodes) { all = nodes; }
        Node node(BasicBlock b) { return all[((Block)b).index]; } 
        abstract BasicBlock[] exits(Node n);
        abstract BasicBlock[] entrances(Node n);
    }

    /**
     * A node of (post) dominator trees. 
     */
    public static class Node {
        private Block block;
        private Node parent;
        private Node[] children;

        Node(Block b) {
            block = b;
            parent = null;
        }

        /**
         * Returns a <code>String</code> representation.
         */
        public String toString() {
            StringBuffer sbuf = new StringBuffer();
            sbuf.append("Node[pos=").append(block().position());
            sbuf.append(", parent=");
            sbuf.append(parent == null ? "*" : Integer.toString(parent.block().position()));
            sbuf.append(", children{");
            for (int i = 0; i < children.length; i++)
                sbuf.append(children[i].block().position()).append(", ");

            sbuf.append("}]");
            return sbuf.toString();
        }

        /**
         * Returns the basic block indicated by this node.
         */
        public Block block() { return block; }

        /**
         * Returns the parent of this node.
         */
        public Node parent() { return parent; }

        /**
         * Returns the number of the children of this node.
         */
        public int children() { return children.length; }

        /**
         * Returns the n-th child of this node.
         *  
         * @param n     an index in the array of children.
         */
        public Node child(int n) { return children[n]; }

        /*
         * After executing this method, distance[] represents the post order of the tree nodes.
         * It also represents distances from the root; a bigger number represents a shorter
         * distance.  parent is set to its parent in the depth first spanning tree.
         */
        int makeDepth1stTree(Node caller, boolean[] visited, int counter, int[] distance, Access access) {
            int index = block.index;
            if (visited[index])
                return counter;

            visited[index] = true;
            parent = caller;
            BasicBlock[] exits = access.exits(this);
            if (exits != null)
                for (int i = 0; i < exits.length; i++) {
                    Node n = access.node(exits[i]);
                    counter = n.makeDepth1stTree(this, visited, counter, distance, access);
                }

            distance[index] = counter++;
            return counter;
        }

        boolean makeDominatorTree(boolean[] visited, int[] distance, Access access) {
            int index = block.index;
            if (visited[index])
                return false;

            visited[index] = true;
            boolean changed = false;
            BasicBlock[] exits = access.exits(this);
            if (exits != null)
                for (int i = 0; i < exits.length; i++) {
                    Node n = access.node(exits[i]);
                    if (n.makeDominatorTree(visited, distance, access))
                        changed = true;
                }

            BasicBlock[] entrances = access.entrances(this);
            if (entrances != null)
                for (int i = 0; i < entrances.length; i++) {
                    if (parent != null) {
                        Node n = getAncestor(parent, access.node(entrances[i]), distance);
                        if (n != parent) {
                            parent = n;
                            changed = true;
                        }
                    }
                }

            return changed;
        }

        private static Node getAncestor(Node n1, Node n2, int[] distance) {
            while (n1 != n2) {
                if (distance[n1.block.index] < distance[n2.block.index])
                    n1 = n1.parent;
                else
                    n2 = n2.parent;

                if (n1 == null || n2 == null)
                    return null;
            }

            return n1;
        }

        private static void setChildren(Node[] all) {
            int size = all.length;
            int[] nchildren = new int[size];
            for (int i = 0; i < size; i++)
                nchildren[i] = 0;

            for (int i = 0; i < size; i++) {
                Node p = all[i].parent;
                if (p != null)
                    nchildren[p.block.index]++;
            }

            for (int i = 0; i < size; i++)
                all[i].children = new Node[nchildren[i]];

            for (int i = 0; i < size; i++)
                nchildren[i] = 0;

            for (int i = 0; i < size; i++) {
                Node n = all[i];
                Node p = n.parent;
                if (p != null)
                    p.children[nchildren[p.block.index]++] = n;            
            }
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
