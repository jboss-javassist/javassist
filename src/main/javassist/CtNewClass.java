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
