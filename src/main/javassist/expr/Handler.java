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
package javassist.expr;

import javassist.*;
import javassist.bytecode.*;
import javassist.compiler.*;
import javassist.compiler.ast.ASTList;

/**
 * Catch clause.
 */
public class Handler extends Expr {
    private static String EXCEPTION_NAME = "$1";
    private ExceptionTable etable;
    private int index;

    /**
     * Undocumented constructor.  Do not use; internal-use only.
     */
    Handler(ExceptionTable et, int nth,
            CodeIterator it, CtClass declaring, MethodInfo m) {
        super(et.handlerPc(nth), it, declaring, m);
        etable = et;
        index = nth;
    }

    /**
     * Returns the method or constructor containing the catch clause.
     */
    public CtBehavior where() { return super.where(); }

    /**
     * Returns the source line number of the catch clause.
     *
     * @return -1       if this information is not available.
     */
    public int getLineNumber() {
        return super.getLineNumber();
    }

    /**
     * Returns the source file containing the catch clause.
     *
     * @return null     if this information is not available.
     */
    public String getFileName() {
        return super.getFileName();
    }

    /**
     * Returns the list of exceptions that the catch clause may throw.
     */
    public CtClass[] mayThrow() {
        return super.mayThrow();
    }

    /**
     * Returns the type handled by the catch clause.
     */
    public CtClass getType() throws NotFoundException {
        ConstPool cp = getConstPool();
        String name = cp.getClassInfo(etable.catchType(index));
        return Descriptor.toCtClass(name, thisClass.getClassPool());
    }

    /**
     * This method has not been implemented yet.
     *
     * @param statement         a Java statement.
     */
    public void replace(String statement) throws CannotCompileException {
        throw new RuntimeException("not implemented yet");
    }

    /**
     * Inserts bytecode at the beginning of the catch clause.
     * The caught exception is stored in <code>$1</code>.
     *
     * @param src       the source code representing the inserted bytecode.
     *                  It must be a single statement or block.
     */
    public void insertBefore(String src) throws CannotCompileException {
        edited = true;

        ConstPool cp = getConstPool();
        CodeAttribute ca = iterator.get();
        Javac jv = new Javac(thisClass);
        Bytecode b = jv.getBytecode();
        b.setStackDepth(1);
        b.setMaxLocals(ca.getMaxLocals());

        try {
            CtClass type = getType();
            int var = jv.recordVariable(type, EXCEPTION_NAME);
            jv.recordReturnType(type, false);
            b.addAstore(var);
            jv.compileStmnt(src);
            b.addAload(var);

            int oldHandler = etable.handlerPc(index);
            b.addOpcode(Opcode.GOTO);
            b.addIndex(oldHandler - iterator.getCodeLength()
                       - b.currentPc() + 1);

            maxStack = b.getMaxStack();
            maxLocals = b.getMaxLocals();

            int pos = iterator.append(b.get());
            iterator.append(b.getExceptionTable(), pos);
            etable.setHandlerPc(index, pos);
        }
        catch (NotFoundException e) {
            throw new CannotCompileException(e);
        }
        catch (CompileError e) {
            throw new CannotCompileException(e);
        }
    }
}
