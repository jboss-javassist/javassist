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

import javassist.bytecode.*;
import javassist.CtClass;
import javassist.CannotCompileException;

final public class TransformNew extends Transformer {
    private int nested;
    private String classname, trapClass, trapMethod;

    public TransformNew(Transformer next,
                 String classname, String trapClass, String trapMethod) {
        super(next);
        this.classname = classname;
        this.trapClass = trapClass;
        this.trapMethod = trapMethod;
    }

    public void initialize(ConstPool cp, CodeAttribute attr) {
        nested = 0;
    }

    /**
     * Replace a sequence of
     *    NEW classname
     *    DUP
     *    ...
     *    INVOKESPECIAL
     * with
     *    NOP
     *    NOP
     *    ...
     *    INVOKESTATIC trapMethod in trapClass
     */
    public int transform(CtClass clazz, int pos, CodeIterator iterator,
                         ConstPool cp) throws CannotCompileException
    {
        int index;
        int c = iterator.byteAt(pos);
        if (c == NEW) {
            index = iterator.u16bitAt(pos + 1);
            if (cp.getClassInfo(index).equals(classname)) {
                if (iterator.byteAt(pos + 3) != DUP)
                    throw new CannotCompileException(
                                "NEW followed by no DUP was found");

                iterator.writeByte(NOP, pos);
                iterator.writeByte(NOP, pos + 1);
                iterator.writeByte(NOP, pos + 2);
                iterator.writeByte(NOP, pos + 3);
                ++nested;
            }
        }
        else if (c == INVOKESPECIAL) {
            index = iterator.u16bitAt(pos + 1);
            int typedesc = cp.isConstructor(classname, index);
            if (typedesc != 0 && nested > 0) {
                int methodref = computeMethodref(typedesc, cp);
                iterator.writeByte(INVOKESTATIC, pos);
                iterator.write16bit(methodref, pos + 1);
                --nested;
            }
        }

        return pos;
    }

    private int computeMethodref(int typedesc, ConstPool cp) {
        int classIndex = cp.addClassInfo(trapClass);
        int mnameIndex = cp.addUtf8Info(trapMethod);
        typedesc = cp.addUtf8Info(
                Descriptor.changeReturnType(classname,
                                            cp.getUtf8Info(typedesc)));
        return cp.addMethodrefInfo(classIndex,
                        cp.addNameAndTypeInfo(mnameIndex, typedesc));
    }
}
