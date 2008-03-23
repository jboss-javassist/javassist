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

package javassist.compiler;

import javassist.*;
import javassist.bytecode.*;
import java.util.HashMap;

/**
 * AccessorMaker maintains accessors to private members of an enclosing
 * class.  It is necessary for compiling a method in an inner class.
 */
public class AccessorMaker {
    private CtClass clazz;
    private int uniqueNumber;
    private HashMap accessors;

    static final String lastParamType = "javassist.runtime.Inner";

    public AccessorMaker(CtClass c) {
        clazz = c;
        uniqueNumber = 1;
        accessors = new HashMap();
    }

    public String getConstructor(CtClass c, String desc, MethodInfo orig)
        throws CompileError
    {
        String key = "<init>:" + desc;
        String consDesc = (String)accessors.get(key);
        if (consDesc != null)
            return consDesc;     // already exists.

        consDesc = Descriptor.appendParameter(lastParamType, desc);
        ClassFile cf = clazz.getClassFile();    // turn on the modified flag. 
        try {
            ConstPool cp = cf.getConstPool();
            ClassPool pool = clazz.getClassPool();
            MethodInfo minfo
                = new MethodInfo(cp, MethodInfo.nameInit, consDesc);
            minfo.setAccessFlags(0);
            minfo.addAttribute(new SyntheticAttribute(cp));
            ExceptionsAttribute ea = orig.getExceptionsAttribute();
            if (ea != null)  
                minfo.addAttribute(ea.copy(cp, null));

            CtClass[] params = Descriptor.getParameterTypes(desc, pool);
            Bytecode code = new Bytecode(cp);
            code.addAload(0);
            int regno = 1;
            for (int i = 0; i < params.length; ++i)
                regno += code.addLoad(regno, params[i]);
            code.setMaxLocals(regno + 1);    // the last parameter is added.
            code.addInvokespecial(clazz, MethodInfo.nameInit, desc);

            code.addReturn(null);
            minfo.setCodeAttribute(code.toCodeAttribute());
            cf.addMethod(minfo);
        }
        catch (CannotCompileException e) {
            throw new CompileError(e);
        }
        catch (NotFoundException e) {
            throw new CompileError(e);
        }

        accessors.put(key, consDesc);
        return consDesc;
    }

    /**
     * Returns the name of the method for accessing a private method.
     *
     * @param name      the name of the private method.
     * @param desc      the descriptor of the private method.
     * @param accDesc   the descriptor of the accessor method.  The first
     *                  parameter type is <code>clazz</code>.
     *                  If the private method is static,
     *              <code>accDesc<code> must be identical to <code>desc</code>. 
     *                  
     * @param orig      the method info of the private method.
     * @return
     */
    public String getMethodAccessor(String name, String desc, String accDesc,
                                    MethodInfo orig)
        throws CompileError
    {
        String key = name + ":" + desc;
        String accName = (String)accessors.get(key);
        if (accName != null)
            return accName;     // already exists.

        ClassFile cf = clazz.getClassFile();    // turn on the modified flag. 
        accName = findAccessorName(cf);
        try {
            ConstPool cp = cf.getConstPool();
            ClassPool pool = clazz.getClassPool();
            MethodInfo minfo
                = new MethodInfo(cp, accName, accDesc);
            minfo.setAccessFlags(AccessFlag.STATIC);
            minfo.addAttribute(new SyntheticAttribute(cp));
            ExceptionsAttribute ea = orig.getExceptionsAttribute();
            if (ea != null)  
                minfo.addAttribute(ea.copy(cp, null));

            CtClass[] params = Descriptor.getParameterTypes(accDesc, pool);
            int regno = 0;
            Bytecode code = new Bytecode(cp);
            for (int i = 0; i < params.length; ++i)
                regno += code.addLoad(regno, params[i]);

            code.setMaxLocals(regno);
            if (desc == accDesc)
                code.addInvokestatic(clazz, name, desc);
            else
                code.addInvokevirtual(clazz, name, desc);

            code.addReturn(Descriptor.getReturnType(desc, pool));
            minfo.setCodeAttribute(code.toCodeAttribute());
            cf.addMethod(minfo);
        }
        catch (CannotCompileException e) {
            throw new CompileError(e);
        }
        catch (NotFoundException e) {
            throw new CompileError(e);
        }

        accessors.put(key, accName);
        return accName;
    }

    /**
     * Returns the method_info representing the added getter.
     */
    public MethodInfo getFieldGetter(FieldInfo finfo, boolean is_static)
        throws CompileError
    {
        String fieldName = finfo.getName();
        String key = fieldName + ":getter";
        Object res = accessors.get(key);
        if (res != null)
            return (MethodInfo)res;     // already exists.

        ClassFile cf = clazz.getClassFile();    // turn on the modified flag. 
        String accName = findAccessorName(cf);
        try {
            ConstPool cp = cf.getConstPool();
            ClassPool pool = clazz.getClassPool();
            String fieldType = finfo.getDescriptor();
            String accDesc;
            if (is_static)
                accDesc = "()" + fieldType;
            else
                accDesc = "(" + Descriptor.of(clazz) + ")" + fieldType;

            MethodInfo minfo = new MethodInfo(cp, accName, accDesc);
            minfo.setAccessFlags(AccessFlag.STATIC);
            minfo.addAttribute(new SyntheticAttribute(cp));
            Bytecode code = new Bytecode(cp);
            if (is_static) {
                code.addGetstatic(Bytecode.THIS, fieldName, fieldType);
            }
            else {
                code.addAload(0);
                code.addGetfield(Bytecode.THIS, fieldName, fieldType);
                code.setMaxLocals(1);
            }

            code.addReturn(Descriptor.toCtClass(fieldType, pool));
            minfo.setCodeAttribute(code.toCodeAttribute());
            cf.addMethod(minfo);
            accessors.put(key, minfo);
            return minfo;
        }
        catch (CannotCompileException e) {
            throw new CompileError(e);
        }
        catch (NotFoundException e) {
            throw new CompileError(e);
        }
    }

    /**
     * Returns the method_info representing the added setter.
     */
    public MethodInfo getFieldSetter(FieldInfo finfo, boolean is_static)
        throws CompileError
    {
        String fieldName = finfo.getName();
        String key = fieldName + ":setter";
        Object res = accessors.get(key);
        if (res != null)
            return (MethodInfo)res;     // already exists.

        ClassFile cf = clazz.getClassFile();    // turn on the modified flag. 
        String accName = findAccessorName(cf);
        try {
            ConstPool cp = cf.getConstPool();
            ClassPool pool = clazz.getClassPool();
            String fieldType = finfo.getDescriptor();
            String accDesc;
            if (is_static)
                accDesc = "(" + fieldType + ")V";
            else
                accDesc = "(" + Descriptor.of(clazz) + fieldType + ")V";

            MethodInfo minfo = new MethodInfo(cp, accName, accDesc);
            minfo.setAccessFlags(AccessFlag.STATIC);
            minfo.addAttribute(new SyntheticAttribute(cp));
            Bytecode code = new Bytecode(cp);
            int reg;
            if (is_static) {
                reg = code.addLoad(0, Descriptor.toCtClass(fieldType, pool));
                code.addPutstatic(Bytecode.THIS, fieldName, fieldType);
            }
            else {
                code.addAload(0);
                reg = code.addLoad(1, Descriptor.toCtClass(fieldType, pool))
                      + 1;
                code.addPutfield(Bytecode.THIS, fieldName, fieldType);
            }

            code.addReturn(null);
            code.setMaxLocals(reg);
            minfo.setCodeAttribute(code.toCodeAttribute());
            cf.addMethod(minfo);
            accessors.put(key, minfo);
            return minfo;
        }
        catch (CannotCompileException e) {
            throw new CompileError(e);
        }
        catch (NotFoundException e) {
            throw new CompileError(e);
        }
    }

    private String findAccessorName(ClassFile cf) {
        String accName;
        do {
            accName = "access$" + uniqueNumber++;
        } while (cf.getMethod(accName) != null);
        return accName;
    }
}
