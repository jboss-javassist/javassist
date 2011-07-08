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

package javassist.convert;

import javassist.bytecode.*;
import javassist.CtClass;
import javassist.CannotCompileException;

final public class TransformNewClass extends Transformer {
    private int nested;
    private String classname, newClassName;
    private int newClassIndex, newMethodNTIndex, newMethodIndex;

    public TransformNewClass(Transformer next,
                             String classname, String newClassName) {
        super(next);
        this.classname = classname;
        this.newClassName = newClassName;
    }

    public void initialize(ConstPool cp, CodeAttribute attr) {
        nested = 0;
        newClassIndex = newMethodNTIndex = newMethodIndex = 0;
    }

    /**
     * Modifies a sequence of
     *    NEW classname
     *    DUP
     *    ...
     *    INVOKESPECIAL classname:method
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

                if (newClassIndex == 0)
                    newClassIndex = cp.addClassInfo(newClassName);

                iterator.write16bit(newClassIndex, pos + 1);
                ++nested;
            }
        }
        else if (c == INVOKESPECIAL) {
            index = iterator.u16bitAt(pos + 1);
            int typedesc = cp.isConstructor(classname, index);
            if (typedesc != 0 && nested > 0) {
                int nt = cp.getMethodrefNameAndType(index);
                if (newMethodNTIndex != nt) {
                    newMethodNTIndex = nt;
                    newMethodIndex = cp.addMethodrefInfo(newClassIndex, nt);
                }

                iterator.write16bit(newMethodIndex, pos + 1);
                --nested;
            }
        }

        return pos;
    }
}
