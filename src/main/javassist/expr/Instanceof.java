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

import javassist.*;
import javassist.bytecode.*;
import javassist.compiler.*;
import javassist.compiler.ast.ASTList;

/**
 * Instanceof operator.
 */
public class Instanceof extends Expr {
    /**
     * Undocumented constructor.  Do not use; internal-use only.
     */
    Instanceof(int pos, CodeIterator i, CtClass declaring, MethodInfo m) {
	super(pos, i, declaring, m);
    }

    /**
     * Returns the method or constructor containing the instanceof
     * expression represented by this object.
     */
    public CtBehavior where() { return super.where(); }

    /**
     * Returns the line number of the source line containing the
     * instanceof expression.
     *
     * @return -1	if this information is not available.
     */
    public int getLineNumber() {
	return super.getLineNumber();
    }

    /**
     * Returns the source file containing the
     * instanceof expression.
     *
     * @return null	if this information is not available.
     */
    public String getFileName() {
	return super.getFileName();
    }

    /**
     * Returns the <code>CtClass</code> object representing
     * the type name on the right hand side
     * of the instanceof operator.
     */
    public CtClass getType() throws NotFoundException {
	ConstPool cp = getConstPool();
	int pos = currentPos;
	int index = iterator.u16bitAt(pos + 1);
	String name = cp.getClassInfo(index);
	return Descriptor.toCtClass(name, thisClass.getClassPool());
    }

    /**
     * Returns the list of exceptions that the expression may throw.
     * This list includes both the exceptions that the try-catch statements
     * including the expression can catch and the exceptions that
     * the throws declaration allows the method to throw.
     */
    public CtClass[] mayThrow() {
	return super.mayThrow();
    }

    /**
     * Replaces the instanceof operator with the bytecode derived from
     * the given source text.
     *
     * <p>$0 is available but the value is <code>null</code>.
     *
     * @param statement		a Java statement.
     */
    public void replace(String statement) throws CannotCompileException {
	ConstPool constPool = getConstPool();
	int pos = currentPos;
	int index = iterator.u16bitAt(pos + 1);

	Javac jc = new Javac(thisClass);
	ClassPool cp = thisClass.getClassPool();
	CodeAttribute ca = iterator.get();

	try {
	    CtClass[] params
		= new CtClass[] { cp.get(javaLangObject) };
	    CtClass retType = CtClass.booleanType;

	    int paramVar = ca.getMaxLocals();
	    jc.recordParams(javaLangObject, params, true, paramVar,
			    withinStatic());
	    int retVar = jc.recordReturnType(retType, true);
	    jc.recordProceed(new ProceedForInstanceof(index));

	    // because $type is not the return type...
	    jc.recordType(getType());

	    /* Is $_ included in the source code?
	     */
	    checkResultValue(retType, statement);

	    Bytecode bytecode = jc.getBytecode();
	    storeStack(params, true, paramVar, bytecode);
	    jc.compileStmnt(statement);
	    bytecode.addLoad(retVar, retType);

	    replace0(pos, bytecode, 3);
	}
	catch (CompileError e) { throw new CannotCompileException(e); }
	catch (NotFoundException e) { throw new CannotCompileException(e); }
	catch (BadBytecode e) {
	    throw new CannotCompileException("broken method");
	}
    }

    /* boolean $proceed(Object obj)
     */
    static class ProceedForInstanceof implements ProceedHandler {
	int index;

	ProceedForInstanceof(int i) {
	    index = i;
	}

	public void doit(JvstCodeGen gen, Bytecode bytecode, ASTList args)
	    throws CompileError
	{
	    if (gen.atMethodArgsLength(args) != 1)
		throw new CompileError(Javac.proceedName
			+ "() cannot take more than one parameter "
			+ "for instanceof");

	    gen.atMethodArgs(args, new int[1], new int[1], new String[1]);
	    bytecode.addOpcode(Opcode.INSTANCEOF);
	    bytecode.addIndex(index);
	    gen.setType(CtClass.booleanType);
	}
    }
}
