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
import javassist.compiler.ast.ASTree;
import javassist.compiler.ast.ASTList;

/**
 * Object creation (<tt>new</tt> expression).
 */
public class NewExpr extends Expr {
    String newTypeName;
    int newPos;

    /**
     * Undocumented constructor.  Do not use; internal-use only.
     */
    NewExpr(int pos, CodeIterator i, CtClass declaring, MethodInfo m,
            String type, int np)
    {
        super(pos, i, declaring, m);
        newTypeName = type;
        newPos = np;
    }

    private int getNameAndType(ConstPool cp) {
        String cname;
        int pos = currentPos;
        int c = iterator.byteAt(pos);
        int index = iterator.u16bitAt(pos + 1);

        if (c == INVOKEINTERFACE)
            return cp.getInterfaceMethodrefNameAndType(index);
        else
            return cp.getMethodrefNameAndType(index);
    }

    /**
     * Returns the method or constructor containing the <tt>new</tt>
     * expression represented by this object.
     */
    public CtBehavior where() { return super.where(); }

    /**
     * Returns the line number of the source line containing the
     * <tt>new</tt> expression.
     *
     * @return -1       if this information is not available.
     */
    public int getLineNumber() {
        return super.getLineNumber();
    }

    /**
     * Returns the source file containing the <tt>new</tt> expression.
     *
     * @return null     if this information is not available.
     */
    public String getFileName() {
        return super.getFileName();
    }

    /**
     * Returns the class of the created object.
     */
    private CtClass getCtClass() throws NotFoundException {
        return thisClass.getClassPool().get(newTypeName);
    }

    /**
     * Returns the class name of the created object.
     */
    public String getClassName() {
        return newTypeName;
    }

    /**
     * Returns the constructor called for creating the object.
     */
    public CtConstructor getConstructor() throws NotFoundException {
        ConstPool cp = getConstPool();
        int index = iterator.u16bitAt(currentPos + 1);
        String desc = cp.getMethodrefType(index);
        return getCtClass().getConstructor(desc);
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

    /*
     * Returns the parameter types of the constructor.

    public CtClass[] getParameterTypes() throws NotFoundException {
        ConstPool cp = getConstPool();
        int index = iterator.u16bitAt(currentPos + 1);
        String desc = cp.getMethodrefType(index);
        return Descriptor.getParameterTypes(desc, thisClass.getClassPool());
    }
    */

    private int canReplace() throws CannotCompileException {
        int op = iterator.byteAt(newPos + 3);
        if (op == Opcode.DUP)
            return 4;
        else if (op == Opcode.DUP_X1
                 && iterator.byteAt(newPos + 4) == Opcode.SWAP)
            return 5;
        else
            throw new CannotCompileException(
                        "sorry, cannot edit NEW followed by no DUP");
    }

    /**
     * Replaces the <tt>new</tt> expression with the bytecode derived from
     * the given source text.
     *
     * <p>$0 is available but the value is null.
     *
     * @param statement         a Java statement.
     */
    public void replace(String statement) throws CannotCompileException {
        final int bytecodeSize = 3;
        int pos = newPos;

        int newIndex = iterator.u16bitAt(pos + 1);

        /* delete the preceding NEW and DUP (or DUP_X1, SWAP) instructions.
         */
        int end = pos + canReplace();
        for (int i = pos; i < end; ++i)
            iterator.writeByte(NOP, i);

        ConstPool constPool = getConstPool();
        pos = currentPos;
        int methodIndex = iterator.u16bitAt(pos + 1);   // constructor

        String signature = constPool.getMethodrefType(methodIndex);

        Javac jc = new Javac(thisClass);
        ClassPool cp = thisClass.getClassPool();
        CodeAttribute ca = iterator.get();
        try {
            CtClass[] params = Descriptor.getParameterTypes(signature, cp);
            CtClass newType = cp.get(newTypeName);
            int paramVar = ca.getMaxLocals();
            jc.recordParams(newTypeName, params,
                            true, paramVar, withinStatic());
            int retVar = jc.recordReturnType(newType, true);
            jc.recordProceed(new ProceedForNew(newType, newIndex,
                                               methodIndex));

            /* Is $_ included in the source code?
             */
            checkResultValue(newType, statement);

            Bytecode bytecode = jc.getBytecode();
            storeStack(params, true, paramVar, bytecode);
            jc.compileStmnt(statement);
            bytecode.addAload(retVar);

            replace0(pos, bytecode, bytecodeSize);
        }
        catch (CompileError e) { throw new CannotCompileException(e); }
        catch (NotFoundException e) { throw new CannotCompileException(e); }
        catch (BadBytecode e) {
            throw new CannotCompileException("broken method");
        }
    }

    static class ProceedForNew implements ProceedHandler {
        CtClass newType;
        int newIndex, methodIndex;

        ProceedForNew(CtClass nt, int ni, int mi) {
            newType = nt;
            newIndex = ni;
            methodIndex = mi;
        }

        public void doit(JvstCodeGen gen, Bytecode bytecode, ASTList args)
            throws CompileError
        {
            bytecode.addOpcode(NEW);
            bytecode.addIndex(newIndex);
            bytecode.addOpcode(DUP);
            gen.atMethodCall2(newType, MethodInfo.nameInit,
                              args, false, true);
            gen.setType(newType);
        }
    }
}
