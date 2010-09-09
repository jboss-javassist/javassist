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

/**
 * Method invocation (caller-side expression).
 */
public class MethodCall extends Expr {
    /**
     * Undocumented constructor.  Do not use; internal-use only.
     */
    protected MethodCall(int pos, CodeIterator i, CtClass declaring,
                         MethodInfo m) {
        super(pos, i, declaring, m);
    }

    private int getNameAndType(ConstPool cp) {
        int pos = currentPos;
        int c = iterator.byteAt(pos);
        int index = iterator.u16bitAt(pos + 1);

        if (c == INVOKEINTERFACE)
            return cp.getInterfaceMethodrefNameAndType(index);
        else
            return cp.getMethodrefNameAndType(index);
    }

    /**
     * Returns the method or constructor containing the method-call
     * expression represented by this object.
     */
    public CtBehavior where() { return super.where(); }

    /**
     * Returns the line number of the source line containing the
     * method call.
     *
     * @return -1       if this information is not available.
     */
    public int getLineNumber() {
        return super.getLineNumber();
    }

    /**
     * Returns the source file containing the method call.
     *
     * @return null     if this information is not available.
     */
    public String getFileName() {
        return super.getFileName();
    }

    /**
     * Returns the class of the target object,
     * which the method is called on.
     */
    protected CtClass getCtClass() throws NotFoundException {
        return thisClass.getClassPool().get(getClassName());
    }

    /**
     * Returns the class name of the target object,
     * which the method is called on.
     */
    public String getClassName() {
        String cname;

        ConstPool cp = getConstPool();
        int pos = currentPos;
        int c = iterator.byteAt(pos);
        int index = iterator.u16bitAt(pos + 1);

        if (c == INVOKEINTERFACE)
            cname = cp.getInterfaceMethodrefClassName(index);
        else
            cname = cp.getMethodrefClassName(index);

         if (cname.charAt(0) == '[')
             cname = Descriptor.toClassName(cname);

         return cname;
    }

    /**
     * Returns the name of the called method. 
     */
    public String getMethodName() {
        ConstPool cp = getConstPool();
        int nt = getNameAndType(cp);
        return cp.getUtf8Info(cp.getNameAndTypeName(nt));
    }

    /**
     * Returns the called method.
     */
    public CtMethod getMethod() throws NotFoundException {
        return getCtClass().getMethod(getMethodName(), getSignature());
    }

    /**
     * Returns the method signature (the parameter types
     * and the return type).
     * The method signature is represented by a character string
     * called method descriptor, which is defined in the JVM specification.
     *
     * @see javassist.CtBehavior#getSignature()
     * @see javassist.bytecode.Descriptor
     * @since 3.1
     */
    public String getSignature() {
        ConstPool cp = getConstPool();
        int nt = getNameAndType(cp);
        return cp.getUtf8Info(cp.getNameAndTypeDescriptor(nt));
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
     * Returns true if the called method is of a superclass of the current
     * class.
     */
    public boolean isSuper() {
        return iterator.byteAt(currentPos) == INVOKESPECIAL
            && !where().getDeclaringClass().getName().equals(getClassName());
    }

    /*
     * Returns the parameter types of the called method.

    public CtClass[] getParameterTypes() throws NotFoundException {
        return Descriptor.getParameterTypes(getMethodDesc(),
                                            thisClass.getClassPool());
    }
    */

    /*
     * Returns the return type of the called method.

    public CtClass getReturnType() throws NotFoundException {
        return Descriptor.getReturnType(getMethodDesc(),
                                        thisClass.getClassPool());
    }
    */

    /**
     * Replaces the method call with the bytecode derived from
     * the given source text.
     *
     * <p>$0 is available even if the called method is static.
     *
     * @param statement         a Java statement except try-catch.
     */
    public void replace(String statement) throws CannotCompileException {
        thisClass.getClassFile();   // to call checkModify().
        ConstPool constPool = getConstPool();
        int pos = currentPos;
        int index = iterator.u16bitAt(pos + 1);

        String classname, methodname, signature;
        int opcodeSize;
        int c = iterator.byteAt(pos);
        if (c == INVOKEINTERFACE) {
            opcodeSize = 5;
            classname = constPool.getInterfaceMethodrefClassName(index);
            methodname = constPool.getInterfaceMethodrefName(index);
            signature = constPool.getInterfaceMethodrefType(index);
        }
        else if (c == INVOKESTATIC
                 || c == INVOKESPECIAL || c == INVOKEVIRTUAL) {
            opcodeSize = 3;
            classname = constPool.getMethodrefClassName(index);
            methodname = constPool.getMethodrefName(index);
            signature = constPool.getMethodrefType(index);
        }
        else
            throw new CannotCompileException("not method invocation");

        Javac jc = new Javac(thisClass);
        ClassPool cp = thisClass.getClassPool();
        CodeAttribute ca = iterator.get();
        try {
            CtClass[] params = Descriptor.getParameterTypes(signature, cp);
            CtClass retType = Descriptor.getReturnType(signature, cp);
            int paramVar = ca.getMaxLocals();
            jc.recordParams(classname, params,
                            true, paramVar, withinStatic());
            int retVar = jc.recordReturnType(retType, true);
            if (c == INVOKESTATIC)
                jc.recordStaticProceed(classname, methodname);
            else if (c == INVOKESPECIAL)
                jc.recordSpecialProceed(Javac.param0Name, classname,
                                        methodname, signature);
            else
                jc.recordProceed(Javac.param0Name, methodname);

            /* Is $_ included in the source code?
             */
            checkResultValue(retType, statement);

            Bytecode bytecode = jc.getBytecode();
            storeStack(params, c == INVOKESTATIC, paramVar, bytecode);
            jc.recordLocalVariables(ca, pos);

            if (retType != CtClass.voidType) {
                bytecode.addConstZero(retType);
                bytecode.addStore(retVar, retType);     // initialize $_
            }

            jc.compileStmnt(statement);
            if (retType != CtClass.voidType)
                bytecode.addLoad(retVar, retType);

            replace0(pos, bytecode, opcodeSize);
        }
        catch (CompileError e) { throw new CannotCompileException(e); }
        catch (NotFoundException e) { throw new CannotCompileException(e); }
        catch (BadBytecode e) {
            throw new CannotCompileException("broken method");
        }
    }
}
