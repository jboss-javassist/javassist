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

package javassist.expr;

import javassist.bytecode.*;
import javassist.CtClass;
import javassist.CannotCompileException;

/**
 * A translator of method bodies.
 *
 * <p>The users can define a subclass of this class to customize how to
 * modify a method body.  The overall architecture is similar to the
 * strategy pattern.
 *
 * <p>If <code>instrument()</code> is called in
 * <code>CtMethod</code>, the method body is scanned from the beginning
 * to the end.
 * Whenever an expression, such as a method call and a <tt>new</tt>
 * expression (object creation),
 * is found, <code>edit()</code> is called in <code>ExprEdit</code>.
 * <code>edit()</code> can inspect and modify the given expression.
 * The modification is reflected on the original method body.  If
 * <code>edit()</code> does nothing, the original method body is not
 * changed.
 *
 * <p>The following code is an example:
 *
 * <ul><pre>
 * CtMethod cm = ...;
 * cm.instrument(new ExprEditor() {
 *     public void edit(MethodCall m) throws CannotCompileException {
 *         if (m.getClassName().equals("Point")) {
 *             System.out.println(m.getMethodName() + " line: "
 *                                + m.getLineNumber());
 *     }
 * });
 * </pre></ul>
 *
 * <p>This code inspects all method calls appearing in the method represented
 * by <code>cm</code> and it prints the names and the line numbers of the
 * methods declared in class <code>Point</code>.  This code does not modify
 * the body of the method represented by <code>cm</code>.  If the method
 * body must be modified, call <code>replace()</code>
 * in <code>MethodCall</code>.
 *
 * @see javassist.CtClass#instrument(ExprEditor)
 * @see javassist.CtMethod#instrument(ExprEditor)
 * @see javassist.CtConstructor#instrument(ExprEditor)
 * @see MethodCall
 * @see NewExpr
 * @see FieldAccess
 *
 * @see javassist.CodeConverter
 */
public class ExprEditor {
    /**
     * Default constructor.  It does nothing.
     */
    public ExprEditor() {}

    static class NewOp {
	NewOp next;
	int pos;
	String type;

	NewOp(NewOp n, int p, String t) {
	    next = n;
	    pos = p;
	    type = t;
	}
    }

    /**
     * Undocumented method.  Do not use; internal-use only.
     */
    public boolean doit(CtClass clazz, MethodInfo minfo)
	throws CannotCompileException
    {
	CodeAttribute codeAttr = minfo.getCodeAttribute();
	if (codeAttr == null)
	    return false;

	CodeIterator iterator = codeAttr.iterator();
	boolean edited = false;
	int maxLocals = codeAttr.getMaxLocals();
	int maxStack = 0;

	NewOp newList = null;
	ConstPool cp = minfo.getConstPool();

	while (iterator.hasNext())
	    try {
		Expr expr = null;
		int pos = iterator.next();
		int c = iterator.byteAt(pos);

		if (c == Opcode.INVOKESTATIC || c == Opcode.INVOKEINTERFACE
		    || c == Opcode.INVOKEVIRTUAL) {
		    expr = new MethodCall(pos, iterator, clazz, minfo);
		    edit((MethodCall)expr);
		}
		else if (c == Opcode.GETFIELD || c == Opcode.GETSTATIC
			|| c == Opcode.PUTFIELD || c == Opcode.PUTSTATIC) {
		    expr = new FieldAccess(pos, iterator, clazz, minfo, c);
		    edit((FieldAccess)expr);
		}
		else if (c == Opcode.NEW) {
		    int index = iterator.u16bitAt(pos + 1);
		    newList = new NewOp(newList, pos,
					cp.getClassInfo(index));
		}
		else if (c == Opcode.INVOKESPECIAL) {
		    if (newList != null && cp.isConstructor(newList.type,
				iterator.u16bitAt(pos + 1)) > 0) {
			expr = new NewExpr(pos, iterator, clazz, minfo,
					   newList.type, newList.pos);
			edit((NewExpr)expr);
			newList = newList.next;
		    }
		    else {
			expr = new MethodCall(pos, iterator, clazz, minfo);
			MethodCall mcall = (MethodCall)expr;
			if (!mcall.getMethodName().equals(
						MethodInfo.nameInit))
			    edit(mcall);
		    }
		}
		else if (c == Opcode.INSTANCEOF) {
		    expr = new Instanceof(pos, iterator, clazz, minfo);
		    edit((Instanceof)expr);
		}
		else if (c == Opcode.CHECKCAST) {
		    expr = new Cast(pos, iterator, clazz, minfo);
		    edit((Cast)expr);
		}

		if (expr != null && expr.edited()) {
		    edited = true;
		    if (maxLocals < expr.locals())
			maxLocals = expr.locals();

		    if (maxStack < expr.stack())
			maxStack = expr.stack();
		}
	    }
	    catch (BadBytecode e) {
		throw new CannotCompileException(e);
	    }

	codeAttr.setMaxLocals(maxLocals);
	codeAttr.setMaxStack(codeAttr.getMaxStack() + maxStack);
	return edited;
    }

    /**
     * Edits a <tt>new</tt> expression (overridable).
     * The default implementation performs nothing.
     *
     * @param e		the <tt>new</tt> expression creating an object.
     */
    public void edit(NewExpr e) throws CannotCompileException {}

    /**
     * Edits a method call (overridable).
     * The default implementation performs nothing.
     */
    public void edit(MethodCall m) throws CannotCompileException {}

    /**
     * Edits a field-access expression (overridable).
     * Field access means both read and write.
     * The default implementation performs nothing.
     */
    public void edit(FieldAccess f) throws CannotCompileException {}

    /**
     * Edits an instanceof expression (overridable).
     * The default implementation performs nothing.
     */
    public void edit(Instanceof i) throws CannotCompileException {}

    /**
     * Edits an expression for explicit type casting (overridable).
     * The default implementation performs nothing.
     */
    public void edit(Cast c) throws CannotCompileException {}
}
