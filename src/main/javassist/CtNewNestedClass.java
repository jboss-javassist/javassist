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

package javassist;

import javassist.bytecode.ClassFile;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.InnerClassesAttribute;

/**
 * A newly created public nested class.
 */
class CtNewNestedClass extends CtNewClass {
    CtNewNestedClass(String realName, ClassPool cp, boolean isInterface,
                     CtClass superclass) {
        super(realName, cp, isInterface, superclass);
    }

    /**
     * This method does not change the STATIC bit.  The original value is kept.
     */
    public void setModifiers(int mod) {
        mod = mod & ~Modifier.STATIC;
        super.setModifiers(mod);
        updateInnerEntry(mod, getName(), this, true);
    }

    private static void updateInnerEntry(int mod, String name, CtClass clazz, boolean outer) {
        ClassFile cf = clazz.getClassFile2();
        InnerClassesAttribute ica = (InnerClassesAttribute)cf.getAttribute(
                                                InnerClassesAttribute.tag);
        if (ica == null)
            return;

        int n = ica.tableLength();
        for (int i = 0; i < n; i++)
            if (name.equals(ica.innerClass(i))) {
                int acc = ica.accessFlags(i) & AccessFlag.STATIC;
                ica.setAccessFlags(i, mod | acc);
                String outName = ica.outerClass(i);
                if (outName != null && outer)
                    try {
                        CtClass parent = clazz.getClassPool().get(outName);
                        updateInnerEntry(mod, name, parent, false);
                    }
                    catch (NotFoundException e) {
                        throw new RuntimeException("cannot find the declaring class: "
                                                   + outName);
                    }

                break;
            }
    }
}
