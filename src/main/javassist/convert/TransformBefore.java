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
package javassist.convert;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.*;
import javassist.CannotCompileException;

public class TransformBefore extends TransformCall {
    protected CtClass[] parameterTypes;
    protected int locals;
    protected int maxLocals;
    protected byte[] saveCode, loadCode;

    public TransformBefore(Transformer next,
                           CtMethod origMethod, CtMethod beforeMethod)
        throws NotFoundException
    {
        super(next, origMethod, beforeMethod);
        parameterTypes = origMethod.getParameterTypes();
        locals = 0;
        maxLocals = 0;
        saveCode = loadCode = null;
    }

    public void initialize(ConstPool cp, CodeAttribute attr) {
        super.initialize(cp, attr);
        locals = 0;
        maxLocals = attr.getMaxLocals();
        saveCode = loadCode = null;
    }

    protected int match(int c, int pos, CodeIterator iterator,
                        int typedesc, ConstPool cp) throws BadBytecode
    {
        if (newIndex == 0) {
            String desc = Descriptor.ofParameters(parameterTypes) + 'V';
            desc = Descriptor.insertParameter(classname, desc);
            int nt = cp.addNameAndTypeInfo(newMethodname, desc);
            int ci = cp.addClassInfo(newClassname);
            newIndex = cp.addMethodrefInfo(ci, nt);
            constPool = cp;
        }

        if (saveCode == null)
            makeCode(parameterTypes, cp);

        return match2(pos, iterator);
    }

    protected int match2(int pos, CodeIterator iterator) throws BadBytecode {
        iterator.move(pos);
        iterator.insert(saveCode);
        iterator.insert(loadCode);
        int p = iterator.insertGap(3);
        iterator.writeByte(INVOKESTATIC, p);
        iterator.write16bit(newIndex, p + 1);
        iterator.insert(loadCode);
        return iterator.next();
    }

    public int extraLocals() { return locals; }

    protected void makeCode(CtClass[] paramTypes, ConstPool cp) {
        Bytecode save = new Bytecode(cp, 0, 0);
        Bytecode load = new Bytecode(cp, 0, 0);

        int var = maxLocals;
        int len = (paramTypes == null) ? 0 : paramTypes.length;
        load.addAload(var);
        makeCode2(save, load, 0, len, paramTypes, var + 1);
        save.addAstore(var);

        saveCode = save.get();
        loadCode = load.get();
    }

    private void makeCode2(Bytecode save, Bytecode load,
                           int i, int n, CtClass[] paramTypes, int var)
    {
        if (i < n) {
            int size = load.addLoad(var, paramTypes[i]);
            makeCode2(save, load, i + 1, n, paramTypes, var + size);
            save.addStore(var, paramTypes[i]);
        }
        else
            locals = var - maxLocals;
    }
}
