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

package javassist.expr;

import javassist.*;
import javassist.bytecode.*;
import javassist.compiler.*;
import javassist.compiler.ast.ASTList;

/**
 * Array creation.
 *
 * <p>This class does not provide methods for obtaining the initial
 * values of array elements.
 */
public class NewArray extends Expr {
    int opcode;

    protected NewArray(int pos, CodeIterator i, CtClass declaring,
                       MethodInfo m, int op) {
        super(pos, i, declaring, m);
        opcode = op;
    }

    /**
     * Returns the method or constructor containing the array creation
     * represented by this object.
     */
    public CtBehavior where() { return super.where(); }

    /**
     * Returns the line number of the source line containing the
     * array creation.
     *
     * @return -1       if this information is not available.
     */
    public int getLineNumber() {
        return super.getLineNumber();
    }

    /**
     * Returns the source file containing the array creation.
     *
     * @return null     if this information is not available.
     */
    public String getFileName() {
        return super.getFileName();
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
     * Returns the type of array components.  If the created array is
     * a two-dimensional array of <tt>int</tt>,
     * the type returned by this method is
     * not <tt>int[]</tt> but <tt>int</tt>.
     */
    public CtClass getComponentType() throws NotFoundException {
        if (opcode == Opcode.NEWARRAY) {
            int atype = iterator.byteAt(currentPos + 1);
            return getPrimitiveType(atype);
        }
        else if (opcode == Opcode.ANEWARRAY
                 || opcode == Opcode.MULTIANEWARRAY) {
            int index = iterator.u16bitAt(currentPos + 1);
            String desc = getConstPool().getClassInfo(index);
            int dim = Descriptor.arrayDimension(desc);
            desc = Descriptor.toArrayComponent(desc, dim);
            return Descriptor.toCtClass(desc, thisClass.getClassPool());
        }
        else
            throw new RuntimeException("bad opcode: " + opcode);
    }

    CtClass getPrimitiveType(int atype) {
        switch (atype) {
        case Opcode.T_BOOLEAN :
            return CtClass.booleanType;
        case Opcode.T_CHAR :
            return CtClass.charType;
        case Opcode.T_FLOAT :
            return CtClass.floatType;
        case Opcode.T_DOUBLE :
            return CtClass.doubleType;
        case Opcode.T_BYTE :
            return CtClass.byteType;
        case Opcode.T_SHORT :
            return CtClass.shortType;
        case Opcode.T_INT :
            return CtClass.intType;
        case Opcode.T_LONG :
            return CtClass.longType;
        default :
            throw new RuntimeException("bad atype: " + atype);        
        }
    }

    /**
     * Returns the dimension of the created array.
     */
    public int getDimension() {
        if (opcode == Opcode.NEWARRAY)
            return 1;
        else if (opcode == Opcode.ANEWARRAY
                 || opcode == Opcode.MULTIANEWARRAY) {
            int index = iterator.u16bitAt(currentPos + 1);
            String desc = getConstPool().getClassInfo(index);
            return Descriptor.arrayDimension(desc)
                    + (opcode == Opcode.ANEWARRAY ? 1 : 0);
        }
        else
            throw new RuntimeException("bad opcode: " + opcode);
    }

    /**
     * Returns the number of dimensions of arrays to be created.
     * If the opcode is multianewarray, this method returns the second
     * operand.  Otherwise, it returns 1.
     */
    public int getCreatedDimensions() {
        if (opcode == Opcode.MULTIANEWARRAY)
            return iterator.byteAt(currentPos + 3);
        else
            return 1;
    }

    /**
     * Replaces the array creation with the bytecode derived from
     * the given source text.
     *
     * <p>$0 is available even if the called method is static.
     * If the field access is writing, $_ is available but the value
     * of $_ is ignored.
     *
     * @param statement         a Java statement except try-catch.
     */
    public void replace(String statement) throws CannotCompileException {
        try {
            replace2(statement);
        }
        catch (CompileError e) { throw new CannotCompileException(e); }
        catch (NotFoundException e) { throw new CannotCompileException(e); }
        catch (BadBytecode e) {
            throw new CannotCompileException("broken method");
        }
    }

    private void replace2(String statement)
        throws CompileError, NotFoundException, BadBytecode,
               CannotCompileException
    {
        thisClass.getClassFile();   // to call checkModify().
        ConstPool constPool = getConstPool();
        int pos = currentPos;
        CtClass retType;
        int codeLength;
        int index = 0;
        int dim = 1;
        String desc;
        if (opcode == Opcode.NEWARRAY) {
            index = iterator.byteAt(currentPos + 1);    // atype
            CtPrimitiveType cpt = (CtPrimitiveType)getPrimitiveType(index); 
            desc = "[" + cpt.getDescriptor();
            codeLength = 2;
        }
        else if (opcode == Opcode.ANEWARRAY) {
            index = iterator.u16bitAt(pos + 1);
            desc = constPool.getClassInfo(index);
            if (desc.startsWith("["))
                desc = "[" + desc;
            else
                desc = "[L" + desc + ";";

            codeLength = 3;
        }
        else if (opcode == Opcode.MULTIANEWARRAY) {
            index = iterator.u16bitAt(currentPos + 1);
            desc = constPool.getClassInfo(index);
            dim = iterator.byteAt(currentPos + 3);
            codeLength = 4;
        }
        else
            throw new RuntimeException("bad opcode: " + opcode);

        retType = Descriptor.toCtClass(desc, thisClass.getClassPool());

        Javac jc = new Javac(thisClass);
        CodeAttribute ca = iterator.get();

        CtClass[] params = new CtClass[dim];
        for (int i = 0; i < dim; ++i)
            params[i] = CtClass.intType;

        int paramVar = ca.getMaxLocals();
        jc.recordParams(javaLangObject, params,
                        true, paramVar, withinStatic());

        /* Is $_ included in the source code?
         */
        checkResultValue(retType, statement);
        int retVar = jc.recordReturnType(retType, true);
        jc.recordProceed(new ProceedForArray(retType, opcode, index, dim));

        Bytecode bytecode = jc.getBytecode();
        storeStack(params, true, paramVar, bytecode);
        jc.recordLocalVariables(ca, pos);

        bytecode.addOpcode(ACONST_NULL);        // initialize $_
        bytecode.addAstore(retVar);

        jc.compileStmnt(statement);
        bytecode.addAload(retVar);

        replace0(pos, bytecode, codeLength);
    }

    /* <array type> $proceed(<dim> ..)
     */
    static class ProceedForArray implements ProceedHandler {
        CtClass arrayType;
        int opcode;
        int index, dimension;

        ProceedForArray(CtClass type, int op, int i, int dim) {
            arrayType = type;
            opcode = op;
            index = i;
            dimension = dim;
        }

        public void doit(JvstCodeGen gen, Bytecode bytecode, ASTList args)
            throws CompileError
        {
            int num = gen.getMethodArgsLength(args); 
            if (num != dimension)
                throw new CompileError(Javac.proceedName
                        + "() with a wrong number of parameters");

            gen.atMethodArgs(args, new int[num],
                             new int[num], new String[num]);
            bytecode.addOpcode(opcode);
            if (opcode == Opcode.ANEWARRAY)
                bytecode.addIndex(index);
            else if (opcode == Opcode.NEWARRAY)
                bytecode.add(index);
            else /* if (opcode == Opcode.MULTIANEWARRAY) */ {
                bytecode.addIndex(index);
                bytecode.add(dimension);
                bytecode.growStack(1 - dimension);
            }

            gen.setType(arrayType);
        }

        public void setReturnType(JvstTypeChecker c, ASTList args)
            throws CompileError
        {
            c.setType(arrayType);
        }
    }
}
