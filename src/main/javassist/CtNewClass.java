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
package javassist;

import java.io.DataOutputStream;
import java.io.IOException;
import javassist.bytecode.ClassFile;

class CtNewClass extends CtClassType {
    /* true if the class is an interface.
     */
    protected boolean hasConstructor;

    CtNewClass(String name, ClassPool cp,
               boolean isInterface, CtClass superclass) {
        super(name, cp);
        wasChanged = true;
        eraseCache();
        String superName;
        if (superclass == null)
            superName = null;
        else
            superName = superclass.getName();

        classfile = new ClassFile(isInterface, name, superName);

        setModifiers(Modifier.setPublic(getModifiers()));
        hasConstructor = isInterface;
    }

    public void addConstructor(CtConstructor c)
        throws CannotCompileException
    {
        hasConstructor = true;
        super.addConstructor(c);
    }

    void toBytecode(DataOutputStream out)
        throws CannotCompileException, IOException
    {
        if (!hasConstructor)
            try {
                inheritAllConstructors();
            }
            catch (NotFoundException e) {
                throw new CannotCompileException(e);
            }

        super.toBytecode(out);
    }

    /**
     * Adds constructors inhrited from the super class.
     *
     * <p>After this method is called, the class inherits all the
     * constructors from the super class.  The added constructor
     * calls the super's constructor with the same signature.
     */
    public void inheritAllConstructors()
        throws CannotCompileException, NotFoundException
    {
        CtClass superclazz;
        CtConstructor[] cs;

        superclazz = getSuperclass();
        cs = superclazz.getDeclaredConstructors();

        int n = 0;
        for (int i = 0; i < cs.length; ++i) {
            CtConstructor c = cs[i];
            if (Modifier.isPublic(c.getModifiers())) {
                CtConstructor cons
                    = CtNewConstructor.make(c.getParameterTypes(),
                                            c.getExceptionTypes(), this);
                addConstructor(cons);
                ++n;
            }
        }

        if (n < 1)
            throw new CannotCompileException(
                        "no public constructor in " + superclazz.getName());

    }
}
