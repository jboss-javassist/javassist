/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2003 Shigeru Chiba. All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
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
