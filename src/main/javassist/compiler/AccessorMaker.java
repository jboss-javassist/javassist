/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2004 Shigeru Chiba. All Rights Reserved.
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

    public AccessorMaker(CtClass c) {
        clazz = c;
        uniqueNumber = 1;
        accessors = new HashMap();
    }

    /**
     * Returns the name of the method for accessing a private method.
     *
     * @param name      the name of the private method.
     * @param desc      the descriptor of the private method.
     * @param accDesc   the descriptor of the accessor method.  The first
     *                  parameter type is <code>clazz</code>.
     *                  If the private method is static,
     *              <code>accDesc<code> must be equal to <code>desc</code>. 
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
        do {
            accName = "access$" + uniqueNumber++;
        } while (cf.getMethod(accName) != null);

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
}
