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
import javassist.CtField;
import javassist.Modifier;
import javassist.CannotCompileException;

final public class TransformFieldAccess extends Transformer {
    private String newClassname, newFieldname;
    private String fieldname;
    private CtClass fieldClass;
    private boolean isPrivate;

    /* cache */
    private int newIndex;
    private ConstPool constPool;

    public TransformFieldAccess(Transformer next, CtField field,
                                String newClassname, String newFieldname)
    {
        super(next);
        this.fieldClass = field.getDeclaringClass();
        this.fieldname = field.getName();
        this.isPrivate = Modifier.isPrivate(field.getModifiers());
        this.newClassname = newClassname;
        this.newFieldname = newFieldname;
        this.constPool = null;
    }

    public void initialize(ConstPool cp, CodeAttribute attr) {
        if (constPool != cp)
            newIndex = 0;
    }

    /**
     * Modify GETFIELD, GETSTATIC, PUTFIELD, and PUTSTATIC so that
     * a different field is accessed.  The new field must be declared
     * in a superclass of the class in which the original field is
     * declared.
     */
    public int transform(CtClass clazz, int pos,
                         CodeIterator iterator, ConstPool cp)
    {
        int c = iterator.byteAt(pos);
        if (c == GETFIELD || c == GETSTATIC
                                || c == PUTFIELD || c == PUTSTATIC) {
            int index = iterator.u16bitAt(pos + 1);
            String typedesc
                = TransformReadField.isField(clazz.getClassPool(), cp,
                                fieldClass, fieldname, isPrivate, index);
            if (typedesc != null) {
                if (newIndex == 0) {
                    int nt = cp.addNameAndTypeInfo(newFieldname,
                                                   typedesc);
                    newIndex = cp.addFieldrefInfo(
                                        cp.addClassInfo(newClassname), nt);
                    constPool = cp;
                }

                iterator.write16bit(newIndex, pos + 1);
            }
        }

        return pos;
    }
}
