/*
 * This file is part of the Javassist toolkit.
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * either http://www.mozilla.org/MPL/.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.  See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is Javassist.
 *
 * The Initial Developer of the Original Code is Shigeru Chiba.  Portions
 * created by Shigeru Chiba are Copyright (C) 1999-2003 Shigeru Chiba.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * The development of this software is supported in part by the PRESTO
 * program (Sakigake Kenkyu 21) of Japan Science and Technology Corporation.
 */

package javassist.compiler.ast;

import javassist.compiler.CompileError;

/**
 * A linked list.
 * The right subtree must be an ASTList object or null.
 */
public class ASTList extends ASTree {
    private ASTree left;
    private ASTList right;

    public ASTList(ASTree _head, ASTList _tail) {
	left = _head;
	right = _tail;
    }

    public ASTList(ASTree _head) {
	left = _head;
	right = null;
    }

    public static ASTList make(ASTree e1, ASTree e2, ASTree e3) {
	return new ASTList(e1, new ASTList(e2, new ASTList(e3)));
    }

    public ASTree getLeft() { return left; }

    public ASTree getRight() { return right; }

    public void setLeft(ASTree _left) { left = _left; }

    public void setRight(ASTree _right) {
	right = (ASTList)_right;
    }

    /**
     * Returns the car part of the list.
     */
    public ASTree head() { return left; }

    public void setHead(ASTree _head) {
	left = _head;
    }

    /**
     * Returns the cdr part of the list.
     */
    public ASTList tail() { return right; }

    public void setTail(ASTList _tail) {
	right = _tail;
    }

    public void accept(Visitor v) throws CompileError { v.atASTList(this); }

    public String toString() {
	StringBuffer sbuf = new StringBuffer();
	sbuf.append("(<");
	sbuf.append(getTag());
	sbuf.append('>');
	ASTList list = this;
	while (list != null) {
	    sbuf.append(' ');
	    ASTree a = list.left;
	    sbuf.append(a == null ? "<null>" : a.toString());
	    list = list.right;
	}

	sbuf.append(')');
	return sbuf.toString();
    }

    /**
     * Returns the number of the elements in this list.
     */
    public int length() {
	return length(this);
    }

    public static int length(ASTList list) {
	if (list == null)
	    return 0;

	int n = 0;
	while (list != null) {
	    list = list.right;
	    ++n;
	}

	return n;
    }

    /**
     * Returns a sub list of the list.  The sub list begins with the
     * n-th element of the list.
     *
     * @param nth	zero or more than zero.
     */
    public ASTList sublist(int nth) {
	ASTList list = this;
	while (nth-- > 0)
	    list = list.right;

	return list;
    }

    /**
     * Substitutes <code>newObj</code> for <code>oldObj</code> in the
     * list.
     */
    public boolean subst(ASTree newObj, ASTree oldObj) {
	for (ASTList list = this; list != null; list = list.right)
	    if (list.left == oldObj) {
		list.left = newObj;
		return true;
	    }

	return false;
    }

    /**
     * Appends an object to a list.
     */
    public static ASTList append(ASTList a, ASTree b) {
	return concat(a, new ASTList(b));
    }

    /**
     * Concatenates two lists.
     */
    public static ASTList concat(ASTList a, ASTList b) {
	if (a == null)
	    return b;
	else {
	    ASTList list = a;
	    while (list.right != null)
		list = list.right;

	    list.right = b;
	    return a;
	}
    }
}
