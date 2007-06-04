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

package javassist.compiler.ast;

import java.io.Serializable;
import javassist.compiler.CompileError;

/**
 * Abstract Syntax Tree.  An ASTree object represents a node of
 * a binary tree.  If the node is a leaf node, both <code>getLeft()</code>
 * and <code>getRight()</code> returns null.
 */
public abstract class ASTree implements Serializable {
    public ASTree getLeft() { return null; }

    public ASTree getRight() { return null; }

    public void setLeft(ASTree _left) {}

    public void setRight(ASTree _right) {}

    /**
     * Is a method for the visitor pattern.  It calls
     * <code>atXXX()</code> on the given visitor, where
     * <code>XXX</code> is the class name of the node object.
     */
    public abstract void accept(Visitor v) throws CompileError;

    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append('<');
        sbuf.append(getTag());
        sbuf.append('>');
        return sbuf.toString();
    }

    /**
     * Returns the type of this node.  This method is used by
     * <code>toString()</code>.
     */
    protected String getTag() {
        String name = getClass().getName();
        return name.substring(name.lastIndexOf('.') + 1);
    }
}
