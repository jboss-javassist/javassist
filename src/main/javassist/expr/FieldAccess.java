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

package javassist.expr;

import javassist.*;
import javassist.bytecode.*;
import javassist.compiler.*;
import javassist.compiler.ast.ASTList;

/**
 * Expression for accessing a field.
 */
public class FieldAccess extends Expr {
    int opcode;

    protected FieldAccess(int pos, CodeIterator i, CtClass declaring,
                          MethodInfo m, int op) {
        super(pos, i, declaring, m);
        opcode = op;
    }

    /**
     * Returns the method or constructor containing the field-access
     * expression represented by this object.
     */
    public CtBehavior where() { return super.where(); }

    /**
     * Returns the line number of the source line containing the
     * field access.
     *
     * @return -1       if this information is not available.
     */
    public int getLineNumber() {
        return super.getLineNumber();
    }

    /**
     * Returns the source file containing the field access.
     *
     * @return null     if this information is not available.
     */
    public String getFileName() {
        return super.getFileName();
    }

    /**
     * Returns true if the field is static.
     */
    public boolean isStatic() {
        return isStatic(opcode);
    }

    static boolean isStatic(int c) {
        return c == Opcode.GETSTATIC || c == Opcode.PUTSTATIC;
    }

    /**
     * Returns true if the field is read.
     */
    public boolean isReader() {
        return opcode == Opcode.GETFIELD || opcode ==  Opcode.GETSTATIC;
    }

    /**
     * Returns true if the field is written in.
     */
    public boolean isWriter() {
        return opcode == Opcode.PUTFIELD || opcode ==  Opcode.PUTSTATIC;
    }

    /**
     * Returns the class in which the field is declared.
     */
    private CtClass getCtClass() throws NotFoundException {
        return thisClass.getClassPool().get(getClassName());
    }

    /**
     * Returns the name of the class in which the field is declared.
     */
    public String getClassName() {
        int index = iterator.u16bitAt(currentPos + 1);
        return getConstPool().getFieldrefClassName(index);
    }

    /**
     * Returns the name of the field.
     */
    public String getFieldName() {
        int index = iterator.u16bitAt(currentPos + 1);
        return getConstPool().getFieldrefName(index);
    }

    /**
     * Returns the field accessed by this expression.
     */
    public CtField getField() throws NotFoundException {
        CtClass cc = getCtClass();
        return cc.getField(getFieldName());
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
     * Returns the signature of the field type.
     * The signature is represented by a character string
     * called field descriptor, which is defined in the JVM specification.
     *
     * @see javassist.bytecode.Descriptor#toCtClass(String, ClassPool)
     * @since 3.1
     */
    public String getSignature() {
        int index = iterator.u16bitAt(currentPos + 1);
        return getConstPool().getFieldrefType(index);
    }

    /**
     * Replaces the method call with the bytecode derived from
     * the given source text.
     *
     * <p>$0 is available even if the called method is static.
     * If the field access is writing, $_ is available but the value
     * of $_ is ignored.
     *
     * @param statement         a Java statement except try-catch.
     */
    public void replace(String statement) throws CannotCompileException {
        thisClass.getClassFile();   // to call checkModify().
        ConstPool constPool = getConstPool();
        int pos = currentPos;
        int index = iterator.u16bitAt(pos + 1);

        Javac jc = new Javac(thisClass);
        CodeAttribute ca = iterator.get();
        try {
            CtClass[] params;
            CtClass retType;
            CtClass fieldType
                = Descriptor.toCtClass(constPool.getFieldrefType(index),
                                       thisClass.getClassPool());
            boolean read = isReader();
            if (read) {
                params = new CtClass[0];
                retType = fieldType;
            }
            else {
                params = new CtClass[1];
                params[0] = fieldType;
                retType = CtClass.voidType;
            }

            int paramVar = ca.getMaxLocals();
            jc.recordParams(constPool.getFieldrefClassName(index), params,
                            true, paramVar, withinStatic());

            /* Is $_ included in the source code?
             */
            boolean included = checkResultValue(retType, statement);
            if (read)
                included = true;

            int retVar = jc.recordReturnType(retType, included);
            if (read)
                jc.recordProceed(new ProceedForRead(retType, opcode,
                                                    index, paramVar));
            else {
                // because $type is not the return type...
                jc.recordType(fieldType);
                jc.recordProceed(new ProceedForWrite(params[0], opcode,
                                                     index, paramVar));
            }

            Bytecode bytecode = jc.getBytecode();
            storeStack(params, isStatic(), paramVar, bytecode);
            jc.recordLocalVariables(ca, pos);

            if (included)
                if (retType == CtClass.voidType) {
                    bytecode.addOpcode(ACONST_NULL);
                    bytecode.addAstore(retVar);
                }
                else {
                    bytecode.addConstZero(retType);
                    bytecode.addStore(retVar, retType);     // initialize $_
                }

            jc.compileStmnt(statement);
            if (read)
                bytecode.addLoad(retVar, retType);

            replace0(pos, bytecode, 3);
        }
        catch (CompileError e) { throw new CannotCompileException(e); }
        catch (NotFoundException e) { throw new CannotCompileException(e); }
        catch (BadBytecode e) {
            throw new CannotCompileException("broken method");
        }
    }

    /* <field type> $proceed()
     */
    static class ProceedForRead implements ProceedHandler {
        CtClass fieldType;
        int opcode;
        int targetVar, index;

        ProceedForRead(CtClass type, int op, int i, int var) {
            fieldType = type;
            targetVar = var;
            opcode = op;
            index = i;
        }

        public void doit(JvstCodeGen gen, Bytecode bytecode, ASTList args)
            throws CompileError
        {
            if (args != null && !gen.isParamListName(args))
                throw new CompileError(Javac.proceedName
                        + "() cannot take a parameter for field reading");

            int stack;
            if (isStatic(opcode))
                stack = 0;
            else {
                stack = -1;
                bytecode.addAload(targetVar);
            }

            if (fieldType instanceof CtPrimitiveType)
                stack += ((CtPrimitiveType)fieldType).getDataSize();
            else
                ++stack;

            bytecode.add(opcode);
            bytecode.addIndex(index);
            bytecode.growStack(stack);
            gen.setType(fieldType);
        }

        public void setReturnType(JvstTypeChecker c, ASTList args)
            throws CompileError
        {
            c.setType(fieldType);
        }
    }

    /* void $proceed(<field type>)
     *          the return type is not the field type but void.
     */
    static class ProceedForWrite implements ProceedHandler {
        CtClass fieldType;
        int opcode;
        int targetVar, index;

        ProceedForWrite(CtClass type, int op, int i, int var) {
            fieldType = type;
            targetVar = var;
            opcode = op;
            index = i;
        }

        public void doit(JvstCodeGen gen, Bytecode bytecode, ASTList args)
            throws CompileError
        {
            if (gen.getMethodArgsLength(args) != 1)
                throw new CompileError(Javac.proceedName
                        + "() cannot take more than one parameter "
                        + "for field writing");

            int stack;
            if (isStatic(opcode))
                stack = 0;
            else {
                stack = -1;
                bytecode.addAload(targetVar);
            }

            gen.atMethodArgs(args, new int[1], new int[1], new String[1]);
            gen.doNumCast(fieldType);
            if (fieldType instanceof CtPrimitiveType)
                stack -= ((CtPrimitiveType)fieldType).getDataSize();
            else
                --stack;

            bytecode.add(opcode);
            bytecode.addIndex(index);
            bytecode.growStack(stack);
            gen.setType(CtClass.voidType);
            gen.addNullIfVoid();
        }

        public void setReturnType(JvstTypeChecker c, ASTList args)
            throws CompileError
        {
            c.atMethodArgs(args, new int[1], new int[1], new String[1]);
            c.setType(CtClass.voidType);
            c.addNullIfVoid();
        }
    }
}
